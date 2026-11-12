package semicoloncolonel.mobilestream.patches.yahoonews.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import semicoloncolonel.mobilestream.patches.shared.misc.HERMES_BUNDLE
import semicoloncolonel.mobilestream.patches.shared.misc.HermesBundle
import semicoloncolonel.mobilestream.patches.shared.misc.disableExpoUpdatesPatch
import semicoloncolonel.mobilestream.patches.yahoonews.shared.Constants
import semicoloncolonel.mobilestream.patches.yahoonews.shared.videoOptOutPatch

// ─────────────────────────────────────────────────────────────────────────────
// Yahoo News 100.1.1 — Show Follow and Local.
//
// Two features in the app are switched off for everyone except Yahoo's own
// testers (readiness "internal-only" in the bundle's remote config):
//
//   isFollowEnabled    following publishers and topics, the Following feed,
//                      and managing what you follow.
//   isLocalTabEnabled  the Local news tab.
//
// Both getters now answer true. Following works with a signed-in account:
// Follow buttons appear on stories and publishers, and a Following feed joins
// the tabs at the top. The Local tab also needs a local TV market, which
// Yahoo assigns from your location, so it shows only once the app has
// location access.
// ─────────────────────────────────────────────────────────────────────────────

private val showHiddenTabsInBundlePatch = rawResourcePatch {
    execute {
        val file = get(HERMES_BUNDLE)
        val bundle = HermesBundle(file.readBytes())
        bundle.returnAtStart("get isFollowEnabled", HermesBundle.Value.TRUE)
        bundle.returnAtStart("get isLocalTabEnabled", HermesBundle.Value.TRUE)
        file.writeBytes(bundle.toByteArray())
    }
}

@Suppress("unused")
val showHiddenTabsPatch = resourcePatch(
    name = "Show Follow and Local",
    description = "Turns on two features Yahoo has not released: following publishers, with a Following feed, and the Local news tab, which needs location access.",
    default = false,
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(showHiddenTabsInBundlePatch, disableExpoUpdatesPatch, videoOptOutPatch)
}
