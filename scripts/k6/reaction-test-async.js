import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * #375 4단계 재측정(멘토 피드백 반영판)의 2단계 - Spring @Async만 적용, Redis 큐는 아직 없음.
 * measure 프로파일에서만 열리는 ReactionAsyncMeasurementService/TestReactionMeasurementController
 * 대상 (POST /records/{recordId}/reaction/measure/async).
 *
 * 실행: K6_WEB_DASHBOARD=true k6 run --summary-export=stage2-async-summary.json reaction-test-async.js
 *
 * vus/duration(순간 폭증) 대신 constant-arrival-rate를 쓴다 - reaction-test.js 참고.
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

const counters = makeStatusCounters('async');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/records/${recordId}/reaction/measure/async`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
