package semicoloncolonel.mobilestream.patches.straitstimes.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "The Straits Times",
        packageName = "com.buuuk.st",
        description = "The Straits Times news app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x002F5F,
        targets = listOf(devTarget("8.5.20.PO(ST)REL", versionCode = 22471))
    )
}
