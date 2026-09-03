window.addEventListener("load", () => {
  const tokenKey = "amaraeAuthToken";
  const nameKey = "amaraeAuthName";
  const emailKey = "amaraeAuthEmail";

  init();

  function init() {
    setupTabs();
    setupLoginForm();
    setupSignupForm();
    setupAddressForm();
    setupLogout();
    refreshAuthState();
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
      return new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
    } catch (error) {
      return "";
    }
  }

  function getToken() {
    return localStorage.getItem(tokenKey);
  }

  function setSession(token, name, email) {
    localStorage.setItem(tokenKey, token);
    localStorage.setItem(nameKey, name);
    localStorage.setItem(emailKey, email);
  }

  function clearSession() {
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(nameKey);
    localStorage.removeItem(emailKey);
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

  function setupTabs() {
    document.querySelectorAll(".auth-tab").forEach((tab) => {
      tab.addEventListener("click", () => {
        document.querySelectorAll(".auth-tab").forEach((t) => {
          t.classList.remove("active");
          t.setAttribute("aria-selected", "false");
        });
        tab.classList.add("active");
        tab.setAttribute("aria-selected", "true");
        const isLogin = tab.dataset.tab === "login";
        document.getElementById("loginForm").hidden = !isLogin;
        document.getElementById("signupForm").hidden = isLogin;
      });
    });
  }

  function setupLoginForm() {
    const form = document.getElementById("loginForm");
    const note = document.getElementById("loginNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      try {
        const result = await api("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({
            email: document.getElementById("loginEmail").value.trim(),
            password: document.getElementById("loginPassword").value,
          }),
        });
        setSession(result.token, result.name, result.email);
        await showDashboard();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      }
    });
  }

  function setupSignupForm() {
    const form = document.getElementById("signupForm");
    const note = document.getElementById("signupNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      try {
        const result = await api("/api/auth/signup", {
          method: "POST",
          body: JSON.stringify({
            name: document.getElementById("signupName").value.trim(),
            email: document.getElementById("signupEmail").value.trim(),
            password: document.getElementById("signupPassword").value,
          }),
        });
        setSession(result.token, result.name, result.email);
        await showDashboard();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      }
    });
  }

  function setupLogout() {
    document.getElementById("logoutBtn")?.addEventListener("click", async () => {
      try {
        await api("/api/auth/logout", { method: "DELETE" });
      } catch (error) {
        // Even if the network call fails, clear the local session so the
        // person isn't stuck looking "logged in" on a dead token.
      }
      clearSession();
      showAuthForms();
    });
  }

  function setupAddressForm() {
    const form = document.getElementById("addressForm");
    const note = document.getElementById("addressNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      try {
        await api("/api/account/addresses", {
          method: "POST",
          body: JSON.stringify({
            label: document.getElementById("addrLabel").value.trim(),
            addressLine: document.getElementById("addrLine").value.trim(),
            city: document.getElementById("addrCity").value.trim(),
            state: document.getElementById("addrState").value.trim(),
            pinCode: document.getElementById("addrPin").value.trim(),
            phone: document.getElementById("addrPhone").value.trim(),
          }),
        });
        form.reset();
        note.textContent = "Address saved.";
        await loadAddresses();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      }
    });
  }

  async function refreshAuthState() {
    if (!getToken()) {
      showAuthForms();
      return;
    }
    try {
      await api("/api/auth/me");
      await showDashboard();
    } catch (error) {
      // Stored token is stale/expired — fall back to the sign-in forms
      // rather than showing a broken dashboard.
      clearSession();
      showAuthForms();
    }
  }

  function showAuthForms() {
    document.getElementById("accountHeading").textContent = "My Account";
    document.getElementById("accountAuth").hidden = false;
    document.getElementById("accountDashboard").hidden = true;
  }

  async function showDashboard() {
    const name = localStorage.getItem(nameKey) || "";
    const email = localStorage.getItem(emailKey) || "";
    document.getElementById("accountHeading").textContent = `Hi, ${name.split(" ")[0] || "there"}`;
    document.getElementById("accountName").textContent = name;
    document.getElementById("accountEmail").textContent = email;
    document.getElementById("accountAuth").hidden = true;
    document.getElementById("accountDashboard").hidden = false;
    await Promise.all([loadAddresses(), loadOrders()]);
  }

  async function loadAddresses() {
    const list = document.getElementById("addressList");
    if (!list) return;
    try {
      const addresses = await api("/api/account/addresses");
      list.innerHTML = addresses.length
        ? addresses.map(addressCardTemplate).join("")
        : '<p class="reviews-empty">No saved addresses yet — add one below.</p>';
      list.querySelectorAll("[data-delete-address]").forEach((button) => {
        button.addEventListener("click", async () => {
          button.disabled = true;
          try {
            await api(`/api/account/addresses/${button.dataset.deleteAddress}`, { method: "DELETE" });
            await loadAddresses();
          } catch (error) {
            button.disabled = false;
          }
        });
      });
    } catch (error) {
      list.innerHTML = '<p class="reviews-empty">Could not load saved addresses right now.</p>';
    }
  }

  function addressCardTemplate(address) {
    return `
      <article class="address-card">
        <div>
          <strong>${escapeHtml(address.label)}</strong>
          <p>${escapeHtml(address.addressLine)}, ${escapeHtml(address.city)}, ${escapeHtml(address.state)} ${escapeHtml(address.pinCode)}</p>
          <p>${escapeHtml(address.phone)}</p>
        </div>
        <button type="button" class="secondary-btn compact" data-delete-address="${address.id}">Remove</button>
      </article>
    `;
  }

  async function loadOrders() {
    const list = document.getElementById("orderHistoryList");
    if (!list) return;
    try {
      const orders = await api("/api/account/orders");
      list.innerHTML = orders.length
        ? orders.map(orderCardTemplate).join("")
        : '<p class="reviews-empty">No orders yet — your order history will show up here once you\'ve placed one.</p>';
    } catch (error) {
      list.innerHTML = '<p class="reviews-empty">Could not load your order history right now.</p>';
    }
  }

  function orderCardTemplate(order) {
    const items = order.lines
      .map((line) => `${line.quantity} \u00d7 ${escapeHtml(line.productName)}`)
      .join(", ");
    const tracking =
      order.trackingCourier || order.trackingNumber
        ? `<p class="order-tracking">${escapeHtml(order.trackingCourier || "Courier")} \u2014 ${escapeHtml(order.trackingNumber || "")}${
            order.trackingUrl ? ` \u00b7 <a href="${escapeHtml(order.trackingUrl)}" target="_blank" rel="noopener">Track</a>` : ""
          }</p>`
        : "";
    return `
      <article class="order-card">
        <div class="order-card-head">
          <strong>Order #${order.id}</strong>
          <span class="order-status order-status-${escapeHtml(order.status.toLowerCase())}">${escapeHtml(order.status)}</span>
        </div>
        <p class="order-items">${items}</p>
        ${tracking}
        <div class="order-card-foot">
          <span>${formatDate(order.createdAt)}</span>
          <strong>${formatMoney(order.total)}</strong>
        </div>
      </article>
    `;
  }
});
