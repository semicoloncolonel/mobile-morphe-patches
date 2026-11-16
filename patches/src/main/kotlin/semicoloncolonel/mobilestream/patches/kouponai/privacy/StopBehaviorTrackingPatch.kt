package semicoloncolonel.mobilestream.patches.kouponai.privacy

import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.kouponai.shared.Constants
import semicoloncolonel.mobilestream.patches.kouponai.shared.requireNativeLibsPatch
import semicoloncolonel.mobilestream.patches.kouponai.shared.enableWebFilter
import semicoloncolonel.mobilestream.patches.kouponai.shared.kouponWebFilterPatch

// ─────────────────────────────────────────────────────────────────────────────
// Koupon 3.1.6 — Stop behavioral tracking (OPT-IN, default OFF).
//
// Each deal opens as a web page (cross-end.koupon.ai/product/detail) with the
// account's Firebase UID in its URL. On load, the page posts the deal, its
// categories, and the keywords of its title to
// /api/browse-history/add-history. That record fills Me > History ("Recently
// Viewed") and feeds the recommendations ("You might also like", related
// deals, search suggestions). KouponWebFilter answers that post with an empty
// 404, so the visit is not recorded.
//
// Not touched: the deal page itself, favorites, rewards, and the History
// screen, which keeps what was recorded before. Clear it there if you want it
// gone.
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("unused")
val stopBehaviorTrackingPatch = bytecodePatch(
    name = "Stop behavioral tracking",
    description = "Stops the app from recording the deals you open, which feeds your viewing history and " +
        "personalized recommendations. Does not delete history already saved.",
    default = false,
) {
    category("Privacy")
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(requireNativeLibsPatch)

    dependsOn(kouponWebFilterPatch)

    execute {
        enableWebFilter("stopHistory")
    }
}
