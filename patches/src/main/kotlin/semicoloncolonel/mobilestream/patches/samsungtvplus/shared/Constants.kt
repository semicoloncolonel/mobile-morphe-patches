package semicoloncolonel.mobilestream.patches.samsungtvplus.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Samsung TV Plus",
        packageName = "com.samsung.android.tvplus",
        description = "The Samsung TV Plus phone app.",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1428A0,
        targets = listOf(AppTarget("1.0.30.8"))
    )
}
