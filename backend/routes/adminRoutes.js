/**
 * Admin routes — mounted at /api/admin
 * All routes require authMiddleware + adminMiddleware (admin only)
 */
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
  adminDeleteUser,
  importImageFromUrl
} = require("../controllers/admin");

router.use(authMiddleware, adminMiddleware);

// Places
router.get("/places", getAllPlaces);
router.get("/places/:id", getPlaceAdminDetail);
router.put("/places/:id", updateAnyPlace);
router.delete("/places/:id", deleteAnyPlace);
router.post("/places", adminAddPlace);

// Image: fetch remote URL → Cloudinary (handles many 403 cases + HTML og:image)
router.post("/import-image", importImageFromUrl);

// Users
router.get("/users", getUsers);
router.get("/users/:id", getUserDetail);
router.post("/users", adminCreateUser);
router.put("/users/:id", adminUpdateUser);
router.delete("/users/:id", adminDeleteUser);

module.exports = router;
