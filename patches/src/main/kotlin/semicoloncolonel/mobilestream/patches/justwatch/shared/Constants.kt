package semicoloncolonel.mobilestream.patches.justwatch.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor
import app.morphe.patcher.patch.SupportedAbi
import semicoloncolonel.mobilestream.patches.shared.misc.byBundle
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    private val PHONE_TARGETS = listOf(
        devTarget("26.45.1", 2645001, description = "Phone build"),
        AppTarget("26.44.2", versionCode = 2644002, description = "Phone build"),
        AppTarget("26.43.10", versionCode = 2643010, description = "Phone build"),
    )
    private val TV_TARGETS = listOf(
        // Two Android TV builds of 26.43.8 exist: APKMirror's bundle (2643008, with
        // 32-bit and 64-bit splits) and the single 32-bit APK Google Play gives an
        // NVIDIA Shield (12643008). Both are listed so Morphe Manager accepts either.
        AppTarget(
            version = "26.43.8",
            versionCodes = mapOf(
                SupportedAbi.ARMEABI_V7A to 12643008,
                SupportedAbi.ARM64_V8A to 2643008,
            ),
            description = "Android TV build",
        ),
        // Matched by version name alone, so both the APKMirror bundle and the
        // copy Google Play gives a Shield are accepted.
        devTarget("26.44.5", description = "Android TV build"),
    )

    private fun justWatch(description: String, targets: List<AppTarget>, formFactor: FormFactor? = null) =
        Compatibility(
            name = "JustWatch",
            packageName = "com.justwatch.justwatch",
            description = description,
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFBC500,
            targets = targets,
            formFactor = formFactor,
        )

    // One entry in the combined bundle, a separate one in each split bundle.
    // See shared/misc/BundleFlavor.kt.
    val COMPATIBILITY = byBundle(
        all = justWatch("The JustWatch streaming guide app, for phones and Android TV.", PHONE_TARGETS + TV_TARGETS),
        phone = justWatch("The JustWatch streaming guide app for phones.", PHONE_TARGETS),
        tv = justWatch("The JustWatch streaming guide app for Android TV.", TV_TARGETS, FormFactor.TV),
    )
}
