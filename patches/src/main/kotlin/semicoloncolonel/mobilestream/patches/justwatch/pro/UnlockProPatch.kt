package semicoloncolonel.mobilestream.patches.justwatch.pro

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val EXT = "Lsemicoloncolonel/mobilestream/extension/justwatch/ProEntitlement;"

val unlockProPatch = bytecodePatch(
    name = "Unlock JustWatch Pro",
    description = "Forces JustWatch Pro entitlements.",
) {
    compatibleWith("com.justwatch.justwatch")
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