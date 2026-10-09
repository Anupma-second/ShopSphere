# 🛒 ShopSphere — Multi-Vendor E-Commerce Marketplace

A full-stack marketplace where **customers** shop, **sellers** manage their own products and orders, and **admins** run the platform. It includes secure JWT login, Razorpay payments checked by webhooks, automatic refunds, and Docker-based cloud deployment.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Razorpay](https://img.shields.io/badge/Payments-Razorpay-0C2451)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)

---

## 🌐 Live Demo

| | Link |
|---|---|
| **Website** | https://shop-sphere-zlqq.vercel.app |
| **API Docs (Swagger)** | https://shopsphere-api-c2pr.onrender.com/swagger-ui.html |

**Try it:**
- Demo customer: `demo-customer@example.com` / `Demo@1234`
- Payments run in **Razorpay Test Mode**, so no real money is charged. At checkout choose **UPI** and enter `success@razorpay`.

> ⏳ Hosted on free plans. If the site has been idle, the first request can take up to a minute while the server wakes up.

---

## ✨ Features

### 👤 Customers
- Register, log in, verify email, and reset a forgotten password by email
- Browse products with **search and filters** (category, price, rating)
- Product pages with **image gallery, variants, ratings and reviews**
- **Wishlist**, **cart**, and a saved **address book**
- **Discount coupons** at checkout
- Pay with **Razorpay**, then see order history and status

### 🏪 Sellers
- Seller dashboard with sales overview
- Create, edit and delete **their own products only** (enforced on the server)
- Upload and manage product photos
- View and update orders for their products

### 🛡️ Admins
- Platform **statistics dashboard**
- **User management**: promote customers to sellers, manage roles
- Manage **categories**, **coupons**, all **products** and **orders**

### 💳 Payments
- Razorpay orders created on the server, and **payment signatures checked server-side**
- **Webhooks** (HMAC-SHA256 checked): `payment.captured`, `payment.failed`, `order.paid`, `refund.processed`, `refund.failed`
- **Automatic refunds** with a scheduled retry job (up to a set number of attempts)
- Unpaid orders **auto-cancel** after a set time

### 🔐 Security & Quality
- **JWT access + refresh tokens**; the frontend refreshes tokens silently in an axios interceptor
- Role-based access (`CUSTOMER`, `SELLER`, `ADMIN`) with Spring Security
- **Login rate limiting** per account and per IP address
- Configurable **CORS**, BCrypt password hashing, secrets kept out of the code in environment variables
- **Automatic created/updated timestamps** on all main records (JPA `@MappedSuperclass`)
- **OpenAPI / Swagger** documentation
- **Integration tests** covering the full marketplace flow

---

## 🧰 Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 17, Spring Boot 4.1, Spring Security, Spring Data JPA (Hibernate), JWT, springdoc-openapi |
| **Frontend** | React 19, Vite, React Router 7, Axios, CSS (theme variables) |
| **Database** | MySQL 8 (H2 in-memory for tests) |
| **Payments** | Razorpay (Orders API, Checkout, Webhooks, Refunds) |
| **Email** | Brevo HTTP API (SMTP fallback for local development) |
| **DevOps** | Docker (multi-stage build), Render, Vercel, Aiven MySQL (SSL) |

---

## 🏗️ Architecture

```
┌──────────────────┐   HTTPS / JSON    ┌───────────────────────┐   JDBC (SSL)   ┌──────────────┐
│  React + Vite    │ ────────────────► │  Spring Boot REST API │ ─────────────► │ MySQL (Aiven)│
│  (Vercel)        │ ◄──────────────── │  (Docker on Render)   │                └──────────────┘
└────────┬─────────┘    JWT auth       └──────────┬────────────┘
         │ Razorpay Checkout                       │ ▲ Webhooks (signed)
         ▼                                         ▼ │
   ┌──────────────┐                        ┌──────────────┐     ┌──────────────┐
   │   Razorpay   │ ─────────────────────► │  Razorpay    │     │ Brevo (email)│
   └──────────────┘                        │  API         │     └──────────────┘
                                           └──────────────┘
```

**Backend layers:** `controller` → `service` → `repository` → `entity`, with DTOs, a global exception handler, and Spring Security filters for JWT.

---

## 📁 Project Structure

```
ShopSphere/
├── ecommerce/                  # Spring Boot backend
│   ├── src/main/java/com/shopsphere/ecommerce/
│   │   ├── config/             # Security, CORS, OpenAPI
│   │   ├── controller/         # REST controllers (auth, products, cart, orders, payments, admin, seller…)
│   │   ├── service/            # Business logic (orders, payments, refunds, email…)
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── entity/             # JPA entities
│   │   └── ...
│   ├── src/test/java/          # Integration tests (H2)
│   ├── Dockerfile
│   └── .env.example
└── frontend/                   # React + Vite frontend
    ├── src/
    │   ├── api/                # Axios instance + token refresh
    │   ├── pages/              # Storefront, admin/, seller/ pages
    │   ├── components/
    │   ├── utils/
    │   └── styles/
    ├── vercel.json             # Sends every page route to index.html
    └── .env.example
```

---

## 🚀 Run Locally

### Prerequisites
- Java 17+
- Node.js 20+
- MySQL 8
- A free [Razorpay](https://razorpay.com) account (Test Mode keys)

### 1. Clone
```bash
git clone https://github.com/Anupma-second/ShopSphere.git
cd ShopSphere
```

### 2. Backend
```bash
cd ecommerce
cp .env.example .env        # then fill in DB password, JWT secret, Razorpay keys
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Tables are created automatically on first run.

> Run from the `ecommerce/` folder so the `.env` file gets loaded.

### 3. Frontend
```bash
cd frontend
cp .env.example .env        # set VITE_RAZORPAY_KEY_ID
npm install
npm run dev
```
- App: http://localhost:5173

### 4. Make yourself an admin
Register on the site, then run:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```
Log out and back in to see the admin dashboard.

---

## ⚙️ Environment Variables

### Backend (`ecommerce/.env` or host environment)
| Variable | Description |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | 32+ character secret for signing tokens |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | Razorpay API keys |
| `RAZORPAY_WEBHOOK_SECRET` | Secret set on the Razorpay webhook |
| `FRONTEND_URL`, `BACKEND_URL` | Used in email links and redirects |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin(s) |
| `BREVO_API_KEY`, `MAIL_FROM` | Real email sending (optional; emails print to the console without it) |
| `REQUIRE_EMAIL_VERIFICATION` | `true` to require email confirmation before login |
| `LOGIN_MAX_FAILURES_PER_ACCOUNT` / `_PER_IP` / `LOGIN_WINDOW_MINUTES` | Rate-limit settings |
| `SWAGGER_ENABLED` | Show/hide API docs |

### Frontend (`frontend/.env`)
| Variable | Description |
|---|---|
| `VITE_API_URL` | Backend base URL, e.g. `http://localhost:8080/api` |
| `VITE_RAZORPAY_KEY_ID` | Razorpay public key ID |

---

## 🧪 Tests

```bash
cd ecommerce
./mvnw test
```
Integration tests run against an in-memory **H2** database and cover:
- Registration, login, and role-based access
- Seller product ownership and admin user management
- Cart → checkout → order → payment flow
- Login rate limiting, timestamps, and API docs

---

## ☁️ Deployment

| Part | Platform | Notes |
|---|---|---|
| Backend | **Render** (Docker) | Multi-stage Dockerfile, JVM tuned for 512 MB, reads `$PORT` |
| Frontend | **Vercel** | Root directory `frontend`, SPA rewrite in `vercel.json` |
| Database | **Aiven MySQL** | SSL required (`sslMode=REQUIRED`) |
| Email | **Brevo API** | HTTP-based, because Render's free plan blocks SMTP ports |
| Uptime | **cron-job.org** | Pings `/api/categories` every 10 min to keep the free services awake |

---

## 👩‍💻 Author

**Anupma**
GitHub: [@Anupma-second](https://github.com/Anupma-second)

⭐ If you found this project interesting, consider giving it a star!