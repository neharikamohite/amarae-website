window.addEventListener("load", () => {
  const apiBase = "";
  const sessionKey = "amaraeSessionId";
  const wishlistKey = "amaraeWishlist";
  const sessionId = getSessionId();
  let activeProducts = [];
  let activeFilter = "all";
  let activeSearch = "";

  const fallbackProducts = [
    perfume(101, "Crown Voyage", "fresh", "Bergamot, green apple, lime, and blackcurrant open into a bold, boundless trail. Amaraè's signature travel-ready scent.", 1499, "assets/crown-voyage.jpg", 40, 100),
    perfume(102, "Wild Sovereign", "woody", "Citrus bergamot and lavender settle into warm amber woods, wild, untamed, and free.", 1499, "assets/wild-sovereign.jpg", 35, 100),
    perfume(103, "Royal White Oud", "luxury", "Saffron and rose petals wrapped around smooth white oud, for an opulent, refined, timeless evening trail.", 1499, "assets/royal-white-oud.jpg", 30, 100),
    perfume(104, "Golden Liberté", "warm", "Orange blossom, lavender, and Madagascar vanilla for a poignant, sensual warmth that lingers.", 1499, "assets/golden-liberte.jpg", 32, 100),
    perfume(105, "Blooming Élise", "floral", "Pink rose and soft petals for a graceful, blooming floral signature.", 1499, "assets/blooming-elise.jpg", 38, 100),
    perfume(106, "Crystal Ember", "luxury", "Saffron threads, white flowers, and warm sandalwood for a radiant, addictive glow.", 1499, "assets/crystal-ember.jpg", 28, 100),
    perfume(107, "Crown Voyage · 10 ml", "fresh", "The same bold Crown Voyage trail of bergamot, green apple, lime, and blackcurrant, in a travel-ready 10 ml bottle.", 399, "assets/crown-voyage-10ml.jpg", 60, 10),
    perfume(108, "Blooming Élise · 10 ml", "floral", "The same graceful Blooming Élise pink rose and soft petals, in a travel-ready 10 ml bottle.", 399, "assets/blooming-elise-10ml.jpg", 60, 10),
    perfume(109, "Double Apple · 10 ml", "gourmand", "Crisp red and green apple layered over warm spice and a soft tobacco-leaf base. A juicy, sweet signature in a travel-ready 10 ml bottle.", 349, "assets/double-apple-10ml.jpg", 55, 10),
    perfume(110, "Grapemint · 10 ml", "fresh", "Sun-ripened green grapes brightened with cool mint and a whisper of citrus. A crisp, fruity signature in a travel-ready 10 ml bottle.", 349, "assets/grapemint-10ml.jpg", 55, 10),
  ];

  moveCursorAura();
  initOrbitGallery();
  initQuiz();
  initShop();
  initRealtime();
  initProductModal();

  function perfume(id, name, category, description, price, imageUrl, stock, sizeMl) {
    return { id, name, category, description, price, imageUrl, stock, sizeMl };
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

      const nextVid = items[index].dataset.vid;
      const resolvedNext = nextVid ? new URL(nextVid, document.baseURI).href : null;
      // Only reload the <video> element when the target clip actually changes.
      // Reloading on every rotation (even to the same clip) was restarting
      // playback from 0 each time, so the video never played past its first
      // few seconds. Comparing against currentSrc lets the same clip keep
      // playing continuously through its full length while the showcase
      // label/thumbnail still rotates normally.
      if (video && resolvedNext && video.currentSrc !== resolvedNext) {
        video.classList.add("slide-out");
        setTimeout(() => {
          video.src = nextVid;
          video.load();
          video.play().catch(() => {});
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
    attachCardOpenHandlers();
  }

  // Card shows only what's needed to browse and compare at a glance: image,
  // name, category, and price. Full description, notes, stock, and reviews
  // live on the detail view so they're not dumped straight into the grid.
  function productCardTemplate(product) {
    const wished = getWishlist().includes(String(product.id));
    return `
      <article class="collection-card product-card fade-up" data-category="${escapeHtml(product.category)}" data-product-id="${product.id}" tabindex="0" role="button" aria-label="View details for ${escapeHtml(product.name)}">
        <button class="wish-btn ${wished ? "active" : ""}" type="button" data-product-id="${product.id}" aria-label="Save ${escapeHtml(product.name)}">♡</button>
        <span class="size-pill">${product.sizeMl} ML</span>
        <img src="${escapeHtml(product.imageUrl)}" class="bottle-png" alt="${escapeHtml(product.name)}" />
        <div class="product-meta">
          <span>${categoryLabel(product.category)}</span>
          <strong>${formatMoney(product.price)}</strong>
        </div>
        <h3>${escapeHtml(product.name)}</h3>
        <div class="card-rating">${cardRatingTemplate(product)}</div>
        <p class="card-hint">Tap to view full details, notes, and reviews</p>
        <button class="add-cart" data-product-id="${product.id}" ${product.stock > 0 ? "" : "disabled"}>
          ${product.stock > 0 ? "Add to cart" : "Sold out"}
        </button>
      </article>
    `;
  }

  function cardRatingTemplate(product) {
    const count = product.reviewCount || 0;
    if (count === 0) return `<span class="rating-pill muted">No ratings yet</span>`;
    const avg = product.avgRating || 0;
    return `<span class="rating-pill">★ ${avg.toFixed(1)} <em>(${count})</em></span>`;
  }

  function attachCardOpenHandlers() {
    document.querySelectorAll(".product-card").forEach((card) => {
      const open = (event) => {
        if (event.target.closest(".wish-btn") || event.target.closest(".add-cart")) return;
        openProductModal(card.dataset.productId);
      };
      card.addEventListener("click", open);
      card.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          open(event);
        }
      });
    });
  }

  function openProductModal(productId) {
    const product = activeProducts.find((item) => String(item.id) === String(productId));
    const modal = document.getElementById("productModal");
    const modalBody = document.getElementById("productModalBody");
    if (!product || !modal || !modalBody) return;

    const wished = getWishlist().includes(String(product.id));
    modalBody.innerHTML = `
      <div class="modal-image-stage">
        <img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" />
        <button class="wish-btn modal-wish ${wished ? "active" : ""}" type="button" data-product-id="${product.id}" aria-label="Save ${escapeHtml(product.name)}">♡</button>
      </div>
      <div class="modal-details">
        <div class="modal-top-row">
          <span class="modal-category">${categoryLabel(product.category)}</span>
        </div>
        <h2>${escapeHtml(product.name)}</h2>
        <div class="modal-rating">${ratingTemplate(product)}</div>
        <strong class="modal-price">${formatMoney(product.price)}</strong>
        <p class="modal-description">${escapeHtml(product.description)}</p>
        <div class="modal-fact-grid">
          <div><span>Size</span><strong>${product.sizeMl} ml · Eau de Parfum</strong></div>
          <div><span>Notes</span><strong>${escapeHtml(productNotes(product.name))}</strong></div>
          <div><span>Availability</span><strong>${product.stock > 20 ? "In stock" : product.stock > 0 ? "Limited stock" : "Sold out"}</strong></div>
          <div><span>MRP</span><strong>${formatMoney(product.price)}</strong></div>
        </div>
        <button class="primary-btn modal-add-cart" data-product-id="${product.id}" ${product.stock > 0 ? "" : "disabled"}>
          ${product.stock > 0 ? "Add to cart" : "Sold out"}
        </button>
        <section class="modal-reviews" id="modalReviews" data-product-id="${product.id}">
          <h3>Ratings &amp; Reviews</h3>
          <div class="reviews-list" id="reviewsList"><p class="modal-reviews-empty">Loading reviews...</p></div>
          ${reviewFormTemplate()}
        </section>
      </div>
    `;

    modal.classList.add("open");
    document.body.classList.add("modal-open");
    attachCartButtons();
    attachWishlistButtons();
    attachReviewForm(product.id);
    loadReviews(product.id);

    modalBody.querySelector(".modal-add-cart")?.addEventListener("click", () => {
      document.querySelector(`.add-cart[data-product-id="${product.id}"]`)?.click();
    });
  }

  function reviewFormTemplate() {
    return `
      <form class="review-form" id="reviewForm" enctype="multipart/form-data">
        <p class="review-form-title">Share your rating</p>
        <div class="star-picker" id="starPicker" role="radiogroup" aria-label="Choose a rating">
          ${[1, 2, 3, 4, 5]
            .map(
              (n) =>
                `<button type="button" class="star-pick" data-value="${n}" aria-label="${n} star${n === 1 ? "" : "s"}">☆</button>`
            )
            .join("")}
        </div>
        <input type="text" id="reviewName" placeholder="Your name" aria-label="Your name" required />
        <textarea id="reviewComment" placeholder="Tell other shoppers what you think (optional)" aria-label="Your review" rows="3"></textarea>
        <label class="review-upload-label" for="reviewFiles">Add photos or a video (optional, up to 6 files)</label>
        <input type="file" id="reviewFiles" accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime" multiple />
        <button type="button" class="secondary-btn compact" id="reviewSubmitBtn">Post review</button>
        <p class="review-form-note" id="reviewFormNote"></p>
      </form>
    `;
  }

  function reviewMediaTemplate(item) {
    return (item.media || [])
      .map((m) =>
        m.mediaType === "VIDEO"
          ? `<video src="${escapeHtml(m.url)}" class="review-media-item" controls muted></video>`
          : `<img src="${escapeHtml(m.url)}" class="review-media-item" alt="Customer photo" />`
      )
      .join("");
  }

  function reviewCardTemplate(item) {
    const stars = "★".repeat(item.rating) + "☆".repeat(5 - item.rating);
    const date = item.createdAt ? new Date(item.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "";
    return `
      <article class="review-card">
        <div class="review-card-head">
          <strong>${escapeHtml(item.customerName)}</strong>
          <span class="stars filled" aria-hidden="true">${stars}</span>
        </div>
        ${date ? `<span class="review-date">${date}</span>` : ""}
        ${item.comment ? `<p class="review-comment">${escapeHtml(item.comment)}</p>` : ""}
        ${item.media && item.media.length ? `<div class="review-media">${reviewMediaTemplate(item)}</div>` : ""}
      </article>
    `;
  }

  async function loadReviews(productId) {
    const list = document.getElementById("reviewsList");
    if (!list) return;
    try {
      const reviews = await api(`/api/products/${productId}/reviews`);
      list.innerHTML = reviews.length
        ? reviews.map(reviewCardTemplate).join("")
        : '<p class="modal-reviews-empty">This is a new launch fragrance, so there are no customer reviews yet. Be the first to try it and share your rating.</p>';
    } catch (error) {
      list.innerHTML = '<p class="modal-reviews-empty">Reviews will appear here once the live store is connected.</p>';
    }
  }

  function attachReviewForm(productId) {
    let selectedRating = 0;
    const picker = document.getElementById("starPicker");
    const stars = picker ? Array.from(picker.querySelectorAll(".star-pick")) : [];

    function paintStars(value) {
      stars.forEach((btn) => {
        const on = Number(btn.dataset.value) <= value;
        btn.textContent = on ? "★" : "☆";
        btn.classList.toggle("active", on);
      });
    }

    stars.forEach((btn) => {
      btn.addEventListener("click", () => {
        selectedRating = Number(btn.dataset.value);
        paintStars(selectedRating);
      });
    });

    document.getElementById("reviewSubmitBtn")?.addEventListener("click", async () => {
      const note = document.getElementById("reviewFormNote");
      const name = document.getElementById("reviewName")?.value.trim();
      const comment = document.getElementById("reviewComment")?.value.trim();
      const fileInput = document.getElementById("reviewFiles");
      const files = fileInput?.files ? Array.from(fileInput.files) : [];

      if (!name) {
        if (note) note.textContent = "Please enter your name.";
        return;
      }
      if (!selectedRating) {
        if (note) note.textContent = "Please choose a star rating.";
        return;
      }
      if (files.length > 6) {
        if (note) note.textContent = "You can attach up to 6 photos/videos.";
        return;
      }

      const submitBtn = document.getElementById("reviewSubmitBtn");
      submitBtn.disabled = true;
      submitBtn.textContent = "Posting...";

      const formData = new FormData();
      formData.append("customerName", name);
      formData.append("rating", String(selectedRating));
      if (comment) formData.append("comment", comment);
      files.forEach((file) => formData.append("files", file));

      try {
        const response = await fetch(`/api/products/${productId}/reviews`, {
          method: "POST",
          headers: { "X-Aether-Session": sessionId },
          body: formData,
        });
        if (!response.ok) {
          const body = await response.json().catch(() => ({}));
          throw new Error(body.error || "Could not post your review.");
        }
        if (note) note.textContent = "Thanks — your review is live.";
        await loadReviews(productId);
        await loadProducts();
      } catch (error) {
        if (note) note.textContent = error.message;
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Post review";
      }
    });
  }

  // Real stars + count when reviews exist; the original honest
  // "no ratings yet" placeholder still shows for products nobody has
  // reviewed.
  function ratingTemplate(product) {
    const count = product.reviewCount || 0;
    if (count === 0) {
      return `
        <span class="stars" aria-hidden="true">${"☆".repeat(5)}</span>
        <span class="rating-label">No ratings yet</span>
      `;
    }
    const avg = product.avgRating || 0;
    const filled = Math.round(avg);
    const stars = "★".repeat(filled) + "☆".repeat(5 - filled);
    return `
      <span class="stars filled" aria-hidden="true">${stars}</span>
      <span class="rating-label">${avg.toFixed(1)} · ${count} review${count === 1 ? "" : "s"}</span>
    `;
  }

  function closeProductModal() {
    const modal = document.getElementById("productModal");
    if (!modal) return;
    modal.classList.remove("open");
    document.body.classList.remove("modal-open");
  }

  function initProductModal() {
    const modal = document.getElementById("productModal");
    if (!modal) return;
    modal.querySelector(".modal-backdrop")?.addEventListener("click", closeProductModal);
    modal.querySelector(".modal-close")?.addEventListener("click", closeProductModal);
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") closeProductModal();
    });
  }

  function productNotes(name) {
    const notes = {
      "Crown Voyage": "bergamot · green apple · lime · blackcurrant",
      "Wild Sovereign": "bergamot · lavender · amber woods",
      "Royal White Oud": "saffron · rose · white oud",
      "Golden Liberté": "orange blossom · lavender · vanilla",
      "Blooming Élise": "pink rose · soft petals",
      "Crystal Ember": "saffron · white flowers · sandalwood",
      "Crown Voyage · 10 ml": "bergamot · green apple · lime · blackcurrant",
      "Blooming Élise · 10 ml": "pink rose · soft petals",
      "Double Apple · 10 ml": "red apple · green apple · clove · tobacco leaf",
      "Grapemint · 10 ml": "green grape · mint · citrus",
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

  // Kept in sync with the backend defaults in application.properties
  // (aether.shipping.flat-fee / aether.shipping.free-threshold) purely so
  // the cart panel can preview a shipping estimate before checkout. The
  // real number that gets charged always comes from the server response
  // on /api/orders/checkout.
  const SHIPPING_FLAT_FEE = 79;
  const SHIPPING_FREE_THRESHOLD = 1499;

  function estimateShipping(subtotal) {
    return subtotal >= SHIPPING_FREE_THRESHOLD ? 0 : SHIPPING_FLAT_FEE;
  }

  async function loadCart() {
    const cartCountEls = document.querySelectorAll(".cart-count");
    const cartItemsEl = document.getElementById("cartItems");
    const cartTotalEl = document.getElementById("cartTotal");
    const cartSubtotalEl = document.getElementById("cartSubtotal");
    const cartShippingEl = document.getElementById("cartShipping");
    const cartShippingLabelEl = document.getElementById("cartShippingLabel");

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
        if (cartSubtotalEl) cartSubtotalEl.textContent = formatMoney(0);
        if (cartShippingEl) cartShippingEl.textContent = formatMoney(0);
        updateLaunchOffer([]);
        return;
      }

      cartItemsEl.innerHTML = cart.items.map(cartRowTemplate).join("");
      const subtotal = Number(cart.total);
      const shipping = estimateShipping(subtotal);
      if (cartSubtotalEl) cartSubtotalEl.textContent = formatMoney(subtotal);
      if (cartShippingEl) cartShippingEl.textContent = shipping === 0 ? "FREE" : formatMoney(shipping);
      if (cartShippingLabelEl) {
        cartShippingLabelEl.textContent =
          shipping === 0
            ? "Shipping"
            : `Shipping (free above ${formatMoney(SHIPPING_FREE_THRESHOLD)})`;
      }
      cartTotalEl.textContent = formatMoney(subtotal + shipping);
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
    // The gift only triggers off a 100 ml fragrance in the cart — a 10 ml
    // travel bottle on its own should not unlock a free 100 ml gift.
    giftEligible = items.some((item) => item.sizeMl === 100 && item.quantity > 0);
    offer.textContent = giftEligible
      ? "Launch offer unlocked: choose a different 100 ml fragrance at checkout. The complimentary bottle will be confirmed with your order."
      : "Add a 100 ml fragrance to unlock your complimentary different 100 ml bottle.";
    if (select) {
      select.disabled = !giftEligible;
      const cartProductIds = new Set(items.map((item) => String(item.productId)));
      const options = activeProducts
        .filter((product) => product.sizeMl === 100 && !cartProductIds.has(String(product.id)) && product.stock > 0)
        .map((product) => `<option value="${product.id}">${escapeHtml(product.name)} — complimentary 100 ml</option>`)
        .join("");
      select.innerHTML = giftEligible
        ? `<option value="">Choose your complimentary bottle</option>${options}`
        : '<option value="">Add a 100 ml fragrance to unlock this selection</option>';
    }
  }

  function cartRowTemplate(item) {
    return `
      <div class="cart-row" data-product-id="${item.productId}">
        <img src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.name)}" />
        <div>
          <h4>${escapeHtml(item.name)}</h4>
          <span>${formatMoney(item.price)} each · ${item.sizeMl} ml</span>
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
        phone: document.getElementById("customerPhone")?.value.trim(),
        shippingAddressLine: document.getElementById("shippingAddressLine")?.value.trim(),
        shippingCity: document.getElementById("shippingCity")?.value.trim(),
        shippingState: document.getElementById("shippingState")?.value.trim(),
        shippingPinCode: document.getElementById("shippingPinCode")?.value.trim(),
        complimentaryProductId: Number(document.getElementById("complimentaryMini")?.value) || null,
      };

      const required = [
        "customerName",
        "email",
        "phone",
        "shippingAddressLine",
        "shippingCity",
        "shippingState",
        "shippingPinCode",
      ];
      if (required.some((field) => !payload[field])) {
        showCheckoutNote("Please fill in your name, email, phone, and full delivery address.");
        return;
      }
      if (!/^[1-9][0-9]{5}$/.test(payload.shippingPinCode)) {
        showCheckoutNote("We currently ship only within India — please enter a valid 6-digit PIN code.");
        return;
      }

      if (!payload.complimentaryProductId) {
        showCheckoutNote("Choose your complimentary different 100 ml fragrance before proceeding to payment.");
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
      stream.addEventListener("reviews", (event) => {
        const openSection = document.getElementById("modalReviews");
        if (openSection && String(openSection.dataset.productId) === String(event.data)) {
          loadReviews(event.data);
        }
      });
    } catch (error) {
      setRealtimeStatus("Preview mode - backend offline");
    }
  }
});
