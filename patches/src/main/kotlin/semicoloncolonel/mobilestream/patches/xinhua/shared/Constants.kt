package semicoloncolonel.mobilestream.patches.xinhua.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Xinhua News",
        packageName = "com.xinhuamm.xinhuanews",
        description = "The Xinhua News app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xD22630,
        targets = listOf(devTarget("5.4.2", versionCode = 432))
    )
}
