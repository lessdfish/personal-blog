import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const users = JSON.parse(open('./comment-users.json'));
const commentTrend = new Trend('comment_create_duration');
const commentSuccessRate = new Rate('comment_create_success_rate');
const notifyUnreadTrend = new Trend('notify_unread_duration');
const notifyUnreadSuccessRate = new Rate('notify_unread_success_rate');

export const options = {
  vus: Number(__ENV.VUS || 3),
  duration: __ENV.DURATION || '10s',
};

function parseBody(res) {
  try {
    return JSON.parse(res.body || '{}');
  } catch (e) {
    return {};
  }
}

function pickUser() {
  return users[(__VU - 1) % users.length];
}

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
  const articleId = Number(__ENV.ARTICLE_ID || 1);
  const user = pickUser();
  const headers = {
    Authorization: `Bearer ${user.token}`,
    'Content-Type': 'application/json',
  };
  const suffix = `${Date.now()}-${__VU}-${__ITER}`;
  const payload = JSON.stringify({
    articleId,
    content: `k6-comment-${user.name}-${suffix}`,
  });

  const createRes = http.post(`${baseUrl}/api/comment`, payload, { headers });
  commentTrend.add(createRes.timings.duration);
  const createBody = parseBody(createRes);
  const createOk = createRes.status === 200 && createBody.code === 200;
  commentSuccessRate.add(createOk);
  check(createRes, { 'comment create ok': () => createOk });

  const notifyRes = http.get(`${baseUrl}/api/notify/unread/count`, { headers });
  notifyUnreadTrend.add(notifyRes.timings.duration);
  const notifyBody = parseBody(notifyRes);
  const notifyOk = notifyRes.status === 200 && notifyBody.code === 200;
  notifyUnreadSuccessRate.add(notifyOk);
  check(notifyRes, { 'notify unread count ok': () => notifyOk });

  sleep(Number(__ENV.SLEEP || 1.5));
}
