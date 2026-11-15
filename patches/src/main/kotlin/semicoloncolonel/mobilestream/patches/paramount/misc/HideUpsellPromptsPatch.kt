package semicoloncolonel.mobilestream.patches.paramount.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.paramount.shared.Constants

// Paramount+ 17.3.0. Two un-obfuscated activities show upgrade/upsell prompts:
//
//   UpsellHomeActivity — the main upsell screen, launched from many places in
//   the app with an "upsellRoute" extra.
//
//   PlanPickerActivity — the plan comparison/selection screen.
//
// Finishing each activity immediately in onCreate suppresses both screens and
// returns the user to whatever they were doing.

private const val UPSELL_HOME =
    "Lcom/paramount/android/pplus/upsell/home/mobile/UpsellHomeActivity;"
private const val PLAN_PICKER =
    "Lcom/paramount/android/pplus/upsell/planpicker/mobile/PlanPickerActivity;"

@Suppress("unused")
val hideUpsellPromptsPatch = bytecodePatch(
    name = "Hide upsell prompts",
    description = "Closes upgrade and plan-picker screens as soon as they open.",
) {
    category("Layout")
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        listOf(UPSELL_HOME, PLAN_PICKER).forEach { activityType ->
            val classDef = classDefByOrNull(activityType) ?: return@forEach
            val onCreate = mutableClassDefBy(activityType).methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes.map { type -> type.toString() } == listOf("Landroid/os/Bundle;")
            } ?: return@forEach
            onCreate.addInstructions(
                0,
                """
                    invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }
    }
}
