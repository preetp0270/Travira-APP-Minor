/**
 * Place routes — mounted at /api/place (and alias /api/places)
 * Public: list places, get by id
 * Auth: wishlist, rating, my-places
 * Create/edit/delete places is admin-only → /api/admin/places
 */
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

// Public
router.get("/", getPlaces);

// Authenticated fixed paths MUST be before /:id
router.get("/user/my-places", authMiddleware, getMyPlaces);
router.get("/user/wishlist", authMiddleware, getWishlist);

// Wishlist / rating
router.post("/:id/wishlist", authMiddleware, addWishlist);
router.delete("/:id/wishlist", authMiddleware, removeWishlist);
router.post("/:id/rating", authMiddleware, ratePlace);

// Single place (public read)
router.get("/:id", getPlaceById);

module.exports = router;
