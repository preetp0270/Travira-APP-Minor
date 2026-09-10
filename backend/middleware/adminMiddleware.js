const User = require("../models/user");

/**
 * Allows role: admin or superadmin.
 */
const adminMiddleware = async (req, res, next) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    if (user.role !== "admin" && user.role !== "superadmin") {
      return res.status(403).json({ message: "Access denied. Admin only." });
    }

    req.adminUser = user;
    next();
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = adminMiddleware;
