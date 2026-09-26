/**
 * Email for Travira.
 *
 * Render FREE tier blocks SMTP ports 25/465/587 → Gmail SMTP will timeout.
 * Use Resend (HTTPS API) instead:
 *   RESEND_API_KEY=re_xxxxx
 *   EMAIL_FROM=Travira <onboarding@resend.dev>   (or your verified domain)
 *
 * Optional SMTP (only works on paid Render instance):
 *   EMAIL_HOST, EMAIL_PORT, EMAIL_USER, EMAIL_PASS, EMAIL_FROM
 *
 * APP_BASE_URL=https://travira-app-minor.onrender.com
 */

let transporter = null;
let lastError = null;
let lastSuccessAt = null;
let lastProvider = null;

function cleanPass(pass) {
  return String(pass || "").replace(/\s+/g, "");
}

function appBaseUrl() {
  return (process.env.APP_BASE_URL || "https://travira-app-minor.onrender.com").replace(
    /\/$/,
    ""
  );
}

function fromAddress() {
  return String(
    process.env.EMAIL_FROM ||
      process.env.EMAIL_USER ||
      "Travira <onboarding@resend.dev>"
  ).trim();
}

function resendConfigured() {
  return Boolean(String(process.env.RESEND_API_KEY || "").trim());
}

function smtpConfigured() {
  return Boolean(
    process.env.EMAIL_HOST && process.env.EMAIL_USER && process.env.EMAIL_PASS
  );
}

function envConfigured() {
  return resendConfigured() || smtpConfigured();
}

function getTransporter() {
  if (transporter) return transporter;
  const host = String(process.env.EMAIL_HOST || "").trim();
  const user = String(process.env.EMAIL_USER || "").trim();
  const pass = cleanPass(process.env.EMAIL_PASS);
  if (!host || !user || !pass) {
    return null;
  }
  try {
    const nodemailer = require("nodemailer");
    const port = Number(process.env.EMAIL_PORT || 587);
    const secure = process.env.EMAIL_SECURE === "true" || port === 465;
    const options = {
      host,
      port,
      secure,
      auth: { user, pass },
      connectionTimeout: 12000,
      greetingTimeout: 12000,
      socketTimeout: 20000,
      tls: { minVersion: "TLSv1.2" }
    };
    if (!secure && port === 587) options.requireTLS = true;
    transporter = nodemailer.createTransport(options);
    return transporter;
  } catch (e) {
    lastError = e.message;
    console.warn("nodemailer unavailable:", e.message);
    return null;
  }
}

/**
 * Send via Resend HTTPS API (works on Render free tier).
 * https://resend.com/docs/api-reference/emails/send-email
 */
async function sendViaResend({ to, subject, html, text }) {
  const apiKey = String(process.env.RESEND_API_KEY || "").trim();
  if (!apiKey) {
    return { sent: false, reason: "RESEND_API_KEY not set" };
  }

  const body = {
    from: fromAddress(),
    to: [to],
    subject,
    html: html || undefined,
    text: text || subject
  };

  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: "Bearer " + apiKey,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(20000)
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const reason =
      (data && data.message) ||
      (data && data.error) ||
      "Resend HTTP " + res.status;
    return {
      sent: false,
      reason: typeof reason === "string" ? reason : JSON.stringify(reason)
    };
  }

  return { sent: true, id: data.id };
}

async function sendViaSmtp({ to, subject, html, text }) {
  const t = getTransporter();
  if (!t) {
    return {
      sent: false,
      reason: lastError || "SMTP not configured (EMAIL_HOST/USER/PASS)"
    };
  }
  try {
    const info = await t.sendMail({
      from: fromAddress(),
      to,
      subject,
      html,
      text: text || subject
    });
    return { sent: true, id: info && info.messageId };
  } catch (e) {
    transporter = null;
    const msg = e.message || String(e);
    // Helpful hint when Render free blocks SMTP
    if (/timeout|ETIMEDOUT|ECONNREFUSED|network is unreachable/i.test(msg)) {
      return {
        sent: false,
        reason:
          msg +
          " — Render free tier blocks SMTP ports. Set RESEND_API_KEY (https://resend.com) instead."
      };
    }
    return { sent: false, reason: msg };
  }
}

