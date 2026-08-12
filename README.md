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
