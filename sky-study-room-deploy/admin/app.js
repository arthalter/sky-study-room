const TOKEN_KEY = "sky-study-room-admin-token";
const USER_KEY = "sky-study-room-admin-profile";

const state = {
  token: localStorage.getItem(TOKEN_KEY) || "",
  user: loadStoredJson(USER_KEY),
  resources: [],
  reservations: [],
  editingResourceId: null,
  reviewingReservation: null
};

const elements = {
  loginPanel: document.getElementById("loginPanel"),
  appPanel: document.getElementById("appPanel"),
  loginForm: document.getElementById("loginForm"),
  userBadge: document.getElementById("userBadge"),
  adminNameText: document.getElementById("adminNameText"),
  logoutButton: document.getElementById("logoutButton"),
  metricGrid: document.getElementById("metricGrid"),
  resourceView: document.getElementById("resourceView"),
  reservationView: document.getElementById("reservationView"),
  resourceList: document.getElementById("resourceList"),
  reservationList: document.getElementById("reservationList"),
  resourceNameFilter: document.getElementById("resourceNameFilter"),
  resourceTypeFilter: document.getElementById("resourceTypeFilter"),
  resourceStatusFilter: document.getElementById("resourceStatusFilter"),
  reservationStatusFilter: document.getElementById("reservationStatusFilter"),
  searchResourcesButton: document.getElementById("searchResourcesButton"),
  refreshResourcesButton: document.getElementById("refreshResourcesButton"),
  refreshReservationsButton: document.getElementById("refreshReservationsButton"),
  refreshDashboardButton: document.getElementById("refreshDashboardButton"),
  openAddButton: document.getElementById("openAddButton"),
  resourceModalMask: document.getElementById("resourceModalMask"),
  resourceModalTitle: document.getElementById("resourceModalTitle"),
  resourceCodeInput: document.getElementById("resourceCodeInput"),
  resourceNameInput: document.getElementById("resourceNameInput"),
  resourceTypeInput: document.getElementById("resourceTypeInput"),
  floorInput: document.getElementById("floorInput"),
  openTimeInput: document.getElementById("openTimeInput"),
  descriptionInput: document.getElementById("descriptionInput"),
  saveResourceButton: document.getElementById("saveResourceButton"),
  closeResourceModalButton: document.getElementById("closeResourceModalButton"),
  reviewModalMask: document.getElementById("reviewModalMask"),
  reviewModalTitle: document.getElementById("reviewModalTitle"),
  reviewResourceText: document.getElementById("reviewResourceText"),
  reviewTimeText: document.getElementById("reviewTimeText"),
  reviewUserText: document.getElementById("reviewUserText"),
  reviewRemarkInput: document.getElementById("reviewRemarkInput"),
  approveButton: document.getElementById("approveButton"),
  rejectButton: document.getElementById("rejectButton"),
  closeReviewModalButton: document.getElementById("closeReviewModalButton"),
  toast: document.getElementById("toast"),
  tabButtons: Array.from(document.querySelectorAll(".tabbar button"))
};

bootstrap();

function bootstrap() {
  bindEvents();
  syncAuthUi();
  if (state.token) {
    loadInitialData();
  }
}

function bindEvents() {
  elements.loginForm.addEventListener("submit", handleLogin);
  elements.logoutButton.addEventListener("click", logout);
  elements.searchResourcesButton.addEventListener("click", loadResources);
  elements.refreshResourcesButton.addEventListener("click", loadResources);
  elements.refreshReservationsButton.addEventListener("click", loadReservations);
  elements.refreshDashboardButton.addEventListener("click", loadInitialData);
  elements.reservationStatusFilter.addEventListener("change", loadReservations);
  elements.openAddButton.addEventListener("click", openAddModal);
  elements.saveResourceButton.addEventListener("click", saveResource);
  elements.closeResourceModalButton.addEventListener("click", closeResourceModal);
  elements.closeReviewModalButton.addEventListener("click", closeReviewModal);
  elements.approveButton.addEventListener("click", () => submitReview(2));
  elements.rejectButton.addEventListener("click", () => submitReview(3));

  elements.tabButtons.forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
}

