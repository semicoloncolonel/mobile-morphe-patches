<h1 align="center">TV Morphe Patches</h1>

<p align="center">Ad-blocking and anti-tracking patches for Android TV streaming apps.</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=semicoloncolonel/tv-morphe-patches"><img alt="Add to Morphe Manager" src="https://img.shields.io/badge/%2B_Add_to-Morphe_Manager-4f46e5?style=for-the-badge"></a>
  <br>
  <sub>Open this link on the device with Morphe Manager installed.</sub>
</p>

These patches are built from [mobile-morphe-patches](https://github.com/semicoloncolonel/mobile-morphe-patches), which holds the source, the documentation, and the issue tracker. This repository holds the releases and the `patches-bundle.json` that Morphe Manager reads. Its files are updated automatically, so changes made here are overwritten.

> Some patches are based on [ajstrick81's Android TV patches](https://github.com/ajstrick81/morphe-androidtv-patches), and the original hook designs are their work.

## Install

1. Download the Android TV build of the app, in the version listed in [Supported apps](https://github.com/semicoloncolonel/mobile-morphe-patches/blob/main/docs/apps.md). [Android TV](https://github.com/semicoloncolonel/mobile-morphe-patches/blob/main/docs/android-tv.md) lists the apps and explains how to get the Android TV build.
2. Add this source in Morphe Manager with the link above, or go to Sources, tap +, choose Remote, and paste `https://raw.githubusercontent.com/semicoloncolonel/tv-morphe-patches/main/patches-bundle.json`.
3. Select the file and apply the patches.

The phone patches are a separate source, [mobile-morphe-patches](https://github.com/semicoloncolonel/mobile-morphe-patches). To patch phone and Android TV apps in one copy of Morphe Manager, add both sources.

## Report a problem

Open an issue in [mobile-morphe-patches](https://github.com/semicoloncolonel/mobile-morphe-patches/issues/new/choose), and say that the app is the Android TV build.

## License

[GPLv3](LICENSE), the same license as mobile-morphe-patches.
