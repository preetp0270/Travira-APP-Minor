/**
 * Fetch a remote image with browser-like headers and upload it to Cloudinary
 * (unsigned preset). Many sites return 403 to bare fetches; this mitigates that.
 */
const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || "yv3rd7a3";
const UPLOAD_PRESET = process.env.CLOUDINARY_UPLOAD_PRESET || "travira_unsigned";
const CLOUDINARY_URL = `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`;

const IMAGE_EXT = /\.(jpe?g|png|gif|webp|bmp|svg|avif|heic|heif)(\?|#|$)/i;
const MAX_BYTES = 12 * 1024 * 1024; // 12 MB

function isCloudinaryUrl(url) {
  return /^https?:\/\/res\.cloudinary\.com\//i.test(url || "");
}

function looksLikeDirectImageUrl(url) {
  try {
    const u = new URL(url);
    if (!/^https?:$/i.test(u.protocol)) return false;
    if (IMAGE_EXT.test(u.pathname)) return true;
    // CDNs often omit extension (e.g. /image/upload/...)
    if (/cloudinary|imgur|unsplash|googleusercontent|ggpht|twimg|fbcdn|cdn/i.test(u.hostname)) {
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

/**
 * Try to pull og:image / twitter:image from an HTML page.
 */
function extractOgImage(html, pageUrl) {
  if (!html || typeof html !== "string") return null;
  const patterns = [
    /<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']/i,
    /<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+content=["']([^"']+)["'][^>]+name=["']twitter:image["']/i
  ];
  for (const re of patterns) {
    const m = html.match(re);
    if (m && m[1]) {
      try {
        return new URL(m[1].trim(), pageUrl).href;
      } catch {
        return m[1].trim();
      }
    }
  }
  return null;
}

async function fetchWithBrowserHeaders(url, extra = {}) {
  const origin = (() => {
    try {
      return new URL(url).origin;
    } catch {
      return undefined;
    }
  })();

  const headers = {
    "User-Agent":
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    Accept: "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
    ...(origin ? { Referer: origin + "/" } : {}),
    ...extra
  };

  const res = await fetch(url, {
    method: "GET",
    headers,
    redirect: "follow",
    signal: AbortSignal.timeout(25000)
  });
  return res;
}

async function uploadBufferToCloudinary(buffer, contentType, filename) {
  const form = new FormData();
  const type = (contentType || "image/jpeg").split(";")[0].trim() || "image/jpeg";
  const blob = new Blob([buffer], { type });
  form.append("file", blob, filename || "travira.jpg");
  form.append("upload_preset", UPLOAD_PRESET);
  form.append("folder", "travira");

  const up = await fetch(CLOUDINARY_URL, {
    method: "POST",
    body: form,
    signal: AbortSignal.timeout(45000)
  });
  const data = await up.json().catch(() => ({}));
  if (!up.ok) {
    const msg =
      (data.error && (data.error.message || data.error)) ||
      data.message ||
      `Cloudinary upload failed (${up.status})`;
    throw new Error(typeof msg === "string" ? msg : JSON.stringify(msg));
  }
  const secure = data.secure_url || data.url || "";
  if (!secure) throw new Error("Cloudinary returned no image URL");
  return secure;
}

/**
 * Import remote URL → Cloudinary secure_url.
 * - Already-Cloudinary URLs returned as-is
 * - Direct images fetched + uploaded
 * - HTML pages: try og:image, then fetch that
 */
async function importImageFromUrl(rawUrl) {
  const url = String(rawUrl || "").trim();
  if (!url) {
    const err = new Error("Image URL is required");
    err.status = 400;
    throw err;
  }
  if (!/^https?:\/\//i.test(url)) {
    const err = new Error("URL must start with http:// or https://");
    err.status = 400;
    throw err;
  }
  if (isCloudinaryUrl(url)) {
    return { imageUrl: url, source: "cloudinary-existing" };
  }

  let res;
  try {
    res = await fetchWithBrowserHeaders(url);
  } catch (e) {
    const err = new Error(
      `Could not reach that URL (${e.message || "network error"}). Try a direct image link (ends with .jpg/.png) or upload a file.`
    );
    err.status = 502;
    throw err;
  }

  if (res.status === 403 || res.status === 401) {
    const err = new Error(
      `That site blocked image download (${res.status} Forbidden). Many blogs and travel sites block hotlinking. Use a direct image URL (right‑click image → Copy image address) or upload a file instead.`
    );
    err.status = 403;
    throw err;
  }
  if (!res.ok) {
    const err = new Error(
      `Failed to download URL (HTTP ${res.status}). Use a public direct image link or upload a file.`
    );
    err.status = 502;
    throw err;
  }

  const contentType = (res.headers.get("content-type") || "").toLowerCase();
  const buf = Buffer.from(await res.arrayBuffer());

  if (buf.length > MAX_BYTES) {
    const err = new Error("Image is too large (max 12 MB)");
    err.status = 400;
    throw err;
  }

  // HTML page → try Open Graph image
  if (
    contentType.includes("text/html") ||
    contentType.includes("application/xhtml") ||
    (!contentType.startsWith("image/") && !looksLikeDirectImageUrl(url) && buf.slice(0, 20).toString().includes("<"))
  ) {
    const html = buf.toString("utf8").slice(0, 200000);
    const og = extractOgImage(html, url);
    if (!og) {
      const err = new Error(
        "That link is a web page, not a direct image. Open the page, right‑click the photo → “Copy image address”, then paste that URL — or upload a file."
      );
      err.status = 400;
      throw err;
    }
    // Recurse once on the og:image URL
    return importImageFromUrl(og);
  }

  if (!contentType.startsWith("image/") && !looksLikeDirectImageUrl(url)) {
    const err = new Error(
      `URL did not return an image (got ${contentType || "unknown"}). Paste a direct image link or upload a file.`
    );
    err.status = 400;
    throw err;
  }

  const ext =
    (contentType.match(/image\/(jpeg|jpg|png|gif|webp|svg\+xml|avif)/) || [])[1] ||
    "jpg";
  const filename = `travira_${Date.now()}.${ext === "svg+xml" ? "svg" : ext === "jpeg" ? "jpg" : ext}`;

  const imageUrl = await uploadBufferToCloudinary(
    buf,
    contentType.startsWith("image/") ? contentType : "image/jpeg",
    filename
  );
  return { imageUrl, source: "imported" };
}

module.exports = {
  importImageFromUrl,
  isCloudinaryUrl,
  looksLikeDirectImageUrl
};
