import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * #375 재측정 ③ 단계 - Redis Write-Back 큐만 적용, 분산 락은 아직 없음.
 * 중복확인은 v1과 동일하게 DB existsBy 조회, insert/count 반영만 Redis 큐를 거쳐
 * 비동기로 이뤄진다. measure 프로파일에서만 열리는
 * QueueOnlyReactionMeasurementService/TestReactionMeasurementController 대상.
 *
 * 실행: K6_WEB_DASHBOARD=true k6 run --summary-export=stage3-summary.json reaction-test-queue-only.js
 */
export const options = {
  vus: 1000,
  duration: '10s',
};

const counters = makeStatusCounters('queue_only');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/records/${recordId}/reaction/measure/queue-only`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
