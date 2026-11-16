package semicoloncolonel.mobilestream.patches.shared.misc

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor

// Which apps this bundle is for. Gradle sets BUNDLE_FLAVOR from -Pflavor:
//
//   all   (default) the phone and Android TV patches in one bundle, as released today.
//   phone           phone apps only. No Android TV target is listed, so Morphe
//                   Manager never suggests a TV version to a phone.
//   tv              Android TV apps only, for the planned tv-morphe-patches source.
//
// Morphe Manager keeps one recommended version per package and does not look at
// the form factor, so when a phone app and its Android TV app share a package
// (Apple TV, NewsNation, Tubi, JustWatch) a combined bundle suggests the TV
// version to phones. Splitting the bundles is the fix.

/** True in the phone-only bundle. */
val isPhoneBundle: Boolean get() = BUNDLE_FLAVOR == "phone"

/** True in the Android TV-only bundle. */
val isTvBundle: Boolean get() = BUNDLE_FLAVOR == "tv"

/** True when [compatibility] belongs in this bundle. */
fun inBundle(compatibility: Compatibility): Boolean = when {
    isTvBundle -> compatibility.formFactor == FormFactor.TV
    isPhoneBundle -> compatibility.formFactor != FormFactor.TV
    else -> true
}

/**
 * Keeps the entries that belong in this bundle, for a patch that lists both
 * phone and Android TV apps. Use it as compatibleWith(*forBundle(...)).
 *
 * Loading the bundle fails if nothing is left, because a patch without
 * compatibility entries is not tied to any app. Leave such a patch out of the
 * flavor's sources in patches/build.gradle.kts instead. Run list-patches on
 * each flavor's .mpp to catch this before release.
 */
fun forBundle(vararg compatibility: Compatibility): Array<Compatibility> =
    compatibility.filter(::inBundle).toTypedArray().also {
        check(it.isNotEmpty()) {
            "No compatibility entries left for the $BUNDLE_FLAVOR bundle. " +
                "Exclude this patch from that flavor in patches/build.gradle.kts."
        }
    }

/**
 * Picks the compatibility entry for an app whose phone and Android TV builds
 * share one entry in the combined bundle (Tubi, JustWatch, Pluto TV).
 */
fun byBundle(all: Compatibility, phone: Compatibility, tv: Compatibility): Compatibility = when {
    isTvBundle -> tv
    isPhoneBundle -> phone
    else -> all
}

/**
 * Splits an entry from the combined bundle into a phone entry and an Android TV
 * entry. Each split entry keeps this entry's name, package, and icon, and takes
 * its own targets and description. The combined bundle keeps this entry as is.
 */
fun Compatibility.splitByBundle(
    phoneTargets: List<AppTarget> = targets,
    tvTargets: List<AppTarget> = targets,
    phoneDescription: String? = description,
    tvDescription: String? = description,
): Compatibility = byBundle(
    all = this,
    phone = Compatibility(
        name = checkNotNull(name),
        packageName = checkNotNull(packageName),
        description = phoneDescription,
        apkFileType = apkFileType,
        appIconColor = appIconColor,
        signatures = signatures,
        targets = phoneTargets,
    ),
    tv = Compatibility(
        name = checkNotNull(name),
        packageName = checkNotNull(packageName),
        description = tvDescription,
        apkFileType = apkFileType,
        appIconColor = appIconColor,
        signatures = signatures,
        targets = tvTargets,
        formFactor = FormFactor.TV,
    ),
)
