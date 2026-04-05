import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const normalTrend = new Trend('article_normal_page_duration');
const successRate = new Rate('article_normal_page_success_rate');

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '10s',
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9020';
  const pageNum = (__ITER % 20) + 1;
  const res = http.get(`${baseUrl}/article/page/normal?pageNum=${pageNum}&pageSize=20&boardId=1`);
  let ok = false;
  try {
    const body = JSON.parse(res.body || '{}');
    ok = res.status === 200 && body.code === 200 && body.data?.list?.length >= 0;
  } catch (e) {
    ok = false;
  }
  normalTrend.add(res.timings.duration);
  successRate.add(ok);
  check(res, { 'article normal page ok': () => ok });
  sleep(0.1);
}
