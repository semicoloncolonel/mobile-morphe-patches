package semicoloncolonel.mobilestream.patches.kanopy.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Kanopy",
        packageName = "com.kanopy",
        description = "The Kanopy library streaming app.",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xDE2F00,
        targets = listOf(devTarget("6.32.0", 1032)),
    )
}
