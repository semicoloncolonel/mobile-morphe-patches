package semicoloncolonel.mobilestream.patches.yttv.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.yttv.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// Volume and brightness sliders.
//
// One class is the player's touch gesture handler: it implements the gesture,
// double-tap, and scale listeners, and it holds the player's node view, which
// is itself an android.view.View (the app casts the field to View to call
// performHapticFeedback). Its onScroll now first offers the drag to
// YttvScreenGestures, passing that view. A vertical drag on the right half
// changes the media volume, and on the left half the screen brightness, set on
// the app's own window only. When the extension handles the drag, onScroll
// returns true so the player does not also act on it. Horizontal drags fall
// through to the player's own gestures.
//
// Because the hook is on the player's gesture handler, it only runs for touches
// on the player, not on the browse or guide lists.
// ─────────────────────────────────────────────────────────────────────────────

private const val GESTURE = "Landroid/view/GestureDetector\$OnGestureListener;"
private const val DOUBLE_TAP = "Landroid/view/GestureDetector\$OnDoubleTapListener;"
private const val SCALE = "Landroid/view/ScaleGestureDetector\$OnScaleGestureListener;"
private const val NODE_VIEW = "Lcom/google/android/libraries/multiplatform/elements/NodeViewInterface;"
private const val MOTION_EVENT = "Landroid/view/MotionEvent;"
private const val EXTENSION = "Lsemicoloncolonel/mobilestream/extension/yttv/YttvScreenGestures;"

@Suppress("unused")
val screenGesturesPatch = bytecodePatch(
    name = "Volume and brightness sliders",
    description = "Lets you slide up and down on the player to change the volume on the right side and the " +
        "screen brightness on the left. All the way down is mute for volume and automatic for brightness. " +
        "The brightness change applies only inside YouTube TV.",
    default = false,
) {
    category("Playback")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        val scrollParams = listOf(MOTION_EVENT, MOTION_EVENT, "F", "F")

        // The gesture handler: implements all three listeners, has onScroll, and
        // holds the player node view.
        val targets = mutableListOf<Pair<String, String>>() // class type to node-view field name
        classDefForEach { classDef ->
            val interfaces = classDef.interfaces.toSet()
            if (!interfaces.containsAll(listOf(GESTURE, DOUBLE_TAP, SCALE))) return@classDefForEach
            val hasOnScroll = classDef.methods.any {
                it.name == "onScroll" && it.returnType == "Z" &&
                    it.parameterTypes.map { p -> p.toString() } == scrollParams
            }
            if (!hasOnScroll) return@classDefForEach
            val field = classDef.fields.firstOrNull { it.type == NODE_VIEW } ?: return@classDefForEach
            targets += classDef.type to field.name
        }

        if (targets.size != 1) {
            throw PatchException("YouTube TV player gesture handler not found (matched ${targets.size})")
        }
        val (classType, fieldName) = targets.single()

        val onScroll = mutableClassDefBy(classType).methods.single {
            it.name == "onScroll" && it.returnType == "Z" &&
                it.parameterTypes.map { p -> p.toString() } == scrollParams
        }

        // p0 = this, p1 = down event, p2 = current event, p3 = distanceX, p4 = distanceY.
        // onScroll has many locals, so the parameters sit in high registers. Move
        // them into low contiguous registers and use invoke-static/range, which
        // addresses any register. v0..v4 are reused by the original code right
        // after, so clobbering them here is safe on fall-through.
        onScroll.addInstructions(
            0,
            """
                move-object/from16 v0, p0
                iget-object v0, v0, $classType->$fieldName:$NODE_VIEW
                check-cast v0, Landroid/view/View;
                move-object/from16 v1, p1
                move-object/from16 v2, p2
                move/from16 v3, p3
                move/from16 v4, p4
                invoke-static/range { v0 .. v4 }, $EXTENSION->onScroll(Landroid/view/View;$MOTION_EVENT${MOTION_EVENT}FF)Z
                move-result v0
                if-eqz v0, :not_handled
                const/4 v0, 0x1
                return v0
                :not_handled
                nop
            """.trimIndent(),
        )
    }
}
