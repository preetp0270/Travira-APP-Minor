/**
 * User model — accounts, wishlist, visited places, notifications, auth tokens.
 *
 * Roles: "user" | "admin" | "superadmin"
 * tokenVersion is bumped on password reset so old JWTs fail everywhere.
 */
const mongoose = require("mongoose");

const userSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },

  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true
  },

  password: {
    type: String,
    required: true
  },

  phone: {
    type: String,
    default: ""
  },

  location: {
    type: String,
    default: ""
  },

  bio: {
    type: String,
    default: ""
  },

  /** Prefer email alerts (in-app still stored when inAppNotifications is true) */
  emailNotifications: {
    type: Boolean,
    default: true
  },

  /** In-app notification preference */
  inAppNotifications: {
    type: Boolean,
    default: true
  },

  resetPasswordToken: {
    type: String,
    default: null
  },

  resetPasswordExpires: {
    type: Date,
    default: null
  },

  /**
   * Bumped on password reset so existing access JWTs become invalid
   * (authMiddleware compares JWT.tv to this value).
   */
  tokenVersion: {
    type: Number,
    default: 0
  },

  addedPlaces: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Place"
    }
  ],

  wishlist: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Place"
    }
  ],

  visitedPlaces: [
    {
      place: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "Place"
      },
      visitedAt: {
        type: Date,
        default: Date.now
      }
    }
  ],

  notifications: [
    {
      title: {
        type: String,
        required: true
      },
      message: {
        type: String,
        required: true
      },
      read: {
        type: Boolean,
        default: false
      },
      createdAt: {
        type: Date,
        default: Date.now
      }
    }
  ],

  role: {
    type: String,
    enum: ["user", "admin", "superadmin"],
    default: "user"
  },

  /**
   * Stored refresh tokens for logout / session invalidation.
   * NOTE: Do NOT put MongoDB TTL `expires` on nested createdAt —
   * TTL indexes delete the entire parent document, which would wipe users.
   * Token age is enforced in refreshToken handler instead.
   */
  refreshTokens: [
    {
      token: {
        type: String,
        required: true
      },
      createdAt: {
        type: Date,
        default: Date.now
      }
    }
  ],

  createdAt: {
    type: Date,
    default: Date.now
  }
});

module.exports = mongoose.model("User", userSchema);
