import http from 'k6/http';
import { check } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  vus: 1000,
  duration: '10s',
};

const reactionTypes = ['AWESOME', 'GREAT', 'AMAZING', 'FIGHTING'];

// 게스트 로그인해서 토큰 받아오기a
export function setup() {
  const loginRes = http.post(
      'http://localhost:8080/auth/guest',
      JSON.stringify({ guestUserId: "" }),  // JSON.stringify 필수!
      {
        headers: { 'Content-Type': 'application/json' },
      }
    );

  console.log(`로그인 응답: ${loginRes.body}`); // 응답 전체 출력

  const body = JSON.parse(loginRes.body);
  const token = body.data.accessToken;
  console.log(`토큰 발급 완료: ${token}`);
  return { token };
}

export default function (data) {
  const recordId = randomIntBetween(1, 100);
  const reactionType = reactionTypes[randomIntBetween(0, 3)];

  const url = `http://localhost:8080/api/v2/records/${recordId}/reaction`;

  const payload = JSON.stringify({ reactionType: reactionType });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,  // 토큰 자동 주입
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'is status 201': (r) => r.status === 201,
  });
}