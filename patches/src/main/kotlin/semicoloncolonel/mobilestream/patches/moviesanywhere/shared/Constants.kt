package semicoloncolonel.mobilestream.patches.moviesanywhere.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY = Compatibility(
        name = "Movies Anywhere",
        packageName = "com.moviesanywhere.goo",
        description = "The Movies Anywhere phone app (Disney-operated digital locker).",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x5A2D82,
        targets = listOf(AppTarget("1.54.0"))
    )
}
