package semicoloncolonel.mobilestream.patches.nytimes.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import semicoloncolonel.mobilestream.patches.nytimes.shared.Constants

// NYT phone 11.99.0. Every native ad slot (section fronts, the article feed,
// slideshows; banners and native ads alike) asks the ad repository for its ad
// (AdRepositoryImpl, renamed to a one-letter class in 11.99.0). Its fetch
// method first reads the app's "Google Ad Manager enabled" config flag. When
// that flag is off, the app writes its own EmptyAd state for the slot, which
// the slot shows as nothing, and returns without requesting an ad.
//
// This patch sets that flag to off right after it is read, so the app takes
// its own no-ads path for every slot. The method is found by the consent
// preferences it sets before an ad request, not by its obfuscated name.
internal object AdRepositoryFetchFingerprint : Fingerprint(
    strings = listOf("gad_has_consent_for_cookies", "gad_rdp"),
    custom = { method, _ ->
        method.parameterTypes.size == 3 &&
            method.parameterTypes.first().toString() == "Ljava/lang/String;"
    },
)

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Removes the ads from the app's sections, feeds, and slideshows.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        AdRepositoryFetchFingerprint.method.apply {
            val instructions = implementation!!.instructions.toList()
            val flagRead = instructions.indexOfFirst { it.opcode == Opcode.IGET_BOOLEAN }
            if (flagRead < 0) throw PatchException("Ad Manager flag read not found")
            val register = (instructions[flagRead] as OneRegisterInstruction).registerA
            addInstruction(flagRead + 1, "const/16 v$register, 0x0")
        }
    }
}
