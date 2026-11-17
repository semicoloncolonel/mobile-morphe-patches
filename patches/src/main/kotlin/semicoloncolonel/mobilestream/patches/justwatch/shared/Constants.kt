package semicoloncolonel.mobilestream.patches.justwatch.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "JustWatch",
        packageName = "com.justwatch.justwatch",
        description = "The JustWatch streaming guide app for phones.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFBC500,
        targets = listOf(
            AppTarget("26.45.1", versionCode = 2645001, description = "Phone build"),
            AppTarget("26.44.2", versionCode = 2644002, description = "Phone build"),
            AppTarget("26.43.10", versionCode = 2643010, description = "Phone build"),
        )
    )
}
