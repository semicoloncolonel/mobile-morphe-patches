package semicoloncolonel.mobilestream.patches.ebay.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.ebay.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.returnEarly
import semicoloncolonel.mobilestream.patches.shared.misc.returnUnitSmali

// ─────────────────────────────────────────────────────────────────────────────
// Hide live listings (experimental).
//
// eBay 6.244.0.1 surfaces eBay Live, the shopping-stream feature, as three
// carousels of live-event listings, each a self-contained Compose composable
// that draws its own heading, tiles, and auto-playing video previews:
//
//   • Recommendations rows. The recommendations framework maps a placement to a
//     UiTreatment and calls its Content(). LiveEventCarouselTreatment is the
//     treatment for the live-event placement; its Content() draws the carousel.
//     The framework already renders nothing when a placement has no treatment,
//     so an empty Content() leaves no gap.
//
//   • Search results. The results list is built from ComposableComponent view
//     models. LiveCarouselComposableComponent is the live carousel's component;
//     its ComposeContent() draws the carousel cell.
//
//   • The Home tab. The home feed is a separate module framework, not the
//     recommendations one. Its live shelf is the discovery live-events module,
//     drawn by one Compose content lambda in LiveEventsCarouselUiComposerProvider
//     that reads the module's LiveEventsCarouselModuleUiState and calls
//     VideoCarouselCoordinatorAware, which starts the auto-playing preview. Both
//     the generic home live module and the personalized one render through this
//     one composer, so emptying it hides both. It is matched by the module state
//     it captures, not by the synthetic lambda name, so it survives a recompile.
//
// Each composable returns early, so no carousel is drawn and no live video
// preview is started. The patch hides these entry points only; it does not
// disable the Live Hub activities or the links that open a stream directly. On
// by default.
//
// Experimental: the carousels have not been confirmed hidden on a device, and
// whether a live event is currently running affects whether they appear at all.
// ─────────────────────────────────────────────────────────────────────────────

private const val LIVE_EVENTS_MODULE_UI_STATE =
    "Lcom/ebay/mobile/discovery/moduledelegates/live/liveevents/LiveEventsCarouselModuleUiState;"

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

// Home-tab live-events carousel. The Compose content lambda ( (scope, Composer,
// flags) -> Unit ) in the live-events module's composer. Pinned by the module
// state it captures and the composer package it lives in, not its lambda name.
internal object HomeLiveEventsCarouselContentFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;", "Ljava/lang/Object;"),
    custom = { method, classDef ->
        method.name == "invoke" &&
            classDef.type.startsWith("Lcom/ebay/mobile/discovery/moduledelegates/live/liveevents/") &&
            classDef.interfaces.contains("Lkotlin/jvm/functions/Function3;") &&
            classDef.methods.any { constructor ->
                constructor.name == "<init>" &&
                    constructor.parameterTypes.any { it.toString() == LIVE_EVENTS_MODULE_UI_STATE }
            }
    },
)

@Suppress("unused")
val hideLiveListingsPatch = bytecodePatch(
    name = "Hide live listings",
    description = "Removes the eBay Live carousels of live-stream listings from the home tab, " +
        "recommendations rows, and search results.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        LiveEventCarouselTreatmentContentFingerprint.method.returnEarly("return-void")
        LiveCarouselComposableComponentFingerprint.method.returnEarly("return-void")
        // The home composer lambda returns Unit, not void.
        HomeLiveEventsCarouselContentFingerprint.method.returnEarly(returnUnitSmali())
    }
}
