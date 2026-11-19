# Art Bazaar

A marketplace where local artists register, list their artworks and sell directly to buyers.

**Stack:** Java 21 · Spring Boot 4 · Spring Security (JWT, role-based access) · Spring Data JPA · MySQL 8 · Flyway · React 18 · Vite · Tailwind CSS

## Project structure

```
backend/    Spring Boot REST API (com.artbazaar)
frontend/   React + Vite + Tailwind single-page app
docker-compose.yml   MySQL + API + web, all in containers
```

## Roles

| Role   | What they can do |
|--------|------------------|
| Buyer  | Browse and search artworks, cart, simulated checkout, order history, reviews |
| Artist | Everything a buyer can, plus list and manage artworks and fulfil orders (after admin approval) |
| Admin  | Approve artists, manage users, categories and listings |

## Running locally

### Option A: everything in Docker

```bash
cp .env.example .env    # then fill in JWT_SECRET and the passwords
docker compose up --build
```

Web app: http://localhost:3000 · API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html

### Option B: dev mode (hot reload)

1. Start MySQL (or use your own MySQL 8 with a database `artbazaar` and user `artbazaar`/`artbazaar`):
   ```bash
   docker compose up -d mysql
   ```
2. Start the API. Flyway creates the tables and seeds the categories on first run.
   Setting `ADMIN_EMAIL` and `ADMIN_PASSWORD` creates an admin account you can sign in with.
   ```bash
   cd backend && ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=ChangeMe123 mvn spring-boot:run
   ```
3. Start the frontend. Vite proxies `/api` to `localhost:8080`.
   ```bash
   cd frontend && npm install && npm run dev
   ```
   Open http://localhost:5173

### Configuration (backend environment variables)

| Variable | Default |
|----------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/artbazaar?...` |
| `DB_USERNAME` / `DB_PASSWORD` | `artbazaar` / `artbazaar` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `UPLOAD_DIR` | `./uploads` |
| `PORT` | `8080` |
| `JWT_SECRET` | development key (**always set in production**: `openssl rand -base64 48`) |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `7d` |
| `REFRESH_COOKIE_SECURE` | `false` (set `true` when served over HTTPS) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | unset (no admin is created) |

## Authentication

- **Register / sign in** at `POST /api/auth/register` and `/api/auth/login`. Buyers and artists can sign up; admins are created only through `ADMIN_EMAIL`/`ADMIN_PASSWORD`.
- **Access token:** a 15-minute JWT returned in the response body. The React app keeps it in memory only and sends it as `Authorization: Bearer ...`.
- **Refresh token:** a random 7-day token in an `HttpOnly`, `SameSite=Strict` cookie, so page scripts can never read it. Only its SHA-256 hash is stored in the database. Each refresh rotates it, and reusing an old token signs that user out everywhere.
- **Staying signed in:** on page load and whenever a request gets a 401, the app calls `/api/auth/refresh` and retries, so sessions survive reloads without storing tokens in `localStorage`.
- **Roles:** `BUYER`, `ARTIST`, `ADMIN`. `/api/admin/**` is admin only and `/api/artist/**` is artist only. New artists can sign in straight away, but can't list artworks until an admin approves them on the **Artist applications** page.

### Tests

```bash
cd backend && mvn test        # runs on in-memory H2 with the same Flyway migrations
cd frontend && npm run lint && npm run build
```

## Roadmap

- [x] Project setup: Spring Boot API, MySQL schema, Docker, Tailwind frontend
- [x] Accounts: register, sign in, JWT with refresh tokens, roles, artist approval
- [ ] Artwork catalog: search, filters, pagination, image upload
- [ ] Cart, checkout (simulated payment) and orders
- [ ] Artist dashboard: manage artworks, fulfil orders, sales
- [ ] Admin tools and reviews
- [ ] Deployment on Railway
