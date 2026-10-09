/**
 * User routes — mounted at /api/users
 * Public: register, login, refresh, forgot/reset password
 * Protected (authMiddleware): profile, notifications, visited, logout
 */
const express = require("express");
const router = express.Router();
const authMiddleware = require("../middleware/authMiddleware");
const {
  register,
  login,
  profile,
  getCurrentUser,
  updateProfile,
  refreshToken,
  logout,
  logoutAll,
  getNotifications,
  markNotificationsRead,
  getVisitedPlaces,
  addVisitedPlace,
  removeVisitedPlace,
  getMyRatings,
  forgotPassword,
  resetPassword
} = require("../controllers/user");

// Public
router.post("/register", register);
router.post("/login", login);
router.post("/refresh-token", refreshToken);
router.post("/forgot-password", forgotPassword);
router.post("/reset-password", resetPassword);

// Authenticated
router.get("/profile", authMiddleware, profile);
router.put("/profile", authMiddleware, updateProfile);
router.get("/me", authMiddleware, getCurrentUser);

router.get("/notifications", authMiddleware, getNotifications);
router.put("/notifications/read", authMiddleware, markNotificationsRead);

router.get("/visited", authMiddleware, getVisitedPlaces);
router.post("/visited/:id", authMiddleware, addVisitedPlace);
router.delete("/visited/:id", authMiddleware, removeVisitedPlace);

router.get("/my-ratings", authMiddleware, getMyRatings);

router.post("/logout", authMiddleware, logout);
router.post("/logout-all", authMiddleware, logoutAll);

module.exports = router;
