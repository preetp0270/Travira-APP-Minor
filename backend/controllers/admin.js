/**
 * Admin controller — manage places and users.
 * All routes require authMiddleware + adminMiddleware (admin or superadmin).
 */
const Place = require("../models/place");
const User = require("../models/user");
const bcrypt = require("bcrypt");

// Fields admins are allowed to set on a place (prevents overwriting ratings etc.)
const PLACE_UPDATE_FIELDS = [
  "name",
  "shortDescription",
  "description",
  "city",
  "state",
  "country",
  "location",
  "imageUrl"
];

// ── Places ───────────────────────────────────────────

/** GET /api/admin/places */
exports.getAllPlaces = async (req, res) => {
  try {
    const places = await Place.find({})
      .populate("addedBy", "name email phone location")
      .sort({ createdAt: -1 });

    const counts = {
      total: await Place.countDocuments({})
    };

    res.json({ success: true, places, counts });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** GET /api/admin/places/:id */
exports.getPlaceAdminDetail = async (req, res) => {
  try {
    const place = await Place.findById(req.params.id)
      .populate("addedBy", "name email phone location role")
      .populate("ratings.user", "name email");

    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const wishlistCount = await User.countDocuments({ wishlist: place._id });

    res.json({
      success: true,
      place,
      stats: {
        visitorsCount: place.visitorsCount || 0,
        averageRating: place.averageRating || 0,
        ratingsCount: (place.ratings || []).length,
        wishlistCount
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** PUT /api/admin/places/:id — whitelist body fields only */
exports.updateAnyPlace = async (req, res) => {
  try {
    const existing = await Place.findById(req.params.id);
    if (!existing) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const updates = {};
    for (const key of PLACE_UPDATE_FIELDS) {
      if (req.body[key] !== undefined) updates[key] = req.body[key];
    }

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        message: "No valid fields to update"
      });
    }

    const place = await Place.findByIdAndUpdate(
      req.params.id,
      { $set: updates },
      { new: true }
    ).populate("addedBy", "name email");

    res.json({ success: true, message: "Place updated", place });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** DELETE /api/admin/places/:id */
exports.deleteAnyPlace = async (req, res) => {
  try {
    const place = await Place.findById(req.params.id);
    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    await Place.findByIdAndDelete(req.params.id);

    // Best-effort: pull from users' lists
    await User.updateMany(
      {},
      {
        $pull: {
          wishlist: place._id,
          addedPlaces: place._id,
          visitedPlaces: { place: place._id }
        }
      }
    );

    res.json({ success: true, message: "Place removed by admin" });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** POST /api/admin/places */
exports.adminAddPlace = async (req, res) => {
  try {
    const name = String(req.body?.name || "").trim();
    if (!name) {
      return res.status(400).json({
        success: false,
        message: "Place name is required"
      });
    }

    const payload = { name, addedBy: req.user.id };
    for (const key of PLACE_UPDATE_FIELDS) {
      if (key === "name") continue;
      if (req.body[key] !== undefined) payload[key] = req.body[key];
    }

    const place = new Place(payload);
    await place.save();

    await User.findByIdAndUpdate(req.user.id, {
      $push: { addedPlaces: place._id }
    });

    const populated = await Place.findById(place._id).populate(
      "addedBy",
      "name email"
    );

    res.json({ success: true, message: "Place added", place: populated });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ── Users ────────────────────────────────────────────

/** GET /api/admin/users */
exports.getUsers = async (req, res) => {
  try {
    const users = await User.find({ role: { $in: ["user", "admin"] } })
      .select(
        "-password -refreshTokens -wishlist -addedPlaces -visitedPlaces -notifications"
      )
      .sort({ createdAt: -1 });
    res.json({ success: true, users });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** GET /api/admin/users/:id */
exports.getUserDetail = async (req, res) => {
  try {
    const user = await User.findById(req.params.id)
      .select("-password -refreshTokens")
      .populate("wishlist", "name city imageUrl averageRating")
      .populate("addedPlaces", "name city imageUrl")
      .populate("visitedPlaces.place", "name city imageUrl");

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    res.json({
      success: true,
      user,
      passwordNote: "Password is hashed and cannot be viewed. Use reset if needed."
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** POST /api/admin/users */
exports.adminCreateUser = async (req, res) => {
  try {
    const name = String(req.body?.name || "").trim();
    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = req.body?.password;
    const phone = req.body?.phone || "";
    const location = req.body?.location || "";
    const role = req.body?.role;

    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        message: "name, email, password required"
      });
    }
    if (String(password).length < 6) {
      return res.status(400).json({
        success: false,
        message: "Password must be at least 6 characters"
      });
    }

    const exists = await User.findOne({ email });
    if (exists) {
      return res.status(400).json({ success: false, message: "User already exists" });
    }

    const hashed = await bcrypt.hash(password, 10);
    const user = await User.create({
      name,
      email,
      password: hashed,
      phone,
      location,
      role: role === "admin" ? "admin" : "user"
    });

    res.json({
      success: true,
      message: "User created",
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** PUT /api/admin/users/:id */
exports.adminUpdateUser = async (req, res) => {
  try {
    const target = await User.findById(req.params.id);
    if (!target) {
      return res.status(404).json({ success: false, message: "User not found" });
    }
    if (target.role === "superadmin") {
      return res.status(403).json({
        success: false,
        message: "Cannot modify main admin account"
      });
    }

    const { name, email, phone, location, bio, password, role } = req.body || {};
    if (name !== undefined) target.name = String(name).trim();
    if (email !== undefined) {
      const normalized = String(email).trim().toLowerCase();
      const clash = await User.findOne({
        email: normalized,
        _id: { $ne: target._id }
      });
      if (clash) {
        return res.status(400).json({
          success: false,
          message: "Email already in use"
        });
      }
      target.email = normalized;
    }
    if (phone !== undefined) target.phone = phone;
    if (location !== undefined) target.location = location;
    if (bio !== undefined) target.bio = bio;
    if (password && String(password).length >= 6) {
      target.password = await bcrypt.hash(String(password), 10);
      // Invalidate all sessions after admin password change
      target.refreshTokens = [];
      target.tokenVersion = (target.tokenVersion || 0) + 1;
    }
    if (role === "admin" || role === "user") {
      target.role = role;
    }

    await target.save();

    const safe = await User.findById(target._id)
      .select("-password -refreshTokens")
      .populate("wishlist", "name city imageUrl averageRating")
      .populate("addedPlaces", "name city imageUrl")
      .populate("visitedPlaces.place", "name city imageUrl");

    res.json({ success: true, message: "User updated", user: safe });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/** DELETE /api/admin/users/:id */
exports.adminDeleteUser = async (req, res) => {
  try {
    const target = await User.findById(req.params.id);
    if (!target) {
      return res.status(404).json({ success: false, message: "User not found" });
    }
    if (target.role === "superadmin") {
      return res.status(403).json({
        success: false,
        message: "Cannot delete main admin"
      });
    }
    if (target._id.toString() === req.user.id) {
      return res.status(403).json({
        success: false,
        message: "Cannot delete yourself"
      });
    }

    await User.findByIdAndDelete(target._id);
    res.json({ success: true, message: "User deleted" });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};
