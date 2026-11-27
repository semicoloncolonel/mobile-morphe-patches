package semicoloncolonel.mobilestream.patches.dealseek.misc

import semicoloncolonel.mobilestream.patches.shared.misc.isOptionOn
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import semicoloncolonel.mobilestream.patches.shared.misc.disableExpoUpdatesPatch
import semicoloncolonel.mobilestream.patches.shared.misc.HERMES_BUNDLE
import semicoloncolonel.mobilestream.patches.shared.misc.HermesBundle

// ─────────────────────────────────────────────────────────────────────────────
// DealSeek 1.64.0 — Hide pop-ups and the text-alert ask.
//
// DealSeek is an Expo / React Native app, and its screens are Hermes bytecode
// (version 98) in assets/index.android.bundle. The "popUps" option removes five
// of them there:
//
//   TrustPilotModal         asks for a review on Trustpilot.
//   AppStoreReviewModal     asks for a review in the Play Store.
//   shouldShowSpinPopup     decides whether the daily "spin the wheel" pop-up
//                           opens on its own; it now always answers no. The
//                           wheel itself is left in place.
//   present (the one that   opens the deal feedback survey when you come back
//   logs "deal_feedback_    from a store after opening a deal. It now does
//   modal_shown")           nothing.
//   PromotionalPopupSheet   the promotional offer / promo-code pop-up that
//                           opens on its own. It now renders nothing.
//
// The three modals render nothing, and the other two return at once.
//
// The "textAlerts" option removes the marketing ask:
//
//   SmsPlacementCard        the in-feed card that asks for your phone number to
//                           get deal alerts by text. It now renders nothing, so
//                           the ask never appears; the phone-number form behind
//                           it (SmsPhoneNumberScreen) is left in place but is no
//                           longer reachable from the feed.
//
// Expo updates are turned off so that the patched bundle stays in use
// (DisableExpoUpdatesPatch).
// ─────────────────────────────────────────────────────────────────────────────

private val hidePopupsInBundlePatch = rawResourcePatch {
    execute {
        val popUps = isOptionOn(hidePromptsAndPopupsPatch, "popUps")
        val textAlerts = isOptionOn(hidePromptsAndPopupsPatch, "textAlerts")
        if (!popUps && !textAlerts) return@execute
        val file = get(HERMES_BUNDLE)
        val bundle = HermesBundle(file.readBytes())
        if (popUps) {
            bundle.returnAtStart("TrustPilotModal", HermesBundle.Value.NULL)
            bundle.returnAtStart("AppStoreReviewModal", HermesBundle.Value.NULL)
            bundle.returnAtStart("shouldShowSpinPopup", HermesBundle.Value.FALSE)
            bundle.returnAtStart("present", "deal_feedback_modal_shown", HermesBundle.Value.UNDEFINED)
            bundle.returnAtStart("PromotionalPopupSheet", HermesBundle.Value.NULL)
        }
        if (textAlerts) {
            bundle.returnAtStart("SmsPlacementCard", HermesBundle.Value.NULL)
        }
        file.writeBytes(bundle.toByteArray())
    }
}

// Part of hidePromptsAndPopupsPatch: runs only when its "popUps" or
// "textAlerts" option is on.
internal val hidePopupsPatch = resourcePatch {
    dependsOn(hidePopupsInBundlePatch, disableExpoUpdatesPatch)
}
