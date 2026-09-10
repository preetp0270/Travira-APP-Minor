const mongoose = require("mongoose");

const placeSchema = new mongoose.Schema({

    name: {
        type: String,
        required: true
    },

    shortDescription: String,

    description: String,

    city: String,

    state: String,

    country: String,

    location: String,

    imageUrl: String,

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

    visitorsCount: {
        type: Number,
        default: 0
    },

    createdAt: {
        type: Date,
        default: Date.now
    }
});

module.exports = mongoose.model("Place", placeSchema, "places");
