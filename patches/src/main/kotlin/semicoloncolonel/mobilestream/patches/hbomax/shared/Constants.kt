package semicoloncolonel.mobilestream.patches.hbomax.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

private const val PHONE_VERSION = "22.0.0.321"
private const val TV_VERSION = "22.0.0.36"

object Constants {
    // The phone and Android TV builds share the package name but not the version.
    val COMPATIBILITY = Compatibility(
        name = "HBO Max",
        packageName = "com.wbd.hbomax",
        description = "The HBO Max app, for phones (version $PHONE_VERSION) and Android TV (version $TV_VERSION).",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x002BE7,
        targets = listOf(devTarget(PHONE_VERSION), devTarget(TV_VERSION))
    )

    // For patches that only apply to the phone build, such as the sign-in
    // screen, which the Android TV build does not have.
    val PHONE_COMPATIBILITY = Compatibility(
        name = "HBO Max",
        packageName = "com.wbd.hbomax",
        description = "The HBO Max phone app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x002BE7,
        targets = listOf(devTarget(PHONE_VERSION))
    )
}
