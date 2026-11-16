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
 * Picks the compatibility entry for this bundle. splitByBundle() covers the
 * usual case.
 */
fun byBundle(all: Compatibility, phone: Compatibility, tv: Compatibility): Compatibility = when {
    isTvBundle -> tv
    isPhoneBundle -> phone
    else -> all
}

private const val PHONE_BUILD = "Phone build"
private const val TV_BUILD = "Android TV build"

/**
 * Splits an entry from the combined bundle into a phone entry and an Android TV
 * entry, for an app whose phone and Android TV builds share a package. The
 * combined bundle keeps this entry as is.
 *
 * By default a target described as "Android TV build" goes to the TV entry
 * only, one described as "Phone build" to the phone entry only, and any other
 * target to both, as when the builds share a version name. Keep the targets in
 * the Compatibility(...) literal: the issue triage and the update bot read and
 * edit them there.
 */
fun Compatibility.splitByBundle(
    phoneTargets: List<AppTarget> = targets.filter { it.description != TV_BUILD },
    tvTargets: List<AppTarget> = targets.filter { it.description != PHONE_BUILD },
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
    tv = asTvEntry(tvTargets, tvDescription),
)

private const val TV_SUFFIX = " (Android TV)"

/**
 * [name] without " (Android TV)" in the TV bundle, where every app is an
 * Android TV app. The phone and combined bundles keep the suffix, because there
 * the phone and Android TV patches for one package sit side by side.
 */
fun tvName(name: String): String = if (isTvBundle) name.removeSuffix(TV_SUFFIX) else name

/**
 * For an entry only an Android TV app uses: in the TV bundle, the same entry
 * without " (Android TV)" in its name or the "Android TV build" label on its
 * versions, and with [tvDescription]. Other bundles keep this entry as is.
 */
fun Compatibility.forTvSource(tvDescription: String? = description): Compatibility =
    if (isTvBundle) asTvEntry(targets, tvDescription) else this

private fun Compatibility.asTvEntry(tvTargets: List<AppTarget>, tvDescription: String?) = Compatibility(
    name = tvName(checkNotNull(name)),
    packageName = checkNotNull(packageName),
    description = tvDescription,
    apkFileType = apkFileType,
    appIconColor = appIconColor,
    signatures = signatures,
    targets = tvTargets.map { target ->
        if (target.description != TV_BUILD) {
            target
        } else {
            AppTarget(
                version = target.version,
                versionCodes = target.versionCodes,
                isExperimental = target.isExperimental,
                minSdk = target.minSdk,
                description = null,
            )
        }
    },
    formFactor = FormFactor.TV,
)

/** [text] without " in the Android TV app" in the TV bundle, where every app is an Android TV app. */
fun tvText(text: String): String = if (isTvBundle) text.replace(" in the Android TV app", "") else text