async function handleLogin(event) {
  event.preventDefault();
  const body = {
    name: document.getElementById("loginName").value.trim(),
    password: document.getElementById("loginPassword").value
  };

  if (!body.name || !body.password) {
    showToast("请输入管理员用户名和密码。");
    return;
  }

  try {
    const result = await request("/api/admin/login", {
      method: "POST",
      body: JSON.stringify(body)
    }, false);

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
  await Promise.all([loadResources(), loadReservations()]);
  renderMetrics();
}

async function loadResources() {
  try {
    const query = new URLSearchParams({
      page: "1",
      pageSize: "20"
    });

    if (elements.resourceNameFilter.value.trim()) {
      query.set("resourceName", elements.resourceNameFilter.value.trim());
    }
    if (elements.resourceTypeFilter.value) {
      query.set("resourceType", elements.resourceTypeFilter.value);
    }
    if (elements.resourceStatusFilter.value) {
      query.set("status", elements.resourceStatusFilter.value);
    }

    const result = await request(`/api/admin/resource/page?${query.toString()}`);
    state.resources = (result.data && result.data.records) || [];
    renderResources();
    renderMetrics();
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

    const result = await request(`/api/admin/reservation/page?${query.toString()}`);
    state.reservations = (result.data && result.data.records) || [];
    renderReservations();
    renderMetrics();
  } catch (error) {
    handleApiError(error);
  }
}

function renderMetrics() {
  const enabledCount = state.resources.filter((item) => item.status === 1).length;
  const pendingCount = state.reservations.filter((item) => item.status === 1).length;
  const approvedCount = state.reservations.filter((item) => item.status === 2).length;

  elements.metricGrid.innerHTML = `
    <article class="metric-card">
      <p class="eyebrow">资源</p>
      <h3>当前资源总数</h3>
      <div class="metric-number">${state.resources.length}</div>
      <p class="muted">当前筛选结果中的资源数量</p>
    </article>
    <article class="metric-card">
      <p class="eyebrow">可预约</p>
      <h3>可用资源</h3>
      <div class="metric-number">${enabledCount}</div>
      <p class="muted">状态为“可预约”的资源数量</p>
    </article>
    <article class="metric-card">
      <p class="eyebrow">审核</p>
      <h3>待审核 / 已通过</h3>
      <div class="metric-number">${pendingCount} / ${approvedCount}</div>
      <p class="muted">帮助你快速把控审核积压情况</p>
    </article>
  `;
}

function renderResources() {
  if (!state.resources.length) {
    elements.resourceList.className = "stack-list empty-state";
    elements.resourceList.innerHTML = "<p>当前筛选条件下没有资源记录。</p>";
    return;
  }

  elements.resourceList.className = "stack-list";
  elements.resourceList.innerHTML = state.resources.map((item) => `
    <article class="resource-card">
      <div class="section-head">
        <div>
          <h3>${escapeHtml(item.resourceName || "未命名资源")}</h3>
          <p class="muted">${escapeHtml(item.resourceCode || "暂无编号")}</p>
        </div>
        <div class="meta-row">
          <span class="pill type-pill">${typeText(item.resourceType)}</span>
          <span class="pill status-pill ${resourceStatusClass(item.status)}">${resourceStatusText(item.status)}</span>
        </div>
      </div>
      <div class="meta-row">
        <span class="meta-item">楼层：${escapeHtml(item.floor || "-")}</span>
        <span class="meta-item">开放时间：${escapeHtml(item.openTime || "-")}</span>
        <span class="meta-item">更新时间：${escapeHtml(formatDateTime(item.updateTime))}</span>
      </div>
      <p>${escapeHtml(item.description || "暂无资源说明。")}</p>
      <div class="resource-actions">
        <button class="ghost-button" data-edit-resource="${item.id}">编辑资源</button>
        <button class="primary-button" data-set-status="${item.id}:1">设为可预约</button>
        <button class="ghost-button" data-set-status="${item.id}:0">停用</button>
        <button class="danger-button" data-set-status="${item.id}:2">维护中</button>
      </div>
    </article>
  `).join("");

  elements.resourceList.querySelectorAll("[data-edit-resource]").forEach((button) => {
    button.addEventListener("click", () => {
      const id = Number(button.dataset.editResource);
      const target = state.resources.find((item) => item.id === id);
      if (target) {
        openEditModal(target);
      }
    });
  });

  elements.resourceList.querySelectorAll("[data-set-status]").forEach((button) => {
    button.addEventListener("click", async () => {
      const [id, status] = button.dataset.setStatus.split(":");
      await updateResourceStatus(Number(id), Number(status));
    });
  });
}

function renderReservations() {
  if (!state.reservations.length) {
    elements.reservationList.className = "stack-list empty-state";
    elements.reservationList.innerHTML = "<p>当前筛选条件下没有预约记录。</p>";
    return;
  }

  elements.reservationList.className = "stack-list";
  elements.reservationList.innerHTML = state.reservations.map((item) => `
    <article class="reservation-card">
      <div class="section-head">
        <div>
          <h3>${escapeHtml(item.resourceName || "未知资源")}</h3>
          <p class="muted">申请人：${escapeHtml(item.username || "未知用户")}</p>
        </div>
        <span class="pill status-pill ${reservationStatusClass(item.status)}">${reservationStatusText(item.status)}</span>
      </div>
      <div class="meta-row">
        <span class="meta-item">日期：${escapeHtml(item.reserveDate || "-")}</span>
        <span class="meta-item">时间：${escapeHtml(item.startTime || "-")} - ${escapeHtml(item.endTime || "-")}</span>
        <span class="meta-item">提交时间：${escapeHtml(formatDateTime(item.createTime))}</span>
      </div>
      <p>预约用途：${escapeHtml(item.purpose || "暂无用途说明。")}</p>
      <p class="muted">审核备注：${escapeHtml(item.reviewRemark || "暂无")}</p>
      <div class="review-actions">
        ${item.status === 1 ? `<button class="primary-button" data-review-id="${item.id}">处理审核</button>` : ""}
      </div>
    </article>
  `).join("");

  elements.reservationList.querySelectorAll("[data-review-id]").forEach((button) => {
    button.addEventListener("click", () => {
      const id = Number(button.dataset.reviewId);
      const target = state.reservations.find((item) => item.id === id);
      if (target) {
        openReviewModal(target);
      }
    });
  });
}

function switchView(view) {
  elements.tabButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.view === view);
  });
  elements.resourceView.classList.toggle("hidden", view !== "resource");
  elements.reservationView.classList.toggle("hidden", view !== "reservation");
}

