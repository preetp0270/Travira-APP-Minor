const express = require("express");
const router = express.Router();

const authMiddleware = require("../middleware/authMiddleware");
const adminMiddleware = require("../middleware/adminMiddleware");

const {
  getAllPlaces,
  getPlaceAdminDetail,
  updateAnyPlace,
  deleteAnyPlace,
  adminAddPlace,
  getUsers,
  getUserDetail,
  adminCreateUser,
  adminUpdateUser,
  adminDeleteUser
} = require("../controllers/admin");

// All routes require logged-in admin (or superadmin)
router.use(authMiddleware, adminMiddleware);

// ── Places (admin only — add / edit / delete) ──
router.get("/places", getAllPlaces);
router.get("/places/:id", getPlaceAdminDetail);
router.put("/places/:id", updateAnyPlace);
router.delete("/places/:id", deleteAnyPlace);
router.post("/places", adminAddPlace);

// ── Users ──
router.get("/users", getUsers);
router.get("/users/:id", getUserDetail);
router.post("/users", adminCreateUser);
router.put("/users/:id", adminUpdateUser);
router.delete("/users/:id", adminDeleteUser);

module.exports = router;
