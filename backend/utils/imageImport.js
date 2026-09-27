/**
 * Import a public image URL for Travira places.
 *
 * Goal: easy link upload — public photos (Google, blogs, CDNs) should work.
 *
 * Order:
 *  1. Already Cloudinary → use as-is
 *  2. Unwrap Google/Bing redirect URLs to the real image
 *  3. Fetch with browser headers → upload buffer to Cloudinary
 *  4. Ask Cloudinary to pull the remote URL itself
 *  5. If page HTML → og:image / twitter:image, then retry
 *  6. Fallback: store the original public URL (app can still display it)
 */
const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || "yv3rd7a3";
const UPLOAD_PRESET = process.env.CLOUDINARY_UPLOAD_PRESET || "travira_unsigned";
const CLOUDINARY_URL = `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`;

const IMAGE_EXT = /\.(jpe?g|png|gif|webp|bmp|svg|avif|heic|heif)(\?|#|$)/i;
const MAX_BYTES = 15 * 1024 * 1024;

function isCloudinaryUrl(url) {
  return /^https?:\/\/res\.cloudinary\.com\//i.test(url || "");
}

function isHttpUrl(url) {
  return /^https?:\/\//i.test(String(url || "").trim());
}

/** Google Images / Bing often wrap the real file in imgurl= or mediaurl= */
function unwrapSearchEngineUrl(url) {
  try {
    const u = new URL(url);
    const host = u.hostname.toLowerCase();
    if (
      host.includes("google.") ||
      host === "www.google.com" ||
      host.includes("bing.com") ||
      host.includes("yahoo.com")
    ) {
      for (const key of ["imgurl", "mediaurl", "imgrefurl", "url"]) {
        const v = u.searchParams.get(key);
        if (v && isHttpUrl(v) && v !== url) {
          try {
            return decodeURIComponent(v);
          } catch {
            return v;
          }
        }
      }
    }
    return url;
  } catch {
    return url;
  }
}

function extractOgImage(html, pageUrl) {
  if (!html || typeof html !== "string") return null;
  const patterns = [
    /<meta[^>]+property=["']og:image:secure_url["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']/i,
    /<meta[^>]+name=["']twitter:image:src["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']/i,
    /<meta[^>]+content=["']([^"']+)["'][^>]+name=["']twitter:image["']/i,
    /<link[^>]+rel=["']image_src["'][^>]+href=["']([^"']+)["']/i
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

async function fetchBytes(url) {
  const origin = (() => {
    try {
      return new URL(url).origin;
    } catch {
      return null;
    }
  })();

  const headerSets = [
    {
      "User-Agent":
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
      Accept: "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9",
      ...(origin ? { Referer: origin + "/" } : {})
    },
    {
      "User-Agent":
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
      Accept: "*/*"
    },
    {
      "User-Agent":
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
      Accept: "image/*,*/*"
    }
  ];

  let lastStatus = 0;
  for (const headers of headerSets) {
    try {
      const res = await fetch(url, {
        method: "GET",
        headers,
        redirect: "follow",
        signal: AbortSignal.timeout(20000)
      });
      lastStatus = res.status;
      if (!res.ok) continue;
      const contentType = (res.headers.get("content-type") || "").toLowerCase();
      const buf = Buffer.from(await res.arrayBuffer());
      if (!buf.length) continue;
      if (buf.length > MAX_BYTES) {
        const err = new Error("Image is larger than 15 MB");
        err.status = 400;
        throw err;
      }
      return { buf, contentType, status: res.status };
    } catch (e) {
      if (e.status === 400) throw e;
    }
  }
  const err = new Error(`download_failed_${lastStatus || "network"}`);
  err.status = lastStatus || 502;
  throw err;
}

async function uploadBufferToCloudinary(buffer, contentType, filename) {
  const form = new FormData();
  const type =
    (contentType || "image/jpeg").split(";")[0].trim() || "image/jpeg";
  const blob = new Blob([buffer], {
    type: type.startsWith("image/") ? type : "image/jpeg"
  });
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
      `Cloudinary ${up.status}`;
    throw new Error(typeof msg === "string" ? msg : JSON.stringify(msg));
  }
  const secure = data.secure_url || data.url || "";
  if (!secure) throw new Error("Cloudinary returned no URL");
  return secure;
}

async function uploadRemoteUrlToCloudinary(url) {
  const form = new FormData();
  form.append("file", url);
  form.append("upload_preset", UPLOAD_PRESET);
  form.append("folder", "travira");
  const up = await fetch(CLOUDINARY_URL, {
    method: "POST",
    body: form,
    signal: AbortSignal.timeout(45000)
  });
  const data = await up.json().catch(() => ({}));
  if (!up.ok) {
    throw new Error(
      (data.error && data.error.message) || `Cloudinary remote ${up.status}`
    );
  }
  const secure = data.secure_url || data.url || "";
  if (!secure) throw new Error("Cloudinary remote returned no URL");
  return secure;
}

/**
 * Prefer a Cloudinary copy; if the site blocks us, still save the public URL
 * so the photo shows in the Travira app.
 */
async function importImageFromUrl(rawUrl) {
  let url = String(rawUrl || "").trim();
  if (!url) {
    const err = new Error("Please paste an image link");
    err.status = 400;
    throw err;
  }
  if (!isHttpUrl(url)) {
    const err = new Error("Link must start with http:// or https://");
    err.status = 400;
    throw err;
  }

  url = unwrapSearchEngineUrl(url);

  if (isCloudinaryUrl(url)) {
    return { imageUrl: url, source: "cloudinary-existing" };
  }

  try {
    const { buf, contentType } = await fetchBytes(url);

    const isHtml =
      contentType.includes("text/html") ||
      contentType.includes("application/xhtml") ||
      (!contentType.startsWith("image/") &&
        buf.slice(0, 64).toString("utf8").toLowerCase().includes("<!doctype"));

    if (isHtml) {
      const og = extractOgImage(buf.toString("utf8").slice(0, 250000), url);
      if (og && og !== url) {
        return importImageFromUrl(og);
      }
    } else {
      const extMatch = (contentType.match(/image\/([\w+]+)/) || [])[1];
      let ext = "jpg";
      if (extMatch) {
        ext =
          extMatch === "jpeg"
            ? "jpg"
            : extMatch === "svg+xml"
              ? "svg"
              : extMatch.replace("+xml", "");
      }
      try {
        const imageUrl = await uploadBufferToCloudinary(
          buf,
          contentType,
          `travira_${Date.now()}.${ext}`
        );
        return { imageUrl, source: "imported" };
      } catch (_) {
        /* try other strategies */
      }
    }
  } catch (_) {
    /* try other strategies */
  }

  try {
    const imageUrl = await uploadRemoteUrlToCloudinary(url);
    return { imageUrl, source: "cloudinary-remote" };
  } catch (_) {
    /* fallback */
  }

  // Easy path: keep the public link — user can see it on Google / the web,
  // so the app can load it the same way.
  return {
    imageUrl: url,
    source: "direct",
    message: "Public photo link saved. It will show in the app."
  };
}

module.exports = {
  importImageFromUrl,
  isCloudinaryUrl
};
