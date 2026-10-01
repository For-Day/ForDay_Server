import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * 멘토 피드백 6-3 검증 - 현재 프로덕션 v1 경로(upsertIncreaseCount, 상대 증가식
 * ON DUPLICATE KEY UPDATE)가 "요청 하나를 버리는 동시성 처리"인지 실측으로 확인한다.
 * pickTarget으로 부하를 여러 기록에 분산시키지 않고, 서로 다른 유저 전원이
 * 정확히 같은 기록·같은 반응 타입 하나에 동시에 반응한다 - 상대 증가식이 행 잠금으로
 * 순서대로 다 반영되는지, 아니면 일부가 유실되는지가 여기서만 드러난다.
 *
 * 실행: TARGET_RECORD_ID=<id> k6 run --summary-export=stage-same-record-summary.json reaction-test-same-record.js
 */
const TARGET_RECORD_ID = __ENV.TARGET_RECORD_ID;
const USER_COUNT = parseInt(__ENV.USER_COUNT || '200', 10);
const REACTION_TYPE = __ENV.REACTION_TYPE || 'AWESOME';

if (!TARGET_RECORD_ID) {
  throw new Error('TARGET_RECORD_ID env var가 필요합니다.');
}

export const options = {
  scenarios: {
    all_at_once: {
      executor: 'per-vu-iterations',
      vus: USER_COUNT,
      iterations: 1,
      maxDuration: '30s',
    },
  },
};

const counters = makeStatusCounters('samerec');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % USER_COUNT];

  const res = http.post(
      `${BASE_URL}/records/${TARGET_RECORD_ID}/reaction`,
      JSON.stringify({ reactionType: REACTION_TYPE }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
