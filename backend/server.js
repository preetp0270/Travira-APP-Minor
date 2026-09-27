const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const express = require("express");
const cors = require("cors");
const dns = require("dns");
const mongoose = require("mongoose");

const placeRoutes = require("./routes/placeroute");
const userRoutes = require("./routes/user");
const adminRoutes = require("./routes/adminRoutes");
const chatRoutes = require("./routes/chatRoutes");

// DNS servers (helps with some Atlas SRV resolution issues when testing locally)
dns.setServers(["8.8.8.8", "8.8.4.4"]);

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, "public")));

// Simple request logger
app.use((req, res, next) => {
  console.log(`${req.method} ${req.url}`);
  next();
});

const connectDB = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URI, { serverSelectionTimeoutMS: 10000 });
    console.log("✅ MongoDB Connected Successfully");
  } catch (error) {
    console.error("MongoDB connection error:", error.message);
    throw error;
  }
};

app.get("/", (req, res) => res.send("🚀 Travira Backend is Running..."));

const { mailStatus, verifyMail } = require("./utils/mailer");

/**
 * Health / status — use this after deploy to verify new code is live:
 *   GET https://travira-app-minor.onrender.com/api/health
 *   GET https://travira-app-minor.onrender.com/api/health/email  (SMTP verify)
 */
function healthHandler(req, res) {
  const mail = mailStatus();
  res.status(200).json({
    success: true,
    status: "ok",
    service: "travira",
    version: "2026-09-27-mail-brevo-v6",
    ts: Date.now(),
    uptime: process.uptime(),
    mongoConfigured: Boolean(process.env.MONGODB_URI),
    jwtConfigured: Boolean(process.env.JWT_SECRET),
    geminiConfigured: Boolean(process.env.GEMINI_API_KEY),
    geminiModel: process.env.GEMINI_MODEL || "gemini-3.8-flash",
    groqConfigured: Boolean(process.env.GROQ_API_KEY),
    emailConfigured: mail.configured,
    emailUser: mail.emailUser,
    emailLastError: mail.lastError,
    emailLastSuccessAt: mail.lastSuccessAt,
    appBaseUrl: process.env.APP_BASE_URL || process.env.RENDER_EXTERNAL_URL || null,
    features: [
      "places",
      "users",
      "chat",
      "forgot-password",
      "reset-password",
      "admin-web-upload",
      "session-invalidate-on-reset",
      "ping",
      "health",
      "health-email"
    ]
  });
}

app.get("/api/health", healthHandler);
app.get("/health", healthHandler);
app.get("/api/health/email", async (req, res) => {
  try {
    const result = await verifyMail();
    res.status(200).json({
      success: true,
      smtpOk: result.ok,
      reason: result.reason || result.note || null,
      version: "2026-09-27-mail-brevo-v6",
      ...mailStatus()
    });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});
app.get("/api/ping", (req, res) => {
  res.status(200).json({ success: true, pong: true, ts: Date.now() });
});
app.get("/ping", (req, res) => {
  res.status(200).json({ success: true, pong: true, ts: Date.now() });
});

// Password reset mounted here too so they never 404 if router is stale
const userController = require("./controllers/user");
app.post("/api/users/forgot-password", userController.forgotPassword);
app.post("/api/users/reset-password", userController.resetPassword);
app.post("/api/auth/forgot-password", userController.forgotPassword);
app.post("/api/auth/reset-password", userController.resetPassword);

app.use("/api/place", placeRoutes);
app.use("/api/places", placeRoutes); // alias for older Android clients
app.use("/api/users", userRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/chat", chatRoutes);

app.use((req, res) => res.status(404).json({ success: false, message: "API Route Not Found" }));

/**
 * Self keep-alive: the process calls its own public URL every few minutes.
 * That creates real inbound HTTP traffic so Render is less likely to mark
 * the service idle and spin it down (free tier ~15 min).
 *
 * Env:
 *   APP_BASE_URL   e.g. https://travira-app-minor.onrender.com  (required for self-ping)
 *   KEEP_ALIVE_MS  interval in ms (default 180000 = 3 min; use 120000–300000)
 *   KEEP_ALIVE     set to "false" to disable
 */
function startSelfKeepAlive() {
  if (process.env.KEEP_ALIVE === "false") {
    console.log("⏸️  Self keep-alive disabled (KEEP_ALIVE=false)");
    return;
  }

  const base = (process.env.APP_BASE_URL || process.env.RENDER_EXTERNAL_URL || "")
    .replace(/\/$/, "");
  if (!base) {
    console.warn(
      "⚠️  Self keep-alive skipped: set APP_BASE_URL (e.g. https://travira-app-minor.onrender.com)"
    );
    return;
  }

  // Clamp 2–5 minutes (default 3)
  let intervalMs = Number(process.env.KEEP_ALIVE_MS || 180000);
  if (!Number.isFinite(intervalMs) || intervalMs < 120000) intervalMs = 120000;
  if (intervalMs > 300000) intervalMs = 300000;

  const url = `${base}/api/ping`;

  const tick = async () => {
    try {
      const res = await fetch(url, {
        method: "GET",
        headers: { "User-Agent": "Travira-SelfKeepAlive/1.0" },
        signal: AbortSignal.timeout(15000)
      });
      console.log(`🔄 keep-alive ${res.status} ${url}`);
    } catch (e) {
      console.warn(`🔄 keep-alive failed: ${e.message}`);
    }
  };

  // First ping after short delay (let listen settle), then on interval
  setTimeout(() => {
    tick();
    setInterval(tick, intervalMs);
  }, 20_000);

  console.log(
    `🔄 Self keep-alive ON → ${url} every ${Math.round(intervalMs / 1000)}s`
  );
}

const startServer = async () => {
  try {
    if (!process.env.JWT_SECRET) {
      console.warn("⚠️  JWT_SECRET is missing — login will fail until you set it in Render env vars");
    }
    if (!process.env.JWT_REFRESH_SECRET) {
      console.warn("⚠️  JWT_REFRESH_SECRET is missing — will fall back to JWT_SECRET if set");
    }
    if (!process.env.MONGODB_URI) {
      console.error("❌ MONGODB_URI is missing");
    }

    await connectDB();
    const PORT = process.env.PORT || 5000;
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`🚀 Server running on port ${PORT}`);
      startSelfKeepAlive();
    });
  } catch (error) {
    console.error("❌ Failed to start server:", error.message);
    process.exit(1);
  }
};

startServer();