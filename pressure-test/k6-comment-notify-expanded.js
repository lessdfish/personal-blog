import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const commentTrend = new Trend('comment_chain_create_duration');
const unreadTrend = new Trend('comment_chain_unread_duration');
const commentSuccessRate = new Rate('comment_chain_create_success_rate');
const unreadSuccessRate = new Rate('comment_chain_unread_success_rate');

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '10s',
};

function login(baseUrl, username, password) {
  const payload = JSON.stringify({ username, password });
  const res = http.post(`${baseUrl}/user/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const body = JSON.parse(res.body || '{}');
  if (res.status !== 200 || body.code !== 200 || !body.data?.token) {
    throw new Error(`login failed for ${username}`);
  }
  return body.data.token;
}

export function setup() {
  const userBaseUrl = __ENV.USER_BASE_URL || 'http://host.docker.internal:9011';
  const commentBaseUrl = __ENV.COMMENT_BASE_URL || 'http://host.docker.internal:9003';
  const notifyBaseUrl = __ENV.NOTIFY_BASE_URL || 'http://host.docker.internal:9004';
  const password = __ENV.PASSWORD || 'password';
  const userCount = Number(__ENV.USER_COUNT || 60);
  const users = [];
  for (let i = 1; i <= userCount; i += 1) {
    const username = `perfuser${String(i).padStart(3, '0')}`;
    users.push({ username, token: login(userBaseUrl, username, password) });
  }
  return {
    userBaseUrl,
    commentBaseUrl,
    notifyBaseUrl,
    users,
    articleId: Number(__ENV.ARTICLE_ID || 1),
  };
}

function parseBody(res) {
  try {
    return JSON.parse(res.body || '{}');
  } catch (e) {
    return {};
  }
}

export default function (data) {
  const current = data.users[(__VU - 1) % data.users.length];
  const headers = {
    Authorization: `Bearer ${current.token}`,
    'Content-Type': 'application/json',
  };
  const payload = JSON.stringify({
    articleId: data.articleId,
    content: `expanded-comment-${current.username}-${__VU}-${__ITER}-${Date.now()}`,
  });

  const createRes = http.post(`${data.commentBaseUrl}/comment`, payload, { headers });
  const createBody = parseBody(createRes);
  const createOk = createRes.status === 200 && createBody.code === 200;
  commentTrend.add(createRes.timings.duration);
  commentSuccessRate.add(createOk);
  check(createRes, { 'comment chain create ok': () => createOk });

  const unreadRes = http.get(`${data.notifyBaseUrl}/notify/unread/count`, { headers });
  const unreadBody = parseBody(unreadRes);
  const unreadOk = unreadRes.status === 200 && unreadBody.code === 200;
  unreadTrend.add(unreadRes.timings.duration);
  unreadSuccessRate.add(unreadOk);
  check(unreadRes, { 'comment chain unread ok': () => unreadOk });

  sleep(Number(__ENV.SLEEP || 0.2));
}
