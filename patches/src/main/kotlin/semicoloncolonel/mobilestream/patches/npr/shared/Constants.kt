package semicoloncolonel.mobilestream.patches.npr.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "NPR",
        packageName = "org.npr.android.news",
        description = "The NPR app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x1A1A1A,
        targets = listOf(AppTarget("7.10.0"), devTarget("7.11.0", versionCode = 140979))
    )
}
