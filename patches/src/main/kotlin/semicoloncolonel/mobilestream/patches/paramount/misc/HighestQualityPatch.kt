package semicoloncolonel.mobilestream.patches.paramount.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.paramount.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.returnEarly

// Paramount+ 17.3.0. The Avia player's AviaAdaptiveTrackSelection wraps
// ExoPlayer's AdaptiveTrackSelection. Two methods gate quality:
//
// 1. shouldInclude: rejects tracks above the Config's max bitrate, max
//    resolution, max frame rate, or when H.265 is disabled. Returning true
//    removes all of those caps.
// 2. canSelectFormat: inherited from AdaptiveTrackSelection, gates which
//    renditions the bandwidth estimator considers. Returning true lets the
//    player pick the highest rendition regardless of measured bandwidth.

private const val AVIA_TRACK =
    "Lcom/paramount/android/avia/player/player/core/network/AviaAdaptiveTrackSelection;"

internal object ShouldIncludeFingerprint : Fingerprint(
    returnType = "Z",
    custom = { method, classDef ->
        classDef.type == AVIA_TRACK && method.name == "shouldInclude"
    }
)

internal object CanSelectFormatFingerprint : Fingerprint(
    returnType = "Z",
    custom = { method, classDef ->
        classDef.type == AVIA_TRACK && method.name == "canSelectFormat"
    }
)

@Suppress("unused")
val highestQualityPatch = bytecodePatch(
    name = "Force highest quality",
    description = "Removes bitrate, resolution, and codec caps so the player always picks the highest " +
        "available quality. Turn it off if videos buffer.",
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        ShouldIncludeFingerprint.method.returnEarly(
            """
                const/4 v0, 0x1
                return v0
            """,
        )

        CanSelectFormatFingerprint.method.returnEarly(
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
