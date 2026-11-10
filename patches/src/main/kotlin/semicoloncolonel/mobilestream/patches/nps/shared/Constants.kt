package semicoloncolonel.mobilestream.patches.nps.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "NPS",
        packageName = "gov.nps.mobileapp",
        description = "The National Park Service app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x2B4423,
        targets = listOf(devTarget("2.16.0", versionCode = 3937))
    )
}
