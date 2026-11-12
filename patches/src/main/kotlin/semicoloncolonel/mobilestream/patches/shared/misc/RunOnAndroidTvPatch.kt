package semicoloncolonel.mobilestream.patches.shared.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import semicoloncolonel.mobilestream.patches.disney.shared.Constants as Disney
import semicoloncolonel.mobilestream.patches.espn.shared.Constants as Espn
import semicoloncolonel.mobilestream.patches.hulu.shared.Constants as Hulu
import semicoloncolonel.mobilestream.patches.kanopy.shared.Constants as Kanopy
import semicoloncolonel.mobilestream.patches.mgm.shared.Constants as Mgm
import semicoloncolonel.mobilestream.patches.mlb.shared.Constants as Mlb
import semicoloncolonel.mobilestream.patches.nba.shared.Constants as Nba
import semicoloncolonel.mobilestream.patches.nfl.shared.Constants as Nfl
import semicoloncolonel.mobilestream.patches.paramount.shared.Constants as Paramount
import semicoloncolonel.mobilestream.patches.peacock.shared.Constants as Peacock
import semicoloncolonel.mobilestream.patches.primevideo.shared.Constants as PrimeVideo
import semicoloncolonel.mobilestream.patches.shudder.shared.Constants as Shudder
import semicoloncolonel.mobilestream.patches.starz.shared.Constants as Starz
import semicoloncolonel.mobilestream.patches.sundancenow.shared.Constants as SundanceNow
import semicoloncolonel.mobilestream.patches.xumo.shared.Constants as Xumo

// Android TV filters and launches apps differently from a phone. An app
// reaches the TV home screen and its "all apps" row only through a launcher
// activity whose intent-filter carries the LEANBACK_LAUNCHER category, and
// the system hides an app whose manifest requires a touchscreen, since a TV
// has none. These streaming apps ship only a phone build, so none of that is
// declared. This patch adds it to the phone build's manifest:
//
//   - android.software.leanback as an optional feature, so the app is marked
//     as TV-aware without demanding leanback hardware a tablet lacks.
//   - android.hardware.touchscreen as not required, so a TV, which reports no
//     touchscreen, is no longer filtered out.
//   - the LEANBACK_LAUNCHER category on every launcher intent-filter, so the
//     app appears on the TV home screen next to the native TV apps.
//   - a banner for the launcher entry, reusing the app's own icon, since the
//     TV launcher shows nothing for an app that declares no banner.
//
// It changes the manifest only. The app's own screens are still drawn for a
// phone, so a remote may not reach every control, and some parts may still
// expect a touchscreen. It is off by default for that reason.

private const val LEANBACK_FEATURE = "android.software.leanback"
private const val TOUCHSCREEN_FEATURE = "android.hardware.touchscreen"
private const val LAUNCHER = "android.intent.category.LAUNCHER"
private const val LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER"

private fun Element.childElements(tag: String): List<Element> {
    val nodes = getElementsByTagName(tag)
    return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
}

private fun Element.hasCategory(name: String) =
    childElements("category").any { it.getAttribute("android:name") == name }

@Suppress("unused")
val runOnAndroidTvPatch = resourcePatch(
    name = "Run on Android TV",
    description = "Lets the phone app install and open on an Android TV device and adds it to the " +
        "TV home screen. The app is not redesigned for a remote, so parts of it may still expect a touchscreen.",
    default = false,
) {
    category("Setup")

    compatibleWith(
        Starz.COMPATIBILITY,
        Disney.COMPATIBILITY,
        Peacock.COMPATIBILITY,
        Paramount.COMPATIBILITY,
        Mgm.COMPATIBILITY,
        Shudder.COMPATIBILITY,
        SundanceNow.COMPATIBILITY,
        Xumo.COMPATIBILITY,
        Kanopy.COMPATIBILITY,
        Hulu.COMPATIBILITY,
        PrimeVideo.COMPATIBILITY,
        Espn.COMPATIBILITY,
        Mlb.COMPATIBILITY,
        Nba.COMPATIBILITY,
        Nfl.COMPATIBILITY,
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement

            // Declare the TV feature flags as optional. Set required=false on
            // any that already exist, so an app that demands a touchscreen no
            // longer filters a TV out.
            val features = manifest.childElements("uses-feature")
                .associateBy { it.getAttribute("android:name") }
            for (name in listOf(LEANBACK_FEATURE, TOUCHSCREEN_FEATURE)) {
                val feature = features[name] ?: document.createElement("uses-feature").also {
                    it.setAttribute("android:name", name)
                    manifest.appendChild(it)
                }
                feature.setAttribute("android:required", "false")
            }

            val application = document.getElementsByTagName("application").item(0) as Element

            // Add the leanback launcher category to every phone launcher
            // intent-filter, so the app reaches the TV home screen.
            var launchers = 0
            for (filter in application.childElements("intent-filter")) {
                if (!filter.hasCategory(LAUNCHER) || filter.hasCategory(LEANBACK_LAUNCHER)) continue
                filter.appendChild(
                    document.createElement("category").also {
                        it.setAttribute("android:name", LEANBACK_LAUNCHER)
                    },
                )
                launchers++
            }
            if (launchers == 0 && application.childElements("intent-filter").none { it.hasCategory(LEANBACK_LAUNCHER) }) {
                throw PatchException("No launcher intent-filter found to make leanback")
            }

            // The TV launcher shows nothing for an app with no banner, so give
            // it one. The app's icon always exists and is the safe reuse.
            if (application.getAttribute("android:banner").isEmpty()) {
                val icon = application.getAttribute("android:icon")
                if (icon.isNotEmpty()) application.setAttribute("android:banner", icon)
            }
        }
    }
}
