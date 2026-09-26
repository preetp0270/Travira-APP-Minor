if (process.env.NODE_ENV !== "production") require("dotenv").config();

const express = require("express");
const cors = require("cors");
const dns = require("dns");
const mongoose = require("mongoose");
const path = require("path");

const placeRoutes = require("./routes/placeroute");
const userRoutes = require("./routes/user");
const adminRoutes = require("./routes/adminRoutes");
const chatRoutes = require("./routes/chatRoutes");

const bcrypt = require("bcrypt");
const User = require("./models/user");

async function seedMainAdmin() {
  try {
    const email = process.env.ROOT_ADMIN_EMAIL;
    let user = await User.findOne({ email });
    if (!user) {
      const hashed = await bcrypt.hash(process.env.ROOT_ADMIN_PASSWORD, 10);
      user = await User.create({
        name: process.env.ROOT_ADMIN_NAME,
        email,
        password: hashed,
        role: "superadmin",
        location: "India",
        phone: ""
      });
      console.log("✅ Main admin created");
    } else if (user.role !== "superadmin") {
      user.role = "superadmin";
      await user.save();
      console.log("✅ Main admin role upgraded to superadmin");
    }
  } catch (e) {
    console.error("seedMainAdmin error:", e.message);
  }
}


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

/** Lightweight keep-alive / uptime endpoints (no DB) — ping every ~10 min to reduce Render cold starts */
app.get("/api/health", (req, res) => {
  res.status(200).json({
    success: true,
    status: "ok",
    service: "travira",
    ts: Date.now(),
    uptime: process.uptime()
  });
});
app.get("/api/ping", (req, res) => {
  res.status(200).json({ success: true, pong: true, ts: Date.now() });
});

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
 *   APP_BASE_URL   e.g. https://travira-app.onrender.com  (required for self-ping)
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
      "⚠️  Self keep-alive skipped: set APP_BASE_URL (e.g. https://travira-app.onrender.com)"
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
    await seedMainAdmin();
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