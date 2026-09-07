window.addEventListener("load", () => {
  const apiBase = "";
  const sessionKey = "amaraeSessionId";
  const wishlistKey = "amaraeWishlist";
  const sessionId = getSessionId();
  let activeProducts = [];
  let activeFilter = "all";
  let activeSearch = "";
  let activeSort = "recommended";
  let giftEligible = false;
  let appliedCoupon = null; // { code, discount } once validated against the cart
  let cartQuantities = {}; // productId (string) -> quantity currently in cart

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
  loadFeaturedTestimonials();
  syncWishlistFromAccount();

  // Homepage-only, but safe to call on every page: no-ops instantly if
  // there's no testimonial section on the current page.
  async function loadFeaturedTestimonials() {
    const grid = document.getElementById("testimonialGrid");
    if (!grid) return;
    try {
      const reviews = await api("/api/reviews/featured");
      if (!reviews.length) return;
      const heading = document.getElementById("testimonial-title");
      const intro = document.getElementById("testimonialIntro");
      if (heading) heading.textContent = "What customers are saying.";
      if (intro) intro.textContent = "Real feedback from verified AMARAÈ reviews.";
      grid.innerHTML = reviews
        .map(
          (review) => `
            <blockquote>
              “${escapeHtml(review.comment)}”<cite>— ${escapeHtml(review.customerName)}, on ${escapeHtml(review.productName)}</cite>
            </blockquote>
          `
        )
        .join("");
    } catch (error) {
      // Leave the honest placeholder in place if this fails for any reason.
    }
  }

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
    const base = {
      "Content-Type": "application/json",
      "X-Aether-Session": sessionId,
    };
    // Optional: if the shopper is signed in (account.html), this links
    // whatever order they place to their account so it shows up in their
    // order history. Guest checkout keeps working with no token at all.
    const authToken = localStorage.getItem("amaraeAuthToken");
    if (authToken) {
      base.Authorization = `Bearer ${authToken}`;
    }
    return base;
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
    setupSort();
    attachCheckout();
    attachCouponForm();
    // Cart loads first so cartQuantities is already known by the time
    // products render — otherwise every card would briefly show "Add to
    // cart" even for items already in the cart, then flip to a stepper a
    // moment later.
    await loadCart();
    await loadProducts();
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
    injectProductStructuredData();
  }

  // Products are fetched client-side via API rather than server-rendered,
  // so search engines can't see them in the page's initial HTML. This
  // injects the same catalog as JSON-LD after it loads — a widely-used,
  // Google-supported way to get product/rating rich results without a
  // full server-rendering rewrite of the site.
  function injectProductStructuredData() {
    const existing = document.getElementById("productSchema");
    if (existing) existing.remove();
    if (!activeProducts.length) return;

    const itemListElement = activeProducts.map((product, index) => {
      const item = {
        "@type": "Product",
        name: product.name,
        image: product.imageUrl,
        description: product.description,
        sku: String(product.id),
        offers: {
          "@type": "Offer",
          priceCurrency: "INR",
          price: Number(product.price).toFixed(2),
          availability: product.stock > 0 ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
          url: "https://amaraeformulations.com/collections.html",
        },
      };
      if (Number(product.reviewCount) > 0) {
        item.aggregateRating = {
          "@type": "AggregateRating",
          ratingValue: Number(product.avgRating).toFixed(1),
          reviewCount: Number(product.reviewCount),
        };
      }
      return { "@type": "ListItem", position: index + 1, item };
    });

    const script = document.createElement("script");
    script.type = "application/ld+json";
    script.id = "productSchema";
    script.textContent = JSON.stringify({
      "@context": "https://schema.org",
      "@type": "ItemList",
      itemListElement,
    });
    document.head.appendChild(script);
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
    sortProducts(products);

    productGrid.innerHTML = products.length
      ? products.map(productCardTemplate).join("")
      : '<div class="empty-state">No perfume matched that search. Try rose, fresh, oud, warm, or vanilla.</div>';
    attachCartButtons();
    attachBuyNowButtons();
    attachWishlistButtons();
    attachCardOpenHandlers();
  }

  // Mutates in place — called right before rendering so search/filter and
  // sort always compose together instead of one silently overriding the
  // other.
  function sortProducts(products) {
    switch (activeSort) {
      case "price-asc":
        products.sort((a, b) => Number(a.price) - Number(b.price));
        break;
      case "price-desc":
        products.sort((a, b) => Number(b.price) - Number(a.price));
        break;
      case "rating":
        products.sort((a, b) => (Number(b.avgRating) || 0) - (Number(a.avgRating) || 0));
        break;
      case "popularity":
        products.sort((a, b) => (Number(b.reviewCount) || 0) - (Number(a.reviewCount) || 0));
        break;
      default:
        // "recommended" — keep the server/catalog order as-is.
        break;
    }
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
        ${cardRatingTemplate(product)}
        <p class="card-hint">Tap to view full details, notes, and reviews</p>
        ${cardActionsTemplate(product)}
      </article>
    `;
  }

  // Once a product is already in the cart, replace Add to Cart/Buy Now
  // with a +/- stepper — same pattern Flipkart/Blinkit use — so quantity
  // changes don't require opening the cart panel, and it's immediately
  // visible that the item was actually added (the earlier "Added" flash
  // was too easy to miss).
  function cardActionsTemplate(product) {
    if (product.stock <= 0) {
      return `<div class="card-actions"><button class="add-cart" disabled>Sold out</button></div>`;
    }
    const qty = cartQuantities[String(product.id)] || 0;
    if (qty > 0) {
      return `
        <div class="card-actions in-cart">
          <div class="qty-controls" aria-label="Quantity of ${escapeHtml(product.name)} in cart" data-product-id="${product.id}">
            <button type="button" data-action="decrease" aria-label="Decrease quantity">-</button>
            <strong>${qty}</strong>
            <button type="button" data-action="increase" aria-label="Increase quantity">+</button>
          </div>
          <a href="#cart" class="go-to-cart-link">Go to cart</a>
        </div>
      `;
    }
    return `
      <div class="card-actions">
        <button class="add-cart" data-product-id="${product.id}">Add to cart</button>
        <button class="buy-now-btn" data-product-id="${product.id}">Buy Now</button>
      </div>
    `;
  }

  function attachCardOpenHandlers() {
    document.querySelectorAll(".product-card").forEach((card) => {
      const open = (event) => {
        if (event.target.closest(".wish-btn") || event.target.closest(".add-cart") || event.target.closest(".buy-now-btn")) return;
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
        <div class="modal-cta-row">
          <button class="primary-btn modal-add-cart" data-product-id="${product.id}" ${product.stock > 0 ? "" : "disabled"}>
            ${product.stock > 0 ? "Add to cart" : "Sold out"}
          </button>
          <button class="secondary-btn modal-buy-now" data-product-id="${product.id}" ${product.stock > 0 ? "" : "disabled"}>Buy Now</button>
        </div>
        ${reviewsSectionTemplate(product.id)}
      </div>
    `;

    modal.classList.add("open");
    document.body.classList.add("modal-open");
    attachCartButtons();
    attachWishlistButtons();
    attachBuyNowButtons();
    attachQuantityStepperButtons();

    modalBody.querySelector(".modal-add-cart")?.addEventListener("click", () => {
      document.querySelector(`.add-cart[data-product-id="${product.id}"]`)?.click();
    });
    modalBody.querySelector(".modal-buy-now")?.addEventListener("click", () => {
      document.querySelector(`.buy-now-btn[data-product-id="${product.id}"]`)?.click();
      document.getElementById("productModal")?.classList.remove("open");
      document.body.classList.remove("modal-open");
    });

    loadReviews(product.id);
    attachReviewForm(product.id);
  }

  function modalActionsTemplate(product) {
    if (product.stock <= 0) {
      return `<div class="modal-cta-row"><button class="primary-btn modal-add-cart" disabled>Sold out</button></div>`;
    }
    const qty = cartQuantities[String(product.id)] || 0;
    if (qty > 0) {
      return `
        <div class="modal-cta-row in-cart">
          <div class="qty-controls" aria-label="Quantity of ${escapeHtml(product.name)} in cart" data-product-id="${product.id}">
            <button type="button" data-action="decrease" aria-label="Decrease quantity">-</button>
            <strong>${qty}</strong>
            <button type="button" data-action="increase" aria-label="Increase quantity">+</button>
          </div>
          <a href="#cart" class="go-to-cart-link">Go to cart</a>
        </div>
      `;
    }
    return `
      <div class="modal-cta-row">
        <button class="primary-btn modal-add-cart" data-product-id="${product.id}">Add to cart</button>
        <button class="secondary-btn modal-buy-now" data-product-id="${product.id}">Buy Now</button>
      </div>
    `;
  }

  // Called after every cart change so Add to Cart/Buy Now flips to a
  // quantity stepper the instant an item is actually in the cart — both
  // in the grid and in the modal, if it's the one currently open.
  function updateProductCardCartState() {
    document.querySelectorAll(".product-card").forEach((card) => {
      const product = activeProducts.find((item) => String(item.id) === card.dataset.productId);
      const actionsEl = card.querySelector(".card-actions");
      if (product && actionsEl) actionsEl.outerHTML = cardActionsTemplate(product);
    });

    const modal = document.getElementById("productModal");
    const modalCta = modal?.querySelector(".modal-cta-row");
    const openProductId = modal?.querySelector("[data-product-id]")?.dataset.productId;
    if (modal?.classList.contains("open") && modalCta && openProductId) {
      const product = activeProducts.find((item) => String(item.id) === openProductId);
      if (product) modalCta.outerHTML = modalActionsTemplate(product);
    }

    attachCartButtons();
    attachBuyNowButtons();
    attachQuantityStepperButtons();
    document.querySelectorAll(".modal-add-cart").forEach((button) => {
      button.onclick = () => document.querySelector(`.add-cart[data-product-id="${button.dataset.productId}"]`)?.click();
    });
    document.querySelectorAll(".modal-buy-now").forEach((button) => {
      button.onclick = () => {
        document.querySelector(`.buy-now-btn[data-product-id="${button.dataset.productId}"]`)?.click();
        document.getElementById("productModal")?.classList.remove("open");
        document.body.classList.remove("modal-open");
      };
    });
  }

  function attachQuantityStepperButtons() {
    document.querySelectorAll(".qty-controls[data-product-id] button").forEach((button) => {
      button.onclick = async () => {
        const wrapper = button.closest(".qty-controls");
        const productId = wrapper.dataset.productId;
        const currentQty = cartQuantities[productId] || 0;
        const nextQty = button.dataset.action === "increase" ? currentQty + 1 : currentQty - 1;
        wrapper.querySelectorAll("button").forEach((b) => (b.disabled = true));
        try {
          await api(`/api/cart/items/${productId}`, {
            method: "PATCH",
            body: JSON.stringify({ quantity: nextQty }),
          });
          await loadCart();
        } catch (error) {
          wrapper.querySelectorAll("button").forEach((b) => (b.disabled = false));
        }
      };
    });
  }

  // Real star rating pulled from the product's live aggregate (avgRating /
  // reviewCount, recomputed server-side every time a review is posted).
  // Falls back to an honest "no ratings yet" for launch fragrances with no
  // reviews rather than showing a fabricated score.
  function ratingTemplate(product) {
    const count = Number(product?.reviewCount) || 0;
    const avg = Number(product?.avgRating) || 0;
    const label = count === 0 ? "No ratings yet" : `${avg.toFixed(1)} out of 5 · ${count} review${count === 1 ? "" : "s"}`;
    return `
      <span class="stars" aria-hidden="true">${starGlyphs(avg)}</span>
      <span class="rating-label">${label}</span>
    `;
  }

  function cardRatingTemplate(product) {
    const count = Number(product?.reviewCount) || 0;
    const avg = Number(product?.avgRating) || 0;
    return `
      <div class="card-rating">
        <span class="stars" aria-hidden="true">${starGlyphs(avg)}</span>
        <span class="rating-label">${count === 0 ? "No ratings yet" : `${avg.toFixed(1)} (${count})`}</span>
      </div>
    `;
  }

  function starGlyphs(rating) {
    const filled = Math.round(Math.min(5, Math.max(0, Number(rating) || 0)));
    return "★".repeat(filled) + "☆".repeat(5 - filled);
  }

  // ---------- Ratings & Reviews: display, submission, real-time refresh ----------

  function reviewsSectionTemplate(productId) {
    return `
      <section class="modal-reviews" id="modalReviews" data-product-id="${productId}">
        <div class="reviews-header">
          <h3>Ratings &amp; Reviews</h3>
          <div class="reviews-summary" id="reviewsSummary"></div>
        </div>
        <div class="reviews-list" id="reviewsList">
          <p class="reviews-empty">Loading reviews…</p>
        </div>
        <form class="review-form" id="reviewForm" novalidate>
          <h4>Write a review</h4>
          <div class="review-form-row">
            <label class="review-name-field">
              Your name
              <input type="text" id="reviewName" placeholder="e.g. Priya S." maxlength="60" autocomplete="name" />
            </label>
            <div class="review-star-input" id="reviewStarInput" role="radiogroup" aria-label="Your star rating">
              ${[1, 2, 3, 4, 5]
                .map(
                  (n) =>
                    `<button type="button" class="star-pick" data-value="${n}" aria-label="${n} star${n > 1 ? "s" : ""}">★</button>`
                )
                .join("")}
            </div>
          </div>
          <label>
            Your review
            <textarea id="reviewComment" rows="3" maxlength="2000" placeholder="How does it wear? Longevity, occasions, what you loved..."></textarea>
          </label>
          <label class="review-file-label">
            <span>Add photos or videos <em>(optional — up to 6 files, 8&nbsp;MB photos / 60&nbsp;MB videos)</em></span>
            <input type="file" id="reviewFiles" accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime" multiple />
          </label>
          <div class="review-file-preview" id="reviewFilePreview"></div>
          <p class="review-form-note" id="reviewFormNote" role="status"></p>
          <button type="submit" class="primary-btn compact review-submit-btn" id="reviewSubmitBtn">Post review</button>
        </form>
      </section>
    `;
  }

  async function loadReviews(productId) {
    const list = document.getElementById("reviewsList");
    const summary = document.getElementById("reviewsSummary");
    if (!list) return;
    try {
      const reviews = await api(`/api/products/${productId}/reviews`);
      if (summary) summary.innerHTML = reviewsSummaryTemplate(reviews);
      list.innerHTML = reviews.length
        ? reviews.map(reviewCardTemplate).join("")
        : '<p class="reviews-empty">No reviews yet — be the first to share yours.</p>';
      attachReviewMediaHandlers(list);
    } catch (error) {
      if (summary) summary.innerHTML = "";
      list.innerHTML =
        '<p class="reviews-empty">Reviews are unavailable right now — start the backend to load and post live reviews.</p>';
    }
  }

  function reviewsSummaryTemplate(reviews) {
    if (!reviews.length) return "";
    const avg = reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length;
    return `
      <span class="stars" aria-hidden="true">${starGlyphs(avg)}</span>
      <strong>${avg.toFixed(1)}</strong>
      <span>out of 5 · ${reviews.length} review${reviews.length === 1 ? "" : "s"}</span>
    `;
  }

  function reviewCardTemplate(review) {
    const initial = (review.customerName || "?").trim().charAt(0).toUpperCase() || "?";
    const date = formatReviewDate(review.createdAt);
    const media = (review.media || []).length
      ? `<div class="review-media">${review.media.map(reviewMediaTemplate).join("")}</div>`
      : "";
    return `
      <article class="review-card">
        <div class="review-card-head">
          <span class="review-avatar" aria-hidden="true">${escapeHtml(initial)}</span>
          <div class="review-card-who">
            <strong class="review-author">${escapeHtml(review.customerName)}</strong>
            <div class="review-card-meta">
              <span class="stars" aria-hidden="true">${starGlyphs(review.rating)}</span>
              ${date ? `<span class="review-date">${escapeHtml(date)}</span>` : ""}
            </div>
          </div>
        </div>
        ${review.comment ? `<p class="review-comment">${escapeHtml(review.comment)}</p>` : ""}
        ${media}
      </article>
    `;
  }

  function reviewMediaTemplate(media) {
    if (media.mediaType === "VIDEO") {
      return `<video class="review-media-item" src="${escapeHtml(media.url)}" controls playsinline preload="metadata"></video>`;
    }
    return `<button type="button" class="review-media-item review-media-image" style="background-image:url('${escapeHtml(media.url)}')" data-full="${escapeHtml(media.url)}" aria-label="View full-size photo"></button>`;
  }

  function attachReviewMediaHandlers(container) {
    container.querySelectorAll(".review-media-image").forEach((button) => {
      button.addEventListener("click", () => openMediaLightbox(button.dataset.full));
    });
  }

  function openMediaLightbox(url) {
    const overlay = document.createElement("div");
    overlay.className = "media-lightbox";
    overlay.innerHTML = `<img src="${escapeHtml(url)}" alt="Customer review photo" />`;
    overlay.addEventListener("click", () => overlay.remove());
    document.addEventListener(
      "keydown",
      function onKey(event) {
        if (event.key === "Escape") {
          overlay.remove();
          document.removeEventListener("keydown", onKey);
        }
      },
      { once: true }
    );
    document.body.appendChild(overlay);
  }

  function formatReviewDate(iso) {
    if (!iso) return "";
    try {
      return new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
    } catch (error) {
      return "";
    }
  }

  function attachReviewForm(productId) {
    const form = document.getElementById("reviewForm");
    if (!form) return;

    const nameInput = document.getElementById("reviewName");
    const starButtons = Array.from(form.querySelectorAll(".star-pick"));
    const commentInput = document.getElementById("reviewComment");
    const fileInput = document.getElementById("reviewFiles");
    const filePreview = document.getElementById("reviewFilePreview");
    const noteEl = document.getElementById("reviewFormNote");
    const submitBtn = document.getElementById("reviewSubmitBtn");
    let selectedRating = 0;

    const savedName = localStorage.getItem("amaraeReviewerName");
    if (savedName && nameInput) nameInput.value = savedName;

    function paintStars(activeValue) {
      starButtons.forEach((button) => {
        button.classList.toggle("filled", Number(button.dataset.value) <= activeValue);
      });
    }

    starButtons.forEach((button) => {
      const value = Number(button.dataset.value);
      button.addEventListener("mouseenter", () => paintStars(value));
      button.addEventListener("mouseleave", () => paintStars(selectedRating));
      button.addEventListener("focus", () => paintStars(value));
      button.addEventListener("blur", () => paintStars(selectedRating));
      button.addEventListener("click", () => {
        selectedRating = value;
        paintStars(selectedRating);
      });
    });

    fileInput?.addEventListener("change", () => {
      const files = Array.from(fileInput.files || []);
      filePreview.innerHTML = files
        .map((file) => `<span class="review-file-chip">${escapeHtml(file.name)}</span>`)
        .join("");
      if (files.length > 6) {
        noteEl.textContent = "You can attach up to 6 photos or videos per review.";
        noteEl.classList.add("error");
      } else {
        noteEl.textContent = "";
        noteEl.classList.remove("error");
      }
    });

    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const customerName = nameInput.value.trim();
      const comment = commentInput.value.trim();
      const files = Array.from(fileInput?.files || []);

      if (!customerName) {
        noteEl.textContent = "Please enter your name.";
        noteEl.classList.add("error");
        nameInput.focus();
        return;
      }
      if (!selectedRating) {
        noteEl.textContent = "Please choose a star rating.";
        noteEl.classList.add("error");
        return;
      }
      if (files.length > 6) {
        noteEl.textContent = "You can attach up to 6 photos or videos per review.";
        noteEl.classList.add("error");
        return;
      }

      submitBtn.disabled = true;
      submitBtn.textContent = "Posting…";
      noteEl.classList.remove("error");
      noteEl.textContent = "";

      try {
        const formData = new FormData();
        formData.append("customerName", customerName);
        formData.append("rating", String(selectedRating));
        if (comment) formData.append("comment", comment);
        files.forEach((file) => formData.append("files", file));

        const response = await fetch(`/api/products/${productId}/reviews`, {
          method: "POST",
          headers: { "X-Aether-Session": sessionId },
          body: formData,
        });

        if (!response.ok) {
          const errorBody = await response.json().catch(() => ({}));
          throw new Error(errorBody.error || "Could not post your review. Please try again.");
        }

        localStorage.setItem("amaraeReviewerName", customerName);
        commentInput.value = "";
        if (fileInput) fileInput.value = "";
        filePreview.innerHTML = "";
        noteEl.classList.remove("error");
        noteEl.textContent = "Thanks — your review is live for every shopper to see.";

        await loadReviews(productId);
        await loadProducts();
      } catch (error) {
        noteEl.textContent = error.message;
        noteEl.classList.add("error");
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Post review";
      }
    });
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

  function setupSort() {
    document.getElementById("productSort")?.addEventListener("change", (event) => {
      activeSort = event.target.value;
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

  function attachBuyNowButtons() {
    document.querySelectorAll(".buy-now-btn").forEach((button) => {
      button.onclick = async () => {
        try {
          button.disabled = true;
          button.textContent = "Adding…";
          await api("/api/cart/items", {
            method: "POST",
            body: JSON.stringify({ productId: Number(button.dataset.productId), quantity: 1 }),
          });
          await loadCart();
          // "Buy Now" adds the item to the existing cart (nothing already
          // in it is lost) and jumps straight to checkout, rather than a
          // true single-item bypass — the fastest path to purchase this
          // cart-based layout supports without a parallel checkout flow.
          document.getElementById("cart")?.scrollIntoView({ behavior: "smooth", block: "start" });
          document.getElementById("customerName")?.focus({ preventScroll: true });
        } catch (error) {
          showCheckoutNote("Start the Spring Boot backend to use live cart and checkout.");
        } finally {
          button.disabled = false;
          button.textContent = "Buy Now";
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
      cartQuantities = Object.fromEntries(cart.items.map((item) => [String(item.productId), item.quantity]));
      updateProductCardCartState();

      if (!cartItemsEl || !cartTotalEl) return;
      if (cart.items.length === 0) {
        cartItemsEl.innerHTML = '<div class="cart-empty">Your cart is empty. Add a perfume from the launch collection.</div>';
        clearAppliedCoupon();
        renderCartTotals(0);
        updateLaunchOffer([]);
        return;
      }

      cartItemsEl.innerHTML = cart.items.map(cartRowTemplate).join("");
      // A coupon was validated against a specific subtotal — if the cart
      // has changed since (item added/removed/qty changed), drop it rather
      // than silently show a stale discount that checkout might reject.
      if (appliedCoupon && appliedCoupon.subtotal !== cart.total) {
        clearAppliedCoupon("Cart changed — please re-apply your coupon.");
      }
      renderCartTotals(cart.total);
      updateLaunchOffer(cart.items);
      attachCartRowButtons();
    } catch (error) {
      cartCountEls.forEach((el) => {
        el.textContent = "0";
      });
      cartQuantities = {};
      updateProductCardCartState();
      if (cartItemsEl) {
        cartItemsEl.innerHTML = '<div class="cart-empty">Live cart appears here after the backend starts.</div>';
      }
      renderCartTotals(0);
    }
  }

  function renderCartTotals(subtotal) {
    const subtotalEl = document.getElementById("cartSubtotal");
    const totalEl = document.getElementById("cartTotal");
    const discountRow = document.getElementById("cartDiscountRow");
    const discountLabel = document.getElementById("cartDiscountLabel");
    const discountEl = document.getElementById("cartDiscount");

    if (subtotalEl) subtotalEl.textContent = formatMoney(subtotal);

    const discount = appliedCoupon ? appliedCoupon.discount : 0;
    if (discountRow) discountRow.hidden = !appliedCoupon;
    if (appliedCoupon) {
      if (discountLabel) discountLabel.textContent = `Discount (${appliedCoupon.code})`;
      if (discountEl) discountEl.textContent = `-${formatMoney(discount)}`;
    }
    if (totalEl) totalEl.textContent = formatMoney(Math.max(0, subtotal - discount));
  }

  function clearAppliedCoupon(message) {
    appliedCoupon = null;
    const note = document.getElementById("couponNote");
    const input = document.getElementById("couponCodeInput");
    if (note) {
      note.textContent = message || "";
      note.classList.remove("error");
    }
    if (input) input.disabled = false;
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

  function attachCouponForm() {
    const applyBtn = document.getElementById("applyCouponBtn");
    const input = document.getElementById("couponCodeInput");
    const note = document.getElementById("couponNote");
    if (!applyBtn || !input) return;

    applyBtn.addEventListener("click", async () => {
      const code = input.value.trim();
      if (!code) {
        note.textContent = "Enter a coupon code first.";
        note.classList.add("error");
        return;
      }

      applyBtn.disabled = true;
      applyBtn.textContent = "Checking…";
      try {
        const result = await api("/api/coupons/validate", {
          method: "POST",
          body: JSON.stringify({ code }),
        });
        appliedCoupon = { code: result.code, discount: Number(result.discount), subtotal: Number(result.subtotal) };
        note.textContent = `"${result.code}" applied — you saved ${formatMoney(result.discount)}.`;
        note.classList.remove("error");
        input.disabled = true;
        renderCartTotals(result.subtotal);
      } catch (error) {
        clearAppliedCoupon();
        note.textContent = error.message;
        note.classList.add("error");
      } finally {
        applyBtn.disabled = false;
        applyBtn.textContent = "Apply";
      }
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
        couponCode: appliedCoupon ? appliedCoupon.code : null,
      };

      // The complimentary-gift selector is only required when the cart
      // actually earned it — a cart with no 100 ml fragrance in it should
      // never be blocked waiting on a gift choice it was never offered.
      if (giftEligible && !payload.complimentaryProductId) {
        showCheckoutNote("Choose your complimentary different 100 ml fragrance before reserving.");
        return;
      }

      try {
        const order = await api("/api/orders/checkout", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        appliedCoupon = null;
        await loadCart();

        if (order.paymentProvider === "razorpay" && order.paymentUrl) {
          // Real payment gateway is live — send the shopper straight to
          // secure checkout, same as a normal store.
          showCheckoutNote(`Order #${order.id} is awaiting payment. Your cart will be kept until payment is verified.`);
          window.location.assign(order.paymentUrl);
          return;
        }

        // No live payment gateway yet — this is a manual reservation.
        // The order already exists (with a real order number, visible in
        // the admin dashboard as "Payment pending"); we just hand the
        // customer to WhatsApp to confirm and pay by UPI instead of
        // pretending a real online payment happened.
        window.open(buildReservationWhatsAppUrl(order, payload), "_blank", "noopener");
        showCheckoutNote(
          `Order #${order.id} reserved — we've opened WhatsApp with your order details. Send that message to confirm, and we'll share our UPI QR code to complete payment.`
        );
      } catch (error) {
        showCheckoutNote(error.message);
      }
    });
  }

  function buildReservationWhatsAppUrl(order, payload) {
    const lines = order.lines
      .map((line) => `${line.quantity} x ${line.productName} (${line.sizeMl}ml) — ${formatMoney(line.unitPrice * line.quantity)}`)
      .join("\n");
    const discountLine =
      order.discountAmount > 0 ? `Discount (${order.couponCode || ""}): -${formatMoney(order.discountAmount)}\n` : "";
    const message = [
      "Hi! I'd like to reserve this order from AMARAÈ.",
      "",
      `Order #${order.id}`,
      lines,
      "",
      `Subtotal: ${formatMoney(order.subtotal)}`,
      discountLine.trim(),
      `Shipping: ${formatMoney(order.shippingFee)}`,
      `Total: ${formatMoney(order.total)}`,
      "",
      "Deliver to:",
      payload.customerName,
      `${payload.shippingAddressLine}, ${payload.shippingCity}, ${payload.shippingState} ${payload.shippingPinCode}`,
      `Phone: ${payload.phone}`,
      "",
      "Please share your UPI QR code so I can complete payment.",
    ]
      .filter(Boolean)
      .join("\n");
    return `https://wa.me/919579222532?text=${encodeURIComponent(message)}`;
  }

  function getWishlist() {
    return JSON.parse(localStorage.getItem(wishlistKey) || "[]");
  }

  function attachWishlistButtons() {
    document.querySelectorAll(".wish-btn").forEach((button) => {
      button.onclick = async () => {
        const id = String(button.dataset.productId);
        const wishlist = getWishlist();
        const nowActive = !wishlist.includes(id);
        const next = nowActive ? [...wishlist, id] : wishlist.filter((item) => item !== id);
        localStorage.setItem(wishlistKey, JSON.stringify(next));
        button.classList.toggle("active", nowActive);

        // Signed-in shoppers also get this synced to their account so it
        // follows them across devices — guests keep the same button
        // behavior, just local-only, same as before.
        const authToken = localStorage.getItem("amaraeAuthToken");
        if (authToken) {
          try {
            await api(`/api/account/wishlist/${id}`, { method: nowActive ? "POST" : "DELETE" });
          } catch (error) {
            // Non-fatal — the visible toggle already happened; it'll
            // simply retry next time this button is clicked.
          }
        }
      };
    });
  }

  // Pulls a signed-in shopper's saved-elsewhere wishlist into this
  // browser's localStorage, so hearts show correctly on a new device too.
  // Silently does nothing for guests or if the backend is unreachable.
  async function syncWishlistFromAccount() {
    const authToken = localStorage.getItem("amaraeAuthToken");
    if (!authToken) return;
    try {
      const items = await api("/api/account/wishlist");
      const remoteIds = items.map((product) => String(product.id));
      const merged = Array.from(new Set([...getWishlist(), ...remoteIds]));
      localStorage.setItem(wishlistKey, JSON.stringify(merged));
      if (document.querySelector(".product-grid")) renderProducts();
    } catch (error) {
      // Leave local wishlist as-is.
    }
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
      // Any shopper posting a star rating / photo / video review broadcasts
      // here, so every open browser tab updates live: product grid ratings
      // refresh everywhere, and anyone with that exact product open sees
      // the new review appear without reloading the page.
      stream.addEventListener("reviews", (event) => {
        loadProducts();
        const openReviews = document.getElementById("modalReviews");
        if (openReviews && String(openReviews.dataset.productId) === String(event.data).trim()) {
          loadReviews(openReviews.dataset.productId);
        }
      });
    } catch (error) {
      setRealtimeStatus("Preview mode - backend offline");
    }
  }
});
