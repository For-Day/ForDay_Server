import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * #375 재측정 ④ 단계 - Redis Write-Back 큐 + 분산 락(SETNX) 적용, v2 경로.
 * 중복확인·큐 push 모두 ReactionRedisLockService를 거친다(운영과 동일 경로).
 *
 * 실행: K6_WEB_DASHBOARD=true k6 run --summary-export=stage4-summary.json reaction-test-redis.js
 */
export const options = {
  vus: 1000,
  duration: '10s',
};

const counters = makeStatusCounters('redis_lock');

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
