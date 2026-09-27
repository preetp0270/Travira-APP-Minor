/**
 * Place controller — public feed, wishlist, ratings.
 * Place create/edit/delete is admin-only (see controllers/admin.js).
 *
 * visitorsCount = baseVisitorsCount (sample/AI) + real users who marked visited
 * ratingsCount  = baseRatingsCount (sample/AI) + place.ratings.length
 * averageRating = real average if any ratings, else seed averageRating
 */
const Place = require("../models/place");
const User = require("../models/user");
const {
  visitorCountMap,
  ratingStats,
  withLiveStats
} = require("../utils/placeStats");

// ── Public feed ──────────────────────────────────────

/** GET /api/place — list all places (newest first) */
exports.getPlaces = async (req, res) => {
  try {
    const places = await Place.find({})
      .populate("addedBy", "name email")
      .sort({ createdAt: -1 })
      .lean();

    const visitMap = await visitorCountMap(places.map((p) => p._id));
    const data = places.map((p) => withLiveStats(p, visitMap));

    res.json({
      success: true,
      data
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

/** GET /api/place/:id — single place detail */
exports.getPlaceById = async (req, res) => {
  try {
    const place = await Place.findById(req.params.id)
      .populate("addedBy", "name email")
      .lean();

    if (!place) {
      return res.status(404).json({
        success: false,
        message: "Place not found"
      });
    }

    const visitMap = await visitorCountMap([place._id]);
    res.json({
      success: true,
      place: withLiveStats(place, visitMap)
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

// ── My places (legacy — places this user added) ──────

/** GET /api/place/user/my-places */
exports.getMyPlaces = async (req, res) => {
  try {
    const places = await Place.find({
      addedBy: req.user.id
    })
      .populate("addedBy", "name email")
      .lean();

    const visitMap = await visitorCountMap(places.map((p) => p._id));
    res.json({
      success: true,
      places: places.map((p) => withLiveStats(p, visitMap))
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

// ── Wishlist ─────────────────────────────────────────

/** POST /api/place/:id/wishlist */
exports.addWishlist = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const place = await Place.findById(req.params.id);
    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const already = (user.wishlist || []).some(
      (id) => id.toString() === req.params.id
    );
    if (already) {
      return res.json({
        success: true,
        message: "Already in wishlist"
      });
    }

    user.wishlist.push(req.params.id);
    await user.save();

    res.json({
      success: true,
      message: "Added to wishlist"
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

/** DELETE /api/place/:id/wishlist */
exports.removeWishlist = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    user.wishlist = (user.wishlist || []).filter(
      (id) => id.toString() !== req.params.id
    );
    await user.save();

    res.json({
      success: true,
      message: "Removed from wishlist"
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

/** GET /api/place/user/wishlist */
exports.getWishlist = async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate(
      "wishlist",
      "name shortDescription description city state country location imageUrl averageRating visitorsCount ratings"
    );

    const list = user?.wishlist || [];
    const lean = list.map((p) => (p.toObject ? p.toObject() : p));
    const visitMap = await visitorCountMap(lean.map((p) => p._id));

    res.json({
      success: true,
      wishlist: lean.map((p) => withLiveStats(p, visitMap))
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};

// ── Rating ───────────────────────────────────────────
// Body: { value: 1-5, feedback?: string }

/** POST /api/place/:id/rating */
exports.ratePlace = async (req, res) => {
  try {
    const raw = req.body?.value;
    const value = Number(raw);
    const feedback =
      typeof req.body?.feedback === "string" ? req.body.feedback.trim() : "";

    if (!Number.isFinite(value) || value < 1 || value > 5) {
      return res.status(400).json({
        success: false,
        message: "Rating must be a number between 1 and 5"
      });
    }

    const place = await Place.findById(req.params.id);
    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const existing = (place.ratings || []).find(
      (r) => r.user && r.user.toString() === req.user.id
    );

    if (existing) {
      existing.value = value;
      if (feedback) existing.feedback = feedback;
      existing.createdAt = new Date();
    } else {
      place.ratings.push({
        user: req.user.id,
        value,
        feedback: feedback || "",
        createdAt: new Date()
      });
    }

    // Real average from ratings array; displayed counts include sample base
    const { averageRating: realAvg } = ratingStats(place.ratings);
    if (realAvg > 0) place.averageRating = realAvg;
    // Sample/AI places may lack required addedBy — only validate changed paths
    await place.save({ validateModifiedOnly: true });

    const visitMap = await visitorCountMap([place._id]);
    const lean = place.toObject ? place.toObject() : place;
    const live = withLiveStats(lean, visitMap);

    res.json({
      success: true,
      message: existing ? "Rating updated" : "Rating submitted",
      averageRating: live.averageRating,
      ratingsCount: live.ratingsCount,
      visitorsCount: live.visitorsCount,
      place: {
        _id: place._id,
        averageRating: live.averageRating,
        rating: live.averageRating,
        visitorsCount: live.visitorsCount,
        ratingsCount: live.ratingsCount
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
};
