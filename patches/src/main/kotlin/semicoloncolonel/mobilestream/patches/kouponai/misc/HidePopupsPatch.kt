package semicoloncolonel.mobilestream.patches.kouponai.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import semicoloncolonel.mobilestream.patches.kouponai.shared.Constants
import semicoloncolonel.mobilestream.patches.kouponai.shared.disableCodePushPatch
import semicoloncolonel.mobilestream.patches.shared.misc.HERMES_BUNDLE
import semicoloncolonel.mobilestream.patches.shared.misc.HermesBundle

// ─────────────────────────────────────────────────────────────────────────────
// Koupon 3.1.6 — Hide pop-ups.
//
// Koupon is a React Native app, and its screens are Hermes bytecode (version
// 96) in assets/index.android.bundle. These functions now render nothing or
// answer no:
//
//   WalkThroughStartDrawer   "Ready for verified codes and deals? Take a quick
//                            tour", which starts the tour after 3 seconds.
//   CopilotModal2            the tour's step-by-step tooltips over the feed.
//   WalkThroughEndDrawer     "Congratulations on receiving 100 points, sign
//                            up to claim".
//   NotificationPrompt,      the "Enable notifications" sheet, and the check
//   shouldPromptNotification that decides when to show it.
//   ColdSurvey, GoogleSurvey, SearchSurvey, RcmdSurveyCard, RcmdSurveyDrawer,
//   shouldShowSurvey         the in-feed and pop-up surveys.
//   Nps                      "How likely are you to recommend Koupon".
//
// Koupon's over-the-air updates are turned off so that the patched bundle
// stays in use (DisableCodePushPatch).
// ─────────────────────────────────────────────────────────────────────────────

private val hidePopupsInBundlePatch = rawResourcePatch {
    execute {
        val file = get(HERMES_BUNDLE)
        val bundle = HermesBundle(file.readBytes())
        listOf(
            "WalkThroughStartDrawer",
            "CopilotModal2",
            "WalkThroughEndDrawer",
            "NotificationPrompt",
            "ColdSurvey",
            "GoogleSurvey",
            "SearchSurvey",
            "RcmdSurveyCard",
            "RcmdSurveyDrawer",
            "Nps",
        ).forEach { bundle.returnAtStart(it, HermesBundle.Value.NULL) }
        bundle.returnAtStart("shouldPromptNotification", HermesBundle.Value.FALSE)
        bundle.returnAtStart("shouldShowSurvey", HermesBundle.Value.FALSE)
        file.writeBytes(bundle.toByteArray())
    }
}

@Suppress("unused")
val hidePopupsPatch = resourcePatch(
    name = "Hide pop-ups",
    description = "Removes the app tour, the sign-up-for-points pop-ups, the notification prompt, and the surveys.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(hidePopupsInBundlePatch, disableCodePushPatch)
}
