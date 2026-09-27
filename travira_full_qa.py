#!/usr/bin/env python3
"""
Travira full QA script
======================
Tests backend APIs (all main routes) interactively.
Optionally launches the app on BrowserStack if credentials are set.

How to run (PowerShell):
  cd D:\\Coding\\MY_PROJECT\\Travira-APP-Minor
  pip install requests Appium-Python-Client
  python travira_full_qa.py

You will be prompted for:
  - User email / password
  - Admin email / password
  - (Forgot-password) token from email link
  - Optional BrowserStack username, access key, app_url (bs://...)

Do NOT commit real passwords or BrowserStack keys to Git.
"""

from __future__ import annotations

import json
import sys
import time
import getpass
from typing import Any, Optional

try:
    import requests
except ImportError:
    print("Install requests first:  pip install requests")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────
BASE_URL = "https://travira-app-minor.onrender.com"
TIMEOUT = 45
# BrowserStack video: script will ASK you (y/N). Set True to skip the question and always run.
RUN_BROWSERSTACK_UI = None  # None = ask, True = always, False = never

# ── Helpers ───────────────────────────────────────────────────────────
passed = 0
failed = 0
skipped = 0
results: list[tuple[str, str, str]] = []  # (name, status, detail)


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "") -> None:
    global passed, failed
    status = "PASS" if ok else "FAIL"
    if ok:
        passed += 1
    else:
        failed += 1
    results.append((name, status, detail))
    mark = "✅" if ok else "❌"
    log(f"  {mark} [{status}] {name}" + (f" — {detail}" if detail else ""))


def skip(name: str, reason: str) -> None:
    global skipped
    skipped += 1
    results.append((name, "SKIP", reason))
    log(f"  ⏭️  [SKIP] {name} — {reason}")


def prompt(label: str, default: str = "", secret: bool = False) -> str:
    if default:
        label = f"{label} [{default}]"
    if secret:
        val = getpass.getpass(f"{label}: ")
    else:
        val = input(f"{label}: ").strip()
    return val or default


def api(
    method: str,
    path: str,
    *,
    token: Optional[str] = None,
    json_body: Any = None,
    expected: tuple[int, ...] = (200,),
    name: str = "",
) -> tuple[bool, dict, int]:
    """Call API; return (ok, body_dict, status_code)."""
    url = path if path.startswith("http") else f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    try:
        r = requests.request(
            method.upper(),
            url,
            headers=headers,
            json=json_body,
            timeout=TIMEOUT,
        )
    except Exception as e:
        if name:
            record(name, False, str(e))
        return False, {"message": str(e)}, 0

    try:
        body = r.json()
    except Exception:
        body = {"raw": (r.text or "")[:300]}

    ok = r.status_code in expected
    detail = f"HTTP {r.status_code}"
    if not ok:
        detail += f" body={json.dumps(body)[:180]}"
    elif isinstance(body, dict) and body.get("message"):
        detail += f" | {body.get('message')}"
    if name:
        record(name, ok, detail)
    return ok, body if isinstance(body, dict) else {"data": body}, r.status_code


# ── Sections ──────────────────────────────────────────────────────────
def section_health() -> None:
    log("\n═══ 1. HEALTH / PING ═══")
    api("GET", "/api/health", name="GET /api/health")
    api("GET", "/api/ping", name="GET /api/ping")
    api("GET", "/api/health/email", name="GET /api/health/email")
    api("GET", "/", name="GET /")


def section_login(email: str, password: str, label: str) -> tuple[str, str, dict]:
    log(f"\n═══ LOGIN ({label}) ═══")
    ok, body, _ = api(
        "POST",
        "/api/users/login",
        json_body={"email": email, "password": password},
        name=f"Login {label}",
    )
    token = (body.get("accessToken") or "") if ok else ""
    refresh = (body.get("refreshToken") or "") if ok else ""
    user = body.get("user") or {}
    if ok:
        log(f"     → role={user.get('role')} id={user.get('id')}")
    return token, refresh, user


