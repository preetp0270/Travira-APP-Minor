/**
 * Email for Travira.
 *
 * Render FREE tier blocks SMTP ports 25/465/587 → Gmail SMTP will timeout.
 *
 * Recommended (no domain, send to ANY user, works on Render free):
 *   BREVO_API_KEY=xkeysib-...
 *   EMAIL_FROM=Travira <yourgmail@gmail.com>
 *
 * Optional:
 *   SENDGRID_API_KEY=SG.xxx     (verify a Single Sender in SendGrid)
 *   RESEND_API_KEY=re_xxx       (without a domain: only your own email)
 *   EMAIL_HOST/USER/PASS        (SMTP — only if host allows it)
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
    process.env.EMAIL_FROM || process.env.EMAIL_USER || ""
  ).trim();
}

function fromParts() {
  const raw = fromAddress();
  const m = raw.match(/^(.*)<([^>]+)>$/);
  if (m) {
    return {
      name: m[1].trim().replace(/^["']|["']$/g, "") || "Travira",
      email: m[2].trim()
    };
  }
  if (raw.includes("@")) return { name: "Travira", email: raw };
  return { name: "Travira", email: raw };
}

function maskEmail(value) {
  const s = String(value || "");
  return s.replace(/(.{2}).+(@.+)/, "$1***$2") || null;
}

function brevoConfigured() {
  return Boolean(String(process.env.BREVO_API_KEY || "").trim());
}

function sendgridConfigured() {
  return Boolean(String(process.env.SENDGRID_API_KEY || "").trim());
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
  return (
    brevoConfigured() ||
    sendgridConfigured() ||
    resendConfigured() ||
    smtpConfigured()
  );
}

function activeProviderName() {
  if (brevoConfigured()) return "brevo";
  if (sendgridConfigured()) return "sendgrid";
  if (resendConfigured()) return "resend";
  if (smtpConfigured()) return "smtp";
  return null;
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

function extractError(data, fallback) {
  if (!data) return fallback;
  if (typeof data.message === "string") return data.message;
  if (typeof data.error === "string") return data.error;
  if (Array.isArray(data.errors) && data.errors[0]) {
    const e = data.errors[0];
    return e.message || JSON.stringify(e);
  }
  try {
    return JSON.stringify(data);
  } catch {
    return fallback;
  }
}

async function sendViaBrevo({ to, subject, html, text }) {
  const apiKey = String(process.env.BREVO_API_KEY || "").trim();
  if (!apiKey) return { sent: false, reason: "BREVO_API_KEY not set" };
  const from = fromParts();
  if (!from.email || !from.email.includes("@")) {
    return {
      sent: false,
      reason: "Set EMAIL_FROM to your verified Brevo sender, e.g. Travira <you@gmail.com>"
    };
  }

  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: {
      accept: "application/json",
      "api-key": apiKey,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      sender: { name: from.name, email: from.email },
      to: [{ email: to }],
      subject,
      htmlContent: html || `<p>${text || subject}</p>`,
      textContent: text || subject
    }),
    signal: AbortSignal.timeout(20000)
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    return { sent: false, reason: extractError(data, "Brevo HTTP " + res.status) };
  }
  return { sent: true, id: data.messageId };
}

async function sendViaSendgrid({ to, subject, html, text }) {
  const apiKey = String(process.env.SENDGRID_API_KEY || "").trim();
  if (!apiKey) return { sent: false, reason: "SENDGRID_API_KEY not set" };
  const from = fromParts();
  if (!from.email || !from.email.includes("@")) {
    return {
      sent: false,
      reason: "Set EMAIL_FROM to your verified SendGrid Single Sender"
    };
  }

  const content = [];
  if (text) content.push({ type: "text/plain", value: text });
  if (html) content.push({ type: "text/html", value: html });
  if (!content.length) content.push({ type: "text/plain", value: subject });

  const res = await fetch("https://api.sendgrid.com/v3/mail/send", {
    method: "POST",
    headers: {
      Authorization: "Bearer " + apiKey,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      personalizations: [{ to: [{ email: to }] }],
      from: { email: from.email, name: from.name },
      subject,
      content
    }),
    signal: AbortSignal.timeout(20000)
  });

  if (res.status === 202 || res.status === 200) {
    return { sent: true };
  }
  const data = await res.json().catch(() => ({}));
  return { sent: false, reason: extractError(data, "SendGrid HTTP " + res.status) };
}

async function sendViaResend({ to, subject, html, text }) {
  const apiKey = String(process.env.RESEND_API_KEY || "").trim();
  if (!apiKey) return { sent: false, reason: "RESEND_API_KEY not set" };

  const from = fromAddress() || "Travira <onboarding@resend.dev>";
  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: "Bearer " + apiKey,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      from,
      to: [to],
      subject,
      html: html || undefined,
      text: text || subject
    }),
    signal: AbortSignal.timeout(20000)
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    return { sent: false, reason: extractError(data, "Resend HTTP " + res.status) };
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
      from: fromAddress() || process.env.EMAIL_USER,
      to,
      subject,
      html,
      text: text || subject
    });
    return { sent: true, id: info && info.messageId };
  } catch (e) {
    transporter = null;
    const msg = e.message || String(e);
    if (/timeout|ETIMEDOUT|ECONNREFUSED|network is unreachable/i.test(msg)) {
      return {
        sent: false,
        reason:
          msg +
          " — Render free blocks SMTP ports. Use BREVO_API_KEY (https://app.brevo.com) instead of Gmail SMTP."
      };
    }
    return { sent: false, reason: msg };
  }
}

async function tryProvider(name, fn, payload) {
  lastProvider = name;
  try {
    const result = await fn(payload);
    if (result.sent) {
      lastSuccessAt = new Date().toISOString();
      lastError = null;
      console.log(`[mail] ${name} OK to ${payload.to}: ${payload.subject} id=${result.id || "?"}`);
      return { sent: true };
    }
    lastError = result.reason;
    console.error(`[mail] ${name} failed:`, result.reason);
    return { sent: false, reason: result.reason };
  } catch (e) {
    lastError = e.message || String(e);
    console.error(`[mail] ${name} error:`, lastError);
    return { sent: false, reason: lastError };
  }
}

/**
 * @returns {{ sent: boolean, reason?: string }}
 */
