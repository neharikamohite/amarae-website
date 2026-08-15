# Deploy Amarae Backend to Render (Live Hosting)

## Goal
Move the Spring Boot backend from local-only (`localhost:8080`, local H2 database) to a live, publicly accessible deployment on Render, using a proper cloud database — so the website can eventually be reached at `amaraeformulations.com` instead of only on this computer.

---

## ⚠️ IMPORTANT: Frontend Sync Workflow

**Every time you edit your frontend files** (HTML, CSS, JavaScript, assets), you MUST sync them into the Spring Boot static resources folder before committing and deploying. Spring Boot serves files from `src/main/resources/static/`, not from the project root.

### How to Sync:

**Option A (Windows PowerShell):**
```powershell
.\sync-static-files.ps1
```

**Option B (Git Bash / Linux / Mac):**
```bash
./sync-static-files.sh
```

**Option C (Manual):**
```bash
git add src/main/resources/static/
```

### Workflow (repeat after every frontend change):
1. Edit your HTML/CSS/JS/assets in the project root
2. Run the sync script above
3. `git add src/main/resources/static/`
4. `git commit -m "Update static files: [describe changes]"`
5. `git push`
6. Deploy on Render

Forgetting this step will result in your live site showing outdated designs. The sync scripts automate this to prevent confusion.

---

## Part 1 — Database migration (do this first)

The current database (`aether-beauty.mv.db`) is a local H2 file — this only works on this machine and will NOT work once deployed.

- [ ] Replace H2 with **PostgreSQL** (Render offers a free/low-cost managed PostgreSQL database that pairs naturally with Render-hosted apps)
- [ ] Update `pom.xml` to include the PostgreSQL driver dependency (remove/keep H2 only for local dev/testing if still useful)
- [ ] Update `application.properties` (or `application.yml`) to use environment variables for database connection details (URL, username, password) instead of hardcoded local values — this is required since Render will provide these values at deploy time, not in the code itself
- [ ] Confirm all existing tables/entities (products, orders, cart, users, etc.) are re-created correctly against PostgreSQL — test locally against a PostgreSQL instance before deploying, if possible, to catch any H2-specific syntax that doesn't work in PostgreSQL
- [ ] Migrate/seed any existing product data (the 8 fragrances, pricing, etc.) into the new database

## Part 2 — Prepare the app for deployment

- [ ] Confirm the app reads the server port from an environment variable (Render assigns its own port at runtime) rather than a hardcoded `8080`
- [ ] Confirm static frontend files (HTML/CSS/JS/assets) are correctly placed in `src/main/resources/static/` so Spring Boot serves them properly in production (per the earlier fix)
- [ ] Add a `.gitignore` entry excluding any local database files, `.env` files, or credentials — these should never be pushed to GitHub
- [ ] Make sure the project builds cleanly with `mvn clean package` and produces a working JAR file

## Part 3 — Deploy to Render

- [ ] Push the project to the private GitHub repo (already set up)
- [ ] Create a free Render account (render.com), connect it to the GitHub repo
- [ ] Create a new **Web Service** on Render, pointing to this repo, with build command `mvn clean package` and start command `java -jar target/[app-name].jar` (adjust filename to match actual build output)
- [ ] Create a **PostgreSQL database** instance on Render (free tier to start)
- [ ] Add the database connection details (URL, username, password) as **environment variables** in Render's dashboard — not in the code
- [ ] Deploy and confirm the app starts successfully (check Render's deploy logs for errors)

## Part 4 — Connect the domain

- [ ] Once deployed and confirmed working on Render's provided URL (something like `amarae-backend.onrender.com`), go to domain DNS settings (in Hostinger, once purchased) and point `amaraeformulations.com` to the Render deployment, following Render's custom domain instructions
- [ ] Confirm SSL (https://) is automatically applied once the domain is connected (Render handles this automatically for custom domains)

## Part 5 — Final test

- [ ] Open `https://amaraeformulations.com` (or the Render `.onrender.com` link if domain isn't connected yet) from a different device/network — confirm the site loads, cart works, and any test checkout flow functions correctly
- [ ] Note: Render's free tier "sleeps" after inactivity — first load after idle time may take 30-50 seconds. This is expected on the free tier and can be resolved later by upgrading to a paid Render plan (~$7/month) once traffic increases.

---

**Priority order:** Part 1 (database) must be done before Part 3 (deployment) — deploying with the local H2 setup will fail. Part 4 (domain connection) only happens after Part 3 is confirmed working.
