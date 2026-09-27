/**
 * JWT auth middleware.
 * - Requires Authorization: Bearer <accessToken>
 * - Rejects tokens issued before a password reset (tokenVersion mismatch)
 * - Sets req.user = { id, email }
 */
const jwt = require("jsonwebtoken");
const User = require("../models/user");

const authMiddleware = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
      return res.status(401).json({ message: "No token provided" });
    }

    const parts = authHeader.split(" ");
    if (parts.length !== 2 || parts[0] !== "Bearer") {
      return res.status(401).json({ message: "Invalid token format" });
    }

    const token = parts[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const userId = decoded.userId || decoded.id;
    const user = await User.findById(userId).select("tokenVersion email role");
    if (!user) {
      return res.status(401).json({ message: "Unauthorized user" });
    }

    // Password reset bumps tokenVersion → force re-login on all devices
    const tokenTv = decoded.tv != null ? decoded.tv : 0;
    const currentTv = user.tokenVersion || 0;
    if (tokenTv !== currentTv) {
      return res.status(401).json({
        message: "Session expired after password change. Please log in again."
      });
    }

    req.user = { id: userId, email: decoded.email || user.email };
    next();
  } catch (error) {
    return res.status(401).json({ message: "Unauthorized user" });
  }
};

module.exports = authMiddleware;
