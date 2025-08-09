# Goody IPTV

Cross-platform IPTV player for Web (PWA), Android phone, Android TV and Fire TV.

- App name: Goody IPTV
- Package: `com.goody.iptv`
- Default playlist: `https://iptv-org.github.io/iptv/countries/ie.m3u`
- DRM: none. EPG: optional (XMLTV).

## Projects
- `goody-iptv-web`: Static PWA. Open `index.html` or deploy via GitHub Pages. Offline shell via `sw.js`.
- `goody-iptv-app`: Android app (Compose + Media3) with phone and TV variants.

## PWA (local)
```
python3 -m http.server 8080
# Visit http://localhost:8080
```

## Android builds
Requirements: Android Studio (or JDK 21 + Android SDK), Gradle or use CI.

Local (Android Studio): Open `goody-iptv-app`, select `phoneDebug` or `tvDebug`, Build > Build APK(s).

ADB sideload:
```
adb install -r app-phone-debug.apk
adb install -r app-tv-debug.apk
```

## Settings inside app
- Playlist URL
- Optional XMLTV URL for EPG grid

Favorites persist, last channel resumes, PiP supported (phone). TV variant includes Leanback launcher and DPAD navigation. 