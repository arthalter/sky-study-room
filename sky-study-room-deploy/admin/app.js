const API_BASE = "/api";
const tokenKey = "sky_admin_token";
const profileKey = "sky_admin_profile";

const state = {
  token: localStorage.getItem(tokenKey),
  profile: JSON.parse(localStorage.getItem(profileKey) || "null"),
  reservationStatus: "",
  resourceFilters: {},
  reservationLoadId: 0,
  resourceLoadId: 0
};

const els = {
  nav: document.getElementById("nav"),
  adminBox: document.getElementById("adminBox"),
  currentAdmin: document.getElementById("currentAdmin"),
  loginView: document.getElementById("loginView"),
  reservationsView: document.getElementById("reservationsView"),
  resourcesView: document.getElementById("resourcesView"),
  loginForm: document.getElementById("loginForm"),
  loginMessage: document.getElementById("loginMessage"),
  reservationFilterForm: document.getElementById("reservationFilterForm"),
  resourceFilterForm: document.getElementById("resourceFilterForm"),
  reservationRows: document.getElementById("reservationRows"),
  resourceRows: document.getElementById("resourceRows"),
  reviewDialog: document.getElementById("reviewDialog"),
  reviewForm: document.getElementById("reviewForm"),
  reviewMessage: document.getElementById("reviewMessage"),
  resourceDialog: document.getElementById("resourceDialog"),
  resourceForm: document.getElementById("resourceForm"),
  resourceMessage: document.getElementById("resourceMessage"),
  resourceDialogTitle: document.getElementById("resourceDialogTitle"),
  toast: document.getElementById("toast")
};

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  renderAuth();
});

function bindEvents() {
  els.loginForm.addEventListener("submit", login);
  document.getElementById("logoutBtn").addEventListener("click", logout);
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
  document.getElementById("refreshReservationsBtn").addEventListener("click", loadReservations);
  els.reservationFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    state.reservationStatus = new FormData(els.reservationFilterForm).get("status");
    loadReservations();
  });
  els.resourceFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    state.resourceFilters = Object.fromEntries(new FormData(els.resourceFilterForm).entries());
    loadResources();
  });
  document.getElementById("newResourceBtn").addEventListener("click", () => openResourceDialog());
  els.reviewForm.addEventListener("submit", submitReview);
  els.resourceForm.addEventListener("submit", submitResource);
  document.querySelectorAll("[data-close]").forEach((button) => {
    button.addEventListener("click", () => document.getElementById(button.dataset.close).close());
  });
}

