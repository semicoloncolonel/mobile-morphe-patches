# Android TV

These apps have patches for their Android TV builds. On a TV, patch the Android TV build, not the phone build. The table lists each app's own patches and the versions they were tested on. Disable auto-updates, Hide device identifiers, and Hide rating prompt also cover most of these apps, as [What the patches do](patches.md) explains.

The Android TV patches have moved to their own source, [TV Morphe Patches](https://github.com/semicoloncolonel/tv-morphe-patches). Add it in Morphe Manager with the link in its README. The pre-releases of mobile-morphe-patches no longer include the Android TV apps, and its stable release includes them only until 1.1.

| App | Patches | Tested version | Notes |
|-----|---------|----------------|-------|
| Apple TV | Auto-skip (experimental)<br>Hide rating prompt (experimental)<br>Show release times (experimental)<br>Block analytics & tracking (experimental, off by default) | `17.0.3`<br>`17.0.2` | A separate app from the phone build, with its own patches. |
| Disney+ | Skip ads<br>Skip launch video<br>Block analytics & tracking (experimental, off by default) | `26.20.0+rc3-2026.11.02` | The phone and Android TV builds share the version name, `26.20.0+rc3-2026.11.02`. The Android TV build's version code is `1793662190`, and the phone build's is `179366219`. |
| HBO Max | Skip ads<br>Block analytics & tracking (experimental, off by default)<br>Video quality (experimental, off by default) | `22.0.0.36` |  |
| Hulu | Skip ads (experimental)<br>Block analytics & tracking (experimental, off by default) | `C59F39CFP3.10.33` | A separate app from the phone build (`com.hulu.livingroomplus`, version `C59F39CFP3.10.33`), on a native WebKit engine that cannot be dex-patched. See [Installing Hulu on Android TV](#installing-hulu-on-android-tv) for the download, patch, and install steps. Skip ads is **experimental**: it bundles a native library that hooks `inflate` inside `libwkf_support.so` and strips the stitched ad periods from the DASH manifest in-process, with no root. On an NVIDIA Shield it removed every ad break from an on-demand episode, but it has not been tested widely and may not remove every ad. It does not affect ad-free content. |
| JustWatch | Block ads<br>Hide sponsored titles<br>Block analytics & tracking (off by default)<br>GmsCore support (experimental, off by default)<br>Hide unavailable titles (experimental, off by default)<br>Hide watched titles (experimental, off by default)<br>Streaming titles only (experimental, off by default) | `26.44.5`<br>`26.43.8` |  |
| NewsNation | Hide rating prompt<br>Skip ads<br>Block analytics & tracking (off by default) | `4.94`<br>`4.71` | A separate app from the phone build, with its own patches. |
| Plex | Hide sponsored content<br>Skip ads<br>Block analytics & tracking (off by default) | `2026.22.0` |  |
| Pluto TV | Mask live ad breaks<br>Skip ads<br>Skip launch video<br>Translate ad break screen (experimental)<br>Block analytics & tracking (experimental, off by default) | `17.4.0`<br>`17.3.0` | The phone and Android TV builds have the same version. The Android TV build's version code is `520000991`, and the phone build's is `420000991`. |
| Starz | Skip pre-roll promo (experimental)<br>Block analytics & tracking (experimental, off by default) | `6.12.0`<br>`6.11.0` | The phone and Android TV builds share the version, `6.12.0`. The Android TV build's version code is `9510`, and the phone build's is `4518`. |
| Tubi | Hide sign-in prompts<br>Hide update prompts<br>Highest quality (experimental)<br>Remove social sign-in<br>Skip ads<br>Block analytics & tracking (experimental, off by default) | `10.44.5001` | The Android TV build has its own version, `10.44.5001`. The phone build is `10.44.3`. |
| YouTube TV | Skip injected ads | `7.32.300` | Not the YouTube TV app itself, which runs on an engine that cannot be patched. This patches **YouTube for Android TV** (`com.google.android.youtube.tv`, version `7.32.300`) and turns it into a separate, ad-free YouTube TV app named "YT TV+". It installs alongside the original, loads the YouTube TV live and DVR service, and removes the ads YouTube injects into it. Sign in with an activation code at tv.youtube.com/start. Live broadcast commercials are part of the broadcast and still play. |

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

