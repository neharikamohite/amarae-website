# AETHER Beauty Storefront

A modern perfume ecommerce website for a 10-fragrance launch, built as a real Spring Boot application with database-backed products, cart, order checkout, realtime updates, and payment gateway wiring.

## What Is Included

- Premium responsive storefront in plain HTML, CSS, and JavaScript
- 10 launch perfumes with mood filters and search
- Guest browser session cart stored through backend APIs
- Quantity updates, cart total, checkout request form, and order creation
- H2 file database for local development
- Server-Sent Events for live product, cart, and order refresh
- Demo payment mode by default
- Razorpay-ready payment bridge in `checkout.html`
- Future category positioning for cosmetics, makeup, and gift sets

## Project Structure

- `index.html` - launch homepage
- `collections.html` - searchable shop, filters, cart, and checkout
- `findFragrance.html` - perfume match quiz
- `chemistry.html` - fragrance notes and scent families
- `checkout.html` - Razorpay checkout bridge
- `style.css` - shared responsive UI
- `javas.js` - live catalog, cart, quiz, search, wishlist, checkout, realtime
- `src/main/java` - Spring Boot backend
- `src/main/resources/application.properties` - database and payment settings
- `data/` - local H2 database files
- `assets/` - product images and videos

## Run Locally

From this folder:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Then open:

```text
http://localhost:8080
```

The frontend can open without the backend, but cart, database products, checkout, and realtime updates need Spring Boot running.

## Database

Local database:

```text
jdbc:h2:file:./data/aether-beauty
```

H2 console:

```text
http://localhost:8080/h2-console
```

Credentials:

```text
User: sa
Password: leave blank
```

The launch perfumes are upserted from:

```text
src/main/java/com/aether/beauty/product/ProductSeedData.java
```

Older demo products are marked inactive so the active shop focuses on the 10 perfume launch.

## API Overview

- `GET /api/products` - list active products
- `GET /api/cart` - get cart for the current browser session
- `POST /api/cart/items` - add product to cart
- `PATCH /api/cart/items/{productId}` - update quantity or remove
- `POST /api/orders/checkout` - create order and payment session
- `GET /api/orders` - latest orders
- `GET /api/realtime/stream` - live product, cart, and order events

## Payments

Local development uses demo mode:

```properties
aether.payment.gateway=demo
```

For Razorpay test/live mode, add keys and switch the gateway:

```powershell
$env:RAZORPAY_KEY_ID="rzp_test_your_key"
$env:RAZORPAY_KEY_SECRET="your_secret"
```

```properties
aether.payment.gateway=razorpay
```

Payment flow:

1. User adds perfumes to cart.
2. `POST /api/orders/checkout` creates the order.
3. Demo mode returns an immediate reference.
4. Razorpay mode opens `checkout.html`.
5. Backend verifies the payment signature and marks the order paid.

## Launch Checklist

- Replace placeholder brand assets with your final perfume bottle photos.
- Add official product names, prices, sizes, and legal descriptions.
- Add shipping, return, privacy, and terms pages before public launch.
- Configure Razorpay test credentials, then live credentials.
- Move from H2 to PostgreSQL or MySQL for production.
- Add admin product management before scaling into cosmetics and makeup.

# Amaraè — Practical Launch Guide

A complete reference for going from "product arriving soon" to "running a real business" — written for a first-time founder.

---

## PART 1: How This Business Actually Works (the basics)

You're running a **two-channel business**:

1. **D2C (Direct-to-Consumer)** — customers buy directly from your website, full retail price, highest margin per unit, but you do all the marketing/selling work yourself.
2. **B2B (Business-to-Business)** — you sell in bulk to retailers/distributors/supermarkets at a lower wholesale price, they resell to customers at your MRP. Lower margin per unit, but far less selling effort per unit sold — one retailer order of 50 units is 50 sales without you doing 50 individual customer conversations.

**Why both matters for you:** D2C builds your brand and gives you full margin; B2B gives you volume and reach without proportional effort. Most successful small consumer brands run both — D2C early on to prove demand and build a customer base, B2B growing over time as you build retailer trust.

### The money flow, simply put

```text
Your cost per unit (₹310 for 100ml, ₹75-100 for 10ml)
        ↓
You sell to retailer at WHOLESALE price (your profit #1)
        ↓
Retailer sells to customer at MRP (their profit)
        ↓
OR you sell directly to customer at MRP yourself (you keep both margins)
```

This is why your wholesale price must leave room for the retailer to ALSO make a healthy margin — if you price too close to your own MRP, no retailer will bother stocking you.