async function login(event) {
  event.preventDefault();
  els.loginMessage.textContent = "";
  const payload = Object.fromEntries(new FormData(els.loginForm).entries());
  try {
    const result = await request("/admin/login", { method: "POST", body: payload, auth: false });
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
    await request("/admin/logout", { method: "POST" });
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
  els.adminBox.hidden = !loggedIn;
  els.reservationsView.hidden = !loggedIn;
  els.resourcesView.hidden = true;
  if (!loggedIn) {
    return;
  }
  els.currentAdmin.textContent = state.profile?.username || "admin";
  switchView("reservations");
}

function switchView(view) {
  document.querySelectorAll(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  document.querySelectorAll(".view").forEach((section) => section.classList.remove("active"));
  els.reservationsView.hidden = false;
  els.resourcesView.hidden = false;
  document.getElementById(`${view}View`).classList.add("active");
  if (view === "reservations") loadReservations();
  if (view === "resources") loadResources();
}

async function loadReservations() {
  const loadId = ++state.reservationLoadId;
  const query = new URLSearchParams({ page: "1", pageSize: "80" });
  if (state.reservationStatus) query.set("status", state.reservationStatus);
  try {
    const result = await request(`/admin/reservation/page?${query}`);
    if (loadId !== state.reservationLoadId) return;
    const rows = result.data?.records || [];
    els.reservationRows.innerHTML = rows.length ? rows.map((item) => `
      <tr>
        <td>${item.id}</td>
        <td>${escapeHtml(item.username || "-")}</td>
        <td>${escapeHtml(item.resourceName || "-")}</td>
        <td>${escapeHtml(item.reserveDate || "-")}</td>
        <td>${formatTime(item.startTime)} - ${formatTime(item.endTime)}</td>
        <td>${escapeHtml(item.purpose || "-")}</td>
        <td>${reservationBadge(item.status)}</td>
        <td>${escapeHtml(item.reviewRemark || "-")}</td>
        <td>
          ${item.status === 1 ? `<button class="primary" data-review='${escapeAttr(JSON.stringify(item))}'>审核</button>` : "-"}
        </td>
      </tr>
    `).join("") : `<tr><td colspan="9" class="empty-cell">暂无预约记录</td></tr>`;
    document.querySelectorAll("[data-review]").forEach((button) => {
      button.addEventListener("click", () => openReviewDialog(JSON.parse(button.dataset.review)));
    });
  } catch (error) {
    if (loadId !== state.reservationLoadId) return;
    els.reservationRows.innerHTML = `<tr><td colspan="9" class="empty-cell">${escapeHtml(error.message)}</td></tr>`;
  }
}

function openReviewDialog(item) {
  els.reviewMessage.textContent = "";
  els.reviewForm.reservationId.value = item.id;
  els.reviewForm.status.value = "2";
  els.reviewForm.reviewRemark.value = "";
  els.reviewDialog.showModal();
}

async function submitReview(event) {
  event.preventDefault();
  els.reviewMessage.textContent = "";
  const data = new FormData(els.reviewForm);
  const payload = {
    reservationId: Number(data.get("reservationId")),
    status: Number(data.get("status")),
    reviewRemark: data.get("reviewRemark").trim()
  };
  try {
    await request("/admin/reservation/review", { method: "POST", body: payload });
    els.reviewDialog.close();
    toast("审核已提交，用户通知将异步生成");
    loadReservations();
  } catch (error) {
    els.reviewMessage.textContent = error.message;
  }
}

async function loadResources() {
  const loadId = ++state.resourceLoadId;
  const query = new URLSearchParams({ page: "1", pageSize: "80" });
  Object.entries(state.resourceFilters).forEach(([key, value]) => {
    if (value) query.set(key, value);
  });
  try {
    const result = await request(`/admin/resource/page?${query}`);
    if (loadId !== state.resourceLoadId) return;
    const rows = result.data?.records || [];
    els.resourceRows.innerHTML = rows.length ? rows.map((item) => `
      <tr>
        <td>${escapeHtml(item.resourceCode)}</td>
        <td>${escapeHtml(item.resourceName)}</td>
        <td>${typeBadge(item.resourceType)}</td>
        <td>${escapeHtml(item.floor || "-")}</td>
        <td>${escapeHtml(item.openTime || "-")}</td>
        <td>${resourceStatusBadge(item.status)}</td>
        <td>${escapeHtml(item.description || "-")}</td>
        <td>
          <div class="row-actions">
            <button class="secondary" data-edit='${escapeAttr(JSON.stringify(item))}'>编辑</button>
            ${statusButtons(item)}
          </div>
        </td>
      </tr>
    `).join("") : `<tr><td colspan="8" class="empty-cell">暂无资源</td></tr>`;
    document.querySelectorAll("[data-edit]").forEach((button) => {
      button.addEventListener("click", () => openResourceDialog(JSON.parse(button.dataset.edit)));
    });
    document.querySelectorAll("[data-status]").forEach((button) => {
      button.addEventListener("click", () => updateResourceStatus(button.dataset.id, button.dataset.status));
    });
  } catch (error) {
    if (loadId !== state.resourceLoadId) return;
    els.resourceRows.innerHTML = `<tr><td colspan="8" class="empty-cell">${escapeHtml(error.message)}</td></tr>`;
  }
}

function statusButtons(item) {
  return `
    <button class="secondary" data-id="${item.id}" data-status="1">可用</button>
    <button class="secondary" data-id="${item.id}" data-status="2">维修</button>
    <button class="danger" data-id="${item.id}" data-status="0">停用</button>
  `;
}

function openResourceDialog(resource = null) {
  els.resourceMessage.textContent = "";
  els.resourceForm.reset();
  els.resourceDialogTitle.textContent = resource ? "编辑资源" : "新增资源";
  if (resource) {
    els.resourceForm.id.value = resource.id;
    els.resourceForm.resourceCode.value = resource.resourceCode || "";
    els.resourceForm.resourceName.value = resource.resourceName || "";
    els.resourceForm.resourceType.value = resource.resourceType || "PUBLIC_SEAT";
    els.resourceForm.floor.value = resource.floor || "";
    els.resourceForm.openTime.value = resource.openTime || "";
    els.resourceForm.description.value = resource.description || "";
  } else {
    els.resourceForm.id.value = "";
    els.resourceForm.resourceType.value = "PUBLIC_SEAT";
    els.resourceForm.openTime.value = "08:00-22:00";
  }
  els.resourceDialog.showModal();
}

async function submitResource(event) {
  event.preventDefault();
  els.resourceMessage.textContent = "";
  const data = Object.fromEntries(new FormData(els.resourceForm).entries());
  const payload = {
    resourceCode: data.resourceCode.trim(),
    resourceName: data.resourceName.trim(),
    resourceType: data.resourceType,
    floor: data.floor.trim(),
    openTime: data.openTime.trim(),
    description: data.description.trim()
  };
  if (data.id) payload.id = Number(data.id);
  try {
    await request("/admin/resource", {
      method: data.id ? "PUT" : "POST",
      body: payload
    });
    els.resourceDialog.close();
    toast(data.id ? "资源已更新" : "资源已新增");
    loadResources();
  } catch (error) {
    els.resourceMessage.textContent = error.message;
  }
}

async function updateResourceStatus(id, status) {
  try {
    await request("/admin/resource/status", {
      method: "POST",
      body: {
        id: Number(id),
        status: Number(status)
      }
    });
    toast("状态已更新");
    loadResources();
  } catch (error) {
    toast(error.message);
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

function reservationBadge(status) {
  const map = {
    1: ["待审核", "amber"],
    2: ["已通过", "green"],
    3: ["已拒绝", "red"],
    4: ["已取消", "gray"]
  };
  const item = map[status] || ["未知", "gray"];
  return `<span class="badge ${item[1]}">${item[0]}</span>`;
}

function resourceStatusBadge(status) {
  const map = {
    0: ["停用", "gray"],
    1: ["可用", "green"],
    2: ["维修中", "amber"]
  };
  const item = map[status] || ["未知", "gray"];
  return `<span class="badge ${item[1]}">${item[0]}</span>`;
}

function typeBadge(type) {
  return `<span class="badge blue">${escapeHtml(typeLabel(type))}</span>`;
}

function typeLabel(type) {
  return {
    PUBLIC_SEAT: "公共自习位",
    PRIVATE_ROOM: "独立自习室",
    MEETING_ROOM: "讨论室"
  }[type] || type || "-";
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : "-";
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
