package semicoloncolonel.mobilestream.patches.bestbuy.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Best Buy",
        packageName = "com.bestbuy.android",
        description = "The Best Buy shopping app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x0046BE,
        targets = listOf(AppTarget("26.10.10"), devTarget("26.11.10", versionCode = 2012312346))
    )
}
