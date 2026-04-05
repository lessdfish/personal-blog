import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const pageTrend = new Trend('read_page_duration');
const detailTrend = new Trend('read_detail_duration');
const hotTrend = new Trend('read_hot_duration');
const successRate = new Rate('read_success_rate');

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
  check(res, { 'read status is 200': () => ok });
}

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
  const mode = __ITER % 3;

  if (mode === 0) {
    const res = http.get(`${baseUrl}/api/article/page?pageNum=1&pageSize=10`);
    pageTrend.add(res.timings.duration);
    assertSuccess(res);
  } else if (mode === 1) {
    const res = http.get(`${baseUrl}/api/article/detail/1`);
    detailTrend.add(res.timings.duration);
    assertSuccess(res);
  } else {
    const res = http.get(`${baseUrl}/api/article/hot?limit=10`);
    hotTrend.add(res.timings.duration);
    assertSuccess(res);
  }

  sleep(0.2);
}
