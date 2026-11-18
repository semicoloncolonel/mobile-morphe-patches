package semicoloncolonel.mobilestream.patches.yttv.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import semicoloncolonel.mobilestream.patches.yttv.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Change the seek interval.
//
// A double tap, and the on-screen rewind and fast-forward buttons, jump a
// fixed 15 seconds. That amount is one Duration built once, in the static
// initializer of a holder class (10.46.0):
//
//   const-wide/16 v0, 0xf            # 15
//   invoke-static {v0, v1}, Lj$/time/Duration;->ofSeconds(J)Lj$/time/Duration;
//   sput-object v0, L<holder>;->a:Lj$/time/Duration;
//
// A Dagger provider hands that one Duration to the quick-seek controller,
// which builds the "Fast forward 15 seconds" / "Rewind 15 seconds" labels and
// performs the seek, so changing this single constant changes both the labels
// and the jump. The holder is found by its static initializer: the only one
// that stores a Duration.ofSeconds(15) into a static Duration field.
// ─────────────────────────────────────────────────────────────────────────────

private const val DURATION = "Lj\$/time/Duration;"

@Suppress("unused")
val seekIntervalPatch = bytecodePatch(
    name = "Change the seek interval",
    description = "Sets how many seconds a double tap, or the rewind and fast-forward buttons, jumps, " +
        "instead of the fixed 15 seconds.",
    default = false,
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)

    val seconds by stringOption(
        key = "seconds",
        default = "15",
        values = mapOf(
            "5 seconds" to "5",
            "10 seconds" to "10",
            "15 seconds (default)" to "15",
            "20 seconds" to "20",
            "30 seconds" to "30",
            "45 seconds" to "45",
            "60 seconds" to "60",
        ),
        title = "Seek interval",
        description = "How many seconds a double tap, or the on-screen rewind and fast-forward buttons, jumps.",
        required = true,
    ) { it?.toIntOrNull()?.let { value -> value in 5..60 } == true }

    execute {
        val value = seconds!!.toInt()

        // Collect first, mutate after: mutableClassDefBy swaps the class map
        // entry, which must not happen mid-iteration.
        val holders = mutableListOf<String>()
        classDefForEach { classDef ->
            classDef.methods.forEach methods@{ method ->
                if (method.name != "<clinit>") return@methods
                val instructions = method.implementation?.instructions?.toList() ?: return@methods

                val buildsDuration = instructions.any {
                    val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
                    reference != null && reference.definingClass == DURATION && reference.name == "ofSeconds"
                }
                if (!buildsDuration) return@methods

                val storesDuration = instructions.any {
                    it.opcode == Opcode.SPUT_OBJECT &&
                        ((it as ReferenceInstruction).reference as? FieldReference)?.type == DURATION
                }
                if (!storesDuration) return@methods

                val holdsFifteen = instructions.any {
                    it.opcode == Opcode.CONST_WIDE_16 && (it as WideLiteralInstruction).wideLiteral == 15L
                }
                if (holdsFifteen) holders += classDef.type
            }
        }

        if (holders.size != 1) {
            throw PatchException("YouTube TV seek-interval holder not found (matched ${holders.size})")
        }

        val clinit = mutableClassDefBy(holders.single()).methods.single { it.name == "<clinit>" }
        val instructions = clinit.implementation!!.instructions.toList()
        val index = instructions.indexOfFirst {
            it.opcode == Opcode.CONST_WIDE_16 && (it as WideLiteralInstruction).wideLiteral == 15L
        }
        val register = (instructions[index] as OneRegisterInstruction).registerA
        clinit.replaceInstruction(index, "const-wide/16 v$register, $value")
    }
}
