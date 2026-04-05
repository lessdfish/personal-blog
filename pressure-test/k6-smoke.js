import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
  const res = http.get(`${baseUrl}/api/article/page?pageNum=1&pageSize=5`);
  console.log(`status=${res.status}`);
}
