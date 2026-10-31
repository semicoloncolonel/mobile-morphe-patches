package semicoloncolonel.mobilestream.patches.hallmark.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Hallmark+",
        packageName = "com.feeln.android",
        description = "The Hallmark+ phone app. Needs the Pairip license-check patch to run once re-signed.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x5B2C83,
        targets = listOf(AppTarget("8.108.2"))
    )
}
