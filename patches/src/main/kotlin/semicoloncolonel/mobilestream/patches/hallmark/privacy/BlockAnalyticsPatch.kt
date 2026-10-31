package semicoloncolonel.mobilestream.patches.hallmark.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.hallmark.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.privacy.analyticsKillSwitchesPatch
import semicoloncolonel.mobilestream.patches.shared.privacy.blockTrackers

// ─────────────────────────────────────────────────────────────────────────────
// Block analytics & tracking (OPT-IN, default OFF).
//
// Third-party SDKs in the 8.108.2 phone build: Kochava, Firebase Analytics,
// Datazoom and Akamai Media Analytics. Hallmark+ renames OkHttp, so only the
// shared URLConnection layer applies (see BlockTrackers.kt). Kochava is also
// stopped at its Accedo plugin, so it never starts, whichever host it picks.
//
// Not touched: sign-in, purchases, consent, age verification, playback, DRM,
// or Marigold push notifications.
// ─────────────────────────────────────────────────────────────────────────────

internal object KochavaPluginStartFingerprint : Fingerprint(
    definingClass = "Ltv/accedo/one/analytics/kochava/KochavaAnalyticsPlugin;",
    name = "start",
    parameters = listOf(),
    returnType = "V",
)

@Suppress("unused")
val blockAnalyticsPatch = bytecodePatch(
    name = "Block analytics & tracking",
    description = "Stops analytics and tracking in the Hallmark+ phone app: Kochava, Datazoom " +
        "and Firebase. Blocking happens on the device, inside the app. Sign-in and playback " +
        "are left alone. Opt-in / experimental; test on-device.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(analyticsKillSwitchesPatch)

    extendWith("extensions/extension.mpe")

    execute {
        KochavaPluginStartFingerprint.methodOrNull?.addInstructions(0, "return-void")

        blockTrackers()
    }
}
