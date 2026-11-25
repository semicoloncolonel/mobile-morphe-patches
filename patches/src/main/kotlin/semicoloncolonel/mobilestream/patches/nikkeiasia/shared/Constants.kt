package semicoloncolonel.mobilestream.patches.nikkeiasia.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    // Nikkei Asia names each version with a build hash, so the update check
    // compares it with the version Google Play lists instead of ordering it.
    val COMPATIBILITY = Compatibility(
        name = "Nikkei Asia",
        packageName = "com.nikkei.asia",
        description = "The Nikkei Asia news app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x0076BF,
        targets = listOf(devTarget("8a14774d3a9cb8f25eaae58f4c4e4f3e", versionCode = 1737598861))
    )
}