---

## PART 2: Your Pricing Structure (reference)

### 100ml bottles — cost ~₹310-340/unit landed (before shipping)

| Tier | Fragrances | DTC price | Wholesale (50+ MOQ) | MRP (retailer resells at) |
|---|---|---|---|---|
| Core Signature | Gucci Flora-type, Dior Sauvage-type | ₹1,299 | ₹750 | ₹1,499 |
| Premium Signature | YSL Libre-type, White Oud | ₹1,599 | ₹900 | ₹1,799 |
| Niche-Inspired | Aventus-type, BR540-type | ₹1,999 | ₹1,100 | ₹2,299 |

### 10ml minis — cost ~₹75-100/unit landed

| Type | DTC price | Wholesale | MRP |
|---|---|---|---|
| Standard minis (Aventus/Gucci Flora sample sizes) | ₹299-349 | ₹180-220 | ₹399-449 |
| Fun/gourmand (Grape Mint, Double Apple) | ₹249-299 | ₹150-180 | ₹349-399 |

### Margin math — always check this before finalizing any price

```text
Margin ₹ = Selling price − Your cost
Margin % = (Margin ₹ ÷ Selling price) × 100
```

Example: Wholesale ₹750, cost ₹310 → Margin = ₹440 → 58.6% margin. Healthy.

**Rule of thumb for this industry:** aim for 50%+ margin even at your lowest bulk price. If a deal pushes you below 40%, reconsider — you need buffer for shipping, breakage, returns, and platform/gateway fees.

---

## PART 3: Your Launch Offer

**Recommended: "Buy any 100ml, get a 10ml of a different fragrance free."**

Why this beats straight discounting or 2-for-1:
- Protects your MRP — customers never see a "cheaper" price, only a bonus
- Costs you very little (~₹85-100) relative to the ₹1,299-1,999 sale
- Doubles as a sampling strategy — the free mini can turn into their next full-size purchase
- Doesn't train customers to expect permanent 50% discounts (a real risk with 2-for-1 offers)

**Save bigger bundle offers** (Buy 2 Get 1, etc.) for specific limited windows — Diwali, your official launch week, festival sales — not as a permanent policy.

### For retailers running your offer
Supply the "free" 10ml units as promotional stock in their bulk order (at your cost, not billed to them) — this way their margin on paid units stays untouched, and the gift is effectively your marketing spend, not their loss.

**Pre-booking is open:** announce a limited pre-book offer before the official product launch so customers can reserve their first bottle and create early demand. Keep it simple: "Pre-book now and receive a complimentary 10ml sample with your first order." This helps you validate demand early and gives you a stronger launch week.

**Launch offer:** use the BOGO mini strategy at launch for the first 2-3 weeks, then move to standard pricing. It keeps the brand premium while creating urgency without training people to wait for constant discounts.

---

## PART 4: Retailer/Distributor Terms

**Your MOQ policy:** 50 pieces minimum per retailer order.

**Sample/tester strategy:** Give 10ml testers (not full 100ml bottles) with bulk orders — much cheaper for you, and lets the retailer demo the scent to many customers over weeks rather than one giveaway bottle.
- Suggested structure: 50-unit order → 2 testers included. 100+ unit order → 1 free full-size bottle included.

**What you need ready before approaching any retailer:**
- A one-page wholesale price sheet/catalog (tiers, MOQ, sample policy, contact info)
- A simple retailer onboarding form (business name, GST number, order quantity, delivery address)
- Physical sample kit to carry when pitching in person (very relevant for your BNI network)

---

## PART 5: Pre-Launch Checklist

