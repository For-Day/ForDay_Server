import http from 'k6/http';
import { Counter } from 'k6/metrics';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = parseInt(__ENV.USER_COUNT || '200', 10);
const RECORD_COUNT = parseInt(__ENV.RECORD_COUNT || '1000', 10);
const REACTION_TYPES = ['AWESOME', 'GREAT', 'AMAZING', 'FIGHTING'];

/**
 * pickTarget이 (vu, iter)만으로 결정되면 같은 스크립트를 다시 실행할 때마다 정확히 같은
 * (userId, recordId, type) 시퀀스가 재생된다 - 1회차에서 이미 DB에 들어간 조합을 2·3회차가
 * 그대로 반복 요청하게 돼, "3회 반복 후 중위값"이 실제로는 "1회차만 유효, 2·3회차는 거의
 * 전부 DUPLICATE_REACTION"이 되어버린다(실측 중 발견). RUN_SEED로 실행마다 매핑을 밀어서
 * 매 회차가 새로운 조합을 쓰게 한다 - 같은 실행 안에서는 각 VU·이터레이션 조합이 여전히
 * 결정론적이라 충돌 회피 성질은 유지된다.
 */
const RUN_SEED = parseInt(__ENV.RUN_SEED || String(Date.now() % 1000000), 10);

/**
 * ReactionMeasurementSeeder가 만든 게스트 유저(measure_user_1..N)로 각각 로그인해
 * USER_COUNT개의 서로 다른 토큰을 발급받는다.
 *
 * 기존 스크립트는 setup()에서 토큰을 1개만 발급해 1000 VU 전부가 같은 유저로 요청했다
 * (#375) - recordId(1~100) x type(4) = 400개 조합뿐이라 대부분 DUPLICATE_REACTION으로
 * 거절되는 경로를 측정한 셈이었다. 이 함수가 그 버그를 고친다.
 */
export function setupGuestTokens() {
  const tokens = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    const res = http.post(
        `${BASE_URL}/auth/guest`,
        JSON.stringify({ guestUserId: `measure_user_${i}` }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) {
      throw new Error(`게스트 로그인 실패 (measure_user_${i}): ${res.status} ${res.body}`);
    }
    tokens.push(JSON.parse(res.body).data.accessToken);
  }
  return tokens;
}

/**
 * (userId, recordId, type) 조합을 VU·이터레이션 번호로 결정론적으로 골라, 순수 랜덤보다
 * 충돌(같은 유저가 같은 기록에 같은 타입으로 또 반응)을 줄인다. 완벽한 충돌 방지는 아니지만
 * 유저 1명 x 조합 400개로 수렴하던 기존 문제는 없앤다 - userCount x recordCount x 4타입 =
 * 최대 조합 수가 요청 수보다 훨씬 크다.
 */
export function pickTarget(vu, iter) {
  const userIndex = (vu - 1) % USER_COUNT;
  const recordId = 1 + ((vu - 1) * 37 + iter + RUN_SEED) % RECORD_COUNT;
  const reactionType = REACTION_TYPES[(vu + iter + RUN_SEED) % REACTION_TYPES.length];
  return { userIndex, recordId, reactionType };
}

/** 상태코드별 Counter - "성공만의 처리량"을 http_reqs 전체와 분리해서 보기 위함 (#375). */
export function makeStatusCounters(prefix) {
  return {
    success: new Counter(`${prefix}_success`),
    duplicate: new Counter(`${prefix}_duplicate`),
    rateLimited: new Counter(`${prefix}_rate_limited`),
    serverError: new Counter(`${prefix}_server_error`),
    otherError: new Counter(`${prefix}_other_error`),
  };
}

export function tagStatus(counters, status) {
  if (status === 200 || status === 201) counters.success.add(1);
  else if (status === 400) counters.duplicate.add(1);
  else if (status === 429) counters.rateLimited.add(1);
  else if (status >= 500) counters.serverError.add(1);
  else counters.otherError.add(1);
}
