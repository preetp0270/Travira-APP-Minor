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
app.use("/api/place", placeRoutes);
app.use("/api/places", placeRoutes); // alias for older Android clients
app.use("/api/users", userRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api/chat", chatRoutes);

app.use((req, res) => res.status(404).json({ success: false, message: "API Route Not Found" }));

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
    app.listen(PORT, "0.0.0.0", () => console.log(`🚀 Server running on port ${PORT}`));
  } catch (error) {
    console.error("❌ Failed to start server:", error.message);
    process.exit(1);
  }
};

startServer();