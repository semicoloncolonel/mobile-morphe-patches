package semicoloncolonel.mobilestream.patches.starz.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Starz",
        packageName = "com.bydeluxe.d3.android.program.starz",
        description = "The Starz phone and tablet app, not the Android TV app. " +
            "Both are version 6.11.0, and the phone build is version code 4513.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x000000,
        targets = listOf(devTarget("6.12.0", versionCode = 4518))
    )
}
