package semicoloncolonel.mobilestream.patches.fandango.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Fandango at Home",
        packageName = "air.com.vudu.air.DownloaderTablet",
        description = "The phone app, formerly Vudu.",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x3D7CF4,
        targets = listOf(devTarget("10.25.preview17.179399459"))
    )
}
