window.addEventListener("load", () => {
  const tokenKey = "amaraeAuthToken";
  const nameKey = "amaraeAuthName";
  const emailKey = "amaraeAuthEmail";
  let orderHistoryData = [];

  // Where UPI payments actually land — keep this in sync with the same
  // constant in javas.js if it ever changes.
  const upiPayeeId = "neharikamohite@okhdfcbank";
  const upiPayeeName = "AMARAE Formulations";

  init();

  function init() {
    setupTabs();
    setupLoginForm();
    setupSignupForm();
    setupForgotPasswordForm();
    setupResetPasswordForm();
    setupAddressForm();
    setupLogout();
    initOrderDetailModal();

    // A password-reset email link lands here with ?resetToken=... —
    // that takes priority over the normal signed-in/signed-out check.
    const resetToken = new URLSearchParams(window.location.search).get("resetToken");
    if (resetToken) {
      document.getElementById("accountHeading").textContent = "Reset your password";
      document.getElementById("accountAuth").hidden = false;
      document.getElementById("accountDashboard").hidden = true;
      showAuthForm("reset");
      return;
    }

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
    document.querySelectorAll(".auth-switch-link").forEach((link) => {
      link.addEventListener("click", () => {
        showAuthForm(link.dataset.tab);
      });
    });
  }

  function showAuthForm(name) {
    const forms = { login: "loginForm", signup: "signupForm", forgot: "forgotPasswordForm", reset: "resetPasswordForm" };
    Object.entries(forms).forEach(([key, id]) => {
      const form = document.getElementById(id);
      if (form) form.hidden = key !== name;
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
        await redirectAfterAuth();
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
        await redirectAfterAuth();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      }
    });
  }

  function setupForgotPasswordForm() {
    const form = document.getElementById("forgotPasswordForm");
    const note = document.getElementById("forgotNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      const submitBtn = form.querySelector("button[type=submit]");
      submitBtn.disabled = true;
      try {
        await api("/api/auth/forgot-password", {
          method: "POST",
          body: JSON.stringify({ email: document.getElementById("forgotEmail").value.trim() }),
        });
        // Deliberately the same message whether or not that email has an
        // account — matches the backend's account-enumeration protection.
        note.textContent = "If an account exists with that email, we've sent a password reset link.";
        form.reset();
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  function setupResetPasswordForm() {
    const form = document.getElementById("resetPasswordForm");
    const note = document.getElementById("resetNote");
    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      note.textContent = "";
      note.classList.remove("error");
      const submitBtn = form.querySelector("button[type=submit]");
      submitBtn.disabled = true;
      try {
        const token = new URLSearchParams(window.location.search).get("resetToken");
        await api("/api/auth/reset-password", {
          method: "POST",
          body: JSON.stringify({ token, newPassword: document.getElementById("resetPassword").value }),
        });
        note.textContent = "Password updated — you can now sign in with your new password.";
        form.reset();
        form.hidden = true;
        // Clean the token out of the URL and drop back to a normal sign-in.
        window.history.replaceState({}, "", "account.html");
        document.getElementById("accountHeading").textContent = "My Account";
        showAuthForm("login");
      } catch (error) {
        note.textContent = error.message;
        note.classList.add("error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  // Sent here from checkout with ?next=checkout when they weren't signed
  // in — after successfully signing in/up, send them straight back to the
  // cart instead of parking them on the account dashboard.
  async function redirectAfterAuth() {
    const params = new URLSearchParams(window.location.search);
    if (params.get("next") === "checkout") {
      window.location.href = "collections.html#cart";
      return;
    }
    await showDashboard();
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
    await Promise.all([loadAddresses(), loadOrders(), loadWishlist()]);
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
      orderHistoryData = orders;
      list.innerHTML = orders.length
        ? orders.map(orderCardTemplate).join("")
        : '<p class="reviews-empty">No orders yet — your order history will show up here once you\'ve placed one.</p>';
      attachOrderCardHandlers();
    } catch (error) {
      list.innerHTML = '<p class="reviews-empty">Could not load your order history right now.</p>';
    }
  }

  function attachOrderCardHandlers() {
    document.querySelectorAll(".order-card[data-order-id]").forEach((card) => {
      card.addEventListener("click", () => {
        const order = orderHistoryData.find((item) => String(item.id) === card.dataset.orderId);
        if (order) showOrderDetailModal(order);
      });
    });
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
    const isPending = order.status === "CREATED" || order.status === "PAYMENT_PENDING";
    return `
      <article class="order-card" data-order-id="${order.id}" role="button" tabindex="0">
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
        <p class="order-card-hint">${isPending ? "Tap to complete payment" : "Tap for details"}</p>
      </article>
    `;
  }

  function buildUpiPaymentUrl(order) {
    // Same encoding rules as javas.js's version — only spaces become
    // %20, "@" stays bare, since that's what UPI apps actually expect.
    const encodeUpiValue = (value) => String(value).replace(/ /g, "%20");
    const params = [
      ["pa", upiPayeeId],
      ["pn", upiPayeeName],
      ["am", Number(order.total).toFixed(2)],
      ["cu", "INR"],
      ["tn", `AMARAE Order ${order.id}`],
    ]
      .map(([key, value]) => `${key}=${encodeUpiValue(value)}`)
      .join("&");
    return `upi://pay?${params}`;
  }

  function buildResumeWhatsAppUrl(order) {
    const message = [
      "Hi! I'd like to confirm payment for my AMARAE order.",
      "",
      `Order #${order.id}`,
      `Total: ${formatMoney(order.total)}`,
      "",
      "I've paid via the QR code on the site — please confirm.",
    ].join("\n");
    return `https://wa.me/919579222532?text=${encodeURIComponent(message)}`;
  }

  function showOrderDetailModal(order) {
    const modal = document.getElementById("orderDetailModal");
    const body = document.getElementById("orderDetailBody");
    if (!modal || !body) return;

    const items = order.lines
      .map((line) => `<li>${line.quantity} &times; ${escapeHtml(line.productName)} (${line.sizeMl}ml)</li>`)
      .join("");
    const isPending = order.status === "CREATED" || order.status === "PAYMENT_PENDING";
    const tracking =
      order.trackingCourier || order.trackingNumber
        ? `<p class="order-confirmed-id">${escapeHtml(order.trackingCourier || "Courier")} \u2014 ${escapeHtml(order.trackingNumber || "")}${
            order.trackingUrl ? ` \u00b7 <a href="${escapeHtml(order.trackingUrl)}" target="_blank" rel="noopener">Track</a>` : ""
          }</p>`
        : "";

    const statusMessages = {
      PAID: "Payment received \u2014 we'll pack and ship your order soon.",
      SHIPPED: "Your order is on its way.",
      DELIVERED: "This order has been delivered.",
      CANCELLED: "This order was cancelled.",
      REFUNDED: "This order was refunded.",
      FAILED: "This order could not be completed.",
    };

    if (isPending) {
      const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(buildUpiPaymentUrl(order))}`;
      body.innerHTML = `
        <div class="order-confirmed">
          <h2>Complete your payment</h2>
          <p class="order-confirmed-id">Order #${order.id}</p>
          <ul class="order-confirmed-items">${items}</ul>
          <div class="order-confirmed-total"><span>Total due</span><strong>${formatMoney(order.total)}</strong></div>
          <div class="order-confirmed-qr">
            <img src="${qrImageUrl}" alt="Scan to pay ${formatMoney(order.total)} via UPI" width="200" height="200" />
            <p>Scan with any UPI app (GPay, PhonePe, Paytm...)</p>
            <p class="order-confirmed-upi-id">or pay manually to: <strong>${escapeHtml(upiPayeeId)}</strong></p>
          </div>
          <button type="button" class="primary-btn order-confirmed-paid" id="orderDetailPaid" data-order-id="${order.id}">I've Paid</button>
          <p class="order-confirmed-paid-note" id="orderDetailPaidNote"></p>
          <a class="secondary-btn order-confirmed-whatsapp" href="${buildResumeWhatsAppUrl(order)}" target="_blank" rel="noopener">Message us on WhatsApp</a>
        </div>
      `;
      document.getElementById("orderDetailPaid")?.addEventListener("click", async (event) => {
        const button = event.currentTarget;
        const note = document.getElementById("orderDetailPaidNote");
        button.disabled = true;
        button.textContent = "Marking\u2026";
        try {
          await api(`/api/orders/${button.dataset.orderId}/claim-paid`, { method: "PATCH" });
          button.textContent = "\u2713 Marked as paid";
          if (note) note.textContent = "Thanks \u2014 we'll confirm and start packing your order shortly.";
        } catch (error) {
          button.disabled = false;
          button.textContent = "I've Paid";
          if (note) {
            note.textContent = "Couldn't reach the server \u2014 message us on WhatsApp instead.";
            note.classList.add("error");
          }
        }
      });
    } else {
      body.innerHTML = `
        <div class="order-confirmed">
          <h2>Order #${order.id}</h2>
          <p class="order-confirmed-id">${escapeHtml(statusMessages[order.status] || order.status)}</p>
          <ul class="order-confirmed-items">${items}</ul>
          <div class="order-confirmed-total"><span>Total</span><strong>${formatMoney(order.total)}</strong></div>
          ${tracking}
        </div>
      `;
    }

    modal.classList.add("open");
    document.body.classList.add("modal-open");
  }

  function closeOrderDetailModal() {
    const modal = document.getElementById("orderDetailModal");
    if (!modal) return;
    modal.classList.remove("open");
    document.body.classList.remove("modal-open");
  }

  function initOrderDetailModal() {
    const modal = document.getElementById("orderDetailModal");
    if (!modal) return;
    modal.querySelector(".modal-backdrop")?.addEventListener("click", closeOrderDetailModal);
    document.getElementById("orderDetailClose")?.addEventListener("click", closeOrderDetailModal);
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") closeOrderDetailModal();
    });
  }

  async function loadWishlist() {
    const grid = document.getElementById("wishlistGrid");
    if (!grid) return;
    try {
      const products = await api("/api/account/wishlist");
      grid.innerHTML = products.length
        ? products.map(wishlistCardTemplate).join("")
        : '<p class="reviews-empty">Nothing saved yet — tap the heart on any product to add it here.</p>';
      grid.querySelectorAll("[data-remove-wishlist]").forEach((button) => {
        button.addEventListener("click", async () => {
          const productId = button.dataset.removeWishlist;
          button.disabled = true;
          try {
            await api(`/api/account/wishlist/${productId}`, { method: "DELETE" });
            // Keep the guest-wishlist heart state (used on collections.html)
            // in sync too, so it doesn't still show as saved there.
            try {
              const local = JSON.parse(localStorage.getItem("amaraeWishlist") || "[]");
              localStorage.setItem("amaraeWishlist", JSON.stringify(local.filter((id) => id !== String(productId))));
            } catch (storageError) {
              // Non-fatal — worst case the heart just stays active until next sync.
            }
            await loadWishlist();
          } catch (error) {
            button.disabled = false;
          }
        });
      });
    } catch (error) {
      grid.innerHTML = '<p class="reviews-empty">Could not load your wishlist right now.</p>';
    }
  }

  function wishlistCardTemplate(product) {
    return `
      <article class="wishlist-card">
        <img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" />
        <strong>${escapeHtml(product.name)}</strong>
        <span>${formatMoney(product.price)}</span>
        <div class="wishlist-card-actions">
          <a href="collections.html">View</a>
          <button type="button" data-remove-wishlist="${product.id}">Remove</button>
        </div>
      </article>
    `;
  }
});
