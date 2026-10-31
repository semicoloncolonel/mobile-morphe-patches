package semicoloncolonel.mobilestream.patches.msnow.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.msnow.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.privacy.analyticsKillSwitchesPatch
import semicoloncolonel.mobilestream.patches.shared.privacy.blockTrackers

// ─────────────────────────────────────────────────────────────────────────────
// Block analytics & tracking (OPT-IN, default OFF).
//
// MS NOW 14.6.0 ships Adobe, Nielsen, comScore, Kochava, Amplitude, Parse.ly,
// Iterate surveys and Firebase Analytics. Kochava never starts; the rest are
// covered by the shared URLConnection layer and host blocklist (OkHttp is
// renamed). Amplitude's Experiment feature-flag hosts are left alone.
//
// There is no Skip ads patch: the paid membership is sold as ad-free.
//
// Not touched: sign-in, TV-provider sign-in, the membership, push, comments,
// audio, OneTrust notices, Firebase Remote Config, playback or DRM.
// ─────────────────────────────────────────────────────────────────────────────

internal object KochavaStartFingerprint : Fingerprint(
    definingClass = "Lcom/kochava/measurement/base/Measurement;",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    returnType = "V",
    strings = listOf("Host called API: Start With App GUID "),
)

@Suppress("unused")
val blockAnalyticsPatch = bytecodePatch(
    name = "Block analytics & tracking",
    description = "Stops analytics and tracking in the MS NOW app: Adobe, Kochava, Nielsen, " +
        "comScore, Amplitude, Parse.ly, Iterate and Firebase. Blocking happens on the device, " +
        "inside the app. Sign-in and playback are left alone. Opt-in / experimental; " +
        "test on-device.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(analyticsKillSwitchesPatch)

    extendWith("extensions/extension.mpe")

    execute {
        KochavaStartFingerprint.methodOrNull?.addInstructions(0, "return-void")

        blockTrackers()
    }
}
