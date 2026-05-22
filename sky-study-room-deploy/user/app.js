const API_BASE = "/api";
const tokenKey = "sky_user_token";
const profileKey = "sky_user_profile";

const state = {
  token: localStorage.getItem(tokenKey),
  profile: JSON.parse(localStorage.getItem(profileKey) || "null"),
  filter: {
    reserveDate: today(),
    startTime: "09:00",
    endTime: "10:00",
    resourceType: ""
  },
  categoryLoadId: 0,
  resourceLoadId: 0,
  reservationLoadId: 0,
  notificationLoadId: 0
};

const els = {
  nav: document.getElementById("nav"),
  userBox: document.getElementById("userBox"),
  currentUser: document.getElementById("currentUser"),
  loginView: document.getElementById("loginView"),
  bookingView: document.getElementById("bookingView"),
  reservationsView: document.getElementById("reservationsView"),
  notificationsView: document.getElementById("notificationsView"),
  loginForm: document.getElementById("loginForm"),
  loginMessage: document.getElementById("loginMessage"),
  resourceFilterForm: document.getElementById("resourceFilterForm"),
  categoryMetrics: document.getElementById("categoryMetrics"),
  resourceGrid: document.getElementById("resourceGrid"),
  reservationRows: document.getElementById("reservationRows"),
  notificationList: document.getElementById("notificationList"),
  bookingDialog: document.getElementById("bookingDialog"),
  bookingForm: document.getElementById("bookingForm"),
  bookingMessage: document.getElementById("bookingMessage"),
  toast: document.getElementById("toast")
};

document.addEventListener("DOMContentLoaded", () => {
  els.resourceFilterForm.reserveDate.value = state.filter.reserveDate;
  bindEvents();
  renderAuth();
});

function bindEvents() {
  els.loginForm.addEventListener("submit", login);
  document.getElementById("logoutBtn").addEventListener("click", logout);
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
  els.resourceFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(els.resourceFilterForm);
    state.filter = {
      resourceType: data.get("resourceType"),
      reserveDate: data.get("reserveDate"),
      startTime: data.get("startTime"),
      endTime: data.get("endTime")
    };
    loadResources();
  });
  document.getElementById("refreshResourcesBtn").addEventListener("click", () => {
    loadCategories();
    loadResources();
  });
  document.getElementById("refreshReservationsBtn").addEventListener("click", loadReservations);
  document.getElementById("refreshNotificationsBtn").addEventListener("click", loadNotifications);
  document.getElementById("closeBookingDialog").addEventListener("click", () => els.bookingDialog.close());
  document.getElementById("cancelBookingDialog").addEventListener("click", () => els.bookingDialog.close());
  els.bookingForm.addEventListener("submit", submitReservation);
}

async function login(event) {
  event.preventDefault();
  els.loginMessage.textContent = "";
  const payload = Object.fromEntries(new FormData(els.loginForm).entries());
  try {
    const result = await request("/user/login", { method: "POST", body: payload, auth: false });
    state.token = result.data.token;
    state.profile = result.data;
    localStorage.setItem(tokenKey, state.token);
    localStorage.setItem(profileKey, JSON.stringify(state.profile));
    renderAuth();
    toast("登录成功");
  } catch (error) {
    els.loginMessage.textContent = error.message;
  }
}

async function logout() {
  try {
    await request("/user/logout", { method: "POST" });
  } catch (error) {
    console.warn(error);
  }
  clearSession();
}

function clearSession() {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem(profileKey);
  state.token = null;
  state.profile = null;
  renderAuth();
}

function renderAuth() {
  const loggedIn = Boolean(state.token);
  els.loginView.hidden = loggedIn;
  els.nav.hidden = !loggedIn;
  els.userBox.hidden = !loggedIn;
  els.bookingView.hidden = !loggedIn;
  els.reservationsView.hidden = true;
  els.notificationsView.hidden = true;
  if (!loggedIn) {
    return;
  }
  els.currentUser.textContent = state.profile?.username || "student";
  switchView("booking");
  loadCategories();
  loadResources();
}