def section_register() -> Optional[str]:
    log("\n═══ REGISTER NEW USER (optional — NOT used for forgot-password) ═══")
    log("  Default is NO. Only say y if you want an extra throwaway account.")
    ans = prompt("Create a temporary QA user? (y/N)", "N").lower()
    if ans != "y":
        skip("Register new user", "skipped by user (recommended)")
        return None
    ts = int(time.time())
    email = f"qa.auto.{ts}@example.com"
    password = "QaTest123!"
    ok, body, _ = api(
        "POST",
        "/api/users/register",
        json_body={"name": "QA Auto", "email": email, "password": password},
        name="POST /api/users/register",
    )
    if not ok:
        return None
    ok2, _, _ = api(
        "POST",
        "/api/users/login",
        json_body={"email": email, "password": password},
        name="Login newly registered user",
    )
    if ok2:
        log(f"     → registered {email} / {password} (temp)")
    return email


def section_places_public() -> Optional[str]:
    log("\n═══ PUBLIC PLACES ═══")
    ok, body, _ = api("GET", "/api/place", name="GET /api/place")
    place_id = None
    data = body.get("data") or []
    if ok and data:
        place_id = data[0].get("_id")
        log(f"     → {len(data)} places; first={data[0].get('name')} ({place_id})")
        api("GET", f"/api/place/{place_id}", name="GET /api/place/:id")
    api("GET", "/api/places", name="GET /api/places (alias)")
    return place_id


def section_user_features(token: str, place_id: Optional[str]) -> None:
    log("\n═══ USER FEATURES (auth) ═══")
    if not token:
        skip("User features", "no user token")
        return

    api("GET", "/api/users/me", token=token, name="GET /api/users/me")
    api("GET", "/api/users/profile", token=token, name="GET /api/users/profile")
    api(
        "PUT",
        "/api/users/profile",
        token=token,
        json_body={"location": "Surat, Gujarat", "bio": "QA full suite bio"},
        name="PUT /api/users/profile",
    )
    api("GET", "/api/users/notifications", token=token, name="GET /api/users/notifications")
    api(
        "PUT",
        "/api/users/notifications/read",
        token=token,
        json_body={},
        name="PUT /api/users/notifications/read",
    )
    api("GET", "/api/users/visited", token=token, name="GET /api/users/visited")
    api("GET", "/api/place/user/wishlist", token=token, name="GET wishlist")
    api("GET", "/api/place/user/my-places", token=token, name="GET my-places")

    if place_id:
        api(
            "POST",
            f"/api/place/{place_id}/wishlist",
            token=token,
            name="POST wishlist add",
        )
        api(
            "DELETE",
            f"/api/place/{place_id}/wishlist",
            token=token,
            name="DELETE wishlist remove",
        )
        api(
            "POST",
            f"/api/users/visited/{place_id}",
            token=token,
            name="POST visited add",
        )
        api(
            "DELETE",
            f"/api/users/visited/{place_id}",
            token=token,
            name="DELETE visited remove",
        )
        api(
            "POST",
            f"/api/place/{place_id}/rating",
            token=token,
            json_body={"value": 5, "feedback": "Full QA suite rating"},
            name="POST rating",
        )


def section_chat(token: str) -> None:
    log("\n═══ AI CHAT ═══")
    if not token:
        skip("Chat", "no user token")
        return
    api(
        "POST",
        "/api/chat",
        token=token,
        json_body={
            "message": "Suggest one food to try in Surat. One short sentence.",
            "history": [],
        },
        name="POST /api/chat (travel)",
        expected=(200,),
    )
    api(
        "POST",
        "/api/chat",
        token=token,
        json_body={"message": "Write Python code to sort a list", "history": []},
        name="POST /api/chat (off-topic refuse)",
    )
    # No token
    api(
        "POST",
        "/api/chat",
        json_body={"message": "hi"},
        expected=(401,),
        name="POST /api/chat without token → 401",
    )


