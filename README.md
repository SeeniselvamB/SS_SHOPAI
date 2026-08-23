# ShopAI — E-Commerce Backend

A full-featured Spring Boot backend for an e-commerce platform, built with Java, Spring Security (JWT), Spring Data JPA, and MySQL. Supports complete customer shopping flows and admin management, and is designed to plug into a future Voice Agent (Intent → Search → Recommendation → Comparison).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Database | MySQL (H2 supported for local/testing) |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| API Docs | Swagger / OpenAPI (springdoc) |
| Boilerplate | Lombok |

---

## Features

### Customer
- Register / Login (JWT access + refresh tokens)
- Browse, search, filter, and sort products
- Product details, images, ratings
- Cart management
- Wishlist management
- Multiple saved addresses
- Place orders, mock payment, order history
- Order tracking and cancellation
- Product reviews (verified purchase badge)
- Product comparison (2–5 products side by side)
- Personalized and general product recommendations

### Admin
- Secure admin login (role-based access)
- Dashboard with sales, inventory, and order statistics
- Full product CRUD + quick price/discount/stock updates
- Category management
- User management (search, enable/disable accounts)
- Order management (view, update status, tracking numbers)
- Review moderation (delete inappropriate reviews)

### Engineering
- Layered architecture: Controller → Service → Repository → Entity
- Centralized error handling with consistent JSON responses
- DTO-based request/response contracts (entities never exposed directly)
- Specification-based dynamic product search/filtering
- Soft-delete for products (preserves order history integrity)
- Order-time data snapshots (price, name, address) so history stays accurate even if products/addresses change later
- Idempotent database seeding (admin account + demo catalog) on startup
- Integration test suite (JUnit + MockMvc) covering auth, products, cart, orders, and admin authorization

---

## Project Structure

```
src/main/java/com/ss/shopai/
├── ShopAiApplication.java
├── config/            → CORS, JPA auditing, Jackson, OpenAPI, DB seeding
├── controller/         → Public/customer REST endpoints
│   └── admin/          → Admin-only REST endpoints
├── dto/
│   ├── request/         → Incoming request bodies
│   └── response/        → Outgoing response shapes
├── entity/              → JPA entities
├── enums/               → Role, OrderStatus, PaymentStatus, etc.
├── exception/           → Custom exceptions + global handler
├── repository/          → Spring Data JPA repositories
├── security/            → JWT, filters, UserDetails, SecurityConfig
├── service/             → Business logic
└── specification/       → Dynamic product search criteria

src/main/resources/
└── application.properties

src/test/java/com/ss/shopai/
├── BaseIntegrationTest.java
├── auth/
├── product/
├── cart/
├── order/
└── admin/

src/test/resources/
└── application-test.properties
```

---

## Prerequisites

- JDK 23 installed (`java --version` to confirm)
- Maven 3.9+ (or use the included `mvnw` wrapper if present)
- MySQL Server running locally (or accessible remotely)

---

## Setup

### 1. Configure the database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ss_shopai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

No manual database creation needed — `createDatabaseIfNotExist=true` and `ddl-auto=update` handle schema creation automatically on first run.

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The server starts at:
```
http://localhost:8080
```

### 4. Confirm it worked

On first successful startup, check the console logs for:

```
ShopAI Admin account created
Email:    admin@shopai.com
Password: Admin@123
```

This confirms the database connected and the seed data loaded correctly.

---

## Default Admin Credentials

| Field | Value |
|---|---|
| Email | `admin@shopai.com` |
| Password | `Admin@123` |

Change these in `application.properties` before deploying anywhere real:

```properties
app.admin.default-email=admin@shopai.com
app.admin.default-password=Admin@123
app.admin.default-name=Super Admin
```

---

## API Documentation (Swagger)

Once the app is running, open:

```
http://localhost:8080/swagger-ui.html
```

To test authenticated endpoints:
1. Expand `POST /api/auth/login`, run it with valid credentials, copy the `accessToken`.
2. Click the green **Authorize** button (top right).
3. Paste the token (no need to type "Bearer").
4. All protected endpoints will now work directly from the browser.

---

## Key API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a new account |
| POST | `/api/auth/login` | Log in, get tokens |
| POST | `/api/auth/refresh` | Refresh an expired access token |

### Customer
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Search/filter/sort products |
| GET | `/api/products/{id}` | Product details |
| GET | `/api/categories` | List categories |
| GET/POST/PUT/DELETE | `/api/cart/**` | Manage cart |
| GET/POST/DELETE | `/api/wishlist/**` | Manage wishlist |
| GET/POST/PUT/DELETE | `/api/addresses/**` | Manage addresses |
| POST | `/api/orders` | Place an order |
| POST | `/api/orders/payment` | Mock payment |
| GET | `/api/orders` | Order history |
| PUT | `/api/orders/{id}/cancel` | Cancel an order |
| GET/POST/PUT/DELETE | `/api/reviews/**` | Manage reviews |
| POST | `/api/comparison` | Compare products |
| GET | `/api/recommendations/**` | Get recommendations |

### Admin (require `ROLE_ADMIN`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/dashboard/stats` | Sales/inventory stats |
| CRUD | `/api/admin/products/**` | Manage products |
| PATCH | `/api/admin/products/{id}/stock` | Update stock |
| PATCH | `/api/admin/products/{id}/price` | Update price |
| PATCH | `/api/admin/products/{id}/discount` | Update discount |
| CRUD | `/api/admin/categories/**` | Manage categories |
| GET | `/api/admin/users` | List/search users |
| PATCH | `/api/admin/users/{id}/toggle-enabled` | Ban/unban a user |
| GET | `/api/admin/orders` | View all orders |
| PATCH | `/api/admin/orders/{id}/status` | Update order status |
| DELETE | `/api/admin/reviews/{id}` | Remove a review |

Authenticated requests require:
```
Authorization: Bearer <your-access-token>
```

---

## Running Tests

```bash
mvn test
```

Tests run against an isolated in-memory H2 database (`application-test.properties`) — your MySQL data is never touched.

---

## Notes & Known Decisions

- **Products are soft-deleted** (`active = false`), never hard-deleted, to preserve historical order integrity.
- **Orders snapshot** shipping address and product details at purchase time, so later edits to a product or address never corrupt past orders.
- **Payments are mocked** — no real payment gateway is integrated. `simulateSuccess: true/false` in the payment request lets you test both success and failure paths.
- `/api/comparison` currently requires authentication. If you want it public (useful for a pre-login Voice Agent flow), add `"/api/comparison/**"` to `PUBLIC_ENDPOINTS` in `SecurityConfig.java`.

---

## Roadmap (Not Implemented Yet, By Design)

- Voice Agent integration (Intent → Search → Recommendation → Comparison) — the backend services (`RecommendationService`, `ComparisonService`) are already structured as plain, reusable Java services to support this later.
- Frontend (React) — intentionally excluded from this repository.
- Real payment gateway integration.

---

## License

Internal academic/personal project. No license specified.