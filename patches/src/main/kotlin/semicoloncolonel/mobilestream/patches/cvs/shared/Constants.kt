package semicoloncolonel.mobilestream.patches.cvs.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "CVS",
        packageName = "com.cvs.launchers.cvs",
        description = "The CVS Pharmacy app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xCC0000,
        targets = listOf(devTarget("26.11.10"))
    )
}
