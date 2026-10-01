import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

/**
 * "기록 생성 요청 안에서 서버가 직접 리사이즈했다면" 가상 시나리오 측정.
 *
 * 실제 클라이언트 흐름을 그대로 재현한다: presign 발급 -> 원본 5장을 S3에 직접 PUT(클라이언트가
 * 하는 것과 동일, 앱 서버는 관여 안 함) -> measure 전용 엔드포인트(/measure/image/resize-sync)
 * 호출로 "서버가 동기로 다운로드->리사이즈->업로드"를 수행하는 구간만 측정한다.
 *
 * usage는 반드시 TEST_ACTIVITY_RECORD - 실제 activity_record/temp/에 올리면 프로덕션
 * Lambda가 같은 객체를 보고 실제로 또 리사이즈해 측정 부하가 실 지표를 오염시킨다.
 *
 * 실행: RATE=10 DURATION=30s BASE_URL=http://<target>:8080 k6 run \
 *   --summary-export=image-resize-sync-summary.json image-resize-sync.js
 */
const IMAGE_COUNT = 5;
const files = [];
for (let i = 1; i <= IMAGE_COUNT; i++) {
  files.push(open(`./fixtures/sample${i}.jpg`, 'b'));
}

const RATE = parseInt(__ENV.RATE || '10', 10);
const DURATION = __ENV.DURATION || '30s';

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
};

const counters = makeStatusCounters('resize');

export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const authHeader = { Authorization: `Bearer ${token}` };

  // 1. presign 발급 (기록 생성 전 클라이언트가 하는 것과 동일)
  const presignBody = JSON.stringify({
    images: Array.from({ length: IMAGE_COUNT }, (_, i) => ({
      originalFilename: `sample${i + 1}.jpg`,
      contentType: 'image/jpeg',
      usage: 'TEST_ACTIVITY_RECORD',
      order: i + 1,
    })),
  });
  const presignRes = http.post(`${BASE_URL}/app/presign`, presignBody, {
    headers: { ...authHeader, 'Content-Type': 'application/json' },
  });
  if (presignRes.status !== 200) {
    tagStatus(counters, presignRes.status);
    check(presignRes, { 'presign 2xx': (r) => r.status >= 200 && r.status < 300 });
    return;
  }
  const targets = presignRes.json().data;

  // 2. 원본을 S3에 직접 업로드 (클라이언트 동작 그대로 - 앱 서버는 관여하지 않는다)
  const keys = targets.map((t, idx) => {
    http.put(t.uploadUrl, files[idx], { headers: { 'Content-Type': 'image/jpeg' } });
    return t.fileUrl.split('.amazonaws.com/')[1];
  });

  // 3. 측정 대상: 서버가 동기로 다운로드->리사이즈->업로드
  const resizeRes = http.post(
      `${BASE_URL}/measure/image/resize-sync`,
      JSON.stringify({ imageKeys: keys }),
      { headers: { ...authHeader, 'Content-Type': 'application/json' } }
  );

  tagStatus(counters, resizeRes.status);
  check(resizeRes, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
