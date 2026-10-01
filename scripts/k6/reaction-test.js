import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * #375 재측정 ①·② 단계 - v1(동기 DB) 경로.
 *
 * 인덱스 유무는 코드가 아니라 측정 당일 DDL로 전환한다(같은 서버에서 인덱스 있음/없음을
 * 동시에 살려둘 수 없음 - docs/perf/reaction-load-test.md 참고). 같은 스크립트를
 * uk_record_user_type 제약을 DROP한 상태(①)와 재생성한 상태(②)로 두 번 실행해서 각각의
 * 수치를 얻는다.
 *
 * 실행: K6_WEB_DASHBOARD=true k6 run --summary-export=stage1-summary.json reaction-test.js
 *
 * vus/duration(순간 폭증) 대신 constant-arrival-rate를 쓴다 - 서버가 빨라질수록(2·3단계)
 * 순간 폭증 방식은 k6-서버 간 연결 자체가 못 버텨 서버 처리와 무관한 에러가 쌓였다(실측
 * 중 발견). 도착률을 고정해야 단계 간 비교가 "서버가 얼마나 처리하는가"로 성립한다.
 */
export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: 250,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
};

const counters = makeStatusCounters('v1');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/records/${recordId}/reaction`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
