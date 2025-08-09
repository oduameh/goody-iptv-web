# Goody IPTV Android app

- Compose UI
- Media3 ExoPlayer (HLS)
- Favorites, search, last-channel resume
- Picture-in-Picture (phone)
- TV launcher (Leanback), DPAD navigation
- Optional XMLTV URL for EPG now/next
- Track/subtitle selection

## Build
Using Gradle from CLI:
```
# Phone
gradle -p goody-iptv-app :app:assemblePhoneDebug
# TV
gradle -p goody-iptv-app :app:assembleTvDebug
# Bundles (store-ready AAB)
gradle -p goody-iptv-app :app:bundlePhoneRelease :app:bundleTvRelease
```

Or open `goody-iptv-app` in Android Studio and build variants `phoneDebug`/`tvDebug`. 