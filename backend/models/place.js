/**
 * Place model — tourist destinations shown in the Travira app feed.
 * Places are created/edited by admins (see /api/admin/places).
 * Users can wishlist, rate, and mark places as visited.
 */
const mongoose = require("mongoose");

const placeSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    trim: true
  },

  shortDescription: {
    type: String,
    default: ""
  },

  description: {
    type: String,
    default: ""
  },

  city: {
    type: String,
    default: ""
  },

  state: {
    type: String,
    default: ""
  },

  country: {
    type: String,
    default: ""
  },

  /** Free-text address / area (e.g. "Citylight, Surat") */
  location: {
    type: String,
    default: ""
  },

  imageUrl: {
    type: String,
    default: ""
  },

  /** Admin (or user) who created this place (optional for AI/sample seed data) */
  addedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: false
  },

  ratings: [
    {
      user: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User"
      },
      value: {
        type: Number,
        min: 1,
        max: 5
      },
      feedback: {
        type: String,
        default: ""
      },
      createdAt: {
        type: Date,
        default: Date.now
      }
    }
  ],

  averageRating: {
    type: Number,
    default: 0
  },

  /**
   * Sample / AI base visitor number. Never overwritten by user mark/unmark.
   * Displayed visitorsCount = baseVisitorsCount + real User.visitedPlaces count.
   */
  baseVisitorsCount: {
    type: Number,
    default: 0
  },

  /**
   * Sample / AI base review count. Displayed ratingsCount =
   * baseRatingsCount + place.ratings.length
   */
  baseRatingsCount: {
    type: Number,
    default: 0
  },

  /**
   * Cached displayed visitor total (base + real). Updated on mark/unmark.
   * Prefer reading via withLiveStats which always recomputes from base + live.
   */
  visitorsCount: {
    type: Number,
    default: 0
  },

  createdAt: {
    type: Date,
    default: Date.now
  }
});

// Explicit collection name "places"
module.exports = mongoose.model("Place", placeSchema, "places");
