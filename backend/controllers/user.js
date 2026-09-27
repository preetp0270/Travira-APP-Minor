/**
 * User controller — register, login, profile, password reset,
 * wishlist/visited helpers, notifications, logout.
 */
const crypto = require("crypto");
const User = require("../models/user");
const Place = require("../models/place");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { sendMail, appBaseUrl, resetPasswordHtml } = require("../utils/mailer");
const { syncVisitorsCount, visitorCountMap, withLiveStats } = require("../utils/placeStats");

/** Access JWT — includes tokenVersion (tv) so password reset invalidates sessions */
const generateAccessToken = (user) => {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET is not set. Add it in Render Environment variables.");
  }
  return jwt.sign(
    {
      userId: user._id,
      email: user.email,
      tv: user.tokenVersion || 0
    },
    secret,
    { expiresIn: "30d" }
  );
};

const generateRefreshToken = (user) => {
  const secret = process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_REFRESH_SECRET (or JWT_SECRET) is not set.");
  }
  return jwt.sign(
    { userId: user._id, tv: user.tokenVersion || 0 },
    secret,
    { expiresIn: "30d" }
  );
};

/** Prepend an in-app notification (respects inAppNotifications preference) */
function pushInApp(user, title, message) {
  if (user.inAppNotifications === false) return;
  user.notifications = user.notifications || [];
  user.notifications.unshift({
    title,
    message,
    read: false,
    createdAt: new Date()
  });
  // Keep last 50
  if (user.notifications.length > 50) {
    user.notifications = user.notifications.slice(0, 50);
  }
}

// ── Register ─────────────────────────────────────────

