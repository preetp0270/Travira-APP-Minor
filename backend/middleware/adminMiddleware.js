/**
 * Admin gate — must run AFTER authMiddleware.
 * Allows role: "admin" only.
 * Legacy "superadmin" accounts are normalized to "admin" once.
 * Sets req.adminUser to the full user document.
 */
const User = require("../models/user");

const adminMiddleware = async (req, res, next) => {
  try {
    if (!req.user || !req.user.id) {
      return res.status(401).json({ message: "Unauthorized user" });
    }

    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    // One-time migration: old superadmin → admin
    if (user.role === "superadmin") {
      user.role = "admin";
      await user.save();
    }

    if (user.role !== "admin") {
      return res.status(403).json({ message: "Access denied. Admin only." });
    }

    req.adminUser = user;
    next();
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = adminMiddleware;
