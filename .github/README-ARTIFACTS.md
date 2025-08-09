# Build Artifacts

CI produces:
- phone debug APK: `goody-iptv-app/app/build/outputs/apk/phone/debug/*.apk`
- tv debug APK: `goody-iptv-app/app/build/outputs/apk/tv/debug/*.apk`
- phone release AAB: `goody-iptv-app/app/build/outputs/bundle/phoneRelease/*.aab`
- tv release AAB: `goody-iptv-app/app/build/outputs/bundle/tvRelease/*.aab`

Sideload via ADB:
```
adb install -r app-phone-debug.apk
adb install -r app-tv-debug.apk
``` 