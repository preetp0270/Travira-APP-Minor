/**
 * Place stats helpers — visitors & ratings from real data (not seed numbers).
 *
 * visitorsCount  = how many users have this place in visitedPlaces
 * ratingsCount   = place.ratings.length
 * averageRating  = mean of rating values (1 decimal)
 */
const mongoose = require("mongoose");
const User = require("../models/user");
const Place = require("../models/place");

/** Count users who marked a place as visited */
async function countVisitors(placeId) {
  if (!placeId) return 0;
  const id =
    placeId instanceof mongoose.Types.ObjectId
      ? placeId
      : new mongoose.Types.ObjectId(String(placeId));
  return User.countDocuments({ "visitedPlaces.place": id });
}

/** Map placeId(string) → visitor count for many places (one aggregation) */
async function visitorCountMap(placeIds) {
  const ids = (placeIds || [])
    .filter(Boolean)
    .map((id) =>
      id instanceof mongoose.Types.ObjectId
        ? id
        : new mongoose.Types.ObjectId(String(id))
    );
  if (!ids.length) return {};

  const rows = await User.aggregate([
    { $unwind: "$visitedPlaces" },
    { $match: { "visitedPlaces.place": { $in: ids } } },
    { $group: { _id: "$visitedPlaces.place", count: { $sum: 1 } } }
  ]);

  const map = {};
  for (const row of rows) {
    map[String(row._id)] = row.count;
  }
  return map;
}

/** Average + count from ratings array */
function ratingStats(ratings) {
  const list = Array.isArray(ratings) ? ratings : [];
  const ratingsCount = list.length;
  if (!ratingsCount) {
    return { averageRating: 0, ratingsCount: 0 };
  }
  const total = list.reduce((sum, r) => sum + (Number(r.value) || 0), 0);
  const averageRating = Math.round((total / ratingsCount) * 10) / 10;
  return { averageRating, ratingsCount };
}

/**
 * Recompute visitorsCount on the Place document from User data and save.
 * Returns the true count.
 */
async function syncVisitorsCount(placeId) {
  const count = await countVisitors(placeId);
  await Place.findByIdAndUpdate(placeId, { $set: { visitorsCount: count } });
  return count;
}

/**
 * Attach live stats onto a lean place object for API responses.
 * visitMap optional: precomputed visitorCountMap for list endpoints.
 */
function withLiveStats(place, visitMap) {
  if (!place) return place;
  const id = String(place._id);
  const { averageRating, ratingsCount } = ratingStats(place.ratings);
  const visitorsCount =
    visitMap && Object.prototype.hasOwnProperty.call(visitMap, id)
      ? visitMap[id]
      : typeof place.visitorsCount === "number"
        ? place.visitorsCount
        : 0;

  return {
    ...place,
    averageRating,
    // So Android displayRating prefers real average over old seed `rating`
    rating: averageRating,
    ratingsCount,
    visitorsCount: visitMap ? visitorsCount || 0 : visitorsCount
  };
}

module.exports = {
  countVisitors,
  visitorCountMap,
  ratingStats,
  syncVisitorsCount,
  withLiveStats
};
