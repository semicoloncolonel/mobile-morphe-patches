package semicoloncolonel.mobilestream.patches.checkout51.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import semicoloncolonel.mobilestream.patches.checkout51.shared.Constants
import semicoloncolonel.mobilestream.patches.checkout51.shared.GetListItemsFromOfferModelsFingerprint
import semicoloncolonel.mobilestream.patches.checkout51.shared.SHOPPING_ADS_FILTER
import semicoloncolonel.mobilestream.patches.checkout51.shared.fixStuckDialogPatch

// ─────────────────────────────────────────────────────────────────────────────
// Hide pop-ups.
//
// Checkout 51 9.22.10, in its own unobfuscated code unless noted:
//
//   Sign-up sheet      SignUpPromptController.showIfPossible opens "Want to
//                      see more offers?" 30 seconds into a guest's first
//                      session. It now returns at once.
//   Review card        a "rate the app" card in the offer list, sent as an
//                      offer model labeled "_app_review_prompt_", is dropped.
//   Home dialogs       HomeDialogManager shows at most one of four dialogs a
//                      session. Three are marketing and now never qualify:
//                      the welcome walkthrough after sign-up
//                      (HomeWelcomeDialogUnit), the push notification primer
//                      (HomePlmDialogUnit), and the Instant Offers promotion
//                      (HomeInstantOfferDialogUnit). The country picker
//                      (HomeCountryDialogUnit) still shows, because it sets
//                      which offers you get.
//   Push banner        the "turn on notifications" global banner
//                      (PushGlobalBannerUnit). The offline banner still shows.
//   Braze messages     Braze's default in-app message listener now discards
//                      every marketing message instead of showing it.
// ─────────────────────────────────────────────────────────────────────────────

internal object ShowSignUpPromptFingerprint : Fingerprint(
    definingClass = "Lcom/c51/feature/offers/offerList/SignUpPromptController;",
    name = "showIfPossible",
    returnType = "V",
)

private const val DIALOGS = "Lcom/c51/feature/offers/offerList/dialogs/"

internal object WelcomeDialogShouldShowFingerprint : Fingerprint(
    definingClass = "${DIALOGS}HomeWelcomeDialogUnit;",
    name = "shouldShow",
    returnType = "Z",
)

internal object PushPrimerShouldShowFingerprint : Fingerprint(
    definingClass = "${DIALOGS}HomePlmDialogUnit;",
    name = "shouldShow",
    returnType = "Z",
)

internal object InstantOfferDialogShouldShowFingerprint : Fingerprint(
    definingClass = "${DIALOGS}HomeInstantOfferDialogUnit;",
    name = "shouldShow",
    returnType = "Z",
)

internal object PushBannerReadyFingerprint : Fingerprint(
    definingClass = "Lcom/c51/feature/global/banner/pushNotification/PushGlobalBannerUnit;",
    name = "isReadyToShow",
    returnType = "Z",
)

private const val IN_APP_OPERATION = "Lcom/braze/ui/inappmessage/InAppMessageOperation;"

internal object BrazeBeforeInAppMessageDisplayedFingerprint : Fingerprint(
    definingClass = "Lcom/braze/ui/inappmessage/listeners/DefaultInAppMessageManagerListener;",
    name = "beforeInAppMessageDisplayed",
    returnType = IN_APP_OPERATION,
)

@Suppress("unused")
val hidePopupsPatch = bytecodePatch(
    name = "Hide pop-ups",
    description = "Removes the sign-up sheet, the welcome walkthrough, the notification and Instant Offers " +
        "prompts, marketing messages, and the card that asks you to rate the app.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(fixStuckDialogPatch)

    extendWith("extensions/extension.mpe")

    execute {
        ShowSignUpPromptFingerprint.method.addInstructions(0, "return-void")

        WelcomeDialogShouldShowFingerprint.method.returnEarly(false)
        PushPrimerShouldShowFingerprint.method.returnEarly(false)
        InstantOfferDialogShouldShowFingerprint.method.returnEarly(false)
        PushBannerReadyFingerprint.method.returnEarly(false)

        BrazeBeforeInAppMessageDisplayedFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $IN_APP_OPERATION->DISCARD:$IN_APP_OPERATION
                return-object v0
            """,
        )

        GetListItemsFromOfferModelsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x2
                invoke-static {p1, v0}, $SHOPPING_ADS_FILTER->checkout51Offers(Ljava/util/List;I)Ljava/util/List;
                move-result-object p1
            """,
        )
    }
}
