package semicoloncolonel.mobilestream.patches.yahoofinance.misc

import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.yahoofinance.shared.Constants
import semicoloncolonel.mobilestream.patches.yahoofinance.shared.offFlags

// Yahoo Finance 26.22.2 — Hide Plus upsells. The flags behind the Yahoo
// Finance Plus (AlphaSpace) banners and sheets, Stock Picks, the sidebar,
// Smart Asset and Portfolio Analytics upsells, the Insights Edge card, and the
// Coinbase "Trade Now" referral button read as off. Paid features stay locked.
// The list is in FinanceFlags.UPSELLS.

@Suppress("unused")
val hidePlusUpsellsPatch = bytecodePatch(
    name = "Hide Plus upsells",
    description = "Removes the Yahoo Finance Plus banners, sheets, and cards, and the Coinbase referral button. Locked features still show the paywall when tapped.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        offFlags("upsellOff")
    }
}
