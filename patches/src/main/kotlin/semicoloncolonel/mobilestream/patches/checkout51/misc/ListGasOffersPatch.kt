package semicoloncolonel.mobilestream.patches.checkout51.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.checkout51.shared.Constants

private const val GAS_OFFER_LIST =
    "Lsemicoloncolonel/mobilestream/extension/checkout51/GasOfferList;"

// ─────────────────────────────────────────────────────────────────────────────
// List gas offers.
//
// Checkout 51's Google Maps key accepts only the app's own signature, so the
// gas map is blank in every patched build ("Authorization failure" in the
// log). The offers still load: the map fragment's markers observer receives
// them before it draws the pins. GasOfferList runs first in that observer and
// lists the offers over the map, nearest first, with price and cash back.
// Tapping one opens the app's own details bar, as tapping a pin does.
// ─────────────────────────────────────────────────────────────────────────────

internal object GupMarkersObserverFingerprint : Fingerprint(
    definingClass = "Lcom/c51/feature/gup/ui/GupMapFragment;",
    name = "onViewCreated\$lambda\$2",
    parameters = listOf("Lcom/c51/feature/gup/ui/GupMapFragment;", "Lcom/c51/core/data/Resource;"),
)

@Suppress("unused")
val listGasOffersPatch = bytecodePatch(
    name = "List gas offers",
    description = "Lists the gas offers over the gas map, which stays blank in a patched app because " +
        "Google Maps rejects its key. Tap an offer to see its details.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        GupMarkersObserverFingerprint.method.addInstructions(
            0,
            "invoke-static { p0, p1 }, $GAS_OFFER_LIST->onMarkers(Ljava/lang/Object;Ljava/lang/Object;)V",
        )
    }
}
