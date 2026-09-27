/**
 * Place stats helpers — sample/base numbers + real user actions.
 *
 * visitorsCount (displayed) = baseVisitorsCount + real users who marked visited
 * ratingsCount  (displayed) = baseRatingsCount  + place.ratings.length
 * averageRating             = real average if any ratings exist, else seed averageRating
 *
 * base* fields are set from AI/sample data and never overwritten by user toggles.
 * Legacy docs without base*: first sync recovers base from stored visitorsCount − real.
 */
const mongoose = require("mongoose");
const User = require("../models/user");
const Place = require("../models/place");

/** Count users who marked a place as visited (real only) */
async function countVisitors(placeId) {
  if (!placeId) return 0;
  const id =
    placeId instanceof mongoose.Types.ObjectId
      ? placeId
      : new mongoose.Types.ObjectId(String(placeId));
  return User.countDocuments({ "visitedPlaces.place": id });
}

/** Map placeId(string) → real visitor count for many places (one aggregation) */
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

/** Average + count from ratings array (real reviews only) */
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
 * Resolve base visitors from place doc (never the live total).
 */
function baseVisitors(place) {
  if (!place) return 0;
  if (typeof place.baseVisitorsCount === "number" && place.baseVisitorsCount >= 0) {
    return place.baseVisitorsCount;
  }
  return 0;
}

function baseRatings(place) {
  if (!place) return 0;
  if (typeof place.baseRatingsCount === "number" && place.baseRatingsCount >= 0) {
    return place.baseRatingsCount;
  }
  return 0;
}

/**
 * Ensure place has baseVisitorsCount set (one-time recover for legacy docs).
 * Returns { base, real, displayed }.
 */
async function resolveVisitorTotals(placeId) {
  const place = await Place.findById(placeId).select(
    "visitorsCount baseVisitorsCount"
  );
  if (!place) {
    const real = await countVisitors(placeId);
    return { base: 0, real, displayed: real };
  }

  const real = await countVisitors(placeId);
  let base = baseVisitors(place);

  // Legacy migration: no baseVisitorsCount yet
  if (
    (place.baseVisitorsCount == null ||
      typeof place.baseVisitorsCount !== "number") &&
    typeof place.visitorsCount === "number"
  ) {
    const stored = place.visitorsCount;
    // If stored looks like a previous displayed total, peel off real
    base = stored >= real ? stored - real : stored;
    await Place.findByIdAndUpdate(placeId, {
      $set: { baseVisitorsCount: base, visitorsCount: base + real }
    });
  }

  const displayed = base + real;
  if (place.visitorsCount !== displayed) {
    await Place.findByIdAndUpdate(placeId, {
      $set: { visitorsCount: displayed }
    });
  }
  return { base, real, displayed };
}

/**
 * Displayed visitors = base (sample) + real user marks.
 * Migrates legacy docs to baseVisitorsCount on first call.
 */
async function syncVisitorsCount(placeId) {
  const { displayed } = await resolveVisitorTotals(placeId);
  return displayed;
}

/**
 * Attach live stats onto a lean place object for API responses.
 * visitMap optional: precomputed visitorCountMap for list endpoints.
 */
function withLiveStats(place, visitMap) {
  if (!place) return place;
  const id = String(place._id);

  const realRatings = ratingStats(place.ratings);
  const baseR = baseRatings(place);
  const ratingsCount = baseR + realRatings.ratingsCount;

  const seedAvg =
    typeof place.averageRating === "number" && place.averageRating > 0
      ? place.averageRating
      : typeof place.rating === "number" && place.rating > 0
        ? place.rating
        : 0;
  const averageRating =
    realRatings.ratingsCount > 0 ? realRatings.averageRating : seedAvg;

  // Real visitor count: from map when provided (0 if place has no visits yet)
  let realVisitors = null;
  if (visitMap) {
    realVisitors = Object.prototype.hasOwnProperty.call(visitMap, id)
      ? visitMap[id] || 0
      : 0;
  }

  let visitorsCount;
  if (realVisitors !== null) {
    let base;
    if (
      typeof place.baseVisitorsCount === "number" &&
      place.baseVisitorsCount >= 0
    ) {
      base = place.baseVisitorsCount;
    } else if (typeof place.visitorsCount === "number") {
      // Legacy recover: stored may already be base+real
      const stored = place.visitorsCount;
      base = stored >= realVisitors ? stored - realVisitors : stored;
    } else {
      base = 0;
    }
    visitorsCount = base + realVisitors;
  } else if (
    typeof place.baseVisitorsCount === "number" &&
    place.baseVisitorsCount >= 0
  ) {
    visitorsCount = place.baseVisitorsCount;
  } else {
    visitorsCount =
      typeof place.visitorsCount === "number" ? place.visitorsCount : 0;
  }

  return {
    ...place,
    averageRating,
    rating: averageRating,
    ratingsCount,
    visitorsCount
  };
}

module.exports = {
  countVisitors,
  visitorCountMap,
  ratingStats,
  syncVisitorsCount,
  withLiveStats,
  baseVisitors,
  baseRatings,
  resolveVisitorTotals
};