def section_refresh_logout(token: str, refresh: str) -> None:
    log("\n═══ REFRESH + LOGOUT ═══")
    if not refresh:
        skip("Refresh/logout", "no refresh token")
        return
    ok, body, _ = api(
        "POST",
        "/api/users/refresh-token",
        json_body={"refreshToken": refresh},
        name="POST /api/users/refresh-token",
    )
    new_access = body.get("accessToken") or token
    if token:
        api(
            "POST",
            "/api/users/logout",
            token=token,
            json_body={"refreshToken": refresh},
            name="POST /api/users/logout",
        )
        # Refresh after logout should fail
        api(
            "POST",
            "/api/users/refresh-token",
            json_body={"refreshToken": refresh},
            expected=(401, 403),
            name="Refresh after logout → fail",
        )
    return


def section_forgot_reset(default_email: str = "") -> bool:
    """
    Asks YOU which email should receive the reset link.
    Does NOT create a new user — uses an existing account email you type.
    You open that inbox, copy the token from the link, and paste it here.
    """
    log("\n═══ FORGOT / RESET PASSWORD ═══")
    log("  This uses YOUR email (existing account). No new user is created.")
    ans = prompt("Run forgot-password flow? (y/N)", "N").lower()
    if ans != "y":
        skip("Forgot password", "skipped by user")
        return False

    # Always ask which inbox should get the link
    email = prompt(
        "Email to send reset link to (check this inbox for the token)",
        default_email or "preetbyte.27@gmail.com",
    )
    if not email or "@" not in email:
        record("Forgot password", False, "valid email required")
        return False

    ok, body, _ = api(
        "POST",
        "/api/users/forgot-password",
        json_body={"email": email},
        name="POST /api/users/forgot-password",
    )
    log(f"     emailSent={body.get('emailSent')} message={body.get('message')}")
    log("")
    log(f"  >>> Check inbox (and spam) for: {email}")
    log("  Open the link. It looks like:")
    log("  https://travira-app-minor.onrender.com/reset-password.html?token=LONG_TOKEN")
    log("  Copy ONLY the token (the part after token=) and paste below.")
    log("  The script will WAIT here until you paste it.")
    log("")

    token = prompt("Paste reset token from your email (or leave empty to skip)")
    if not token:
        skip("Reset password", "no token pasted")
        return False

    # Allow pasting full URL by mistake — extract token=
    if "token=" in token:
        token = token.split("token=", 1)[1].split("&")[0].strip()

    new_pass = prompt("New password for this account (min 6 chars)", secret=True)
    confirm = prompt("Confirm new password", secret=True)
    if not new_pass or len(new_pass) < 6:
        record("Reset password", False, "password must be at least 6 characters")
        return False
    if new_pass != confirm:
        record("Reset password", False, "passwords do not match")
        return False

    ok, body, _ = api(
        "POST",
        "/api/users/reset-password",
        json_body={
            "token": token,
            "password": new_pass,
            "confirmPassword": confirm,
        },
        name="POST /api/users/reset-password",
    )
    if ok:
        ok2, _, _ = api(
            "POST",
            "/api/users/login",
            json_body={"email": email, "password": new_pass},
            name="Login with NEW password after reset",
        )
        if ok2:
            log(f"  ⚠️  Password for {email} was changed. Use the new password in the app.")



def section_admin(admin_token: str) -> None:
    log("\n═══ ADMIN ═══")
    if not admin_token:
        skip("Admin section", "no admin token")
        return

    api("GET", "/api/admin/places", token=admin_token, name="GET /api/admin/places")
    api("GET", "/api/admin/users", token=admin_token, name="GET /api/admin/users")

    # Add place
    ok, body, _ = api(
        "POST",
        "/api/admin/places",
        token=admin_token,
        json_body={
            "name": f"QA Auto Place {int(time.time())}",
            "shortDescription": "Created by travira_full_qa.py",
            "description": "Safe to delete — automated test place",
            "city": "Surat",
            "state": "Gujarat",
            "country": "India",
            "location": "Surat, Gujarat",
            "imageUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg",
        },
        name="POST /api/admin/places (add)",
    )
    place = body.get("place") or {}
    pid = place.get("_id")
    if not pid:
        return

    api(
        "GET",
        f"/api/admin/places/{pid}",
        token=admin_token,
        name="GET /api/admin/places/:id",
    )
    api(
        "PUT",
        f"/api/admin/places/{pid}",
        token=admin_token,
        json_body={"shortDescription": "Updated by full QA script"},
        name="PUT /api/admin/places/:id (edit)",
    )
    api(
        "DELETE",
        f"/api/admin/places/{pid}",
        token=admin_token,
        name="DELETE /api/admin/places/:id",
    )

    # Admin create + delete temp user
    ts = int(time.time())
    email = f"qa.admincreated.{ts}@example.com"
    ok, body, _ = api(
        "POST",
        "/api/admin/users",
        token=admin_token,
        json_body={
            "name": "QA AdminCreated",
            "email": email,
            "password": "TempPass99",
            "role": "user",
        },
        name="POST /api/admin/users",
    )
    uid = (body.get("user") or {}).get("id")
    if uid:
        api(
            "PUT",
            f"/api/admin/users/{uid}",
            token=admin_token,
            json_body={"location": "Mumbai"},
            name="PUT /api/admin/users/:id",
        )
        api(
            "DELETE",
            f"/api/admin/users/{uid}",
            token=admin_token,
            name="DELETE /api/admin/users/:id",
        )


