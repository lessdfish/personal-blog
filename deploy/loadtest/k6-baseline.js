import http from 'k6/http';
import { check, sleep } from 'k6';

const gatewayBaseUrl = __ENV.BASE_URL || 'http://localhost:18080';
const webBaseUrl = __ENV.WEB_URL || 'http://localhost:18081';

export const options = {
  scenarios: {
    public_read_baseline: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m', target: 10 },
        { duration: '3m', target: 30 },
        { duration: '1m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const responses = http.batch([
    ['GET', `${webBaseUrl}/`],
    ['GET', `${gatewayBaseUrl}/api/article/page/normal?pageNum=1&pageSize=10`],
    ['GET', `${gatewayBaseUrl}/api/article/page/hot?pageNum=1&pageSize=10`],
    ['GET', `${gatewayBaseUrl}/api/article/board/list`],
  ]);

  check(responses[0], { 'web homepage is not 5xx': (r) => r.status < 500 });
  check(responses[1], { 'normal article page is 200': (r) => r.status === 200 });
  check(responses[2], { 'hot article page is not 5xx': (r) => r.status < 500 });
  check(responses[3], { 'board list is not 5xx': (r) => r.status < 500 });

  sleep(1);
}
