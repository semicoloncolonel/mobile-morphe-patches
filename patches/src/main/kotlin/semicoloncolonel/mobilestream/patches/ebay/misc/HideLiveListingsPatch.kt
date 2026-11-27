package semicoloncolonel.mobilestream.patches.ebay.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.ebay.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.returnEarly

// ─────────────────────────────────────────────────────────────────────────────
// Hide live listings (experimental).
//
// eBay 6.244.0.1 surfaces eBay Live, the shopping-stream feature, as two
// carousels of live-event listings, each a self-contained Compose composable
// that draws its own heading, tiles, and auto-playing video previews:
//
//   • Browse and home. The recommendations framework maps a placement to a
//     UiTreatment and calls its Content(). LiveEventCarouselTreatment is the
//     treatment for the live-event placement; its Content() draws the carousel.
//     The framework already renders nothing when a placement has no treatment,
//     so an empty Content() leaves no gap.
//
//   • Search results. The results list is built from ComposableComponent view
//     models. LiveCarouselComposableComponent is the live carousel's component;
//     its ComposeContent() draws the carousel cell.
//
// Both composables return early, so neither carousel is drawn and no live
// video preview is started. The patch hides these two entry points only; it
// does not disable the Live Hub activities or deep links that open a stream
// directly. On by default.
//
// Experimental: the carousels have not been confirmed hidden on a device, and
// whether a live event is currently running affects whether they appear at all.
// ─────────────────────────────────────────────────────────────────────────────

// Recommendations live-event carousel. Content() draws the carousel for the
// LiveEventCarouselTreatment placement.
internal object LiveEventCarouselTreatmentContentFingerprint : Fingerprint(
    definingClass =
        "Lcom/ebay/mobile/businesscomponent/recommendations/impl/composables/selectors/treatments/" +
            "LiveEventCarouselTreatment;",
    name = "Content",
    returnType = "V",
)

// Search-results live carousel. ComposeContent() draws the carousel cell.
internal object LiveCarouselComposableComponentFingerprint : Fingerprint(
    definingClass = "Lcom/ebay/mobile/search/result/live/view/LiveCarouselComposableComponent;",
    name = "ComposeContent",
    returnType = "V",
)

@Suppress("unused")
val hideLiveListingsPatch = bytecodePatch(
    name = "Hide live listings",
    description = "Removes the eBay Live carousels of live-stream listings from browse and search results.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        LiveEventCarouselTreatmentContentFingerprint.method.returnEarly("return-void")
        LiveCarouselComposableComponentFingerprint.method.returnEarly("return-void")
    }
}
