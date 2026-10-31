package semicoloncolonel.mobilestream.patches.samsungtvplus.privacy

import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.samsungtvplus.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.privacy.analyticsKillSwitchesPatch
import semicoloncolonel.mobilestream.patches.shared.privacy.blockTrackers

// ─────────────────────────────────────────────────────────────────────────────
// Block analytics & tracking (OPT-IN, default OFF).
//
// Samsung TV Plus 1.0.30.8 has no Skip ads patch: its video ads are stitched
// into the stream by Samsung's servers, and the player gets no ad-break
// signal to act on. The only banner code belongs to the Samsung ads SDK's
// own news widget, which the app does not use.
//
// Trackers: Braze, Firebase Analytics, and Samsung's own telemetry (DI Atlas,
// ureca log collection, DiagMon). The Samsung SDKs and OkHttp are renamed,
// so this relies on the shared URLConnection layer and host blocklist.
//
// Not touched: Samsung Account sign-in, consent, push, the channel lineup
// and EPG (api-prd.samsungrs.com), playback or DRM.
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("unused")
val blockAnalyticsPatch = bytecodePatch(
    name = "Block analytics & tracking",
    description = "Stops analytics and tracking in the Samsung TV Plus phone app: Braze, " +
        "Firebase, and Samsung's own usage and diagnostics logging. Blocking happens on the " +
        "device, inside the app. Sign-in, channels and playback are left alone. Opt-in / " +
        "experimental; test on-device.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(analyticsKillSwitchesPatch)

    extendWith("extensions/extension.mpe")

    execute { blockTrackers() }
}