### Legal & Compliance
- [x] Brand name registration (done)
- [ ] GST registration — needed for invoicing retailers and payment gateway live mode
- [ ] Legal Metrology-compliant labels — confirm with manufacturer: MRP, net quantity, batch no., manufacturing date, your business address, consumer care contact
- [ ] Business current account (bank account in Amaraè's name)

### Website & Selling Infrastructure
- [x] Domain purchased (amaraeformulations.com)
- [ ] Website fully live and tested (in progress)
- [ ] Payment gateway (Razorpay/Cashfree) — set up now, even in test mode
- [ ] Branded email (hello@amaraeformulations.com via Zoho, free)
- [ ] Courier/logistics tie-up — decide COD vs prepaid-only for launch
- [ ] Order/invoice template + simple inventory tracker (spreadsheet: one row per SKU × size, tracking stock in/out)

### Sales Materials
- [ ] Wholesale price sheet/catalog PDF
- [ ] Retailer onboarding form
- [ ] Physical sample kit for in-person pitching

### Product & Naming
- [ ] Finalized safe fragrance names and descriptions (notes/family-based, not referencing original luxury brands directly) — needed on packaging AND website
- [ ] Confirm manufacturer's documentation: concentration %, cruelty-free certification, IFRA compliance — keep these on file, you'll reference them in marketing
- [ ] Ensure the fragrances are made from IFRA-certified oils and the manufacturer provides proof for all fragrance loads, ingredient safety, and compliance records

### Marketing (deliberately after stock arrives)
- [ ] Product photography and video (your call — post-manufacturing)
- [ ] Launch week content calendar
- [ ] Launch offer finalized and ready to activate (Buy 1 Get 1 mini, as above)

---

## PART 6: First-Time Founder Tips (things people usually learn the hard way)

**1. Keep a simple cash flow log from day one.** Not fancy accounting — just: money in (sales), money out (manufacturing, shipping, ads, fees), running balance. This is the single most important habit for a first-time founder; running out of visibility on cash is how small businesses get into trouble, even profitable ones.

**2. Don't discount your MRP casually.** Once a price drops in a customer's mind, it's very hard to raise it back. Use bonuses/freebies (like your BOGO mini strategy) instead of price cuts wherever possible — this protects long-term brand value.

**3. Track which channel is actually working.** In the first few months, note where each sale came from (Instagram, BNI referral, website direct, retailer). This tells you where to spend more effort — most first-time founders guess instead of tracking, and end up spending time on the wrong channel.

**4. Retailers will ask for credit terms eventually — decide your policy now.** Some retailers expect to pay after they sell your stock (30/60 day credit), not upfront. Decide early whether you'll offer this (riskier, but sometimes necessary to land bigger retailers) or require payment on delivery (safer for a new brand with limited cash cushion). For your first 6 months, payment-on-delivery is the safer default.

**5. Expect returns/damage in transit — budget for it.** Perfume bottles can leak or break in shipping. Build a small buffer (2-3%) into your cost expectations, and have a clear policy (replace vs refund) ready before your first customer complaint arrives, not after.

**6. Your first 20-30 sales matter more than their profit.** Early sales are about proof and testimonials/reviews, not maximizing margin. Consider a generous but limited "founding customer" offer to get real reviews and word-of-mouth started — this pays off far more than the margin you'd protect by not doing it.

**7. Watch for the "just one more thing before launch" trap.** It's tempting to keep delaying launch for one more feature, one more design tweak. Set a real launch date once your core checklist (Parts 5) is done, and launch even if things aren't 100% perfect — you'll learn more from 2 weeks of real customer feedback than 2 more months of solo preparation.

---

## PART 7: Quick Glossary (terms you'll keep encountering)

| Term | Meaning |
|---|---|
| **MOQ** | Minimum Order Quantity — smallest amount a retailer/distributor must order |
| **MRP** | Maximum Retail Price — the final price the end customer pays |
| **DTC** | Direct-to-Consumer — selling straight to the customer, no middleman |
| **D2C** | Same as DTC |
| **B2B** | Business-to-Business — selling to retailers/distributors, not end customers |
| **Landed cost** | Your true cost per unit including manufacturing + shipping/logistics |
| **Margin** | Profit as a % of selling price |
| **GST** | Goods and Services Tax — required registration for invoicing businesses |
| **COD** | Cash on Delivery — customer pays when the product arrives |
| **SKU** | Stock Keeping Unit — one specific product+size combination (e.g., "Gucci Flora-type, 100ml" is one SKU) |
| **IFRA compliance** | International Fragrance Association safety standard — a real, checkable certification |

---

## Your immediate next actions (do these while waiting for stock)

1. Finish GST registration
2. Set up Razorpay/Cashfree account (test mode is fine for now)
3. Draft your wholesale price sheet using Part 2's numbers
4. Finalize your 8 fragrance names/descriptions (safe, original naming)
5. Set up your branded email
6. Decide your courier/logistics partner and COD policy
7. Build a simple spreadsheet: cash flow log + inventory tracker

Everything else on this list can happen in the weeks right before and after launch — these seven are worth starting now, since none of them depend on the product physically arriving first.

---

## Final founder note

Amaraè's fragrances should be positioned as premium, clean, and safe — with the product story built around high-quality IFRA-certified fragrance oils, thoughtful formulation, and a polished retail experience. The launch should feel intentionally premium, not discount-led. Start with pre-booking, activate the launch offer, and let early customer feedback sharpen your product messaging before scaling further.
