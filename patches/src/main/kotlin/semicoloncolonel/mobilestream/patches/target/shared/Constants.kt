package semicoloncolonel.mobilestream.patches.target.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Target",
        packageName = "com.target.ui",
        description = "The Target shopping app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xCC0000,
        targets = listOf(devTarget("2026.44.0"))
    )
}
