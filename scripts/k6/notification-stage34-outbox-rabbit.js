import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * 알림 파이프라인 3·4단계 - 아웃박스 / RabbitMQ.
 *
 * 운영 경로({@code POST /records/{id}/reaction})를 그대로 때린다. 이 경로는 요청 트랜잭션
 * 안에서 outbox 행만 저장하고 끝나며, 실제 발송은 NotificationOutboxRelay가 별도로 한다.
 * 3단계와 4단계의 구분은 스크립트가 아니라 서버 설정(발행 대상: 직접 FCM / RabbitMQ)으로
 * 바꾼다 - 클라이언트가 때리는 엔드포인트는 같아야 비교가 성립하기 때문이다.
 *
 * 부하 프로파일은 stage1과 동일하게 고정한다.
 *
 * 실행:
 *   K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage3-outbox.json \
 *     scripts/k6/notification-stage34-outbox-rabbit.js
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

const counters = makeStatusCounters('stage34');

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