function switchView(view) {
  document.querySelectorAll(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  document.querySelectorAll(".view").forEach((section) => section.classList.remove("active"));
  els.bookingView.hidden = false;
  els.reservationsView.hidden = false;
  els.notificationsView.hidden = false;
  document.getElementById(`${view}View`).classList.add("active");
  if (view === "reservations") loadReservations();
  if (view === "notifications") loadNotifications();
}

async function loadCategories() {
  const loadId = ++state.categoryLoadId;
  try {
    const result = await request("/resource/category");
    if (loadId !== state.categoryLoadId) return;
    const rows = result.data || [];
    els.categoryMetrics.innerHTML = rows.length ? rows.map((item) => `
      <div class="metric">
        <span>${escapeHtml(item.resourceTypeName || typeLabel(item.resourceType))}</span>
        <strong>${item.availableCount}/${item.totalCount}</strong>
      </div>
    `).join("") : `<div class="empty">暂无资源分类</div>`;
  } catch (error) {
    if (loadId !== state.categoryLoadId) return;
    els.categoryMetrics.innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
  }
}

async function loadResources() {
  const loadId = ++state.resourceLoadId;
  try {
    const query = new URLSearchParams({
      reserveDate: state.filter.reserveDate,
      startTime: withSeconds(state.filter.startTime),
      endTime: withSeconds(state.filter.endTime)
    });
    if (state.filter.resourceType) query.set("resourceType", state.filter.resourceType);
    const result = await request(`/resource/list?${query}`);
    if (loadId !== state.resourceLoadId) return;
    const rows = result.data || [];
    els.resourceGrid.innerHTML = rows.length ? rows.map(resourceCard).join("") : `<div class="empty">当前条件下没有可预约资源</div>`;
    document.querySelectorAll("[data-book]").forEach((button) => {
      button.addEventListener("click", () => openBookingDialog(JSON.parse(button.dataset.book)));
    });
  } catch (error) {
    if (loadId !== state.resourceLoadId) return;
    els.resourceGrid.innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
  }
}

function resourceCard(resource) {
  return `
    <article class="resource-card">
      <div class="card-top">
        <h3>${escapeHtml(resource.resourceName)}</h3>
        <span class="badge">${escapeHtml(typeLabel(resource.resourceType))}</span>
      </div>
      <div class="resource-meta">
        <span>编号：${escapeHtml(resource.resourceCode)}</span>
        <span>楼层：${escapeHtml(resource.floor || "-")}</span>
        <span>开放：${escapeHtml(resource.openTime || "-")}</span>
        <span>${escapeHtml(resource.description || "暂无描述")}</span>
      </div>
      <button class="primary" data-book='${escapeAttr(JSON.stringify(resource))}'>预约</button>
    </article>
  `;
}

function openBookingDialog(resource) {
  els.bookingMessage.textContent = "";
  els.bookingForm.resourceId.value = resource.id;
  els.bookingForm.resourceName.value = `${resource.resourceName} (${resource.resourceCode})`;
  els.bookingForm.purpose.value = "";
  els.bookingDialog.showModal();
}

async function submitReservation(event) {
  event.preventDefault();
  els.bookingMessage.textContent = "";
  const data = new FormData(els.bookingForm);
  const payload = {
    resourceId: Number(data.get("resourceId")),
    reserveDate: state.filter.reserveDate,
    startTime: withSeconds(state.filter.startTime),
    endTime: withSeconds(state.filter.endTime),
    purpose: data.get("purpose").trim()
  };
  try {
    await request("/user/reservation/submit", { method: "POST", body: payload });
    els.bookingDialog.close();
    toast("预约已提交，等待审核");
    loadResources();
    loadReservations();
  } catch (error) {
    els.bookingMessage.textContent = error.message;
  }
}

async function loadReservations() {
  const loadId = ++state.reservationLoadId;
  try {
    const result = await request("/user/reservation/page?page=1&pageSize=50");
    if (loadId !== state.reservationLoadId) return;
    const rows = result.data?.records || [];
    els.reservationRows.innerHTML = rows.length ? rows.map((item) => `
      <tr>
        <td>${escapeHtml(item.resourceName || "-")}</td>
        <td>${escapeHtml(item.reserveDate || "-")}</td>
        <td>${formatTime(item.startTime)} - ${formatTime(item.endTime)}</td>
        <td>${escapeHtml(item.purpose || "-")}</td>
        <td>${statusBadge(item.status)}</td>
        <td>${escapeHtml(item.reviewRemark || "-")}</td>
        <td>${canCancel(item.status) ? `<button class="secondary" data-cancel="${item.id}">取消</button>` : "-"}</td>
      </tr>
    `).join("") : `<tr><td colspan="7" class="empty-cell">暂无预约记录</td></tr>`;
    document.querySelectorAll("[data-cancel]").forEach((button) => {
      button.addEventListener("click", () => cancelReservation(button.dataset.cancel));
    });
  } catch (error) {
    if (loadId !== state.reservationLoadId) return;
    els.reservationRows.innerHTML = `<tr><td colspan="7" class="empty-cell">${escapeHtml(error.message)}</td></tr>`;
  }
}

async function cancelReservation(id) {
  if (!confirm("确认取消这条预约吗？")) return;
  try {
    await request(`/user/reservation/cancel/${id}`, { method: "POST" });
    toast("预约已取消");
    loadReservations();
    loadResources();
  } catch (error) {
    toast(error.message);
  }
}

async function loadNotifications() {
  const loadId = ++state.notificationLoadId;
  try {
    const result = await request("/user/notification/page?page=1&pageSize=30");
    if (loadId !== state.notificationLoadId) return;
    const rows = result.data?.records || [];
    els.notificationList.innerHTML = rows.length ? rows.map((item) => `
      <article class="notice-item">
        <h3>${escapeHtml(item.title)}</h3>
        <p>${escapeHtml(item.content)}</p>
        <div class="notice-time">${formatDateTime(item.createTime)} · ${item.readStatus === 0 ? "未读" : "已读"}</div>
      </article>
    `).join("") : `<div class="empty">暂无通知</div>`;
  } catch (error) {
    if (loadId !== state.notificationLoadId) return;
    els.notificationList.innerHTML = `<div class="empty">${escapeHtml(error.message)}</div>`;
  }
}

async function request(path, options = {}) {
  const init = {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json"
    }
  };
  if (options.auth !== false && state.token) {
    init.headers.token = state.token;
  }
  if (options.body) {
    init.body = JSON.stringify(options.body);
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, init);
  } catch (error) {
    throw new Error("无法连接后端服务，请确认后端已启动");
  }
  if (response.status === 401) {
    clearSession();
    throw new Error("登录已失效，请重新登录");
  }
  if (response.status === 403) {
    throw new Error("没有访问权限");
  }
  const result = await parseApiResponse(response);
  if (result.code !== 1) {
    throw new Error(result.msg || "请求失败");
  }
  return result;
}