function openAddModal() {
  state.editingResourceId = null;
  elements.resourceModalTitle.textContent = "新增资源";
  elements.resourceCodeInput.value = "";
  elements.resourceNameInput.value = "";
  elements.resourceTypeInput.value = "PUBLIC_SEAT";
  elements.floorInput.value = "1F";
  elements.openTimeInput.value = "08:00-22:00";
  elements.descriptionInput.value = "";
  elements.resourceModalMask.style.display = "flex";
}

function openEditModal(item) {
  state.editingResourceId = item.id;
  elements.resourceModalTitle.textContent = "编辑资源";
  elements.resourceCodeInput.value = item.resourceCode || "";
  elements.resourceNameInput.value = item.resourceName || "";
  elements.resourceTypeInput.value = item.resourceType || "PUBLIC_SEAT";
  elements.floorInput.value = item.floor || "";
  elements.openTimeInput.value = item.openTime || "";
  elements.descriptionInput.value = item.description || "";
  elements.resourceModalMask.style.display = "flex";
}

function closeResourceModal() {
  elements.resourceModalMask.style.display = "none";
}

async function saveResource() {
  const body = {
    id: state.editingResourceId,
    resourceCode: elements.resourceCodeInput.value.trim(),
    resourceName: elements.resourceNameInput.value.trim(),
    resourceType: elements.resourceTypeInput.value,
    floor: elements.floorInput.value.trim(),
    openTime: elements.openTimeInput.value.trim(),
    description: elements.descriptionInput.value.trim()
  };

  if (!body.resourceCode || !body.resourceName || !body.openTime) {
    showToast("请至少填写资源编号、资源名称和开放时间。");
    return;
  }

  try {
    await request("/api/admin/resource", {
      method: state.editingResourceId ? "PUT" : "POST",
      body: JSON.stringify(body)
    });
    closeResourceModal();
    showToast(state.editingResourceId ? "资源已更新。" : "资源已新增。");
    await loadResources();
  } catch (error) {
    handleApiError(error);
  }
}

async function updateResourceStatus(id, status) {
  try {
    await request("/api/admin/resource/status", {
      method: "POST",
      body: JSON.stringify({ id, status })
    });
    showToast("资源状态已更新。");
    await loadResources();
  } catch (error) {
    handleApiError(error);
  }
}

function openReviewModal(item) {
  state.reviewingReservation = item;
  elements.reviewModalTitle.textContent = `处理预约 #${item.id}`;
  elements.reviewResourceText.textContent = `资源：${item.resourceName || "未知资源"}`;
  elements.reviewTimeText.textContent = `时间：${item.reserveDate || "-"} ${item.startTime || "-"} - ${item.endTime || "-"}`;
  elements.reviewUserText.textContent = `申请人：${item.username || "未知用户"}`;
  elements.reviewRemarkInput.value = "";
  elements.reviewModalMask.style.display = "flex";
}

function closeReviewModal() {
  state.reviewingReservation = null;
  elements.reviewModalMask.style.display = "none";
}

async function submitReview(status) {
  if (!state.reviewingReservation) {
    return;
  }

  const defaultRemark = status === 2 ? "审核通过" : "该时间段不可用";
  const body = {
    reservationId: state.reviewingReservation.id,
    status,
    reviewRemark: elements.reviewRemarkInput.value.trim() || defaultRemark
  };

  try {
    await request("/api/admin/reservation/review", {
      method: "POST",
      body: JSON.stringify(body)
    });
    closeReviewModal();
    showToast(status === 2 ? "预约已审核通过。" : "预约已拒绝。");
    await loadReservations();
  } catch (error) {
    handleApiError(error);
  }
}

function syncAuthUi() {
  const loggedIn = Boolean(state.token);
  elements.loginPanel.classList.toggle("hidden", loggedIn);
  elements.appPanel.classList.toggle("hidden", !loggedIn);
  elements.userBadge.classList.toggle("hidden", !loggedIn);
  elements.adminNameText.textContent = state.user && (state.user.username || state.user.name)
    ? `管理员：${state.user.username || state.user.name}`
    : "管理员已登录";
}

function logout() {
  state.token = "";
  state.user = null;
  state.resources = [];
  state.reservations = [];
  state.editingResourceId = null;
  state.reviewingReservation = null;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  syncAuthUi();
  elements.resourceList.className = "stack-list empty-state";
  elements.resourceList.innerHTML = "<p>资源列表加载后会显示在这里。</p>";
  elements.reservationList.className = "stack-list empty-state";
  elements.reservationList.innerHTML = "<p>预约记录加载后会显示在这里。</p>";
  elements.metricGrid.innerHTML = "";
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

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
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
