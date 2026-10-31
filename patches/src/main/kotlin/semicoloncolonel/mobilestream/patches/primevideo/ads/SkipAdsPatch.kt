package semicoloncolonel.mobilestream.patches.primevideo.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.primevideo.shared.Constants

// Prime Video phone 3.0.477.1447. Unlike the TV app (whose ads live in the
// native libignite engine), the phone app plays through Amazon's AIV player
// with a Java ad layer that keeps its names: com.amazon.avod.media.ads.
//
// PrepareAdPlan, a step of every playback session, asks the AdPlanFactory
// for the title's AdPlan (the breaks and their ad clips). Amazon ships its
// own EmptyAdPlan.INSTANCE, the plan used when a session has no ads. The
// patch has getAdPlan() return that, so no ad break is ever scheduled.
//
// The playback request itself is left alone: VideoSpecification's
// shouldShowAds flag also shapes what the server returns, and asking the
// server for an ad-free stream is not this patch's job.
internal object PrepareAdPlanGetAdPlanFingerprint : Fingerprint(
    returnType = "Lcom/amazon/avod/media/ads/AdPlan;",
    custom = { method, classDef ->
        method.name == "getAdPlan" &&
            classDef.type == "Lcom/amazon/avod/playback/session/workflow/tasks/PrepareAdPlan;"
    },
)

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Gives every Prime Video playback Amazon's own empty ad plan in the phone app, " +
        "so no ad break is scheduled. The stream request is not changed. Experimental; " +
        "test on-device.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // getAdPlan has .locals 12.
        PrepareAdPlanGetAdPlanFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lcom/amazon/avod/media/ads/internal/EmptyAdPlan;->INSTANCE:Lcom/amazon/avod/media/ads/internal/EmptyAdPlan;
                return-object v0
            """.trimIndent(),
        )
    }
}
