const TOKEN_KEY = "sky-study-room-user-token";
const USER_KEY = "sky-study-room-user-profile";

const state = {
  token: localStorage.getItem(TOKEN_KEY) || "",
  user: loadStoredJson(USER_KEY),
  categories: [],
  resources: [],
  selectedResourceId: null,
  reservations: []
};

const elements = {
  loginPanel: document.getElementById("loginPanel"),
  appPanel: document.getElementById("appPanel"),
  loginForm: document.getElementById("loginForm"),
  userBadge: document.getElementById("userBadge"),
  userNameText: document.getElementById("userNameText"),
  logoutButton: document.getElementById("logoutButton"),
  categoryGrid: document.getElementById("categoryGrid"),
  resourceList: document.getElementById("resourceList"),
  resourceDetail: document.getElementById("resourceDetail"),
  reservationList: document.getElementById("reservationList"),
  resourceCountText: document.getElementById("resourceCountText"),
  resourceTypeFilter: document.getElementById("resourceTypeFilter"),
  reserveDateFilter: document.getElementById("reserveDateFilter"),
  startTimeFilter: document.getElementById("startTimeFilter"),
  endTimeFilter: document.getElementById("endTimeFilter"),
  reservationStatusFilter: document.getElementById("reservationStatusFilter"),
  searchButton: document.getElementById("searchButton"),
  refreshOverviewButton: document.getElementById("refreshOverviewButton"),
  refreshReservationsButton: document.getElementById("refreshReservationsButton"),
  toast: document.getElementById("toast")
};

bootstrap();

function bootstrap() {
  applyDefaultFilters();
  bindEvents();
  syncAuthUi();
  if (state.token) {
    loadInitialData();
  }
}

function bindEvents() {
  elements.loginForm.addEventListener("submit", handleLogin);
  elements.logoutButton.addEventListener("click", logout);
  elements.searchButton.addEventListener("click", loadResources);
  elements.refreshOverviewButton.addEventListener("click", loadCategories);
  elements.refreshReservationsButton.addEventListener("click", loadReservations);
  elements.reservationStatusFilter.addEventListener("change", loadReservations);
}

function applyDefaultFilters() {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  elements.reserveDateFilter.value = `${yyyy}-${mm}-${dd}`;
  elements.startTimeFilter.value = "09:00";
  elements.endTimeFilter.value = "11:00";
}

async function handleLogin(event) {
  event.preventDefault();
  const body = {
    name: document.getElementById("loginName").value.trim(),
    password: document.getElementById("loginPassword").value
  };

  if (!body.name || !body.password) {
    showToast("请输入用户名和密码。");
    return;
  }

  try {
    const result = await request(
      "/api/user/login",
      {
        method: "POST",
        body: JSON.stringify(body)
      },
      false
    );

    state.token = result.data.token || result.data.Token || "";
    state.user = result.data || {};
    localStorage.setItem(TOKEN_KEY, state.token);
    localStorage.setItem(USER_KEY, JSON.stringify(state.user));
    syncAuthUi();
    await loadInitialData();
    showToast("登录成功。");
  } catch (error) {
    showToast(error.message || "登录失败。");
  }
}

async function loadInitialData() {
  await Promise.all([loadCategories(), loadResources(), loadReservations()]);
}

async function loadCategories() {
  try {
    const result = await request("/api/resource/category");
    state.categories = result.data || [];
    renderCategories();
  } catch (error) {
    handleApiError(error);
  }
}

async function loadResources() {
  const reserveDate = elements.reserveDateFilter.value;
  const startTime = elements.startTimeFilter.value;
  const endTime = elements.endTimeFilter.value;

  if (!reserveDate || !startTime || !endTime) {
    showToast("请选择完整的日期和时间段。");
    return;
  }

  if (startTime >= endTime) {
    showToast("开始时间必须早于结束时间。");
    return;
  }

  try {
    const query = new URLSearchParams({
      reserveDate,
      startTime: withSeconds(startTime),
      endTime: withSeconds(endTime)
    });

    if (elements.resourceTypeFilter.value) {
      query.set("resourceType", elements.resourceTypeFilter.value);
    }

    const result = await request(`/api/resource/list?${query.toString()}`);
    state.resources = result.data || [];
    if (!state.resources.some((item) => item.id === state.selectedResourceId)) {
      state.selectedResourceId = state.resources[0] ? state.resources[0].id : null;
    }
    renderResources();
    renderResourceDetail();
  } catch (error) {
    handleApiError(error);
  }
}

async function loadReservations() {
  try {
    const query = new URLSearchParams({
      page: "1",
      pageSize: "20"
    });

    if (elements.reservationStatusFilter.value) {
      query.set("status", elements.reservationStatusFilter.value);
    }

    const result = await request(`/api/user/reservation/page?${query.toString()}`);
    state.reservations = (result.data && result.data.records) || [];
    renderReservations();
  } catch (error) {
    handleApiError(error);
  }
}