async function sendMail({ to, subject, html, text }) {
  if (!envConfigured()) {
    lastError =
      "No email provider. Set BREVO_API_KEY (recommended on Render free) or SENDGRID_API_KEY.";
    console.warn(`[mail] skipped. To=${to} Subject=${subject}`);
    return { sent: false, reason: lastError };
  }

  const payload = { to, subject, html, text };
  const chain = [];
  if (brevoConfigured()) chain.push(["brevo", sendViaBrevo]);
  if (sendgridConfigured()) chain.push(["sendgrid", sendViaSendgrid]);
  if (resendConfigured()) chain.push(["resend", sendViaResend]);
  if (smtpConfigured()) chain.push(["smtp", sendViaSmtp]);

  let lastFail = null;
  for (const [name, fn] of chain) {
    const result = await tryProvider(name, fn, payload);
    if (result.sent) return { sent: true };
    lastFail = result.reason;
  }
  return { sent: false, reason: lastFail || lastError || "Email send failed" };
}

async function verifyMail() {
  if (brevoConfigured()) {
    lastProvider = "brevo";
    try {
      const res = await fetch("https://api.brevo.com/v3/account", {
        headers: {
          accept: "application/json",
          "api-key": String(process.env.BREVO_API_KEY).trim()
        },
        signal: AbortSignal.timeout(15000)
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok) {
        return { ok: true, provider: "brevo", note: "Brevo API key valid" };
      }
      return { ok: false, reason: extractError(data, "Brevo HTTP " + res.status) };
    } catch (e) {
      return { ok: false, reason: e.message || String(e) };
    }
  }

  if (sendgridConfigured()) {
    lastProvider = "sendgrid";
    try {
      const res = await fetch("https://api.sendgrid.com/v3/user/account", {
        headers: {
          Authorization: "Bearer " + String(process.env.SENDGRID_API_KEY).trim()
        },
        signal: AbortSignal.timeout(15000)
      });
      if (res.ok) {
        return { ok: true, provider: "sendgrid", note: "SendGrid API key valid" };
      }
      const data = await res.json().catch(() => ({}));
      return { ok: false, reason: extractError(data, "SendGrid HTTP " + res.status) };
    } catch (e) {
      return { ok: false, reason: e.message || String(e) };
    }
  }

  if (resendConfigured()) {
    lastProvider = "resend";
    const key = String(process.env.RESEND_API_KEY || "").trim();
    if (!key.startsWith("re_")) {
      return { ok: false, reason: "RESEND_API_KEY should start with re_" };
    }
    try {
      const res = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          Authorization: "Bearer " + key,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({}),
        signal: AbortSignal.timeout(15000)
      });
      const data = await res.json().catch(() => ({}));
      const msg = String(data.message || data.error || "");
      if (
        res.status === 422 ||
        /required|validation|from|to|subject/i.test(msg)
      ) {
        return { ok: true, provider: "resend", note: "API key can send emails" };
      }
      if (/restricted to only send/i.test(msg)) {
        return {
          ok: true,
          provider: "resend",
          note: "Sending-only API key (correct for Travira)"
        };
      }
      if (res.status === 401 || res.status === 403) {
        return {
          ok: false,
          reason: msg || "Resend API key rejected — create a new Sending key"
        };
      }
      if (res.status < 500) {
        return { ok: true, provider: "resend", note: msg || "Resend reachable" };
      }
      return { ok: false, reason: msg || "Resend HTTP " + res.status };
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
            " — Render free blocks SMTP. Use BREVO_API_KEY instead of Gmail SMTP."
        };
      }
      return { ok: false, reason: msg };
    }
  }

  return {
    ok: false,
    reason: "Set BREVO_API_KEY (recommended, no domain) or SENDGRID_API_KEY"
  };
}

function mailStatus() {
  const provider = activeProviderName();
  const from = fromParts();
  return {
    mailCodeVersion: "2026-09-27-mail-brevo-v6",
    configured: envConfigured(),
    provider,
    hasBrevoKey: brevoConfigured(),
    emailUser: maskEmail(from.email || process.env.EMAIL_USER),
    lastError: lastError || null,
    lastSuccessAt: lastSuccessAt || null,
    lastProvider: lastProvider || null,
    note:
      provider === "brevo" || provider === "sendgrid"
        ? "HTTPS API — works on Render free. EMAIL_FROM must be your verified sender Gmail."
        : provider === "resend"
          ? "Resend without a domain can only send to your own Resend account email."
          : provider === "smtp"
            ? "Render free blocks SMTP ports. Prefer BREVO_API_KEY."
            : "Set BREVO_API_KEY from https://app.brevo.com/settings/keys/api"
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
    .replace(/&/g, "\u0026amp;")
    .replace(/</g, "\u0026lt;")
    .replace(/>/g, "\u0026gt;")
    .replace(/"/g, "\u0026quot;");
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
