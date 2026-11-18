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
// Press and hold to speed up.
//
// Three hooks, because the gesture layer and the media layer are separate:
//
//   1. The media class that extends PlaybackControllerCallbacks hands the active
//      PlaybackController to its setter, which stores it in a field. That setter
//      now also passes the controller to the extension, which keeps it.
//   2. The player gesture handler's onLongPress tells the extension to raise the
//      playback rate to the chosen speed (the controller's native setPlaybackRate
//      is called by reflection in the extension).
//   3. The player gesture detector (a GestureDetector subclass) sees every touch
//      event through onTouchEvent, so the extension learns when the finger lifts
//      and returns the rate to normal.
//
// Drag-to-lock is not included yet; this is press, hold, release.
// ─────────────────────────────────────────────────────────────────────────────

private const val PC = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackController;"
private const val PC_CALLBACKS = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackControllerCallbacks;"
private const val GESTURE = "Landroid/view/GestureDetector\$OnGestureListener;"
private const val DOUBLE_TAP = "Landroid/view/GestureDetector\$OnDoubleTapListener;"
private const val SCALE = "Landroid/view/ScaleGestureDetector\$OnScaleGestureListener;"
private const val MOTION = "Landroid/view/MotionEvent;"
private const val GESTURE_DETECTOR = "Landroid/view/GestureDetector;"
private const val EXT = "Lsemicoloncolonel/mobilestream/extension/yttv/YttvPlaybackSpeed;"

@Suppress("unused")
val pressAndHoldSpeedPatch = bytecodePatch(
    name = "Press and hold to speed up",
    description = "Hold your finger on the player to speed up playback, and let go to return to normal speed. " +
        "Choose the hold speed in the patch options.",
    default = false,
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)
    extendWith("extensions/extension.mpe")

    val speed by stringOption(
        key = "speed",
        default = "2",
        values = mapOf(
            "1.25x" to "1.25",
            "1.5x" to "1.5",
            "2x" to "2",
            "2.5x" to "2.5",
            "3x" to "3",
        ),
        title = "Hold speed",
        description = "The playback speed while you hold your finger on the player.",
        required = true,
    ) { it?.toFloatOrNull()?.let { v -> v in 1.1f..4f } == true }

    execute {
        val rateBits = java.lang.Float.floatToRawIntBits(speed!!.toFloat())
        val rateHex = "0x" + (rateBits.toLong() and 0xffffffffL).toString(16)

        // 1. Capture the active playback controller. Hook the setter(s) on the
        // callbacks class: the method whose first parameter is the controller and
        // that stores a controller into a field.
        val setters = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.superclass != PC_CALLBACKS) return@classDefForEach
            val hasSetter = classDef.methods.any { method ->
                method.returnType == "V" &&
                    method.parameterTypes.firstOrNull()?.toString() == PC &&
                    method.implementation?.instructions?.any {
                        it.opcode == Opcode.IPUT_OBJECT &&
                            ((it as ReferenceInstruction).reference as? FieldReference)?.type == PC
                    } == true
            }
            if (hasSetter) setters += classDef.type
        }
        if (setters.size != 1) throw PatchException("Playback controller setter not found (${setters.size})")
        mutableClassDefBy(setters.single()).methods
            .filter { method ->
                method.returnType == "V" &&
                    method.parameterTypes.firstOrNull()?.toString() == PC &&
                    method.implementation?.instructions?.any {
                        it.opcode == Opcode.IPUT_OBJECT &&
                            ((it as ReferenceInstruction).reference as? FieldReference)?.type == PC
                    } == true
            }
            .forEach { method ->
                method.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        invoke-static { v0 }, $EXT->setController(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }

        // 2. Start the boost on long press, in the player gesture handler.
        val handlers = mutableListOf<String>()
        classDefForEach { classDef ->
            if (!classDef.interfaces.toSet().containsAll(listOf(GESTURE, DOUBLE_TAP, SCALE))) return@classDefForEach
            if (classDef.methods.any { onLongPressMatch(it.name, it.returnType, it.parameterTypes.map { p -> p.toString() }) }) {
                handlers += classDef.type
            }
        }
        if (handlers.size != 1) throw PatchException("Player gesture handler not found (${handlers.size})")
        mutableClassDefBy(handlers.single()).methods
            .single { onLongPressMatch(it.name, it.returnType, it.parameterTypes.map { p -> p.toString() }) }
            .addInstructions(
                0,
                """
                    const v0, $rateHex
                    invoke-static { v0 }, $EXT->onLongPress(F)V
                """.trimIndent(),
            )

        // 3. Restore on touch release, in the gesture detector.
        val detectors = mutableListOf<String>()
        classDefForEach { classDef ->
            if (classDef.superclass != GESTURE_DETECTOR) return@classDefForEach
            if (classDef.methods.any { onTouchMatch(it.name, it.returnType, it.parameterTypes.map { p -> p.toString() }) }) {
                detectors += classDef.type
            }
        }
        if (detectors.size != 1) throw PatchException("Player gesture detector not found (${detectors.size})")
        mutableClassDefBy(detectors.single()).methods
            .single { onTouchMatch(it.name, it.returnType, it.parameterTypes.map { p -> p.toString() }) }
            .addInstructions(
                0,
                """
                    move-object/from16 v0, p1
                    invoke-static { v0 }, $EXT->onTouch(${MOTION})V
                """.trimIndent(),
            )
    }
}

private fun onLongPressMatch(name: String, returnType: String, params: List<String>) =
    name == "onLongPress" && returnType == "V" && params == listOf("Landroid/view/MotionEvent;")

private fun onTouchMatch(name: String, returnType: String, params: List<String>) =
    name == "onTouchEvent" && returnType == "Z" && params == listOf("Landroid/view/MotionEvent;")