async function loadResourceDetail(resourceId) {
  state.selectedResourceId = resourceId;
  try {
    const result = await request(`/api/resource/${resourceId}`);
    const detail = result.data;
    const index = state.resources.findIndex((item) => item.id === resourceId);

    if (index >= 0) {
      state.resources[index] = { ...state.resources[index], ...detail };
    } else {
      state.resources.unshift(detail);
    }

    renderResources();
    renderResourceDetail();
  } catch (error) {
    handleApiError(error);
  }
}

async function submitReservation(resourceId) {
  const purposeInput = document.getElementById("purposeInput");
  if (!purposeInput) {
    return;
  }

  const purpose = purposeInput.value.trim();
  if (!purpose) {
    showToast("请填写预约用途。");
    return;
  }

  const body = {
    resourceId,
    reserveDate: elements.reserveDateFilter.value,
    startTime: withSeconds(elements.startTimeFilter.value),
    endTime: withSeconds(elements.endTimeFilter.value),
    purpose
  };

  try {
    await request("/api/user/reservation/submit", {
      method: "POST",
      body: JSON.stringify(body)
    });
    showToast("预约申请已提交。");
    purposeInput.value = "";
    await Promise.all([loadResources(), loadReservations()]);
  } catch (error) {
    handleApiError(error);
  }
}

async function cancelReservation(reservationId) {
  try {
    await request(`/api/user/reservation/cancel/${reservationId}`, {
      method: "POST"
    });
    showToast("预约已取消。");
    await loadReservations();
  } catch (error) {
    handleApiError(error);
  }
}

function renderCategories() {
  if (!state.categories.length) {
    elements.categoryGrid.innerHTML = '<div class="empty-state category-card"><p>暂无分类数据。</p></div>';
    return;
  }

  elements.categoryGrid.innerHTML = state.categories.map((item) => `
    <article class="category-card">
      <p class="eyebrow">${typeText(item.resourceType)}</p>
      <h3>${escapeHtml(item.resourceTypeName || typeText(item.resourceType))}</h3>
      <div class="category-number">${item.availableCount ?? 0}</div>
      <p class="muted">当前可预约 ${item.availableCount ?? 0} 个 / 共 ${item.totalCount ?? 0} 个</p>
    </article>
  `).join("");
}

function renderResources() {
  elements.resourceCountText.textContent = `${state.resources.length} 条结果`;

  if (!state.resources.length) {
    elements.resourceList.className = "stack-list empty-state";
    elements.resourceList.innerHTML = "<p>当前筛选条件下没有可预约资源。</p>";
    return;
  }

  elements.resourceList.className = "stack-list";
  elements.resourceList.innerHTML = state.resources.map((item) => `
    <article class="resource-card ${item.id === state.selectedResourceId ? "active" : ""}">
      <div class="section-head">
        <div>
          <h3>${escapeHtml(item.resourceName || item.resourceCode || "未命名资源")}</h3>
          <p class="muted">${escapeHtml(item.resourceCode || "暂无编号")}</p>
        </div>
        <button class="ghost-button" data-resource-id="${item.id}">查看详情</button>
      </div>
      <div class="meta-row">
        <span class="type-pill">${typeText(item.resourceType)}</span>
        <span class="status-pill ${resourceStatusClass(item.status)}">${resourceStatusText(item.status)}</span>
        <span class="meta-item">${escapeHtml(item.floor || "未设置楼层")}</span>
        <span class="meta-item">${escapeHtml(item.openTime || "未设置开放时间")}</span>
      </div>
      <p class="muted">${escapeHtml(item.description || "暂无资源说明。")}</p>
    </article>
  `).join("");

  elements.resourceList.querySelectorAll("[data-resource-id]").forEach((button) => {
    button.addEventListener("click", () => loadResourceDetail(Number(button.dataset.resourceId)));
  });
}

function renderResourceDetail() {
  const selected = state.resources.find((item) => item.id === state.selectedResourceId);

  if (!selected) {
    elements.resourceDetail.className = "detail-card empty-state";
    elements.resourceDetail.innerHTML = "<p>选择一个资源后即可查看详情。</p>";
    return;
  }

  elements.resourceDetail.className = "detail-card";
  elements.resourceDetail.innerHTML = `
    <div class="detail-head">
      <p class="eyebrow">${typeText(selected.resourceType)}</p>
      <h3>${escapeHtml(selected.resourceName || "未命名资源")}</h3>
      <p class="muted">${escapeHtml(selected.description || "暂无资源说明。")}</p>
    </div>
    <div class="detail-meta">
      <div class="meta-box">
        <strong>资源编号</strong>
        <p>${escapeHtml(selected.resourceCode || "-")}</p>
      </div>
      <div class="meta-box">
        <strong>楼层</strong>
        <p>${escapeHtml(selected.floor || "-")}</p>
      </div>
      <div class="meta-box">
        <strong>开放时间</strong>
        <p>${escapeHtml(selected.openTime || "-")}</p>
      </div>
      <div class="meta-box">
        <strong>当前状态</strong>
        <p>${resourceStatusText(selected.status)}</p>
      </div>
    </div>
    <div class="detail-form">
      <label>
        预约用途
        <textarea id="purposeInput" placeholder="例如：课程复习、小组讨论、模拟面试准备"></textarea>
      </label>
      <div class="detail-actions">
        <span class="subtle-pill">${elements.reserveDateFilter.value} ${withSeconds(elements.startTimeFilter.value)} - ${withSeconds(elements.endTimeFilter.value)}</span>
        <button id="submitReservationButton" class="primary-button" ${selected.status !== 1 ? "disabled" : ""}>提交预约申请</button>
      </div>
    </div>
  `;

  const submitButton = document.getElementById("submitReservationButton");
  if (submitButton) {
    submitButton.addEventListener("click", () => submitReservation(selected.id));
  }
}

