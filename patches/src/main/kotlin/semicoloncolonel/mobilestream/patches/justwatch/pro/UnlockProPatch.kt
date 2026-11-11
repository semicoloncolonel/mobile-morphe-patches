package semicoloncolonel.mobilestream.patches.justwatch.pro

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.justwatch.shared.Constants

private const val EXT = "Lsemicoloncolonel/mobilestream/extension/justwatch/ProEntitlement;"

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock JustWatch Pro",
    description = "Spoofs an active Pro/Plus entitlement so the app unlocks premium filters, lists and layouts. (Ads are handled by a separate patch.)",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        GetActiveFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $EXT->patchActive(Ljava/util/Map;)Ljava/util/Map;
                move-result-object p0
                return-object p0
            """.trimIndent()
        )

        GetAllFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $EXT->patchAll(Ljava/util/Map;)Ljava/util/Map;
                move-result-object p0
                return-object p0
            """.trimIndent()
        )
    }
}
