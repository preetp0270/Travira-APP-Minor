const express = require("express");

const router = express.Router();

const authMiddleware = require("../middleware/authMiddleware");

const {
  getPlaces,
  getPlaceById,
  getMyPlaces,
  addWishlist,
  removeWishlist,
  getWishlist,
  ratePlace
} = require("../controllers/place");

// ── Public ──────────────────────────────────────────
router.get("/", getPlaces);

// ── Authenticated user routes (MUST be before /:id) ─
router.get("/user/my-places", authMiddleware, getMyPlaces);
router.get("/user/wishlist", authMiddleware, getWishlist);

// ── Wishlist / rating ─
router.post("/:id/wishlist", authMiddleware, addWishlist);
router.delete("/:id/wishlist", authMiddleware, removeWishlist);
router.post("/:id/rating", authMiddleware, ratePlace);

// ── Single place (public read only — add/edit/delete is admin-only via /api/admin) ─
router.get("/:id", getPlaceById);

module.exports = router;
