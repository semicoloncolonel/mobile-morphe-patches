package semicoloncolonel.mobilestream.patches.yttv.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import semicoloncolonel.mobilestream.patches.yttv.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Set playback speed.
//
// YouTube TV can change playback speed on the web but shows no speed control in
// the app. The media class that extends PlaybackControllerCallbacks hands the
// active PlaybackController to its setter when a video starts. That setter now
// also tells the extension to set the chosen speed on the controller, so every
// recording and on-demand video plays at it. Live channels play at normal speed,
// because the player core sets their rate itself.
//
// This is a fixed speed chosen in the options, not an in-player menu. The
// "Press and hold to speed up" patch covers a temporary speed-up by touch.
// ─────────────────────────────────────────────────────────────────────────────

private const val PC = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackController;"
private const val PC_CALLBACKS = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackControllerCallbacks;"
private const val EXT = "Lsemicoloncolonel/mobilestream/extension/yttv/YttvPlaybackSpeed;"

@Suppress("unused")
val setPlaybackSpeedPatch = bytecodePatch(
    name = "Set playback speed",
    description = "Plays recordings and on-demand videos at a speed you choose in the patch options, " +
        "since YouTube TV has no speed control in the app. Live channels play at normal speed.",
    default = false,
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)
    extendWith("extensions/extension.mpe")

    val speed by stringOption(
        key = "speed",
        default = "1.5",
        values = mapOf(
            "0.5x" to "0.5",
            "0.75x" to "0.75",
            "1x (normal)" to "1",
            "1.25x" to "1.25",
            "1.5x" to "1.5",
            "1.75x" to "1.75",
            "2x" to "2",
        ),
        title = "Playback speed",
        description = "The speed recordings and on-demand videos play at.",
        required = true,
    ) { it?.toFloatOrNull()?.let { v -> v in 0.25f..4f } == true }

    execute {
        val rateBits = java.lang.Float.floatToRawIntBits(speed!!.toFloat())
        val rateHex = "0x" + (rateBits.toLong() and 0xffffffffL).toString(16)

        fun isControllerSetter(returnType: String, params: List<String>, instructions: Iterable<com.android.tools.smali.dexlib2.iface.instruction.Instruction>?) =
            returnType == "V" && params.firstOrNull() == PC &&
                instructions?.any {
                    it.opcode == Opcode.IPUT_OBJECT &&
                        ((it as ReferenceInstruction).reference as? FieldReference)?.type == PC
                } == true

        val holders = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.superclass != PC_CALLBACKS) return@classDefForEach
            if (classDef.methods.any {
                    isControllerSetter(it.returnType, it.parameterTypes.map { p -> p.toString() }, it.implementation?.instructions)
                }
            ) {
                holders += classDef.type
            }
        }
        if (holders.size != 1) throw PatchException("Playback controller setter not found (${holders.size})")

        mutableClassDefBy(holders.single()).methods
            .filter { isControllerSetter(it.returnType, it.parameterTypes.map { p -> p.toString() }, it.implementation?.instructions) }
            .forEach { method ->
                method.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const v1, $rateHex
                        invoke-static { v0, v1 }, $EXT->applyDefault(Ljava/lang/Object;F)V
                    """.trimIndent(),
                )
            }
    }
}
