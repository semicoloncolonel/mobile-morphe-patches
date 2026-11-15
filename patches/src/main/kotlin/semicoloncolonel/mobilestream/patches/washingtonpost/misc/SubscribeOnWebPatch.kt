package semicoloncolonel.mobilestream.patches.washingtonpost.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.washingtonpost.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Subscribe on the web. WaPo 7.19.0.
//
// Google Play bills only the copy of an app it installed, so the paywall's
// Subscribe button in a patched app ends in "This version of the application
// is not configured for billing through Google Play". The paywall still shows,
// because for a reader who is not a subscriber it is what covers the article;
// only its purchase buttons change.
//
// The Play billing class (the one subclass of paywall.billing.b) starts every
// purchase from a method that takes the Activity and reports through a
// paywall.billing.c$i callback: the new subscription (k), add-ons (d, f) and
// plan changes (j). Each now opens the Post's subscription page in the
// browser and reports the purchase as cancelled, so the paywall resets.
// Checking an existing subscription and restoring purchases are not changed.
// ─────────────────────────────────────────────────────────────────────────────

private const val BILLING = "Lcom/washingtonpost/android/paywall/billing/"
private const val BILLING_BASE = "${BILLING}b;"
private const val CALLBACK = "${BILLING}c\$i;"
private const val WEB_SUBSCRIBE = "Lsemicoloncolonel/mobilestream/extension/washingtonpost/WebSubscribe;"

@Suppress("unused")
val subscribeOnWebPatch = bytecodePatch(
    name = "Subscribe on the web",
    description = "Makes the paywall's Subscribe button open the Post's subscription page in your browser, since a patched app cannot buy through Google Play.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        var billingType: String? = null
        classDefForEach { if (billingType == null && it.superclass == BILLING_BASE) billingType = it.type }
        val billing = billingType ?: throw PatchException("Play billing class not found")
        val purchases = mutableClassDefBy(billing).methods.filter { method ->
            val parameters = method.parameterTypes.map { it.toString() }
            method.returnType == "V" && method.implementation != null &&
                parameters.firstOrNull() == "Landroid/app/Activity;" && parameters.lastOrNull() == CALLBACK
        }
        if (purchases.isEmpty()) throw PatchException("No purchase methods found in $billing")

        // WebSubscribe opens the page and reports the purchase as cancelled to the
        // callback. invoke-static reaches only v0-v15, so parameters numbered
        // higher are first copied to v0 and v1.
        purchases.forEach { method ->
            val registers = method.implementation!!.registerCount
            val parameters = method.parameterTypes.size // no wide types in these methods
            val firstParameter = registers - parameters - 1 // p0 is this
            val callbackRegister = firstParameter + parameters
            val call = "$WEB_SUBSCRIBE->open(Landroid/app/Activity;Ljava/lang/Object;)V"
            val smali = if (callbackRegister <= 15) {
                "invoke-static {p1, p$parameters}, $call\nreturn-void"
            } else {
                if (firstParameter < 2) throw PatchException("${method.name} has no room to call WebSubscribe")
                "move-object/from16 v0, p1\nmove-object/from16 v1, p$parameters\n" +
                    "invoke-static {v0, v1}, $call\nreturn-void"
            }
            method.addInstructions(0, smali)
        }
    }
}
