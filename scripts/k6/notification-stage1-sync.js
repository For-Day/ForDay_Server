import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * 알림 파이프라인 1·2단계 - 동기 발송 / @Async 비동기 분리.
 *
 * 두 단계가 같은 스크립트를 쓴다. 엔드포인트는 그대로 두고 서버 설정
 * ({@code measure.push.mode=sync|async})으로 전환하기 때문이다 - 클라이언트가 보내는 부하가
 * 완전히 같아야 응답 시간 차이를 "발송이 요청 스레드 안에 있었는가"로 돌릴 수 있다.
 *
 * FCM 호출은 MeasuringPushSenderAdapter가 latency-ms만큼 sleep으로 대체한다.
 *
 * 부하 프로파일은 4단계 전부 동일해야 비교가 성립한다. 바꾸려면 모든 스크립트를 같이 바꿀 것.
 * 목표는 피드백 문서 기준인 "초당 200~300건".
 *
 * 실행:
 *   k6 run --summary-export=docs/perf/results/stage1-sync-summary.json \
 *     scripts/k6/notification-stage1-sync.js
 */
export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      // 초당 250건 - 도착률을 고정해야 "서버가 못 받아내는 상황"이 드러난다.
      // VU 고정 방식은 서버가 느려지면 요청도 같이 느려져서 고갈이 가려진다.
      rate: 250,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
};

const counters = makeStatusCounters('notif');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/records/${recordId}/reaction/test`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
