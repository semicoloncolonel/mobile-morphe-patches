package semicoloncolonel.mobilestream.patches.nytimes.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import semicoloncolonel.mobilestream.patches.nytimes.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Library pass reminder (OPT-IN, default OFF). NYT 12.1.0.
//
// A patron reads the NYT through a California library pass that attaches to
// their NYT account and lasts 72 hours. It must be re-redeemed every three
// days at the library's EZproxy link (for Palo Alto,
// https://pacl.idm.oclc.org/login/NYT). When the pass lapses the app has no
// active entitlement and shows its gateway: the paywall (GatewayType.BLOCKED),
// the meter, or the regiwall. GatewayFragmentCard renders that gateway.
//
// This patch calls the extension from GatewayFragmentCard.onActivityCreated,
// which shows a dialog whose button opens the configured redeem link in a
// browser. The patron then completes the proper, intended redeem flow in one
// tap. Nothing about the gateway changes: it still shows, and the article
// stays locked until the real redeem finishes. This is a convenience prompt,
// not a paywall bypass, and it adds nothing when the URL option is blank.
//
// The extension decides whether to actually show the dialog (URL set, device
// online, not within the cooldown), so the patch only needs the one entry
// hook. Note: NYT also has a newer Jetpack Compose paywall path whose class is
// obfuscated; if a build renders BLOCKED through that path instead of this
// fragment, the reminder will not fire. That is why this ships experimental
// and wants an on-device check.
// ─────────────────────────────────────────────────────────────────────────────

private const val REMINDER =
    "Lsemicoloncolonel/mobilestream/extension/nytimes/misc/LibraryPassReminder;"

private const val DEFAULT_URL = "https://pacl.idm.oclc.org/login/NYT"

// The gateway card fragment keeps a stable class name across builds. Its
// onActivityCreated runs when the gateway (paywall / meter / regiwall) is shown.
internal object GatewayFragmentCardOnActivityCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/nytimes/android/gateway/GatewayFragmentCard;",
    name = "onActivityCreated",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

// The extension getter the patch rewrites with the chosen URL.
internal object RenewUrlFingerprint : Fingerprint(
    definingClass = REMINDER,
    name = "renewUrl",
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
)

@Suppress("unused")
val libraryPassReminderPatch = bytecodePatch(
    name = "Library pass reminder",
    description = "Experimental. When your NYT access is blocked, shows a dialog whose button opens your " +
        "library's redeem link so you can renew your 72-hour pass. Set the link in the Library auth URL option. " +
        "It does not change the paywall.",
    default = false,
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    val libraryUrl by stringOption(
        key = "libraryUrl",
        default = DEFAULT_URL,
        values = mapOf(
            "Palo Alto (NYT)" to DEFAULT_URL,
            "Off (blank)" to "",
        ),
        title = "Library auth URL",
        description = "Your library's NYT redeem link, opened when you tap Renew. The default is the Palo Alto " +
            "City Library link. Leave it blank to turn the reminder off.",
        required = false,
    )

    execute {
        val url = libraryUrl.orEmpty().trim()
        if (url.contains('"') || url.contains('\\') || url.contains('\n')) {
            throw PatchException("Library auth URL cannot contain quotes, backslashes, or line breaks")
        }
        if (url.isNotEmpty() && !url.startsWith("https://") && !url.startsWith("http://")) {
            throw PatchException("Library auth URL must start with https:// or http://")
        }

        // Tell the extension the configured URL.
        RenewUrlFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "$url"
                return-object v0
            """,
        )

        // Hook the gateway so the extension can offer the renew prompt. p0 is
        // the fragment; onActivityCreated uses high registers, so pass it with
        // an /range invoke. The extension reads its activity by reflection.
        GatewayFragmentCardOnActivityCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $REMINDER->onGatewayShown(Ljava/lang/Object;)V",
        )
    }
}
