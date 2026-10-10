/**
 * Admin controller — manage places and users.
 * All routes require authMiddleware + adminMiddleware (admin only).
 */
const Place = require("../models/place");
const User = require("../models/user");
const bcrypt = require("bcrypt");
const { visitorCountMap, withLiveStats, ratingStats } = require("../utils/placeStats");
const { importImageFromUrl: importRemoteImage } = require("../utils/imageImport");

// Fields admins are allowed to set on a place (prevents overwriting ratings etc.)
const PLACE_UPDATE_FIELDS = [
  "name",
  "shortDescription",
  "description",
  "city",
  "state",
  "country",
  "location",
  "imageUrl",
  // Sample / AI stats (base numbers users build on top of)
  "baseVisitorsCount",
  "baseRatingsCount",
  "visitorsCount",
  "averageRating"
];

// ── Places ───────────────────────────────────────────

/** GET /api/admin/places */
exports.getAllPlaces = async (req, res) => {
  try {
    const places = await Place.find({})
      .populate("addedBy", "name email phone location")
      .sort({ createdAt: -1 })
      .lean();

    const visitMap = await visitorCountMap(places.map((p) => p._id));
    const withStats = places.map((p) => withLiveStats(p, visitMap));

    const counts = {
      total: await Place.countDocuments({})
    };

    res.json({ success: true, places: withStats, counts });
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
    const visitMap = await visitorCountMap([place._id]);
    const lean = place.toObject ? place.toObject() : place;
    const live = withLiveStats(lean, visitMap);

    res.json({
      success: true,
      place: live,
      stats: {
        visitorsCount: live.visitorsCount,
        averageRating: live.averageRating,
        ratingsCount: live.ratingsCount,
        wishlistCount,
        baseVisitorsCount: lean.baseVisitorsCount || 0,
        baseRatingsCount: lean.baseRatingsCount || 0
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

    // Keep base + displayed in sync when admin/AI sets sample visitor numbers
    if (updates.visitorsCount != null && updates.baseVisitorsCount == null) {
      const n = Number(updates.visitorsCount);
      if (Number.isFinite(n) && n >= 0) {
        updates.baseVisitorsCount = n;
        updates.visitorsCount = n;
      }
    }
    if (updates.baseVisitorsCount != null) {
      const n = Number(updates.baseVisitorsCount);
      if (Number.isFinite(n) && n >= 0) {
        updates.baseVisitorsCount = n;
        if (updates.visitorsCount == null) updates.visitorsCount = n;
      }
    }
    if (updates.baseRatingsCount != null) {
      const n = Number(updates.baseRatingsCount);
      if (Number.isFinite(n) && n >= 0) updates.baseRatingsCount = n;
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

    // Sample/AI numbers become the permanent base; live users add on top.
    if (payload.visitorsCount != null && payload.baseVisitorsCount == null) {
      const n = Number(payload.visitorsCount);
      if (Number.isFinite(n) && n >= 0) {
        payload.baseVisitorsCount = n;
        payload.visitorsCount = n; // initial displayed = base (0 real yet)
      }
    }
    if (payload.baseVisitorsCount != null) {
      const n = Number(payload.baseVisitorsCount);
      if (Number.isFinite(n) && n >= 0) {
        payload.baseVisitorsCount = n;
        if (payload.visitorsCount == null) payload.visitorsCount = n;
      }
    }
    if (payload.baseRatingsCount != null) {
      const n = Number(payload.baseRatingsCount);
      if (Number.isFinite(n) && n >= 0) payload.baseRatingsCount = n;
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
        "-password -refreshTokens -wishlist -addedPlaces -visitedPlaces"
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

/**
 * Extract password from a MongoDB connection URI.
 * Supports mongodb:// and mongodb+srv:// with URL-encoded passwords.
 */
function extractMongoPasswordFromUri(uri) {
  if (!uri || typeof uri !== "string") return null;
  // mongodb[+srv]://user:password@host...
  const m = uri.match(/^mongodb(?:\+srv)?:\/\/([^:@/]+):([^@/]+)@/i);
  if (!m) return null;
  try {
    return decodeURIComponent(m[2]);
  } catch {
    return m[2];
  }
}

function expectedMongoGatePassword() {
  // Prefer explicit bootstrap secret, else password embedded in MONGODB_URI
  if (process.env.ADMIN_BOOTSTRAP_PASSWORD) {
    return String(process.env.ADMIN_BOOTSTRAP_PASSWORD);
  }
  if (process.env.MONGO_PASSWORD) {
    return String(process.env.MONGO_PASSWORD);
  }
  return extractMongoPasswordFromUri(process.env.MONGODB_URI || "");
}

/**
 * POST /api/admin-bootstrap/register  (public — gated by MongoDB password only)
 * Body: { mongoPassword, name, email, password, phone?, location? }
 * Always creates role: "admin"
 */
exports.bootstrapRegisterAdmin = async (req, res) => {
  try {
    const mongoPassword = String(req.body?.mongoPassword || "");
    const name = String(req.body?.name || "").trim();
    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = req.body?.password;
    const phone = String(req.body?.phone || "").trim();
    const location = String(req.body?.location || "").trim();
    const role = "admin";

    if (!mongoPassword) {
      return res.status(400).json({
        success: false,
        message: "MongoDB password is required"
      });
    }

    const expected = expectedMongoGatePassword();
    if (!expected) {
      return res.status(503).json({
        success: false,
        message:
          "Server is not configured for bootstrap (set MONGODB_URI with a password, or ADMIN_BOOTSTRAP_PASSWORD)"
      });
    }

    if (mongoPassword !== expected) {
      return res.status(401).json({
        success: false,
        message: "Invalid MongoDB password"
      });
    }

    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        message: "name, email, and password are required"
      });
    }
    if (String(password).length < 6) {
      return res.status(400).json({
        success: false,
        message: "Account password must be at least 6 characters"
      });
    }

    const exists = await User.findOne({ email });
    if (exists) {
      return res.status(400).json({
        success: false,
        message: "A user with this email already exists"
      });
    }

    const hashed = await bcrypt.hash(String(password), 10);
    const user = await User.create({
      name,
      email,
      password: hashed,
      phone,
      location,
      role
    });

    res.json({
      success: true,
      message: `${role} account created. You can log in on the admin page.`,
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


/** POST /api/admin/import-image  Body: { url: string } */
exports.importImageFromUrl = async (req, res) => {
  try {
    const url = req.body?.url;
    const result = await importRemoteImage(url);
    const viaCloudinary = result.source !== "direct";
    res.json({
      success: true,
      imageUrl: result.imageUrl,
      source: result.source,
      message:
        result.message ||
        (viaCloudinary
          ? "Image ready"
          : "Public image link saved (will show in the app)")
    });
  } catch (error) {
    const status = error.status || 500;
    res.status(status).json({
      success: false,
      message: error.message || "Could not use that link. Try another photo link or upload a file."
    });
  }
};
