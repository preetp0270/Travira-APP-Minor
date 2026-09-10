const User = require("../models/user");
const Place = require("../models/place");

const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");




// Generate Access Token

const generateAccessToken = (user)=>{

    const secret = process.env.JWT_SECRET;
    if (!secret) {
        throw new Error("JWT_SECRET is not set. Add it in Render Environment variables.");
    }

    return jwt.sign(

        {
            userId:user._id,
            email:user.email
        },

        secret,

        {
            expiresIn:"30d"
        }

    );

};





// Generate Refresh Token

const generateRefreshToken = (user)=>{

    const secret = process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET;
    if (!secret) {
        throw new Error("JWT_REFRESH_SECRET (or JWT_SECRET) is not set. Add it in Render Environment variables.");
    }

    return jwt.sign(

        {
            userId:user._id
        },

        secret,

        {
            expiresIn:"30d"
        }

    );

};







// ================= Register =================


exports.register = async(req,res)=>{

try{


const {
name,
email,
password
}=req.body;



const existingUser =
await User.findOne({email});



if(existingUser){

return res.status(400).json({

message:"User already exists"

});

}




const hashedPassword =
await bcrypt.hash(password,10);



const user =
await User.create({

name,
email,
password:hashedPassword

});




res.json({

message:"Registration successful",

user:{

id:user._id,

name:user.name,

email:user.email

}

});



}catch(error){

res.status(500).json({

message:error.message

});

}


};










// ================= Login =================


exports.login = async(req,res)=>{

try{


const {
email,
password
}=req.body;



const user =
await User.findOne({email});



if(!user){

return res.status(404).json({

message:"User not found"

});

}




const match =
await bcrypt.compare(

password,

user.password

);



if(!match){

return res.status(400).json({

message:"Invalid password"

});

}




const accessToken =
generateAccessToken(user);



const refreshToken =
generateRefreshToken(user);




user.refreshTokens.push({

token:refreshToken

});


await user.save();




res.json({

message:"Login successful",

accessToken,

refreshToken,


user:{

id:user._id,

name:user.name,

email:user.email,

role:user.role,

phone:user.phone || "",

location:user.location || ""

}

});



}catch(error){

res.status(500).json({

message:error.message

});

}


};









// ================= Refresh Token =================


exports.refreshToken = async(req,res)=>{

try{


const {
refreshToken
}=req.body;



if(!refreshToken){

return res.status(401).json({

message:"Refresh token required"

});

}




const decoded =
jwt.verify(

refreshToken,

process.env.JWT_REFRESH_SECRET

);



const user =
await User.findById(
decoded.userId
);



if(!user){

return res.status(404).json({

message:"User not found"

});

}




const exists =
user.refreshTokens.some(

item=>item.token===refreshToken

);



if(!exists){

return res.status(403).json({

message:"Invalid refresh token"

});

}




const accessToken =
generateAccessToken(user);



res.json({

accessToken

});



}catch(error){

res.status(403).json({

message:"Invalid refresh token"

});

}

};









// ================= Profile =================


exports.profile = async(req,res)=>{

try{


const user =
await User.findById(req.user.id)

.select("-password -refreshTokens");



res.json({

success:true,

user

});



}catch(error){

res.status(500).json({

message:error.message

});

}

};









// ================= Current User =================


exports.getCurrentUser = async (req, res) => {
  try {
    const user = await User.findById(req.user.id)
      .select("-password -refreshTokens")
      .populate(
        "addedPlaces",
        "name shortDescription description city state country location imageUrl averageRating visitorsCount createdAt"
      )
      .populate(
        "wishlist",
        "name shortDescription description city state country location imageUrl averageRating visitorsCount"
      )
      .populate({
        path: "visitedPlaces.place",
        select:
          "name shortDescription description city state country location imageUrl averageRating visitorsCount"
      });

    res.json({
      success: true,
      user
    });
  } catch (error) {
    res.status(500).json({
      message: error.message
    });
  }
};