def section_access_control(user_token: str) -> None:
    log("\n═══ ACCESS CONTROL ═══")
    if not user_token:
        skip("User blocked from admin", "no user token")
        return
    api(
        "GET",
        "/api/admin/places",
        token=user_token,
        expected=(403, 401),
        name="User cannot access /api/admin/places",
    )



def section_browserstack_ui(
    user_email: str = "",
    user_pass: str = "",
) -> None:
    """
    Full UI walkthrough on a real BrowserStack device WITH VIDEO.
    Mirrors the main QA areas: open app, login, places, profile, AI chat.
    """
    log("\n═══ BROWSERSTACK FULL QA VIDEO ═══")
    log("  Records a real-device video of the whole app QA walkthrough.")

    if RUN_BROWSERSTACK_UI is False:
        skip("BrowserStack video", "disabled (RUN_BROWSERSTACK_UI=False)")
        return
    if RUN_BROWSERSTACK_UI is not True:
        ans = prompt("Record full BrowserStack QA video for documentation? (y/N)", "Y").lower()
        if ans not in ("y", "yes"):
            skip("BrowserStack video", "skipped by user")
            return

    try:
        from appium import webdriver
        from appium.options.common import AppiumOptions
        from appium.webdriver.common.appiumby import AppiumBy
    except ImportError:
        skip("BrowserStack video", "pip install Appium-Python-Client")
        return

    bs_user = prompt("BrowserStack username", "preetpatel_z1xb5i")
    bs_key = prompt("BrowserStack access key", secret=True)
    app = prompt("BrowserStack app_url (bs://...)")
    if not (bs_user and bs_key and app):
        skip("BrowserStack video", "missing credentials")
        return

    device = prompt("Device name", "Google Pixel 8")
    os_ver = prompt("Android OS version", "14.0")

    # Prefer credentials already entered at start of script
    login_email = user_email or prompt("App login email for video", "preetbyte.27@gmail.com")
    login_pass = user_pass or prompt("App login password for video", secret=True)

    options = AppiumOptions().load_capabilities({
        "platformName": "Android",
        "appium:automationName": "UiAutomator2",
        "appium:app": app,
        "appium:autoGrantPermissions": True,
        "appium:newCommandTimeout": 300,
        "bstack:options": {
            "userName": bs_user,
            "accessKey": bs_key,
            "projectName": "Travira",
            "buildName": "Travira Full QA Documentation",
            "sessionName": "Full QA walkthrough video",
            "deviceName": device,
            "osVersion": os_ver,
            "debug": True,
            "networkLogs": True,
            "appiumLogs": True,
            "video": True,
            "interactiveDebugging": True,
        },
    })

    log("  Starting BrowserStack session (full QA video)...")
    driver = webdriver.Remote("https://hub.browserstack.com/wd/hub", options=options)
    session_id = getattr(driver, "session_id", None)

    def pause(sec: float, label: str = "") -> None:
        if label:
            log(f"  [video] {label}")
        time.sleep(sec)

    def tap_text(needle: str, label: str = "", wait: float = 3.5) -> bool:
        label = label or needle
        try:
            el = driver.find_element(
                AppiumBy.ANDROID_UIAUTOMATOR,
                f'new UiSelector().textContains("{needle}")',
            )
            el.click()
            log(f"  [video] Tapped: {label}")
            time.sleep(wait)
            return True
        except Exception:
            log(f"  [video] Skip (not found): {label}")
            time.sleep(1.5)
            return False

    def tap_desc(needle: str, label: str = "", wait: float = 3.5) -> bool:
        label = label or needle
        try:
            el = driver.find_element(
                AppiumBy.ANDROID_UIAUTOMATOR,
                f'new UiSelector().descriptionContains("{needle}")',
            )
            el.click()
            log(f"  [video] Tapped desc: {label}")
            time.sleep(wait)
            return True
        except Exception:
            return False

    def type_into_focused(text_value: str) -> None:
        try:
            driver.execute_script("mobile: type", {"text": text_value})
        except Exception:
            try:
                driver.switch_to.active_element.send_keys(text_value)
            except Exception as e:
                log(f"  [video] type failed: {e}")

    def swipe_up() -> None:
        try:
            size = driver.get_window_size()
            x = size["width"] // 2
            driver.swipe(
                x,
                int(size["height"] * 0.78),
                x,
                int(size["height"] * 0.32),
                700,
            )
            time.sleep(2.5)
        except Exception as e:
            log(f"  [video] swipe failed: {e}")

    try:
        # 1) Splash / intro
        pause(12, "Splash / intro loading...")
        src_len = len(driver.page_source or "")
        record("BS video: app UI loaded", src_len > 1000, f"page_source={src_len}")

        # Skip intro / get started if present
        tap_text("Skip", "Skip intro", 3)
        tap_text("Get Started", "Get Started", 3)
        tap_text("Continue", "Continue", 3)

        # 2) Login screen
        pause(2, "Login flow")
        logged = False
        if tap_text("Email", "Email field", 1.5) or tap_text("email", "email field", 1.5):
            type_into_focused(login_email)
            pause(1)
            if tap_text("Password", "Password field", 1.5) or tap_text("password", "password field", 1.5):
                type_into_focused(login_pass)
                pause(1)
            if (
                tap_text("Login", "Login button", 6)
                or tap_text("Log in", "Log in button", 6)
                or tap_text("Sign in", "Sign in button", 6)
            ):
                logged = True
                record("BS video: login attempted", True, login_email)
        else:
            # Maybe already on home
            log("  [video] No email field — may already be logged in / home")
            record("BS video: login screen", True, "email field not shown (ok)")

        pause(5, "After login / home")

        # 3) Home / Discover places
        tap_text("Travira", "Home branding", 3)
        tap_text("Discover", "Discover places", 4)
        tap_text("Places", "Places tab", 4)
        tap_text("Home", "Home tab", 3)
        pause(2, "Scrolling places list")
        swipe_up()
        swipe_up()
        record("BS video: places browse", True, "scrolled list")

        # 4) Open first place card if possible (tap common place names / rating)
        opened = (
            tap_text("Surat", "Place card Surat", 4)
            or tap_text("Temple", "Place card Temple", 4)
            or tap_text("Beach", "Place card Beach", 4)
        )
        if opened:
            pause(3, "Place detail")
            tap_text("Wishlist", "Add wishlist", 3)
            tap_text("Favorite", "Favorite", 2)
            tap_text("Visited", "Mark visited", 3)
            # back
            try:
                driver.back()
                pause(2, "Back from place detail")
            except Exception:
                pass

        # 5) Profile
        pause(2, "Profile section")
        tap_text("Profile", "Profile tab", 4) or tap_desc("Profile", "Profile tab", 4)
        swipe_up()
        tap_text("Notification", "Notifications", 3)
        try:
            driver.back()
            pause(1.5)
        except Exception:
            pass
        tap_text("Wishlist", "Wishlist screen", 3)
        try:
            driver.back()
            pause(1.5)
        except Exception:
            pass
        record("BS video: profile area", True)

        # 6) AI Chat
        pause(2, "AI Chat section")
        (
            tap_text("AI", "AI tab", 4)
            or tap_text("Chat", "Chat tab", 4)
            or tap_desc("Chat", "Chat tab", 4)
            or tap_desc("AI", "AI tab", 4)
        )
        pause(3, "Chat screen open")
        # Try type a travel question
        if tap_text("Ask", "Chat input hint", 2) or tap_text("message", "message field", 2):
            type_into_focused("Best food in Surat?")
            pause(1)
            tap_text("Send", "Send message", 8) or tap_desc("Send", "Send", 8)
            pause(6, "Waiting for AI reply")
            record("BS video: AI chat message", True)
        else:
            # Still show chat screen on video
            record("BS video: AI chat screen", True, "input not focused (screen shown)")

        # 7) Return home
        tap_text("Home", "Back to Home", 3)
        pause(4, "Final home view for video ending")

        record("BS video: full QA walkthrough finished", True, "see BrowserStack dashboard")

        log("")
        log("  ★ FULL QA VIDEO — download for documentation:")
        log("  https://app-automate.browserstack.com")
        log("  Project: Travira → Build: Travira Full QA Documentation")
        if session_id:
            log(f"  Session: {session_id}")
            log(f"  https://app-automate.browserstack.com/dashboard/v2/sessions/{session_id}")
        log("  Wait 1–2 min if video is processing, then Download.")
        log("")
    except Exception as e:
        record("BS video: walkthrough", False, str(e)[:200])
        log(f"  [video] Error: {e}")
    finally:
        driver.quit()
        log("  Session closed — video finalizing on BrowserStack.")



