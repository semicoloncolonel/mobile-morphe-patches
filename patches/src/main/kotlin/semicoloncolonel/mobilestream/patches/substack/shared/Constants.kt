package semicoloncolonel.mobilestream.patches.substack.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Substack",
        packageName = "com.substack.app",
        description = "The Substack reader app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF6719,
        targets = listOf(devTarget("3.13.1"), AppTarget("3.12.0")),
    )
}
