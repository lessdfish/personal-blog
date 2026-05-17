import http from 'k6/http';
import { check, sleep } from 'k6';

const gatewayBaseUrl = __ENV.BASE_URL || 'http://localhost:18080';
const webBaseUrl = __ENV.WEB_URL || 'http://localhost:18081';

export const options = {
  vus: 2,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const web = http.get(`${webBaseUrl}/`);
  check(web, {
    'web homepage is not 5xx': (r) => r.status < 500,
  });

  const articlePage = http.get(`${gatewayBaseUrl}/api/article/page/normal?pageNum=1&pageSize=10`);
  check(articlePage, {
    'article page is 200': (r) => r.status === 200,
  });

  const hotPage = http.get(`${gatewayBaseUrl}/api/article/page/hot?pageNum=1&pageSize=10`);
  check(hotPage, {
    'hot page is not 5xx': (r) => r.status < 500,
  });

  const login = http.post(
    `${gatewayBaseUrl}/api/user/login`,
    JSON.stringify({
      username: __ENV.LOGIN_USERNAME || 'loadtest-invalid-user',
      password: __ENV.LOGIN_PASSWORD || 'loadtest-invalid-password',
    }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(login, {
    'login path is protected without 5xx': (r) => r.status >= 200 && r.status < 500,
  });

  sleep(1);
}
