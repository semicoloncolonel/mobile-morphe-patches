package semicoloncolonel.mobilestream.patches.hallmark.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.hallmark.shared.Constants

// Hallmark+ plays client-side FreeWheel ads through an Akamai player plugin,
// bids for ads through Amazon Publisher Services, and shows Google ads on the
// pause screen. Clearing the two player configs and the pause-screen flag
// leaves each feature on the path the app takes when it is not configured.
// Streams that arrive with ads already stitched in cannot be changed here.
@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Stops FreeWheel ad breaks, Amazon ad bidding, and pause-screen ads in the " +
        "Hallmark+ phone app. Ads stitched into a stream by the server stay. Experimental; " +
        "test on-device.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        listOf(FreewheelConfigFingerprint, AmazonPublisherServicesConfigFingerprint).forEach {
            it.method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
        }
        PauseScreenAdsEnabledFingerprint.method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
    }
}
