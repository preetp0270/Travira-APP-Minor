/**
 * Travira backend entry point.
 * Routes:
 *   /api/place, /api/places  → places feed, wishlist, ratings
 *   /api/users               → auth, profile, visited
 *   /api/admin               → admin places & users
 *   /api/chat                → Gemini travel chatbot
 */
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
const userController = require("./controllers/user");
const adminController = require("./controllers/admin");

// Helps with Atlas SRV resolution issues when testing locally
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

app.get("/", (req, res) => res.send("🚀 Travira Backend is Running..."));
// Lightweight ping for external uptime monitors (UptimeRobot, cron-job.org, etc.)
app.get("/api/ping", (req, res) => res.json({ success: true, pong: true }));

// Password reset (also mounted here so they never 404 if the router is stale)
app.post("/api/users/forgot-password", userController.forgotPassword);
app.post("/api/users/reset-password", userController.resetPassword);
app.post("/api/auth/forgot-password", userController.forgotPassword);
app.post("/api/auth/reset-password", userController.resetPassword);

app.use("/api/place", placeRoutes);
app.use("/api/places", placeRoutes); // alias for older Android clients
app.use("/api/users", userRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/chat", chatRoutes);

// Public bootstrap: create admin after proving MongoDB password
// (HTML form: /admin-register.html)
app.post("/api/admin-bootstrap/register", adminController.bootstrapRegisterAdmin);

app.use((req, res) =>
  res.status(404).json({ success: false, message: "API Route Not Found" })
);

const connectDB = async () => {
  await mongoose.connect(process.env.MONGODB_URI, {
    serverSelectionTimeoutMS: 10000,
  });
  console.log("✅ MongoDB Connected Successfully");
};

const startServer = async () => {
  try {
    if (!process.env.JWT_SECRET) {
      console.warn("⚠️  JWT_SECRET is missing — login will fail until you set it");
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

      // Self-ping keep-alive: hits /api/ping every 9 minutes so the
      // process stays active longer on Render (helps during demos/submission).
      // Set APP_BASE_URL or RENDER_EXTERNAL_URL in Render Environment, e.g.
      //   APP_BASE_URL=https://travira-app-minor.onrender.com
      const url = process.env.RENDER_EXTERNAL_URL || process.env.APP_BASE_URL;
      if (!url) {
        console.warn("⚠️  Keep-alive skipped: set APP_BASE_URL (or RENDER_EXTERNAL_URL)");
        return;
      }

      const pingUrl = `${url.replace(/\/$/, "")}/api/ping`;
      console.log(`🔄 Keep-alive started → ${pingUrl} every 9 min`);

      setInterval(async () => {
        try {
          const res = await fetch(pingUrl);
          console.log(
            `✅ Keep-alive ok (ping ${res.status}) at ${new Date().toLocaleTimeString()}`
          );
        } catch (e) {
          console.warn(`❌ Keep-alive failed: ${e.message}`);
        }
      }, 9 * 60 * 1000);
    });
  } catch (error) {
    console.error("❌ Failed to start server:", error.message);
    process.exit(1);
  }
};

startServer();
