package semicoloncolonel.mobilestream.patches.scmp.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "South China Morning Post",
        packageName = "com.scmp.newspulse",
        description = "The SCMP news app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x001246,
        targets = listOf(devTarget("7.2.9", versionCode = 2000394))
    )
}
