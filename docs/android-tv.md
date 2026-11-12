# Android TV

Android TV support comes in two forms. The apps in the table below have their own Android TV build that these patches cover, so on a TV, patch the Android TV build, not the phone build. Other apps have no Android TV build that this project patches, and the Run on Android TV patch lets their phone build run on a TV instead, as [Running a phone app on a TV](#running-a-phone-app-on-a-tv) explains. [Supported apps](apps.md) lists the tested versions and the patches for each app.

| App | Notes |
|-----|-------|
| Apple TV (Android TV) | A separate app from the phone build, with its own patches. |
| HBO Max | |
| JustWatch | |
| NewsNation (Android TV) | A separate app from the phone build, with its own patches. |
| Plex | |
| Pluto TV | The phone and Android TV builds have the same version. The Android TV build's version code is `520000991`, and the phone build's is `420000991`. |
| Tubi | The Android TV build has its own version, `10.44.5001`. The phone build is `10.44.3`. |

Starz and Disney+ also have Android TV builds with the same version names as their phone builds, but their patches support only the phone builds. Their version codes tell them apart. For example, the phone build of Disney+ 26.20.0+rc3-2026.11.02 has the version code `179366219`, and its Android TV build has `1793662190`. To use these on a TV, patch the phone build and turn on Run on Android TV, below.

[Known issues](known-issues.md) includes the problems that happen only on a TV.

## Running a phone app on a TV

Some streaming apps have no Android TV build that this project patches. For those, the **Run on Android TV** patch makes the phone build install and open on an Android TV device and adds it to the TV home screen. It is available on AMC+, Disney+, ESPN, Fandango at Home, Fox One, Hulu, Kanopy, MGM+, MLB, NBA, NFL, Paramount+, PBS, Peacock, Prime Video, Shudder, Starz, Sundance Now, and Xumo Play.

This is not the same as the apps above, which have their own Android TV builds. Run on Android TV only changes the manifest of the phone build: it marks the leanback and touchscreen features as optional, so a TV no longer filters the app out, and it adds a leanback launcher entry, so the app appears on the home screen. The app's own screens are still drawn for a phone, so a remote may not reach every control, and some parts may still expect a touchscreen. For that reason the patch is experimental and off by default. Turn it on in Morphe Manager when you patch.

## Getting the Android TV build

APKMirror lists the Android TV build separately, with "Android TV" in its name. Morphe Manager's download link searches by package name and version, so it can lead to the phone build instead. When the phone and Android TV builds share a version, as Pluto TV's do, check the version code on the download page.

To download the Android TV build directly, use [Aurora Store](https://auroraoss.com/) with a spoofed Android TV device. Aurora's [config generator](https://auroraoss.com/config-generator) creates the device config for spoofing.

