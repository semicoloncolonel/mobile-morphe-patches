package semicoloncolonel.mobilestream.patches.checkout51.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.checkout51.shared.Constants
import semicoloncolonel.mobilestream.patches.checkout51.shared.GetListItemsFromOfferModelsFingerprint
import semicoloncolonel.mobilestream.patches.checkout51.shared.SHOPPING_ADS_FILTER

// ─────────────────────────────────────────────────────────────────────────────
// Hide pop-ups.
//
// Checkout 51 9.22.10, in its own unobfuscated code:
//
//   Sign-up sheet  SignUpPromptController.showIfPossible opens "Want to see
//                  more offers?" 30 seconds into a guest's first session. It
//                  now returns at once. Sign-up from the account button is
//                  unchanged.
//   Review card    a "rate the app" card in the offer list, sent as an offer
//                  model with the "_app_review_prompt_" label. That model is
//                  dropped before it becomes a row.
// ─────────────────────────────────────────────────────────────────────────────

internal object ShowSignUpPromptFingerprint : Fingerprint(
    definingClass = "Lcom/c51/feature/offers/offerList/SignUpPromptController;",
    name = "showIfPossible",
    returnType = "V",
)

@Suppress("unused")
val hidePopupsPatch = bytecodePatch(
    name = "Hide pop-ups",
    description = "Removes the sign-up sheet shown to guests and the card that asks you to rate the app.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        ShowSignUpPromptFingerprint.method.addInstructions(0, "return-void")

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
