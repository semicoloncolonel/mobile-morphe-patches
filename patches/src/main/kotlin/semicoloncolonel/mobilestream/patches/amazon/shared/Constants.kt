package semicoloncolonel.mobilestream.patches.amazon.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Amazon Shopping",
        packageName = "com.amazon.mShop.android.shopping",
        description = "The Amazon Shopping app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x232F3E,
        targets = listOf(devTarget("32.20.2.100"))
    )
}
