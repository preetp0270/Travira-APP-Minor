const Place = require("../models/place");
const User = require("../models/user");
const bcrypt = require("bcrypt");
if (process.env.NODE_ENV !== "production") require("dotenv").config();

// ================= All places (admin) =================

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
    res.status(500).json({ message: error.message });
  }
};

exports.getPlaceAdminDetail = async (req, res) => {
  try {
    const place = await Place.findById(req.params.id)
      .populate("addedBy", "name email phone location role")
      .populate("ratings.user", "name email");

    if (!place) return res.status(404).json({ message: "Place not found" });

    const wishlistCount = await User.countDocuments({ wishlist: place._id });

    res.json({
      success: true,
      place,
      stats: {
        visitorsCount: place.visitorsCount || 0,
        averageRating: place.averageRating || place.rating || 0,
        ratingsCount: (place.ratings || []).length,
        wishlistCount
      }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.updateAnyPlace = async (req, res) => {
  try {
    const existing = await Place.findById(req.params.id);
    if (!existing) return res.status(404).json({ message: "Place not found" });

    const place = await Place.findByIdAndUpdate(
      req.params.id,
      { ...req.body },
      { new: true }
    ).populate("addedBy", "name email");

    res.json({ success: true, message: "Place updated", place });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.deleteAnyPlace = async (req, res) => {
  try {
    const place = await Place.findById(req.params.id);
    if (!place) return res.status(404).json({ message: "Place not found" });

    await Place.findByIdAndDelete(req.params.id);

    res.json({ success: true, message: "Place removed by admin" });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.adminAddPlace = async (req, res) => {
  try {
    const place = new Place({
      ...req.body,
      addedBy: req.user.id
    });
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
    res.status(500).json({ message: error.message });
  }
};

// ================= Users =================

exports.getUsers = async (req, res) => {
  try {
    const users = await User.find({ role: { $in: ["user", "admin"] } })
      .select("-password -refreshTokens -wishlist -addedPlaces -visitedPlaces -notifications")
      .sort({ createdAt: -1 });
    res.json({ success: true, users });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.getUserDetail = async (req, res) => {
  try {
    const user = await User.findById(req.params.id)
      .select("-password -refreshTokens")
      .populate("wishlist", "name city imageUrl averageRating rating")
      .populate("addedPlaces", "name city imageUrl")
      .populate("visitedPlaces.place", "name city imageUrl");

    if (!user) return res.status(404).json({ message: "User not found" });

    res.json({
      success: true,
      user,
      passwordNote: "Password is hashed and cannot be viewed. Use reset if needed."
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.adminCreateUser = async (req, res) => {
  try {
    const { name, email, password, phone, location, role } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ message: "name, email, password required" });
    }

    const exists = await User.findOne({ email });
    if (exists) return res.status(400).json({ message: "User already exists" });

    const hashed = await bcrypt.hash(password, 10);
    const user = await User.create({
      name,
      email,
      password: hashed,
      phone: phone || "",
      location: location || "",
      role: role === "admin" ? "admin" : "user"
    });

    res.json({
      success: true,
      message: "User created",
      user: { id: user._id, name: user.name, email: user.email, role: user.role }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

/** Admin updates a user (name, email, phone, location, bio, password, role). */
exports.adminUpdateUser = async (req, res) => {
  try {
    const target = await User.findById(req.params.id);
    if (!target) return res.status(404).json({ message: "User not found" });
    if (target.role === "superadmin") {
      return res.status(403).json({ message: "Cannot modify main admin account" });
    }

    const { name, email, phone, location, bio, password, role } = req.body;
    if (name !== undefined) target.name = name;
    if (email !== undefined) {
      const clash = await User.findOne({ email, _id: { $ne: target._id } });
      if (clash) return res.status(400).json({ message: "Email already in use" });
      target.email = email;
    }
    if (phone !== undefined) target.phone = phone;
    if (location !== undefined) target.location = location;
    if (bio !== undefined) target.bio = bio;
    if (password && String(password).length >= 6) {
      target.password = await bcrypt.hash(String(password), 10);
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
    res.status(500).json({ message: error.message });
  }
};

exports.adminDeleteUser = async (req, res) => {
  try {
    const target = await User.findById(req.params.id);
    if (!target) return res.status(404).json({ message: "User not found" });
    if (target.role === "superadmin") {
      return res.status(403).json({ message: "Cannot delete main admin" });
    }
    if (target._id.toString() === req.user.id) {
      return res.status(403).json({ message: "Cannot delete yourself" });
    }

    await User.findByIdAndDelete(target._id);
    res.json({ success: true, message: "User deleted" });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};
