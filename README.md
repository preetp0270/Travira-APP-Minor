# 🌍 Travira

Travira is an AI-powered **Android travel app** with a **Node.js + MongoDB backend**. Users discover destinations, mark places as visited, leave reviews, manage a wishlist, and chat with a Gemini-powered travel assistant. Admins manage places and users via the app and browser tools.

**Live backend:** [https://travira-app-minor.onrender.com](https://travira-app-minor.onrender.com)

**Download Semi-Final Touch:** [https://github.com/preetp0270/Travira-APP-Minor/actions/runs/36417253172/artifacts/10967718115](https://github.com/preetp0270/Travira-APP-Minor/actions/runs/36417253172/artifacts/10967718115)

---

## 🔗 Web links (HTML tools)

These pages are served from the backend (`backend/public/`) on the same host as the API.

| Page | URL | Purpose |
|------|-----|---------|
| **Admin login / place uploader** | [https://travira-app-minor.onrender.com/admin.html](https://travira-app-minor.onrender.com/admin.html) | Sign in as admin; add places via **file upload** or **image URL** (both → Cloudinary → MongoDB) |
| **Admin registration** | [https://travira-app-minor.onrender.com/admin-register.html](https://travira-app-minor.onrender.com/admin-register.html) | Create a new admin account after verifying the **MongoDB connection password** |
| **Reset password** | [https://travira-app-minor.onrender.com/reset-password.html](https://travira-app-minor.onrender.com/reset-password.html) | Set a new password from the email reset link (`?token=…`) |
| **API health** | [https://travira-app-minor.onrender.com/api/health](https://travira-app-minor.onrender.com/api/health) | Service status, Mongo/JWT/Gemini config flags |
| **API ping** | [https://travira-app-minor.onrender.com/api/ping](https://travira-app-minor.onrender.com/api/ping) | Keep-alive / uptime check |

**Local equivalents** (when the backend runs on your machine):

- `http://localhost:5000/admin.html`
- `http://localhost:5000/admin-register.html`
- `http://localhost:5000/reset-password.html`
- `http://localhost:5000/api/health`

---

## ✨ Features

### App (Android)

* **Authentication** — Register, login, JWT access + refresh tokens, forgot/reset password  
* **Places feed** — Browse destinations (sample/AI + user-contributed) with images and details  
* **Visited places** — Mark / unmark visited; counts use **sample base + real users**  
* **Reviews & ratings** — Rate 1–5 with optional feedback; counts use **sample base + real reviews**  
* **Wishlist** — Save places for later  
* **AI travel chatbot** — Gemini (travel tips, culture, itinerary ideas)  
* **Profile** — Edit profile, visited list, contributions  
* **Admin (in-app)** — Dashboard for admins  

### Web (browser)

* **Admin place uploader** — Login and add places with image upload  
* **Admin registration** — Bootstrap new admins using MongoDB password gate  
* **Password reset form** — Completes the email reset flow  

---

## 🔄 Project flow

```text
┌─────────────────┐     HTTPS/JSON      ┌──────────────────────────┐
│  Android app    │ ◄─────────────────► │  Node.js API (Express)   │
│  (Kotlin/Compose│                     │  travira-app-minor…      │
└─────────────────┘                     └────────────┬─────────────┘
                                                     │
                     ┌───────────────────────────────┼───────────────────┐
                     │                               │                   │
                     ▼                               ▼                   ▼
              MongoDB Atlas                    Gemini API          Cloudinary
              (users, places)                  (chat)              (images)
                     │
                     ▼
              Browser HTML tools
              /admin.html
              /admin-register.html
              /reset-password.html
```

### 1. User journey (app)

1. **Splash / intro** → Login or Register  
2. **Home / Places** → Browse list from `GET /api/place`  
3. **Place detail** → Wishlist, **Mark visited**, **Rate**  
4. **Profile** → Wishlist, Visited, Edit profile  
5. **AI Chat** → `POST /api/chat` (auth required)  
6. **Forgot password** → email link → `/reset-password.html?token=…`  

### 2. Visitor & review counts (important)

Displayed numbers are **not** only live app users. Sample/AI places keep a permanent base:

| Field | Meaning |
|-------|---------|
| `baseVisitorsCount` | Sample/AI visitor number (never overwritten by mark/unmark) |
| `visitorsCount` (API) | **baseVisitorsCount +** real users who marked visited |
| `baseRatingsCount` | Sample/AI review count |
| `ratingsCount` (API) | **baseRatingsCount +** length of `place.ratings[]` |
| `averageRating` | Real average if any user ratings exist; otherwise seed average |

**Example:** base visitors = 400 → user marks visited → **401** → unmarks → **400**.

Admin/create place: sending `visitorsCount` (or `baseVisitorsCount`) stores the sample base. Logic lives in `backend/utils/placeStats.js`.

### 3. Admin journey

**A. First admin (bootstrap)**

1. Open [admin-register.html](https://travira-app-minor.onrender.com/admin-register.html)  
2. Enter **MongoDB password** only (password segment of `MONGODB_URI`, or env `ADMIN_BOOTSTRAP_PASSWORD` / `MONGO_PASSWORD`)  
3. Create admin (name, email, password, role)  
4. API: `POST /api/admin-bootstrap/register`  

**B. Upload places in browser**

1. Open [admin.html](https://travira-app-minor.onrender.com/admin.html)  
2. Log in with admin account  
3. Add places (name, description, location)  
4. **Image:** either pick a **file** (JPG/PNG/…) or paste an **image link** from anywhere → on save the image is uploaded to **Cloudinary**, then the Cloudinary URL is stored in **MongoDB**  
5. API: `POST /api/admin/places` (JWT + admin role)  

**C. In-app admin (Add / Edit place)**

- Same credentials; admin dashboard and place management in the Android app  
- **Add place** and **Edit place** support the same image options as the web tool:  
  - **Upload file** — gallery photo → Cloudinary → MongoDB  
  - **Image link** — paste any public URL → Cloudinary fetch → MongoDB  
- Edit: leave image empty to keep the current photo; use file or URL only when replacing  

### 4. Auth flow

```text
Register/Login  →  accessToken + refreshToken
Protected routes  →  Authorization: Bearer <accessToken>
Token expired     →  POST /api/users/refresh-token
Password reset    →  POST /api/users/forgot-password
                  →  email with link to /reset-password.html?token=…
                  →  POST /api/users/reset-password  (invalidates old sessions)
```

---

## 🛠 API overview

| Area | Base path | Notes |
|------|-----------|--------|
| Places | `/api/place` or `/api/places` | List, detail, wishlist, rating |
| Users | `/api/users` | Auth, profile, visited |
| Admin | `/api/admin` | Places & users (JWT + admin) |
| Admin bootstrap | `/api/admin-bootstrap/register` | Public; gated by MongoDB password |
| Chat | `/api/chat` | Gemini travel chatbot (auth) |
| Health | `/api/health`, `/api/ping` | Status / keep-alive |

### Useful endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/users/register` | No | Create user |
| POST | `/api/users/login` | No | Login → tokens |
| GET | `/api/place` | No | List places (live stats) |
| POST | `/api/users/visited/:id` | Yes | Mark visited → returns new `visitorsCount` |
| DELETE | `/api/users/visited/:id` | Yes | Unmark visited |
| POST | `/api/place/:id/rating` | Yes | Submit/update rating |
| POST | `/api/admin/places` | Admin | Create place |
| POST | `/api/admin-bootstrap/register` | Mongo password | Create admin |
| POST | `/api/chat` | Yes | AI travel message |

---

## 🚀 Tech stack

| Technology | Purpose |
|------------|---------|
| **Kotlin + Jetpack Compose** | Android UI |
| **Retrofit** | HTTP client |
| **Node.js + Express** | REST API |
| **MongoDB Atlas** | Users, places, ratings, visits |
| **JWT** | Access + refresh tokens |
| **Gemini API** | Travel chatbot |
| **Cloudinary** | Place image hosting |
| **Render** | Backend hosting |
| **Git & GitHub** | Version control |

---

## 📂 Project structure

```text
Travira-APP-Minor/
├── README.md
├── travira_full_qa.py              # Optional API / device QA script
│
├── android/                        # Kotlin + Jetpack Compose
│   └── app/src/main/java/com/example/travira/
│       ├── MainActivity.kt
│       ├── auth/                   # Token storage
│       ├── components/
│       ├── model/                  # Place, User
│       ├── remote/                 # Retrofit APIs
│       ├── screens/                # auth, places, profile, ai, admin, splash
│       └── ui/theme/
│
└── backend/                        # Node.js + Express
    ├── server.js                   # Entry, routes, static HTML, keep-alive
    ├── package.json
    ├── public/
    │   ├── admin.html              # Admin login + place uploader
    │   ├── admin-register.html     # Bootstrap admin (Mongo password gate)
    │   └── reset-password.html     # Password reset form
    ├── controllers/
    │   ├── admin.js                # Places/users + bootstrapRegisterAdmin
    │   ├── chat.js
    │   ├── place.js                # Feed, wishlist, rating
    │   └── user.js                 # Auth, visited, profile
    ├── middleware/
    │   ├── adminMiddleware.js
    │   └── authMiddleware.js
    ├── models/
    │   ├── place.js                # baseVisitorsCount, baseRatingsCount, ratings[]
    │   └── user.js                 # wishlist, visitedPlaces, role
    ├── routes/
    │   ├── adminRoutes.js
    │   ├── chatRoutes.js
    │   ├── placeroute.js
    │   └── user.js
    └── utils/
        ├── placeStats.js           # base + live visitor/rating math
        └── mailer.js               # Password-reset email
```

---

## ⚙️ Setup (local)

### Backend

```bash
cd backend
npm install
# .env (example keys — use your real values)
# MONGODB_URI=mongodb+srv://user:PASSWORD@cluster/...
# JWT_SECRET=...
# JWT_REFRESH_SECRET=...
# GEMINI_API_KEY=...
# APP_BASE_URL=http://localhost:5000
# Optional: ADMIN_BOOTSTRAP_PASSWORD=...  (separate gate for admin-register)
npm start
```

Default port: **5000**.

### Android

1. Open `android/` in Android Studio  
2. Point `retrofitInstance` / base URL at your backend (Render or local)  
3. Build & run on device/emulator  

### Create first admin

1. Start backend with a valid `MONGODB_URI`  
2. Open `http://localhost:5000/admin-register.html`  
3. Enter the **MongoDB password** from the URI  
4. Create admin → then use `http://localhost:5000/admin.html` to log in  

---

## 🎯 Project goal

Travira aims to be a practical travel companion: discover places, learn from community and sample data, track what you’ve visited, and get AI help for planning — with a simple admin path to grow the destination catalog.

---

## 👨‍💻 Developers

[**Preet Patel**](https://github.com/preetp0270) (Founder, idea, most contributions) · **Yagnik Padaliya** (Co-founder, problem-solving, help & suggestions)

### Vision

**"Explore smarter, discover deeper, and travel with confidence using Travira."**
