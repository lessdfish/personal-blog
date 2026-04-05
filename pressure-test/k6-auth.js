import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const meTrend = new Trend('auth_me_duration');
const contextTrend = new Trend('auth_context_duration');
const activityTrend = new Trend('auth_activity_duration');
const successRate = new Rate('auth_success_rate');

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '5s',
};

function assertSuccess(res) {
  let ok = false;
  try {
    const body = JSON.parse(res.body);
    ok = res.status === 200 && body.code === 200;
  } catch (e) {
    ok = false;
  }
  successRate.add(ok);
  check(res, { 'auth status is 200': () => ok });
}

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
  const token = __ENV.ADMIN_TOKEN;
  const headers = {
    Authorization: `Bearer ${token}`,
  };
  const mode = __ITER % 3;

  if (mode === 0) {
    const res = http.get(`${baseUrl}/api/user/me`, { headers });
    meTrend.add(res.timings.duration);
    assertSuccess(res);
  } else if (mode === 1) {
    const res = http.get(`${baseUrl}/api/user/context`, { headers });
    contextTrend.add(res.timings.duration);
    assertSuccess(res);
  } else {
    const res = http.get(`${baseUrl}/api/user/activity/summary`, { headers });
    activityTrend.add(res.timings.duration);
    assertSuccess(res);
  }

  sleep(0.2);
}
