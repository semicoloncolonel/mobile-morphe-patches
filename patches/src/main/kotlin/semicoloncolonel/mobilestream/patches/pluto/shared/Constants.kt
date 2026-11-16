package semicoloncolonel.mobilestream.patches.pluto.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor
import semicoloncolonel.mobilestream.patches.shared.misc.byBundle

object Constants {
    // The phone and Android TV builds share these version names.
    private val TARGETS = listOf(
        AppTarget("17.4.0"),
        AppTarget("17.3.0"),
    )

    private fun pluto(description: String, formFactor: FormFactor? = null) = Compatibility(
        name = "Pluto TV",
        packageName = "tv.pluto.android",
        description = description,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xB8B004,
        targets = TARGETS,
        formFactor = formFactor,
    )

    // One entry in the combined bundle, a separate one in each split bundle.
    // See shared/misc/BundleFlavor.kt.
    val COMPATIBILITY = byBundle(
        all = pluto(
            "The Pluto TV app, for phones and Android TV. The phone and TV builds " +
                "share a version name. Phone builds have version codes that start with 4, and " +
                "Android TV builds have version codes that start with 5. On a TV, patch the Android TV build.",
        ),
        phone = pluto("The Pluto TV app for phones. Phone builds have version codes that start with 4."),
        tv = pluto(
            "The Pluto TV app for Android TV. Android TV builds have version codes that start with 5.",
            FormFactor.TV,
        ),
    )
}
