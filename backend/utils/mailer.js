/**
 * Optional email sender for Travira.
 * Env (Render):
 *   EMAIL_HOST, EMAIL_PORT, EMAIL_USER, EMAIL_PASS, EMAIL_FROM
 *   APP_BASE_URL  e.g. https://travira-app-minor.onrender.com
 *
 * If SMTP is not configured, sendMail logs and resolves without throwing
 * so auth still works offline / during local dev.
 */

let transporter = null;

function getTransporter() {
  if (transporter) return transporter;
  const host = process.env.EMAIL_HOST;
  const user = process.env.EMAIL_USER;
  const pass = process.env.EMAIL_PASS;
  if (!host || !user || !pass) {
    return null;
  }
  try {
    const nodemailer = require("nodemailer");
    transporter = nodemailer.createTransport({
      host,
      port: Number(process.env.EMAIL_PORT || 587),
      secure: process.env.EMAIL_SECURE === "true",
      auth: { user, pass },
      // Prevent forgot-password / login from hanging if SMTP is slow
      connectionTimeout: 10000,
      greetingTimeout: 10000,
      socketTimeout: 15000
    });
  } catch (e) {
    console.warn("nodemailer unavailable:", e.message);
    return null;
  }
  return transporter;
}

function fromAddress() {
  return process.env.EMAIL_FROM || process.env.EMAIL_USER || "Travira <noreply@travira.app>";
}

function appBaseUrl() {
  return (process.env.APP_BASE_URL || "https://travira-app-minor.onrender.com").replace(/\/$/, "");
}

/**
 * @returns {{ sent: boolean, reason?: string }}
 */
async function sendMail({ to, subject, html, text }) {
  const t = getTransporter();
  if (!t) {
    console.warn(
      `[mail] skipped (no SMTP). To=${to} Subject=${subject}`
    );
    return { sent: false, reason: "Email not configured on server (set EMAIL_HOST/USER/PASS)." };
  }
  try {
    await t.sendMail({
      from: fromAddress(),
      to,
      subject,
      html,
      text: text || subject
    });
    console.log(`[mail] sent to ${to}: ${subject}`);
    return { sent: true };
  } catch (e) {
    console.error("[mail] failed:", e.message);
    return { sent: false, reason: e.message };
  }
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
    <p style="color:#78909C;font-size:12px;word-break:break-all">Or open: ${link}</p>
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
  appBaseUrl,
  welcomeHtml,
  loginAlertHtml,
  resetPasswordHtml
};
