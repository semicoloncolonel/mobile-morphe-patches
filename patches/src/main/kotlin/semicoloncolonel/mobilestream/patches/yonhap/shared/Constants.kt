package semicoloncolonel.mobilestream.patches.yonhap.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Yonhap News English",
        packageName = "kr.co.yna.YonhapNewsEnglish",
        description = "The English edition of the Yonhap News app.",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x005BAC,
        targets = listOf(devTarget("1.1.01", versionCode = 14))
    )
}
