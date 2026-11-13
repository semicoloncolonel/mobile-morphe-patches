# Android TV

These apps have patches for their Android TV builds. On a TV, patch the Android TV build, not the phone build. [Supported apps](apps.md) lists the tested versions and the patches for each app.

| App | Notes |
|-----|-------|
| Apple TV (Android TV) | A separate app from the phone build, with its own patches. |
| Disney+ | The phone and Android TV builds share the version name, `26.20.0+rc3-2026.11.02`. The Android TV build's version code is `1793662190`, and the phone build's is `179366219`. |
| HBO Max | |
| Hulu (Android TV) | A separate app from the phone build (`com.hulu.livingroomplus`, version `C59F39CFP3.10.33`), on a native WebKit engine that cannot be dex-patched. See [Installing Hulu on Android TV](#installing-hulu-on-android-tv) for the download, patch, and install steps. Skip ads is **experimental**: it bundles a native library that hooks `inflate` inside `libwkf_support.so` and strips the stitched ad periods from the DASH manifest in-process, with no root. It is not yet verified against a real ad break, so it may not remove every ad; it is conservative and does not affect ad-free content. |
| JustWatch | |
| NewsNation (Android TV) | A separate app from the phone build, with its own patches. |
| Plex | |
| Pluto TV | The phone and Android TV builds have the same version. The Android TV build's version code is `520000991`, and the phone build's is `420000991`. |
| Starz | The phone and Android TV builds share the version, `6.12.0`. The Android TV build's version code is `9510`, and the phone build's is `4518`. Skip launch video does not apply to the Android TV build, which opens straight to its landing screen. |
| Tubi | The Android TV build has its own version, `10.44.5001`. The phone build is `10.44.3`. |
| YouTube TV (Android TV) | Not the YouTube TV app itself, which runs on an engine that cannot be patched. This patches **YouTube for Android TV** (`com.google.android.youtube.tv`, version `7.32.300`) and turns it into a separate, ad-free YouTube TV app named "YT TV+". It installs alongside the original, loads the YouTube TV live and DVR service, and removes the ads YouTube injects into it. Sign in with an activation code at tv.youtube.com/start. Live broadcast commercials are part of the broadcast and still play. |

[Known issues](known-issues.md) includes the problems that happen only on a TV.

## Getting the Android TV build

APKMirror lists the Android TV build separately, with "Android TV" in its name. Morphe Manager's download link searches by package name and version, so it can lead to the phone build instead. When the phone and Android TV builds share a version, as Pluto TV's do, check the version code on the download page.

To download the Android TV build directly, use [Aurora Store](https://auroraoss.com/) with a spoofed Android TV device. Aurora's [config generator](https://auroraoss.com/config-generator) creates the device config for spoofing.

## Installing Hulu on Android TV

Hulu on Android TV takes a few extra steps, because its app is a split bundle and the patched result installs as a single file. Follow these in order.

1. **Download the full app bundle, not a single APK.** You need the Android TV build: `com.hulu.livingroomplus`, version `C59F39CFP3.10.33`, including the `armeabi_v7a` split. Hulu's native libraries live in that split, so a base-only APK, or Morphe Manager's "Use installed app" on a Play Store copy, leaves them out and the patched app closes at launch. The simplest source is APKMirror. In the APK Download Helper, select Hulu (Android TV), set the source to APKMirror, and download the bundle.

2. **Patch the downloaded bundle in Morphe Manager.** Patch the file you downloaded, not an installed copy, and select the Hulu (Android TV) patches. Morphe Manager merges the splits and the patch into one APK.

3. **Install the patched file as a single APK.** The patched result is one complete APK, even though it keeps a bundle extension such as `.apkm` or `.apks`. A split installer expects several APKs inside the file and refuses the single file with an error about the package kind. Rename the patched file to end in `.apk`, and install it as an ordinary app.

4. **Sign in** with the activation code at hulu.com/activate. Live broadcast commercials are part of the broadcast and still play.

