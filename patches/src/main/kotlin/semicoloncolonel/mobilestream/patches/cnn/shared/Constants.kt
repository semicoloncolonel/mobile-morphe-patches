package semicoloncolonel.mobilestream.patches.cnn.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "CNN",
        packageName = "com.cnn.mobile.android.phone",
        description = "The CNN app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xCC0000,
        targets = listOf(devTarget("26.23.0"))
    )
}
