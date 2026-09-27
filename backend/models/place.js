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

  /** Admin (or user) who created this place */
  addedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: true
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

  /** How many users marked this place as visited */
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
