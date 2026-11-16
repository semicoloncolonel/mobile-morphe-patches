package semicoloncolonel.mobilestream.patches.tubi.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor
import semicoloncolonel.mobilestream.patches.shared.misc.byBundle
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    private val PHONE_TARGETS = listOf(
        devTarget("10.44.3", versionCode = 1066, description = "Phone build"),
        AppTarget("10.44.1", versionCode = 1063, description = "Phone build"),
    )
    private val TV_TARGETS = listOf(
        devTarget("10.44.5001", versionCode = 1067, description = "Android TV build"),
    )

    private fun tubi(description: String, targets: List<AppTarget>, formFactor: FormFactor? = null) = Compatibility(
        name = "Tubi",
        packageName = "com.tubitv",
        description = description,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xE9181C,
        targets = targets,
        formFactor = formFactor,
    )

    // One entry in the combined bundle, a separate one in each split bundle.
    // See shared/misc/BundleFlavor.kt.
    val COMPATIBILITY = byBundle(
        all = tubi(
            "The Tubi app, for phones and Android TV. On a TV, patch the Android TV build, version 10.44.5001.",
            PHONE_TARGETS + TV_TARGETS,
        ),
        phone = tubi("The Tubi app for phones.", PHONE_TARGETS),
        tv = tubi("The Tubi app for Android TV.", TV_TARGETS, FormFactor.TV),
    )
}
