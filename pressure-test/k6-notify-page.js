import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const notifyTrend = new Trend('notify_page_duration');
const successRate = new Rate('notify_page_success_rate');

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '10s',
};

export function setup() {
  const userBaseUrl = __ENV.USER_BASE_URL || 'http://host.docker.internal:9011';
  const notifyBaseUrl = __ENV.NOTIFY_BASE_URL || 'http://host.docker.internal:9004';
  const payload = JSON.stringify({ username: 'tomuser', password: 'password' });
  const loginRes = http.post(`${userBaseUrl}/user/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const body = JSON.parse(loginRes.body || '{}');
  if (loginRes.status !== 200 || body.code !== 200 || !body.data?.token) {
    throw new Error('notify page login failed');
  }
  return { notifyBaseUrl, token: body.data.token };
}

export default function (data) {
  const pageNum = (__ITER % 100) + 1;
  const res = http.post(`${data.notifyBaseUrl}/notify/page`, JSON.stringify({
    pageNum,
    pageSize: 20,
  }), {
    headers: {
      Authorization: `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  });
  let ok = false;
  try {
    const body = JSON.parse(res.body || '{}');
    ok = res.status === 200 && body.code === 200 && body.data?.list?.length >= 0;
  } catch (e) {
    ok = false;
  }
  notifyTrend.add(res.timings.duration);
  successRate.add(ok);
  check(res, { 'notify page ok': () => ok });
  sleep(0.1);
}
