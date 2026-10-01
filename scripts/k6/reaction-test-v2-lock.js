import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * 3단계 - 실제 프로덕션 v2 경로. Redis SETNX 분산락으로 DB 중복확인(existsBy) 쿼리 자체를
 * 없애고, 큐+스케줄러(진짜 멀티로우 INSERT)로 반영한다. `ReactionRedisLockService` 참고.
 *
 * queue-only(락 없음) 단계와 구분되는 지점: 락이 "중복 방지"가 목적이 아니라, DB 조회를
 * 아예 안 거치게 하는 게 핵심이다 - 요청마다 DB를 왕복하던 existsBy 쿼리를 Redis SETNX
 * 하나로 대체한다.
 *
 * 실행: k6 run --summary-export=stage3-v2lock-summary.json reaction-test-v2-lock.js
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

const counters = makeStatusCounters('v2lock');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/api/v2/records/${recordId}/reaction`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
