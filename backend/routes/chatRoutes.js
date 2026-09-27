/**
 * Chat routes — mounted at /api/chat
 * POST /  → travel chatbot (Gemini 3.8 Flash → 3.5 Flash-Lite)
 * Requires login (authMiddleware)
 */
const express = require("express");
const router = express.Router();
const authMiddleware = require("../middleware/authMiddleware");
const { chat } = require("../controllers/chat");

router.post("/", authMiddleware, chat);

module.exports = router;
