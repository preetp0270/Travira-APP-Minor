/**
 * Email sender for Travira (nodemailer).
 * Render env required:
 *   EMAIL_HOST   e.g. smtp.gmail.com
 *   EMAIL_PORT   e.g. 587
 *   EMAIL_USER   full Gmail address
 *   EMAIL_PASS   Gmail App Password (16 chars, no spaces)
 *   EMAIL_FROM   e.g. Travira <you@gmail.com>
 *   APP_BASE_URL e.g. https://travira-app-minor.onrender.com
 */

let transporter = null;
let lastError = null;
let lastSuccessAt = null;

function envConfigured() {
  return Boolean(
    process.env.EMAIL_HOST && process.env.EMAIL_USER && process.env.EMAIL_PASS
  );
}

function cleanPass(pass) {
  // Gmail app passwords are often shown as "abcd efgh ijkl mnop"
  return String(pass || "").replace(/\s+/g, "");
}

function getTransporter() {
  if (transporter) return transporter;
  const host = String(process.env.EMAIL_HOST || "").trim();
  const user = String(process.env.EMAIL_USER || "").trim();
  const pass = cleanPass(process.env.EMAIL_PASS);
  if (!host || !user || !pass) {
    lastError = "Missing EMAIL_HOST, EMAIL_USER, or EMAIL_PASS";
    return null;
  }
  try {
    const nodemailer = require("nodemailer");
    const port = Number(process.env.EMAIL_PORT || 587);
    const secure = process.env.EMAIL_SECURE === "true" || port === 465;

    // Prefer explicit SMTP (works for Gmail + App Password)
    const options = {
      host,
      port,
      secure,
      auth: { user, pass },
      connectionTimeout: 12000,
      greetingTimeout: 12000,
      socketTimeout: 20000,
      tls: {
        // Gmail on 587 uses STARTTLS
        minVersion: "TLSv1.2"
      }
    };
    if (!secure && port === 587) {
      options.requireTLS = true;
    }

    transporter = nodemailer.createTransport(options);
    return transporter;
  } catch (e) {
    lastError = e.message;
    console.warn("nodemailer unavailable:", e.message);
    return null;
  }
}

function fromAddress() {
  const from = process.env.EMAIL_FROM || process.env.EMAIL_USER;
  return String(from || "Travira <noreply@travira.app>").trim();
}

function appBaseUrl() {
  return (process.env.APP_BASE_URL || "https://travira-app-minor.onrender.com").replace(
    /\/$/,
    ""
  );
}

/**
 * @returns {{ sent: boolean, reason?: string }}
 */
async function sendMail({ to, subject, html, text }) {
  if (!envConfigured()) {
    lastError = "Email not configured (set EMAIL_HOST/USER/PASS on Render)";
    console.warn(`[mail] skipped (no SMTP). To=${to} Subject=${subject}`);
    return { sent: false, reason: lastError };
  }

  const t = getTransporter();
  if (!t) {
    return { sent: false, reason: lastError || "Could not create mail transporter" };
  }

  try {
    const info = await t.sendMail({
      from: fromAddress(),
      to,
      subject,
      html,
      text: text || subject
    });
    lastSuccessAt = new Date().toISOString();
    lastError = null;
    console.log(
      `[mail] sent to ${to}: ${subject} id=${info && info.messageId ? info.messageId : "?"}`
    );
    return { sent: true };
  } catch (e) {
    lastError = e.message || String(e);
    console.error("[mail] failed:", lastError);
    // Reset transporter so next attempt rebuilds with current env
    transporter = null;
    return { sent: false, reason: lastError };
  }
}

/** Optional SMTP verify (does not send a message). */
async function verifyMail() {
  if (!envConfigured()) {
    return { ok: false, reason: "EMAIL_HOST/USER/PASS not set" };
  }
  const t = getTransporter();
  if (!t) return { ok: false, reason: lastError || "no transporter" };
  try {
    await t.verify();
    return { ok: true };
  } catch (e) {
    lastError = e.message || String(e);
    transporter = null;
    return { ok: false, reason: lastError };
  }
}

function mailStatus() {
  return {
    configured: envConfigured(),
    emailUser: process.env.EMAIL_USER
      ? String(process.env.EMAIL_USER).replace(/(.{2}).+(@.+)/, "$1***$2")
      : null,
    lastError: lastError || null,
    lastSuccessAt: lastSuccessAt || null
  };
}

function welcomeHtml(name) {
  return `
  <div style="font-family:Segoe UI,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;background:#f0f6fc;border-radius:16px">
    <h1 style="color:#1565C0;margin:0 0 8px">Welcome to Travira ✈️</h1>
    <p style="color:#0D1B2A;font-size:16px">Hi ${escapeHtml(name)},</p>
    <p style="color:#546E7A;line-height:1.5">Your account is ready. Discover places, save a wishlist, mark visits, and chat with Travira AI.</p>
    <p style="color:#78909C;font-size:13px;margin-top:24px">If you did not create this account, ignore this email.</p>
  </div>`;
}

function loginAlertHtml(name, whenIso) {
  return `
  <div style="font-family:Segoe UI,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;background:#f0f6fc;border-radius:16px">
    <h1 style="color:#1565C0;margin:0 0 8px">New login to Travira</h1>
    <p style="color:#0D1B2A;font-size:16px">Hi ${escapeHtml(name)},</p>
    <p style="color:#546E7A;line-height:1.5">Someone signed in to your Travira account on <strong>${escapeHtml(whenIso)}</strong>.</p>
    <p style="color:#546E7A">If this was you, no action needed. If not, reset your password from the app (Forgot password).</p>
  </div>`;
}

function resetPasswordHtml(name, link) {
  return `
  <div style="font-family:Segoe UI,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;background:#f0f6fc;border-radius:16px">
    <h1 style="color:#1565C0;margin:0 0 8px">Reset your password</h1>
    <p style="color:#0D1B2A;font-size:16px">Hi ${escapeHtml(name)},</p>
    <p style="color:#546E7A;line-height:1.5">Tap the button below to choose a new password. This link expires in <strong>1 hour</strong>.</p>
    <p style="margin:28px 0">
      <a href="${link}" style="background:#1565C0;color:#fff;padding:14px 28px;border-radius:12px;text-decoration:none;font-weight:600;display:inline-block">
        Change password
      </a>
    </p>
    <p style="color:#78909C;font-size:12px;word-break:break-all">Or open: ${escapeHtml(link)}</p>
    <p style="color:#78909C;font-size:13px">If you did not request this, you can ignore this email.</p>
  </div>`;
}

function escapeHtml(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

module.exports = {
  sendMail,
  verifyMail,
  mailStatus,
  appBaseUrl,
  welcomeHtml,
  loginAlertHtml,
  resetPasswordHtml
};
