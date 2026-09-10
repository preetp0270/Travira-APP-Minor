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
  removeVisitedPlace
} = require("../controllers/user");

router.post("/register", register);
router.post("/login", login);
router.post("/refresh-token", refreshToken);

router.get("/profile", authMiddleware, profile);
router.put("/profile", authMiddleware, updateProfile);
router.get("/me", authMiddleware, getCurrentUser);

router.get("/notifications", authMiddleware, getNotifications);
router.put("/notifications/read", authMiddleware, markNotificationsRead);

router.get("/visited", authMiddleware, getVisitedPlaces);
router.post("/visited/:id", authMiddleware, addVisitedPlace);
router.delete("/visited/:id", authMiddleware, removeVisitedPlace);

router.post("/logout", authMiddleware, logout);
router.post("/logout-all", authMiddleware, logoutAll);

module.exports = router;
