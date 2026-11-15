package semicoloncolonel.mobilestream.patches.paramount.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.paramount.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.returnEarly

// Paramount+ 17.3.0. PiP is gated by two server-side feature flags, one for
// VOD and one for live TV. A single obfuscated gate method checks whether the
// device supports PiP (API 26+ with the system feature) AND whether the flag
// for the requested content type is on.
//
// Returning true unconditionally from the gate method enables PiP for all
// content. The player UI picks up the result and shows the PiP button; the
// activity's onUserLeaveHint path also reads it for auto-PiP on home press.

internal object PipGateFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("android.software.picture_in_picture"),
    custom = { method, _ -> method.parameterTypes.size == 1 }
)

@Suppress("unused")
val enablePipPatch = bytecodePatch(
    name = "Enable picture-in-picture",
    description = "Enables picture-in-picture for on-demand and live content, even when the server " +
        "feature flag is off. The device must still support PiP (Android 8+).",
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        PipGateFingerprint.method.returnEarly(
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
