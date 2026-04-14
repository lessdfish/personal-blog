const API_BASE = "";
const LOGIN_USER_KEY = "blogCloudLoginUser";

function getLoginUser() {
  const raw = localStorage.getItem(LOGIN_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function setLoginUser(user) {
  if (user) {
    localStorage.setItem(LOGIN_USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(LOGIN_USER_KEY);
  }
  syncLoginState();
}

function syncLoginState() {
  const user = getLoginUser();
  document.querySelectorAll("[data-login-status]").forEach((el) => {
    el.textContent = user ? "登录成功" : "未登录";
  });
  document.querySelectorAll("[data-login-user]").forEach((el) => {
    el.hidden = !user;
  });
  document.querySelectorAll("[data-login-username]").forEach((el) => {
    el.textContent = user?.username || "未命名用户";
    el.onclick = () => {
      const nicknameEl = el.parentElement?.querySelector("[data-login-nickname]");
      if (nicknameEl) {
        nicknameEl.hidden = !nicknameEl.hidden;
      }
    };
  });
  document.querySelectorAll("[data-login-nickname]").forEach((el) => {
    el.textContent = user?.nickname ? `昵称：${user.nickname}` : "未设置昵称";
    el.hidden = true;
  });
  document.querySelectorAll("[data-login-avatar]").forEach((el) => {
    if (user?.avatar) {
      el.src = user.avatar;
      el.hidden = false;
    } else {
      el.removeAttribute("src");
      el.hidden = true;
    }
  });
}

function handleLoginSuccess(result) {
  const user = result?.data?.data?.user;
  if (user) {
    setLoginUser(user);
  }
}

function readInput(id, fallback = "") {
  const el = document.getElementById(id);
  return el ? el.value.trim() : fallback;
}

async function callApi({ method, url, body, token }) {
  const headers = {};
  if (token) {
    headers.Authorization = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(API_BASE + url, {
    method,
    headers,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = { raw: text };
  }
  return { status: response.status, data };
}

function renderResult(targetId, payload) {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }
  target.textContent = JSON.stringify(payload, null, 2);
}

async function runAction(targetId, requestFactory, afterSuccess) {
  try {
    const result = await callApi(requestFactory());
    renderResult(targetId, result);
    if (afterSuccess) {
      afterSuccess(result);
    }
  } catch (error) {
    renderResult(targetId, { error: String(error) });
  }
}

document.addEventListener("DOMContentLoaded", syncLoginState);