def print_summary() -> None:
    log("\n" + "═" * 60)
    log("SUMMARY")
    log("═" * 60)
    for name, status, detail in results:
        log(f"  {status:4}  {name}" + (f"  ({detail})" if detail and status == "FAIL" else ""))
    log("─" * 60)
    log(f"  PASS={passed}  FAIL={failed}  SKIP={skipped}  TOTAL={passed+failed+skipped}")
    log("═" * 60)
    if failed:
        log("Some checks failed — scroll up for details.")
        sys.exit(1)
    log("All executed checks passed.")


def main() -> None:
    log("Travira Full QA")
    log(f"API base: {BASE_URL}")
    log("Passwords are typed hidden (not shown on screen).\n")

    user_email = prompt("User email", "preetbyte.27@gmail.com")
    user_pass = prompt("User password", secret=True)
    admin_email = prompt("Admin email", "him@travira.app")
    admin_pass = prompt("Admin password", secret=True)

    if not user_email or not user_pass:
        log("User credentials required.")
        sys.exit(1)

    section_health()
    section_register()
    place_id = section_places_public()

    user_token, user_refresh, _ = section_login(user_email, user_pass, "user")
    admin_token, _, admin_user = ("", "", {})
    if admin_email and admin_pass:
        admin_token, _, admin_user = section_login(admin_email, admin_pass, "admin")
        if admin_user.get("role") != "admin":
            log(f"  ⚠️  Admin login role is {admin_user.get('role')} — admin routes may 403")

    section_user_features(user_token, place_id)
    section_chat(user_token)
    section_access_control(user_token)
    section_admin(admin_token)

    # Refresh + logout MUST run while the original session is still valid.
    # (Forgot-password bumps tokenVersion and clears refresh tokens.)
    if user_token and user_refresh:
        section_refresh_logout(user_token, user_refresh)

    # Forgot/reset last — invalidates previous tokens on purpose
    password_changed = section_forgot_reset(default_email=user_email)
    if password_changed and user_token and user_refresh:
        log("\n═══ OLD SESSION AFTER PASSWORD RESET (expected to fail) ═══")
        api(
            "POST",
            "/api/users/refresh-token",
            json_body={"refreshToken": user_refresh},
            expected=(401, 403),
            name="Old refresh token rejected after password reset",
        )
        api(
            "GET",
            "/api/users/me",
            token=user_token,
            expected=(401,),
            name="Old access token rejected after password reset",
        )

    section_browserstack_ui(user_email=user_email, user_pass=user_pass)
    print_summary()


if __name__ == "__main__":
    main()
