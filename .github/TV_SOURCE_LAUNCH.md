# Launching the Android TV patch source

The Android TV patches can ship as their own patch source, `tv-morphe-patches`. Morphe Manager keeps one recommended version for each package and does not look at the form factor. While the phone and TV patches share one bundle, phones are offered the Android TV versions of apps that use one package for both, such as Apple TV 17.0.3 instead of 3.0.0.

Everything for the split is in place and switched off. Until the switch is on, releases build the same combined bundle as before.

## How it works

- `-Pflavor=all|phone|tv` in `patches/build.gradle.kts` selects the bundle. The default, `all`, is the combined bundle. `shared/misc/BundleFlavor.kt` explains the helpers.
- When the repository variable `TV_SOURCE` is set, `release.yml` builds the phone bundle for this repository's release. It then builds the TV bundle, attests it, and runs `.github/scripts/publish_tv_bundle.py`.
- The script releases the TV `.mpp` in `TV_SOURCE` and updates its `patches-bundle.json`: on `main` for a stable release, and on `dev` for a pre-release. It also keeps the TV repository's README and license in step with `.github/tv-repo/`.
- The TV release takes this repository's version number. It is published only when a file the TV bundle is built from changed since the last TV release on that branch. Adding or removing a phone app in a shared patch does not count. The TV version numbers therefore skip the phone-only releases.
- The TV release notes list the commits that touch a TV app or name one. Changes to the shared code are summarized in one line.

## Launch steps

1. Release 1.1 on main first.
2. Create the public repository `semicoloncolonel/tv-morphe-patches`, empty, with no README, license, or `.gitignore`. The first publish creates `main` from `.github/tv-repo/` and creates `dev` from `main`.
3. Create a fine-grained personal access token with access to `tv-morphe-patches` only, and the Contents permission set to read and write. Add it to this repository as the Actions secret `TV_SOURCE_TOKEN`.
4. Build the three flavors, and check that they split cleanly:

   ```bash
   for f in all phone tv; do ./gradlew :extensions:extension:clean :patches:clean :patches:build -Pflavor=$f && cp patches/build/libs/patches-*[0-9].mpp /tmp/$f.mpp; done
   python3 .github/scripts/check_bundle_flavors.py --cli tools/latest/morphe-desktop-2.1.1-all.jar --all /tmp/all.mpp --phone /tmp/phone.mpp --tv /tmp/tv.mpp
   ```

   The versions both split bundles offer should be only the apps whose phone and TV builds share a version name: Disney+, Plex, Pluto TV, and Starz.
5. Dry-run the publish, which prints what it would do and changes nothing:

   ```bash
   GH_TOKEN=<token> python3 .github/scripts/publish_tv_bundle.py --repo semicoloncolonel/tv-morphe-patches --branch dev --version <current version> --mpp /tmp/tv.mpp --dry-run
   ```

6. Update the documentation in the same change that turns the switch on:
   - `README.md`: point TV users to the TV source.
   - `docs/android-tv.md`: add the steps for adding the TV source. Remove the notes that only matter while the phone and TV targets share a bundle.
   - The release notes: tell TV users to add the TV source, because the phone source no longer lists Android TV versions.
7. Set the repository variable `TV_SOURCE` to `semicoloncolonel/tv-morphe-patches`. The next release on each branch publishes the first TV release there.

To turn the split off again, delete `TV_SOURCE`. The next release builds the combined bundle again.

## Keeping the split working

- A new Android TV app goes in `tvBundleApps` in `patches/build.gradle.kts` and in `docs/android-tv.md`.
- An app whose phone and TV builds share one compatibility entry uses `splitByBundle()` or `byBundle()`, so each split bundle lists only its own targets.
- A shared patch that lists a TV app filters its list with `forBundle()`. A shared patch with no TV app goes in the TV flavor's excludes.
- A patch that applies only to one build goes in a `tv/` folder, or in the TV flavor's excludes.
- CI compiles both split flavors on every push. Run `check_bundle_flavors.py` before a release that changes any of the above.
