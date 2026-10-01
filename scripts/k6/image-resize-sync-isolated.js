import http from 'k6/http';
import { check } from 'k6';
import { makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * "기록 생성 요청 안에서 서버가 직접 리사이즈했다면" 측정 - 순수 격리판.
 *
 * image-resize-sync.js는 매 반복마다 원본 5장(32MB)을 새로 S3에 올렸는데, 그 업로드
 * 자체가 발생기 EC2(t3.small)의 네트워크를 다 써버려서 SSH까지 끊기는 걸 실측으로
 * 확인했다 - 측정하려는 변수(서버의 리사이즈 비용)와 무관한 발생기 네트워크가 먼저
 * 병목이 되어 버렸다.
 *
 * 그래서 원본 업로드는 딱 한 번(setup 스크립트, scripts/k6/prepare-resize-fixtures.sh)만
 * 미리 해두고, 이 스크립트는 이미 S3에 있는 고정된 키 5개에 대해
 * /measure/image/resize-sync만 반복 호출한다 - 다운로드->리사이즈->업로드라는
 * "서버가 하는 일"만 순수하게 반복 측정한다. 같은 원본 키를 여러 요청이 동시에 읽어도
 * S3 GetObject는 읽기 전용이라 충돌이 없고, 리사이즈 결과를 같은 출력 키에 덮어써도
 * S3는 동시 PUT을 그냥 마지막 쓰기가 이기는 방식으로 처리해 에러가 나지 않는다.
 *
 * 실행: RATE=2 DURATION=30s BASE_URL=http://<target>:8080 TOKEN=<Bearer> k6 run \
 *   --summary-export=image-resize-isolated-summary.json image-resize-sync-isolated.js
 */
const KEYS = [
  __ENV.KEY1,
  __ENV.KEY2,
  __ENV.KEY3,
  __ENV.KEY4,
  __ENV.KEY5,
];

const RATE = parseInt(__ENV.RATE || '2', 10);
const DURATION = __ENV.DURATION || '30s';
const TOKEN = __ENV.TOKEN;

if (!TOKEN || KEYS.some((k) => !k)) {
  throw new Error('TOKEN과 KEY1..KEY5 env var가 모두 필요합니다.');
}

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
};

const counters = makeStatusCounters('resize');

export default function () {
  const resizeRes = http.post(
      `${BASE_URL}/measure/image/resize-sync`,
      JSON.stringify({ imageKeys: KEYS }),
      { headers: { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' } }
  );

  tagStatus(counters, resizeRes.status);
  check(resizeRes, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