function renderReservations() {
  if (!state.reservations.length) {
    elements.reservationList.className = "stack-list empty-state";
    elements.reservationList.innerHTML = "<p>当前没有预约记录。</p>";
    return;
  }

  const username = state.user && (state.user.username || state.user.name) ? state.user.username || state.user.name : "";

  elements.reservationList.className = "stack-list";
  elements.reservationList.innerHTML = state.reservations.map((item) => `
    <article class="reservation-card">
      <div class="section-head">
        <div>
          <h3>${escapeHtml(item.resourceName || "未知资源")}</h3>
          <p class="muted">${escapeHtml(item.username || username)}</p>
        </div>
        <span class="status-pill ${reservationStatusClass(item.status)}">${reservationStatusText(item.status)}</span>
      </div>
      <div class="meta-row">
        <span class="meta-item">${escapeHtml(item.reserveDate || "-")}</span>
        <span class="meta-item">${escapeHtml(item.startTime || "-")} - ${escapeHtml(item.endTime || "-")}</span>
      </div>
      <p>${escapeHtml(item.purpose || "暂无用途说明。")}</p>
      <p class="muted">审核备注：${escapeHtml(item.reviewRemark || "暂无")}</p>
      <div class="reservation-actions">
        ${(item.status === 1 || item.status === 2)
          ? `<button class="danger-button" data-cancel-id="${item.id}">取消预约</button>`
          : ""}
      </div>
    </article>
  `).join("");

  elements.reservationList.querySelectorAll("[data-cancel-id]").forEach((button) => {
    button.addEventListener("click", () => cancelReservation(Number(button.dataset.cancelId)));
  });
}

function syncAuthUi() {
  const loggedIn = Boolean(state.token);
  elements.loginPanel.classList.toggle("hidden", loggedIn);
  elements.appPanel.classList.toggle("hidden", !loggedIn);
  elements.userBadge.classList.toggle("hidden", !loggedIn);
  elements.userNameText.textContent = state.user && (state.user.username || state.user.name)
    ? state.user.username || state.user.name
    : "已登录";
}

function logout() {
  state.token = "";
  state.user = null;
  state.categories = [];
  state.resources = [];
  state.selectedResourceId = null;
  state.reservations = [];
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  syncAuthUi();
  renderCategories();
  renderResources();
  renderResourceDetail();
  renderReservations();
  showToast("已退出登录。");
}

async function request(url, options = {}, withAuth = true) {
  const config = {
    method: options.method || "GET",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {})
    }
  };

  if (withAuth && state.token) {
    config.headers.token = state.token;
  }

  if (options.body) {
    config.body = options.body;
  }

  const response = await fetch(url, config);
  let payload = null;

  try {
    payload = await response.json();
  } catch (error) {
    payload = null;
  }

  if (response.status === 401) {
    logout();
    throw new Error("登录状态已失效，请重新登录。");
  }

  if (!response.ok) {
    throw new Error((payload && payload.msg) || `请求失败（${response.status}）。`);
  }

  if (payload && payload.code !== 1) {
    throw new Error(payload.msg || "接口返回错误。");
  }

  return payload || { code: 1, data: null };
}

function handleApiError(error) {
  showToast(error.message || "请求失败。");
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.remove("hidden");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => {
    elements.toast.classList.add("hidden");
  }, 2200);
}

function withSeconds(timeValue) {
  return timeValue && timeValue.length === 5 ? `${timeValue}:00` : timeValue;
}

function typeText(type) {
  return {
    PUBLIC_SEAT: "公共自习位",
    PRIVATE_ROOM: "独立自习室",
    MEETING_ROOM: "讨论室"
  }[type] || type || "未知类型";
}

function resourceStatusText(status) {
  return {
    0: "已停用",
    1: "可预约",
    2: "维护中"
  }[status] || "未知状态";
}

function reservationStatusText(status) {
  return {
    1: "待审核",
    2: "已通过",
    3: "已拒绝",
    4: "已取消"
  }[status] || "未知状态";
}

function resourceStatusClass(status) {
  return {
    0: "disabled",
    1: "enabled",
    2: "repair"
  }[status] || "disabled";
}

function reservationStatusClass(status) {
  return {
    1: "pending",
    2: "approved",
    3: "rejected",
    4: "canceled"
  }[status] || "pending";
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function loadStoredJson(key) {
  const raw = localStorage.getItem(key);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
}
