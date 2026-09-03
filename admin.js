window.addEventListener("load", () => {
  const tokenKey = "amaraeAdminToken";
  let orders = [];

  init();

  function init() {
    setupLoginForm();
    setupRefresh();
    setupLogout();
    if (getToken()) {
      showDashboard();
    } else {
      showLogin();
    }
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function formatMoney(value) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0,
    }).format(Number(value));
  }

  function formatDate(iso) {
    if (!iso) return "";
    try {
      return new Date(iso).toLocaleString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch (error) {
      return "";
    }
  }

  function getToken() {
    return localStorage.getItem(tokenKey);
  }

  async function api(path, options = {}) {
    const token = getToken();
    const response = await fetch(path, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
      },
    });
    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.error || "Something went wrong");
    }
    return response.status === 204 ? null : response.json();
  }

  function setupLoginForm() {
    const form = document.getElementById("adminLoginForm");
    const note = document.getElementById("adminLoginNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      try {
        const result = await api("/api/admin/login", {
          method: "POST",
          body: JSON.stringify({ password: document.getElementById("adminPassword").value }),
        });
        localStorage.setItem(tokenKey, result.token);
        form.reset();
        await showDashboard();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      }
    });
  }

  function setupRefresh() {
    document.getElementById("adminRefreshBtn")?.addEventListener("click", loadOrders);
  }

  function setupLogout() {
    document.getElementById("adminLogoutBtn")?.addEventListener("click", async () => {
      try {
        await api("/api/admin/logout", { method: "DELETE" });
      } catch (error) {
        // Clear locally regardless — a dead token shouldn't trap the
        // admin on a broken dashboard.
      }
      localStorage.removeItem(tokenKey);
      showLogin();
    });
  }

  function showLogin() {
    document.getElementById("adminLogin").hidden = false;
    document.getElementById("adminDashboard").hidden = true;
  }

  async function showDashboard() {
    document.getElementById("adminLogin").hidden = true;
    document.getElementById("adminDashboard").hidden = false;
    await loadOrders();
  }

  async function loadOrders() {
    const list = document.getElementById("adminOrderList");
    const countEl = document.getElementById("adminOrderCount");
    if (!list) return;
    list.innerHTML = '<p class="reviews-empty">Loading orders…</p>';
    try {
      orders = await api("/api/admin/orders");
      if (countEl) countEl.textContent = `${orders.length} order${orders.length === 1 ? "" : "s"}`;
      list.innerHTML = orders.length
        ? orders.map(adminOrderRowTemplate).join("")
        : '<p class="reviews-empty">No orders yet.</p>';
      attachOrderFormHandlers();
    } catch (error) {
      // A 401 here almost always means the admin token expired — bounce
      // back to the login form instead of showing a dead dashboard.
      localStorage.removeItem(tokenKey);
      showLogin();
      const note = document.getElementById("adminLoginNote");
      if (note) {
        note.textContent = "Your session expired — please sign in again.";
        note.classList.add("error");
      }
    }
  }

  function adminOrderRowTemplate(admin) {
    const order = admin.order;
    const items = order.lines.map((line) => `${line.quantity} \u00d7 ${escapeHtml(line.productName)}`).join(", ");
    const statusOptions = [
      "CREATED",
      "PAYMENT_PENDING",
      "PAID",
      "SHIPPED",
      "DELIVERED",
      "REFUNDED",
      "FAILED",
      "CANCELLED",
    ]
      .map((status) => `<option value="${status}" ${status === order.status ? "selected" : ""}>${status}</option>`)
      .join("");

    return `
      <article class="admin-order-card" data-order-id="${order.id}">
        <div class="admin-order-head">
          <div>
            <strong>Order #${order.id}</strong>
            <span class="order-status order-status-${escapeHtml(order.status.toLowerCase())}">${escapeHtml(order.status)}</span>
          </div>
          <span>${formatDate(order.createdAt)}</span>
        </div>

        <div class="admin-order-body">
          <div>
            <p><strong>${escapeHtml(admin.customerName)}</strong></p>
            <p>${escapeHtml(admin.email)} \u00b7 ${escapeHtml(admin.phone)}</p>
            <p>${escapeHtml(admin.shippingAddressLine)}, ${escapeHtml(admin.shippingCity)}, ${escapeHtml(admin.shippingState)} ${escapeHtml(admin.shippingPinCode)}</p>
          </div>
          <div>
            <p>${items}</p>
            <p>Subtotal ${formatMoney(order.subtotal)}${
              order.discountAmount > 0 ? ` \u2014 discount (${escapeHtml(order.couponCode || "")}) -${formatMoney(order.discountAmount)}` : ""
            } \u2014 shipping ${formatMoney(order.shippingFee)}</p>
            <p><strong>Total: ${formatMoney(order.total)}</strong></p>
          </div>
        </div>

        <form class="admin-order-form" data-order-id="${order.id}">
          <label>
            Status
            <select name="status">${statusOptions}</select>
          </label>
          <label>
            Courier
            <input name="trackingCourier" type="text" placeholder="e.g. Shiprocket / Delhivery" value="${escapeHtml(order.trackingCourier || "")}" />
          </label>
          <label>
            Tracking / AWB number
            <input name="trackingNumber" type="text" placeholder="Tracking number" value="${escapeHtml(order.trackingNumber || "")}" />
          </label>
          <label>
            Tracking link
            <input name="trackingUrl" type="text" placeholder="https://..." value="${escapeHtml(order.trackingUrl || "")}" />
          </label>
          <button type="submit" class="secondary-btn compact">Save</button>
          <span class="admin-order-note"></span>
        </form>
      </article>
    `;
  }

  function attachOrderFormHandlers() {
    document.querySelectorAll(".admin-order-form").forEach((form) => {
      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const orderId = form.dataset.orderId;
        const note = form.querySelector(".admin-order-note");
        const submitBtn = form.querySelector("button[type=submit]");
        const payload = {
          status: form.status.value,
          trackingCourier: form.trackingCourier.value.trim(),
          trackingNumber: form.trackingNumber.value.trim(),
          trackingUrl: form.trackingUrl.value.trim(),
        };

        submitBtn.disabled = true;
        note.textContent = "Saving…";
        note.classList.remove("error");
        try {
          await api(`/api/admin/orders/${orderId}`, {
            method: "PATCH",
            body: JSON.stringify(payload),
          });
          note.textContent = "Saved.";
          const card = form.closest(".admin-order-card");
          const statusBadge = card?.querySelector(".order-status");
          if (statusBadge) {
            statusBadge.textContent = payload.status;
            statusBadge.className = `order-status order-status-${payload.status.toLowerCase()}`;
          }
        } catch (error) {
          note.textContent = error.message;
          note.classList.add("error");
        } finally {
          submitBtn.disabled = false;
        }
      });
    });
  }
});
