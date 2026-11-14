package semicoloncolonel.mobilestream.patches.netflix.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import semicoloncolonel.mobilestream.patches.netflix.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.forEachMatchingMethod
import semicoloncolonel.mobilestream.patches.shared.misc.returnEarly

// Removes the Clips (short-video feed) and/or Games tabs from the bottom
// navigation bar.
//
// Each tab is a class that implements BottomTab and holds its identity as a
// BottomTab.Name field (Games -> BottomTab$Name$Games, Clips -> ...$Name$
// VerticalVideoFeed). The nav bar decides whether to show a tab by calling the
// tab's a() method (BottomTab.a()Z, the interface's own visibility gate; the
// nav already hides tabs whose a() is false). Forcing the chosen tab's a() to
// return false drops it from the bar. The tabs are matched by their Name field,
// not the obfuscated class name, so this survives version bumps.

private const val BOTTOM_TAB = "Lcom/netflix/android/widgetry/widget/tabs/BottomTab;"
private const val GAMES = "Lcom/netflix/android/widgetry/widget/tabs/BottomTab\$Name\$Games;"
private const val CLIPS = "Lcom/netflix/android/widgetry/widget/tabs/BottomTab\$Name\$VerticalVideoFeed;"

private fun isTabWithName(nameType: String): (ClassDef) -> Boolean = { classDef ->
    classDef.interfaces.contains(BOTTOM_TAB) && classDef.fields.any { it.type == nameType }
}

// BottomTab.a()Z — the visibility gate. The name is the interface contract, so
// it is kept across obfuscated implementors.
private val isVisibilityGate: (Method) -> Boolean = { method ->
    method.name == "a" &&
        method.returnType == "Z" &&
        method.parameterTypes.isEmpty() &&
        method.implementation != null
}

@Suppress("unused")
val removeNavTabsPatch = bytecodePatch(
    name = "Remove bottom tabs",
    description = "Removes the Clips (short videos) and/or Games tabs from the bottom navigation " +
        "bar. Each can be removed on its own.",
    default = false,
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    val removeClips by booleanOption(
        key = "removeClips",
        default = true,
        title = "Remove Clips tab",
        description = "Removes the Clips (short videos) tab from the bottom bar.",
        required = true,
    )
    val removeGames by booleanOption(
        key = "removeGames",
        default = true,
        title = "Remove Games tab",
        description = "Removes the Games tab from the bottom bar.",
        required = true,
    )

    execute {
        if (removeClips != true && removeGames != true) {
            throw PatchException("Enable at least one tab to remove (Clips and/or Games).")
        }
        if (removeClips == true) {
            val count = forEachMatchingMethod(isTabWithName(CLIPS), isVisibilityGate) {
                it.returnEarly("const/4 v0, 0x0\nreturn v0")
            }
            if (count == 0) throw PatchException("Clips (VerticalVideoFeed) tab not found")
        }
        if (removeGames == true) {
            val count = forEachMatchingMethod(isTabWithName(GAMES), isVisibilityGate) {
                it.returnEarly("const/4 v0, 0x0\nreturn v0")
            }
            if (count == 0) throw PatchException("Games tab not found")
        }
    }
}
