package semicoloncolonel.mobilestream.patches.netflix.categories

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import semicoloncolonel.mobilestream.patches.netflix.shared.Constants

// "Browse hidden categories".
//
// Netflix's thousands of hidden genre codes (netflix.com/browse/genre/ID) are
// not exposed in the phone app, but the app already registers a deep-link
// handler for that path. This patch bundles the current list of codes as an
// asset and adds a small self-contained screen (HiddenCategoriesActivity) that
// lists and filters them; tapping one opens that category inside Netflix
// through its own deep link. None of Netflix's own UI is touched.
//
// The screen is reachable two ways: its own launcher icon ("Hidden
// Categories"), and a long-press shortcut on the Netflix icon. The code list
// is the set that resolved against a US account, so a few may be empty in
// other regions.

private const val EXTENSION_CLASSES = "semicoloncolonel.mobilestream.extension.netflix.categories"
private const val ACTIVITY = "$EXTENSION_CLASSES.HiddenCategoriesActivity"
private const val SHORTCUTS = "msp_netflix_categories_shortcuts"
private const val ASSET = "assets/netflix_hidden_genres.tsv"

private val bundleHiddenCategoriesPatch = resourcePatch {
    execute {
        // 1. The bundled genre list, copied in as an app asset.
        val bytes = object {}.javaClass.getResourceAsStream("/netflix/hidden_genres.tsv")
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("bundled /netflix/hidden_genres.tsv missing")
        val asset = get(ASSET)
        asset.parentFile?.mkdirs()
        asset.writeBytes(bytes)

        // 2. Labels for the launcher entry and the long-press shortcut.
        document("res/values/strings.xml").use { document ->
            val resources = document.documentElement
            mapOf(
                "msp_hidden_categories" to "Hidden Categories",
                "msp_hidden_categories_short" to "Categories",
                "msp_hidden_categories_long" to "Browse hidden categories",
            ).forEach { (name, text) ->
                val string = document.createElement("string")
                string.setAttribute("name", name)
                string.textContent = text
                resources.appendChild(string)
            }
        }

        // 3. The long-press app shortcut, pointing at our activity.
        val shortcuts = get("res/xml/$SHORTCUTS.xml")
        shortcuts.parentFile.mkdirs()
        shortcuts.writeText(
            listOf(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<shortcuts xmlns:android=\"http://schemas.android.com/apk/res/android\">",
                "  <shortcut android:shortcutId=\"msp_hidden_categories\" android:enabled=\"true\" " +
                    "android:icon=\"@android:drawable/ic_menu_search\" " +
                    "android:shortcutShortLabel=\"@string/msp_hidden_categories_short\" " +
                    "android:shortcutLongLabel=\"@string/msp_hidden_categories_long\">",
                "    <intent android:action=\"android.intent.action.MAIN\" " +
                    "android:targetPackage=\"${Constants.COMPATIBILITY.packageName}\" " +
                    "android:targetClass=\"$ACTIVITY\" />",
                "  </shortcut>",
                "</shortcuts>",
            ).joinToString("\n"),
        )

        // 4. Register the activity (its own launcher icon) and attach the
        //    shortcut list to the app's existing launcher activity.
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", ACTIVITY)
            activity.setAttribute("android:exported", "true")
            activity.setAttribute("android:label", "@string/msp_hidden_categories")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
            activity.setAttribute("android:taskAffinity", "")
            val filter = document.createElement("intent-filter")
            val action = document.createElement("action")
            action.setAttribute("android:name", "android.intent.action.MAIN")
            filter.appendChild(action)
            val category = document.createElement("category")
            category.setAttribute("android:name", "android.intent.category.LAUNCHER")
            filter.appendChild(category)
            activity.appendChild(filter)
            application.appendChild(activity)

            // Attach the shortcut resource to the app's own launcher entry
            // (activity or activity-alias), not ours.
            fun Element.hasLauncherCategory(): Boolean {
                val categories = getElementsByTagName("category")
                return (0 until categories.length).any {
                    (categories.item(it) as Element).getAttribute("android:name") ==
                        "android.intent.category.LAUNCHER"
                }
            }
            val entries = listOf("activity", "activity-alias").flatMap { tag ->
                val nodes = application.getElementsByTagName(tag)
                (0 until nodes.length).map { nodes.item(it) as Element }
            }
            val launcher = entries.firstOrNull {
                it.getAttribute("android:name") != ACTIVITY && it.hasLauncherCategory()
            } ?: throw PatchException("Launcher activity not found")
            val metaData = document.createElement("meta-data")
            metaData.setAttribute("android:name", "android.app.shortcuts")
            metaData.setAttribute("android:resource", "@xml/$SHORTCUTS")
            launcher.appendChild(metaData)
        }
    }
}

@Suppress("unused")
val hiddenCategoriesPatch = bytecodePatch(
    name = "Browse hidden categories",
    description = "Adds a searchable list of Netflix's hidden genre categories (the " +
        "netflix.com/browse/genre codes) and opens any of them inside the app. Reachable from " +
        "its own \"Hidden Categories\" icon and from a long-press shortcut on the Netflix icon.",
    default = false,
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(bundleHiddenCategoriesPatch)

    extendWith("extensions/extension.mpe")

    // The screen is a bundled extension activity, wired entirely through the
    // manifest, so there is no bytecode to change here.
    execute {}
}
