#!/usr/bin/env bash
# Captures real screenshots from a running emulator.
# Taps are located by reading the view hierarchy (uiautomator), with a fixed
# coordinate fallback, so the sequence survives layout changes.
set -u
PKG=com.herowalls.app.debug
ACT=$PKG/com.herowalls.app.MainActivity
OUT=docs/screenshots
mkdir -p "$OUT"

tap_text() {
  local needle="$1"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || return 1
  adb shell cat /sdcard/ui.xml > /tmp/ui.xml 2>/dev/null || return 1
  local coords
  coords=$(python3 - "$needle" <<'PY'
import re, sys
needle = sys.argv[1].lower()
try:
    xml = open('/tmp/ui.xml', encoding='utf-8', errors='ignore').read()
except Exception:
    sys.exit(0)
for tag in re.findall(r'<node[^>]*>', xml):
    text = (re.search(r'text="([^"]*)"', tag) or [None, ""])[1]
    desc = (re.search(r'content-desc="([^"]*)"', tag) or [None, ""])[1]
    if needle in (text + " " + desc).lower():
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        if b:
            x = (int(b.group(1)) + int(b.group(3))) // 2
            y = (int(b.group(2)) + int(b.group(4))) // 2
            print(f"{x} {y}")
            break
PY
)
  [ -z "$coords" ] && return 1
  adb shell input tap $coords
}

shot() { adb exec-out screencap -p > "$OUT/$1"; echo "captured $1"; }

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n $ACT
sleep 60
shot 01-home.png

# Wallpaper detail: tap the daily hero card
tap_text "wallpaper" || adb shell input tap 270 900
sleep 16
shot 02-detail.png
adb shell input keyevent KEYCODE_BACK
sleep 5

# Home scrolled to the collection rails
adb shell input swipe 540 1400 540 600 400
sleep 10
shot 03-collections.png

# Search tab
tap_text "Search" || adb shell input tap 410 2250
sleep 8
shot 04-search.png

# Type a character and capture the results grid
tap_text "Search heroes" || adb shell input tap 540 300
sleep 3
adb shell input text "Spider-Man"
sleep 2
adb shell input keyevent KEYCODE_ENTER
sleep 22
shot 05-search-results.png

ls -la "$OUT"
