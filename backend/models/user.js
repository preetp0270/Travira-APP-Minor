const mongoose = require("mongoose");

const userSchema = new mongoose.Schema({

    name: {
        type: String,
        required: true
    },

    email: {
        type: String,
        required: true,
        unique: true
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

    refreshTokens: [
        {
            token: {
                type: String,
                required: true
            },
            createdAt: {
                type: Date,
                default: Date.now,
                expires: 2592000
            }
        }
    ],

    createdAt: {
        type: Date,
        default: Date.now
    }
});

module.exports = mongoose.model("User", userSchema);