/** POST /api/users/register */
exports.register = async (req, res) => {
  try {
    const { name, email, password } = req.body || {};
    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        message: "Name, email and password are required"
      });
    }
    if (String(password).length < 6) {
      return res.status(400).json({
        success: false,
        message: "Password must be at least 6 characters"
      });
    }

    const normalizedEmail = String(email).trim().toLowerCase();
    const existingUser = await User.findOne({ email: normalizedEmail });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: "User already exists"
      });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const user = await User.create({
      name: String(name).trim(),
      email: normalizedEmail,
      password: hashedPassword
    });

    pushInApp(
      user,
      "Welcome to Travira",
      "Your account was created successfully. Explore places and try Travira AI."
    );
    await user.save();

    res.json({
      success: true,
      message: "Registration successful",
      user: {
        id: user._id,
        name: user.name,
        email: user.email
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Login ────────────────────────────────────────────

/** POST /api/users/login */
exports.login = async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: "Email and password are required"
      });
    }

    const user = await User.findOne({
      email: String(email).trim().toLowerCase()
    });
    // Same response for missing user / bad password (avoid account enumeration)
    if (!user) {
      return res.status(401).json({
        success: false,
        message: "Invalid email or password"
      });
    }

    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.status(401).json({
        success: false,
        message: "Invalid email or password"
      });
    }

    // Legacy: normalize old superadmin role → admin
    if (user.role === "superadmin") {
      user.role = "admin";
    }

    const accessToken = generateAccessToken(user);
    const refreshToken = generateRefreshToken(user);

    user.refreshTokens = user.refreshTokens || [];
    user.refreshTokens.push({ token: refreshToken, createdAt: new Date() });
    // Cap stored refresh tokens (prevent unbounded growth)
    if (user.refreshTokens.length > 10) {
      user.refreshTokens = user.refreshTokens.slice(-10);
    }

    const when = new Date().toUTCString();
    pushInApp(
      user,
      "New login",
      `You signed in on ${when}. If this wasn't you, reset your password.`
    );
    await user.save();

    res.json({
      success: true,
      message: "Login successful",
      accessToken,
      refreshToken,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        phone: user.phone || "",
        location: user.location || ""
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Forgot / Reset password ──────────────────────────

/** POST /api/users/forgot-password */
exports.forgotPassword = async (req, res) => {
  try {
    const email = String(req.body?.email || "").trim().toLowerCase();
    if (!email) {
      return res.status(400).json({ success: false, message: "Email is required" });
    }

    const user = await User.findOne({ email });
    // Always respond success to avoid email enumeration
    if (!user) {
      return res.json({
        success: true,
        message: "If that email is registered, a reset link has been sent."
      });
    }

    const token = crypto.randomBytes(32).toString("hex");
    user.resetPasswordToken = crypto.createHash("sha256").update(token).digest("hex");
    user.resetPasswordExpires = new Date(Date.now() + 60 * 60 * 1000); // 1 hour
    await user.save();

    const link = `${appBaseUrl()}/reset-password.html?token=${token}`;
    console.log(
      `[forgot-password] requested for ${user.email} emailWillSend=${Boolean(
        process.env.BREVO_API_KEY ||
          process.env.SENDGRID_API_KEY ||
          process.env.RESEND_API_KEY ||
          process.env.EMAIL_HOST
      )}`
    );

    const mailResult = await sendMail({
      to: user.email,
      subject: "Travira — reset your password",
      html: resetPasswordHtml(user.name, link),
      text: `Reset your Travira password: ${link}`
    });

    pushInApp(
      user,
      "Password reset requested",
      mailResult.sent
        ? "A password reset link was sent to your email (valid 1 hour)."
        : "Password reset requested. If email delivery failed, try again later or contact support."
    );
    await user.save();

    res.json({
      success: true,
      message: mailResult.sent
        ? "Reset link sent to your email. Check inbox (and spam)."
        : "If that email is registered, a reset link will arrive shortly. Check inbox and spam.",
      emailSent: !!mailResult.sent
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** POST /api/users/reset-password */
exports.resetPassword = async (req, res) => {
  try {
    const { token, password, confirmPassword } = req.body || {};
    if (!token || !password) {
      return res.status(400).json({
        success: false,
        message: "Token and new password are required"
      });
    }
    if (confirmPassword !== undefined && password !== confirmPassword) {
      return res.status(400).json({
        success: false,
        message: "Password and confirm password do not match"
      });
    }
    if (String(password).length < 6) {
      return res.status(400).json({
        success: false,
        message: "Password must be at least 6 characters"
      });
    }

    const hashedToken = crypto.createHash("sha256").update(String(token)).digest("hex");
    const user = await User.findOne({
      resetPasswordToken: hashedToken,
      resetPasswordExpires: { $gt: new Date() }
    });

    if (!user) {
      return res.status(400).json({
        success: false,
        message: "Reset link is invalid or has expired. Request a new one."
      });
    }

    user.password = await bcrypt.hash(password, 10);
    user.resetPasswordToken = null;
    user.resetPasswordExpires = null;
    // Log out all devices
    user.refreshTokens = [];
    user.tokenVersion = (user.tokenVersion || 0) + 1;
    pushInApp(
      user,
      "Password changed",
      "Your password was updated. You were signed out on all other devices."
    );
    await user.save();

    res.json({
      success: true,
      message:
        "Password updated. All other devices were signed out. Log in with your new password."
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Refresh token ────────────────────────────────────

/** POST /api/users/refresh-token */
exports.refreshToken = async (req, res) => {
  try {
    const { refreshToken } = req.body || {};
    if (!refreshToken) {
      return res.status(401).json({
        success: false,
        message: "Refresh token required"
      });
    }

    const secret = process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET;
    const decoded = jwt.verify(refreshToken, secret);
    const user = await User.findById(decoded.userId);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    // Reject if password was reset after this refresh token was issued
    if ((decoded.tv || 0) !== (user.tokenVersion || 0)) {
      return res.status(403).json({
        success: false,
        message: "Session expired. Please log in again."
      });
    }

    const stored = (user.refreshTokens || []).find(
      (item) => item.token === refreshToken
    );
    if (!stored) {
      return res.status(403).json({
        success: false,
        message: "Invalid refresh token"
      });
    }

    // Soft max age: 30 days from when we stored it
    const maxAgeMs = 30 * 24 * 60 * 60 * 1000;
    if (stored.createdAt && Date.now() - new Date(stored.createdAt).getTime() > maxAgeMs) {
      user.refreshTokens = (user.refreshTokens || []).filter(
        (item) => item.token !== refreshToken
      );
      await user.save();
      return res.status(403).json({
        success: false,
        message: "Refresh token expired. Please log in again."
      });
    }

    const accessToken = generateAccessToken(user);
    res.json({ success: true, accessToken });
  } catch (error) {
    res.status(403).json({ success: false, message: "Invalid refresh token" });
  }
};

// ── Profile ──────────────────────────────────────────

/** GET /api/users/profile */
exports.profile = async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select("-password -refreshTokens");
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }
    res.json({ success: true, user });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** GET /api/users/me — profile + populated lists */
exports.getCurrentUser = async (req, res) => {
  try {
    const user = await User.findById(req.user.id)
      .select("-password -refreshTokens")
      .populate(
        "addedPlaces",
        "name shortDescription description city state country location imageUrl averageRating visitorsCount createdAt"
      )
      .populate(
        "wishlist",
        "name shortDescription description city state country location imageUrl averageRating visitorsCount"
      )
      .populate({
        path: "visitedPlaces.place",
        select:
          "name shortDescription description city state country location imageUrl averageRating visitorsCount"
      });

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    res.json({ success: true, user });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** PUT /api/users/profile */
exports.updateProfile = async (req, res) => {
  try {
    const allowed = [
      "name",
      "phone",
      "location",
      "bio",
      "emailNotifications",
      "inAppNotifications"
    ];
    const updates = {};
    for (const key of allowed) {
      if (req.body[key] !== undefined) updates[key] = req.body[key];
    }

    const user = await User.findByIdAndUpdate(
      req.user.id,
      { $set: updates },
      { new: true }
    ).select("-password -refreshTokens");

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    res.json({ success: true, message: "Profile updated", user });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Notifications ────────────────────────────────────

/** GET /api/users/notifications */
exports.getNotifications = async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select("notifications");
    res.json({ success: true, notifications: user?.notifications || [] });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** PUT /api/users/notifications/read — body: { ids?: string[] } */
exports.markNotificationsRead = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const { ids } = req.body || {};
    if (Array.isArray(ids) && ids.length > 0) {
      user.notifications.forEach((n) => {
        if (ids.includes(n._id.toString())) n.read = true;
      });
    } else {
      user.notifications.forEach((n) => {
        n.read = true;
      });
    }
    await user.save();

    res.json({
      success: true,
      message: "Notifications marked as read",
      notifications: user.notifications
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Logout ───────────────────────────────────────────

/** POST /api/users/logout — remove one refresh token */
exports.logout = async (req, res) => {
  try {
    const { refreshToken } = req.body || {};
    const user = await User.findById(req.user.id);
    if (user) {
      user.refreshTokens = (user.refreshTokens || []).filter(
        (item) => item.token !== refreshToken
      );
      await user.save();
    }
    res.json({ success: true, message: "Logged out successfully" });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** POST /api/users/logout-all — clear all refresh tokens */
exports.logoutAll = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (user) {
      user.refreshTokens = [];
      await user.save();
    }
    res.json({ success: true, message: "Logged out from all devices" });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Visited places ───────────────────────────────────

/** GET /api/users/visited */
exports.getVisitedPlaces = async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate({
      path: "visitedPlaces.place",
      select:
        "name shortDescription description city state country location imageUrl averageRating visitorsCount"
    });

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const raw = (user.visitedPlaces || [])
      .filter((v) => v.place)
      .map((v) => ({
        ...(v.place.toObject ? v.place.toObject() : v.place),
        visitedAt: v.visitedAt
      }));
    const visitMap = await visitorCountMap(raw.map((p) => p._id));
    const places = raw.map((p) => ({
      ...withLiveStats(p, visitMap),
      visitedAt: p.visitedAt
    }));

    res.json({ success: true, places });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** POST /api/users/visited/:id */
exports.addVisitedPlace = async (req, res) => {
  try {
    const placeId = req.params.id;
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const place = await Place.findById(placeId);
    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const exists = (user.visitedPlaces || []).some(
      (v) => v.place && v.place.toString() === placeId
    );
    if (exists) {
      // Still return live count so UI stays correct
      const visitorsCount = await syncVisitorsCount(placeId);
      return res.json({
        success: true,
        message: "Already marked as visited",
        visitorsCount
      });
    }

    user.visitedPlaces.push({ place: placeId, visitedAt: new Date() });
    await user.save();

    // Displayed = baseVisitorsCount (sample) + real users who marked visited
    const visitorsCount = await syncVisitorsCount(placeId);

    res.json({
      success: true,
      message: "Marked as visited",
      visitorsCount
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** DELETE /api/users/visited/:id */
exports.removeVisitedPlace = async (req, res) => {
  try {
    const placeId = req.params.id;
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    user.visitedPlaces = (user.visitedPlaces || []).filter(
      (v) => !v.place || v.place.toString() !== placeId
    );
    await user.save();

    // Displayed = baseVisitorsCount (sample) + remaining real visitors
    const visitorsCount = await syncVisitorsCount(placeId);

    res.json({
      success: true,
      message: "Removed from visited places",
      visitorsCount
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};