/**
 * @returns {{ sent: boolean, reason?: string }}
 */
async function sendMail({ to, subject, html, text }) {
  if (!envConfigured()) {
    lastError =
      "No email provider. Set RESEND_API_KEY (recommended on Render free) or EMAIL_HOST/USER/PASS.";
    console.warn(`[mail] skipped. To=${to} Subject=${subject}`);
    return { sent: false, reason: lastError };
  }

  // Prefer Resend on free hosting (HTTPS, not blocked)
  if (resendConfigured()) {
    lastProvider = "resend";
    try {
      const result = await sendViaResend({ to, subject, html, text });
      if (result.sent) {
        lastSuccessAt = new Date().toISOString();
        lastError = null;
        console.log(`[mail] resend OK to ${to}: ${subject} id=${result.id || "?"}`);
        return { sent: true };
      }
      lastError = result.reason;
      console.error("[mail] resend failed:", result.reason);
      // Fall through to SMTP if also configured
      if (!smtpConfigured()) {
        return { sent: false, reason: result.reason };
      }
    } catch (e) {
      lastError = e.message || String(e);
      console.error("[mail] resend error:", lastError);
      if (!smtpConfigured()) {
        return { sent: false, reason: lastError };
      }
    }
  }

  if (smtpConfigured()) {
    lastProvider = "smtp";
    const result = await sendViaSmtp({ to, subject, html, text });
    if (result.sent) {
      lastSuccessAt = new Date().toISOString();
      lastError = null;
      console.log(`[mail] smtp OK to ${to}: ${subject}`);
      return { sent: true };
    }
    lastError = result.reason;
    console.error("[mail] smtp failed:", result.reason);
    return { sent: false, reason: result.reason };
  }

  return { sent: false, reason: lastError || "Email send failed" };
}

async function verifyMail() {
  if (resendConfigured()) {
    lastProvider = "resend";
    try {
      const res = await fetch("https://api.resend.com/domains", {
        method: "GET",
        headers: { Authorization: "Bearer " + String(process.env.RESEND_API_KEY).trim() },
        signal: AbortSignal.timeout(15000)
      });
      if (res.ok || res.status === 401 || res.status === 403) {
        // 401/403 means API reached (key may be wrong); not a network block
        if (res.status === 401 || res.status === 403) {
          const data = await res.json().catch(() => ({}));
          return {
            ok: false,
            reason: data.message || "Resend API key rejected (check RESEND_API_KEY)"
          };
        }
        return { ok: true, provider: "resend" };
      }
      return { ok: false, reason: "Resend HTTP " + res.status };
    } catch (e) {
      return { ok: false, reason: e.message || String(e) };
    }
  }

  if (smtpConfigured()) {
    lastProvider = "smtp";
    const t = getTransporter();
    if (!t) return { ok: false, reason: lastError || "no transporter" };
    try {
      await t.verify();
      return { ok: true, provider: "smtp" };
    } catch (e) {
      transporter = null;
      const msg = e.message || String(e);
      if (/timeout|ETIMEDOUT|ECONNREFUSED|unreachable/i.test(msg)) {
        return {
          ok: false,
          reason:
            msg +
            " — Render free blocks SMTP. Use RESEND_API_KEY instead of Gmail SMTP."
        };
      }
      return { ok: false, reason: msg };
    }
  }

  return {
    ok: false,
    reason: "Set RESEND_API_KEY (recommended) or EMAIL_HOST/USER/PASS"
  };
}

function mailStatus() {
  return {
    configured: envConfigured(),
    provider: resendConfigured() ? "resend" : smtpConfigured() ? "smtp" : null,
    emailUser: process.env.EMAIL_USER
      ? String(process.env.EMAIL_USER).replace(/(.{2}).+(@.+)/, "$1***$2")
      : resendConfigured()
        ? "resend"
        : null,
    lastError: lastError || null,
    lastSuccessAt: lastSuccessAt || null,
    lastProvider: lastProvider || null,
    note: resendConfigured()
      ? null
      : "Render free tier blocks SMTP. Set RESEND_API_KEY from https://resend.com"
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
