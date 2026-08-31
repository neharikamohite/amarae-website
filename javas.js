window.addEventListener("load", () => {
  const apiBase = "";
  const sessionKey = "amaraeSessionId";
  const wishlistKey = "amaraeWishlist";
  const sessionId = getSessionId();
  let activeProducts = [];
  let activeFilter = "all";
  let activeSearch = "";

  const fallbackProducts = [
    perfume(101, "Crown Voyage", "fresh", "Bergamot, green apple, lime, and blackcurrant open into a bold, boundless trail. Amaraè's signature travel-ready scent.", 1499, "assets/crown-voyage.jpg", 40),
    perfume(102, "Wild Sovereign", "woody", "Citrus bergamot and lavender settle into warm amber woods, wild, untamed, and free.", 1499, "assets/wild-sovereign.jpg", 35),
    perfume(103, "Royal White Oud", "luxury", "Saffron and rose petals wrapped around smooth white oud, for an opulent, refined, timeless evening trail.", 1499, "assets/royal-white-oud.jpg", 30),
    perfume(104, "Golden Liberté", "warm", "Orange blossom, lavender, and Madagascar vanilla for a poignant, sensual warmth that lingers.", 1499, "assets/golden-liberte.jpg", 32),
    perfume(105, "Blooming Élise", "floral", "Pink rose and soft petals for a graceful, blooming floral signature.", 1499, "assets/blooming-elise.jpg", 38),
    perfume(106, "Crystal Ember", "luxury", "Saffron threads, white flowers, and warm sandalwood for a radiant, addictive glow.", 1499, "assets/crystal-ember.jpg", 28),
  ];

  moveCursorAura();
  initOrbitGallery();
  initQuiz();
  initShop();
  initRealtime();

  function perfume(id, name, category, description, price, imageUrl, stock) {
    return { id, name, category, description, price, imageUrl, stock };
  }

  function getSessionId() {
    let value = localStorage.getItem(sessionKey);
    if (!value) {
      value = crypto.randomUUID ? crypto.randomUUID() : `guest-${Date.now()}`;
      localStorage.setItem(sessionKey, value);
    }
    return value;
  }

  function headers() {
    return {
      "Content-Type": "application/json",
      "X-Aether-Session": sessionId,
    };
  }

  async function api(path, options = {}) {
    const response = await fetch(`${apiBase}${path}`, {
      ...options,
      headers: { ...headers(), ...(options.headers || {}) },
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.error || "Something went wrong");
    }

    return response.status === 204 ? null : response.json();
  }

  function formatMoney(value) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0,
    }).format(Number(value));
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function moveCursorAura() {
    const aura = document.querySelector(".cursor-aura");
    if (!aura) return;
    window.addEventListener("mousemove", (event) => {
      aura.style.transform = `translate3d(${event.clientX}px, ${event.clientY}px, 0)`;
    });
  }

  function initOrbitGallery() {
    const items = document.querySelectorAll(".orbit-item");
    const video = document.getElementById("active-video");
    const scentDisplay = document.getElementById("scent-display");
    let currentIndex = 0;
    let autoSlideTimer;

    if (items.length === 0) return;

    function updateGallery(index) {
      items.forEach((item, i) => item.classList.toggle("active", i === index));
      if (scentDisplay) {
        scentDisplay.style.opacity = "0";
        setTimeout(() => {
          scentDisplay.textContent = items[index].dataset.name || "";
          scentDisplay.style.opacity = "1";
        }, 220);
      }

      if (video && items[index].dataset.vid) {
        video.classList.add("slide-out");
        setTimeout(() => {
          video.src = items[index].dataset.vid;
          video.load();
          video.classList.remove("slide-out");
          video.classList.add("slide-in-start");
          void video.offsetWidth;
          video.classList.remove("slide-in-start");
          video.classList.add("slide-in");
          setTimeout(() => video.classList.remove("slide-in"), 650);
        }, 420);
      }
      currentIndex = index;
    }

    function startAutoSlide() {
      clearInterval(autoSlideTimer);
      autoSlideTimer = setInterval(() => updateGallery((currentIndex + 1) % items.length), 4600);
    }

    items.forEach((item, i) => {
      item.addEventListener("click", () => {
        updateGallery(i);
        startAutoSlide();
      });
    });

    updateGallery(0);
    startAutoSlide();
  }

  function initQuiz() {
    const recs = {
      "Fresh & Easy": ["Crown Voyage", "Bergamot, green apple, lime, and blackcurrant for a bold, everyday-ready freshness."],
      "Soft & Romantic": ["Blooming Élise", "Pink rose and soft petals for a graceful, romantic trail."],
      "Bold & Confident": ["Royal White Oud", "Saffron, rose, and smooth white oud for confident evening wear."],
      "Warm & Cozy": ["Golden Liberté", "Orange blossom, lavender, and vanilla for a sensual, cozy warmth."],
      "Signature Scent": ["Crystal Ember", "Saffron, white flowers, and sandalwood for a radiant signature scent."],
    };

    document.querySelectorAll(".options").forEach((group) => {
      group.addEventListener("click", (event) => {
        const target = event.target.closest(".opt");
        if (!target) return;
        group.querySelectorAll(".opt").forEach((option) => option.classList.remove("selected"));
        target.classList.add("selected");
      });
    });

    document.getElementById("revealBtn")?.addEventListener("click", () => {
      const selectedMood = document.querySelector('[data-group="mood"] .selected');
      const selectedGoal = document.querySelector('[data-group="body"] .selected');
      const result = document.getElementById("result");
      const rTitle = document.getElementById("rTitle");
      const rDesc = document.getElementById("rDesc");
      const choice = selectedMood?.textContent.trim() || selectedGoal?.textContent.trim();
      const [name, desc] = recs[choice] || ["Wild Sovereign", "A balanced, versatile scent that feels easy, modern, and giftable."];

      rTitle.textContent = choice ? name : "Choose a mood first";
      rDesc.textContent = choice ? desc : "Pick at least one option to reveal your AMARAÈ perfume match.";
      result?.classList.add("show");
      result?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }

  async function initShop() {
    setupFilters();
    setupSearch();
    attachCheckout();
    await loadProducts();
    await loadCart();
  }

  async function loadProducts() {
    const productGrid = document.querySelector(".product-grid");
    if (!productGrid) return;

    try {
      activeProducts = await api("/api/products");
      setRealtimeStatus("Live store connected");
    } catch (error) {
      activeProducts = fallbackProducts;
      setRealtimeStatus("Preview mode - start backend for live database");
      showCheckoutNote("Preview catalog is showing because the backend is not running.");
    }
    renderProducts();
  }

  function renderProducts() {
    const productGrid = document.querySelector(".product-grid");
    if (!productGrid) return;

    const query = activeSearch.trim().toLowerCase();
    const products = activeProducts.filter((product) => {
      const categoryMatch = activeFilter === "all" || product.category === activeFilter;
      const searchText = `${product.name} ${product.category} ${product.description}`.toLowerCase();
      return categoryMatch && (!query || searchText.includes(query));
    });

    productGrid.innerHTML = products.length
      ? products.map(productCardTemplate).join("")
      : '<div class="empty-state">No perfume matched that search. Try rose, fresh, oud, warm, or vanilla.</div>';
    attachCartButtons();
    attachWishlistButtons();
  }

  function productCardTemplate(product) {
    const wished = getWishlist().includes(String(product.id));
    return `
      <article class="collection-card product-card fade-up" data-category="${escapeHtml(product.category)}">
        <button class="wish-btn ${wished ? "active" : ""}" type="button" data-product-id="${product.id}" aria-label="Save ${escapeHtml(product.name)}">♡</button>
        <img src="${escapeHtml(product.imageUrl)}" class="bottle-png" alt="${escapeHtml(product.name)}" />
        <div class="product-meta">
          <span>${categoryLabel(product.category)}</span>
          <strong>${formatMoney(product.price)}</strong>
        </div>
        <h3>${escapeHtml(product.name)}</h3>
        <p>${escapeHtml(product.description)}</p>
        <div class="product-details"><span>100 ml · Eau de Parfum</span><span>Notes: ${escapeHtml(productNotes(product.name))}</span></div>
        <div class="product-promises">
          <span>${product.stock > 20 ? "In stock" : "Limited stock"}</span>
          <span>MRP ${formatMoney(product.price)}</span>
        </div>
        <button class="add-cart" data-product-id="${product.id}" ${product.stock > 0 ? "" : "disabled"}>
          ${product.stock > 0 ? "Add to cart" : "Sold out"}
        </button>
      </article>
    `;
  }

  function productNotes(name) {
    const notes = {
      "Crown Voyage": "bergamot · green apple · lime · blackcurrant",
      "Wild Sovereign": "bergamot · lavender · amber woods",
      "Royal White Oud": "saffron · rose · white oud",
      "Golden Liberté": "orange blossom · lavender · vanilla",
      "Blooming Élise": "pink rose · soft petals",
      "Crystal Ember": "saffron · white flowers · sandalwood",
    };
    return notes[name] || "details coming soon";
  }

  function categoryLabel(category) {
    const labels = {
      fresh: "Fresh",
      floral: "Floral",
      warm: "Warm",
      woody: "Woody",
      luxury: "Luxury",
      gourmand: "Sweet",
      fragrance: "Perfume",
      face: "Face",
      hair: "Hair",
      body: "Body",
    };
    return labels[category] || category;
  }

  function setupFilters() {
    document.querySelectorAll(".filter-chip").forEach((button) => {
      button.onclick = () => {
        activeFilter = button.dataset.filter;
        document.querySelectorAll(".filter-chip").forEach((chip) => chip.classList.remove("active"));
        button.classList.add("active");
        renderProducts();
      };
    });
  }

  function setupSearch() {
    document.getElementById("productSearch")?.addEventListener("input", (event) => {
      activeSearch = event.target.value;
      renderProducts();
    });
  }

  function attachCartButtons() {
    document.querySelectorAll(".add-cart").forEach((button) => {
      button.onclick = async () => {
        try {
          button.disabled = true;
          await api("/api/cart/items", {
            method: "POST",
            body: JSON.stringify({ productId: Number(button.dataset.productId), quantity: 1 }),
          });
          await loadCart();
          button.textContent = "Added";
        } catch (error) {
          showCheckoutNote("Start the Spring Boot backend to use live cart and checkout.");
        } finally {
          setTimeout(() => {
            button.disabled = false;
            button.textContent = "Add to cart";
          }, 900);
        }
      };
    });
  }

  async function loadCart() {
    const cartCountEls = document.querySelectorAll(".cart-count");
    const cartItemsEl = document.getElementById("cartItems");
    const cartTotalEl = document.getElementById("cartTotal");

    try {
      const cart = await api("/api/cart");
      const count = cart.items.reduce((sum, item) => sum + item.quantity, 0);
      cartCountEls.forEach((el) => {
        el.textContent = count;
      });

      if (!cartItemsEl || !cartTotalEl) return;
      if (cart.items.length === 0) {
        cartItemsEl.innerHTML = '<div class="cart-empty">Your cart is empty. Add a perfume from the launch collection.</div>';
        cartTotalEl.textContent = formatMoney(0);
        updateLaunchOffer([]);
        return;
      }

      cartItemsEl.innerHTML = cart.items.map(cartRowTemplate).join("");
      cartTotalEl.textContent = formatMoney(cart.total);
      updateLaunchOffer(cart.items);
      attachCartRowButtons();
    } catch (error) {
      cartCountEls.forEach((el) => {
        el.textContent = "0";
      });
      if (cartItemsEl) {
        cartItemsEl.innerHTML = '<div class="cart-empty">Live cart appears here after the backend starts.</div>';
      }
      if (cartTotalEl) cartTotalEl.textContent = formatMoney(0);
    }
  }

  function updateLaunchOffer(items) {
    const offer = document.getElementById("cartOffer");
    const select = document.getElementById("complimentaryMini");
    if (!offer) return;
    const eligible = items.some((item) => item.quantity > 0);
    offer.textContent = eligible
      ? "Launch offer unlocked: choose a different 10 ml fragrance at checkout. The complimentary mini will be confirmed with your order."
      : "Add a 100 ml fragrance to unlock your complimentary different 10 ml scent.";
    if (select) {
      select.disabled = !eligible;
      const cartProductIds = new Set(items.map((item) => String(item.productId)));
      const options = activeProducts
        .filter((product) => !cartProductIds.has(String(product.id)) && product.stock > 0)
        .map((product) => `<option value="${product.id}">${escapeHtml(product.name)} — complimentary 10 ml</option>`)
        .join("");
      select.innerHTML = eligible
        ? `<option value="">Choose your complimentary mini</option>${options}`
        : '<option value="">Add a fragrance to unlock this selection</option>';
    }
  }

  function cartRowTemplate(item) {
    return `
      <div class="cart-row" data-product-id="${item.productId}">
        <img src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.name)}" />
        <div>
          <h4>${escapeHtml(item.name)}</h4>
          <span>${formatMoney(item.price)} each</span>
        </div>
        <div class="qty-controls" aria-label="Quantity controls">
          <button type="button" data-action="decrease" aria-label="Decrease ${escapeHtml(item.name)}">-</button>
          <strong>${item.quantity}</strong>
          <button type="button" data-action="increase" aria-label="Increase ${escapeHtml(item.name)}">+</button>
        </div>
        <button type="button" class="remove-item" data-action="remove">Remove</button>
      </div>
    `;
  }

  function attachCartRowButtons() {
    document.querySelectorAll(".cart-row button").forEach((button) => {
      button.onclick = async (event) => {
        const row = event.target.closest(".cart-row");
        const qty = Number(row.querySelector(".qty-controls strong").textContent);
        const action = button.dataset.action;
        const nextQty = action === "increase" ? qty + 1 : action === "decrease" ? qty - 1 : 0;

        await api(`/api/cart/items/${row.dataset.productId}`, {
          method: "PATCH",
          body: JSON.stringify({ quantity: nextQty }),
        });
        await loadCart();
      };
    });
  }

  function attachCheckout() {
    document.querySelector(".checkout-btn")?.addEventListener("click", async () => {
      const payload = {
        sessionId,
        customerName: document.getElementById("customerName")?.value.trim(),
        email: document.getElementById("customerEmail")?.value.trim(),
        deliveryCity: document.getElementById("deliveryCity")?.value.trim(),
        complimentaryMiniProductId: Number(document.getElementById("complimentaryMini")?.value) || null,
      };

      if (!payload.complimentaryMiniProductId) {
        showCheckoutNote("Choose your complimentary different 10 ml fragrance before proceeding to payment.");
        return;
      }

      try {
        const order = await api("/api/orders/checkout", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        await loadCart();
        showCheckoutNote(`Order #${order.id} is awaiting payment. Your cart will be kept until payment is verified.`);
        if (order.paymentUrl) window.location.assign(order.paymentUrl);
      } catch (error) {
        showCheckoutNote(error.message);
      }
    });
  }

  function getWishlist() {
    return JSON.parse(localStorage.getItem(wishlistKey) || "[]");
  }

  function attachWishlistButtons() {
    document.querySelectorAll(".wish-btn").forEach((button) => {
      button.onclick = () => {
        const id = String(button.dataset.productId);
        const wishlist = getWishlist();
        const next = wishlist.includes(id) ? wishlist.filter((item) => item !== id) : [...wishlist, id];
        localStorage.setItem(wishlistKey, JSON.stringify(next));
        button.classList.toggle("active", next.includes(id));
      };
    });
  }

  function showCheckoutNote(message) {
    const checkoutNote = document.getElementById("checkoutNote");
    if (checkoutNote) checkoutNote.textContent = message;
  }

  function setRealtimeStatus(message) {
    const status = document.getElementById("realtimeStatus");
    if (status) status.textContent = message;
  }

  function initRealtime() {
    if (!window.EventSource) return;
    try {
      const stream = new EventSource("/api/realtime/stream");
      stream.onopen = () => setRealtimeStatus("Live store connected");
      stream.onerror = () => setRealtimeStatus("Preview mode - backend offline");
      stream.addEventListener("products", loadProducts);
      stream.addEventListener("cart", loadCart);
      stream.addEventListener("orders", loadCart);
    } catch (error) {
      setRealtimeStatus("Preview mode - backend offline");
    }
  }
});
