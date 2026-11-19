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
cp .env.example .env
docker compose up --build
```

Web app: http://localhost:3000 · API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html

### Option B: dev mode (hot reload)

1. Start MySQL (or use your own MySQL 8 with a database `artbazaar` and user `artbazaar`/`artbazaar`):
   ```bash
   docker compose up -d mysql
   ```
2. Start the API. Flyway creates the tables and seeds the categories on first run.
   ```bash
   cd backend && mvn spring-boot:run
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

### Tests

```bash
cd backend && mvn test        # runs on in-memory H2 with the same Flyway migrations
cd frontend && npm run lint && npm run build
```

## Roadmap

- [x] **Phase 0:** monorepo, Spring Boot skeleton, Flyway schema, security skeleton, Docker, Tailwind frontend rebuild
- [ ] **Phase 1:** JWT auth (register/login/refresh), RBAC, login/register pages
- [ ] **Phase 2:** artwork catalog API (search, filter, pagination, image upload), frontend wired to API
- [ ] **Phase 3:** cart, simulated checkout, orders
- [ ] **Phase 4:** artist dashboard
- [ ] **Phase 5:** admin panel (artist approval), reviews
- [ ] **Phase 6:** polish, integration tests, deployment