// ================= Notifications =================


exports.getNotifications = async(req,res)=>{

try{


const user =
await User.findById(req.user.id)

.select("notifications");



res.json({

success:true,

notifications:user.notifications

});



}catch(error){

res.status(500).json({

success:false,

message:error.message

});

}

};









// ================= Logout =================


exports.logout = async(req,res)=>{

try{


const {
refreshToken
}=req.body;



const user =
await User.findById(req.user.id);



user.refreshTokens =
user.refreshTokens.filter(

item=>item.token!==refreshToken

);



await user.save();



res.json({

success:true,

message:"Logged out successfully"

});



}catch(error){

res.status(500).json({

message:error.message

});

}

};









// ================= Logout All =================


exports.logoutAll = async(req,res)=>{

try{


const user =
await User.findById(req.user.id);



user.refreshTokens=[];


await user.save();



res.json({

success:true,

message:"Logged out from all devices"

});


}catch(error){

res.status(500).json({

message:error.message

});

}

};

// ================= Update Profile =================
// Profile picture / cover image edit removed — text fields only.

exports.updateProfile = async (req, res) => {
  try {
    const allowed = ["name", "phone", "location", "bio"];
    const updates = {};
    for (const key of allowed) {
      if (req.body[key] !== undefined) updates[key] = req.body[key];
    }

    const user = await User.findByIdAndUpdate(
      req.user.id,
      { $set: updates },
      { new: true }
    ).select("-password -refreshTokens");

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    res.json({ success: true, message: "Profile updated", user });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ================= Visited Places =================

exports.getVisitedPlaces = async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate({
      path: "visitedPlaces.place",
      select: "name shortDescription description city state country location imageUrl averageRating visitorsCount"
    });

    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const places = (user.visitedPlaces || [])
      .filter((v) => v.place)
      .map((v) => ({
        ...v.place.toObject(),
        visitedAt: v.visitedAt
      }));

    res.json({ success: true, places });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

exports.addVisitedPlace = async (req, res) => {
  try {
    const placeId = req.params.id;
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const place = await Place.findById(placeId);
    if (!place) {
      return res.status(404).json({ success: false, message: "Place not found" });
    }

    const exists = (user.visitedPlaces || []).some(
      (v) => v.place && v.place.toString() === placeId
    );
    if (exists) {
      return res.json({
        success: true,
        message: "Already marked as visited",
        visitorsCount: place.visitorsCount || 0
      });
    }

    user.visitedPlaces.push({ place: placeId, visitedAt: new Date() });
    await user.save();

    place.visitorsCount = Math.max(0, (place.visitorsCount || 0) + 1);
    await place.save();

    res.json({
      success: true,
      message: "Marked as visited",
      visitorsCount: place.visitorsCount
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

exports.removeVisitedPlace = async (req, res) => {
  try {
    const placeId = req.params.id;
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const had = (user.visitedPlaces || []).some(
      (v) => v.place && v.place.toString() === placeId
    );

    user.visitedPlaces = (user.visitedPlaces || []).filter(
      (v) => !v.place || v.place.toString() !== placeId
    );
    await user.save();

    let visitorsCount = 0;
    const place = await Place.findById(placeId);
    if (place) {
      if (had) {
        place.visitorsCount = Math.max(0, (place.visitorsCount || 0) - 1);
        await place.save();
      }
      visitorsCount = place.visitorsCount || 0;
    }

    res.json({
      success: true,
      message: "Removed from visited places",
      visitorsCount
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// ================= Mark notifications read =================

exports.markNotificationsRead = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const { ids } = req.body || {};
    if (Array.isArray(ids) && ids.length > 0) {
      user.notifications.forEach((n) => {
        if (ids.includes(n._id.toString())) n.read = true;
      });
    } else {
      user.notifications.forEach((n) => {
        n.read = true;
      });
    }
    await user.save();

    res.json({
      success: true,
      message: "Notifications marked as read",
      notifications: user.notifications
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};