async function parseApiResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const text = await response.text();
  const isJson = contentType.includes("application/json") || /^[\s\n\r]*[\[{]/.test(text);

  if (!isJson) {
    if (response.status === 502 || response.status === 503 || response.status === 504) {
      throw new Error(`后端服务暂不可用（${response.status}），请确认服务已启动`);
    }
    if (text.includes("<html")) {
      throw new Error("接口返回了页面内容，请检查 /api 代理配置");
    }
    throw new Error(response.ok ? "接口返回格式异常" : `请求失败（${response.status}）`);
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error("接口返回的 JSON 格式异常");
  }
}

function statusBadge(status) {
  const map = {
    1: ["待审核", "amber"],
    2: ["已通过", "green"],
    3: ["已拒绝", "red"],
    4: ["已取消", "gray"]
  };
  const item = map[status] || ["未知", "gray"];
  return `<span class="badge ${item[1]}">${item[0]}</span>`;
}

function canCancel(status) {
  return status === 1 || status === 2;
}

function typeLabel(type) {
  return {
    PUBLIC_SEAT: "公共自习位",
    PRIVATE_ROOM: "独立自习室",
    MEETING_ROOM: "讨论室"
  }[type] || type || "-";
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function withSeconds(value) {
  return value && value.length === 5 ? `${value}:00` : value;
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : "-";
}

function formatDateTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value);
}

function toast(message) {
  els.toast.textContent = message;
  els.toast.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => {
    els.toast.hidden = true;
  }, 2600);
}
