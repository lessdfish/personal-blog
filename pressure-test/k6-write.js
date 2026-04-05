import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const likeTrend = new Trend('write_like_duration');
const favoriteTrend = new Trend('write_favorite_duration');
const successRate = new Rate('write_success_rate');

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
  check(res, { 'write status is 200': () => ok });
}

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
  const token = __ENV.WRITE_TOKEN;
  const headers = {
    Authorization: `Bearer ${token}`,
  };
  const mode = __ITER % 4;

  if (mode === 0) {
    const res = http.put(`${baseUrl}/api/article/like/5`, null, { headers });
    likeTrend.add(res.timings.duration);
    assertSuccess(res);
  } else if (mode === 1) {
    const res = http.del(`${baseUrl}/api/article/like/5`, null, { headers });
    likeTrend.add(res.timings.duration);
    assertSuccess(res);
  } else if (mode === 2) {
    const res = http.put(`${baseUrl}/api/article/favorite/5`, null, { headers });
    favoriteTrend.add(res.timings.duration);
    assertSuccess(res);
  } else {
    const res = http.del(`${baseUrl}/api/article/favorite/5`, null, { headers });
    favoriteTrend.add(res.timings.duration);
    assertSuccess(res);
  }

  sleep(0.2);
}
