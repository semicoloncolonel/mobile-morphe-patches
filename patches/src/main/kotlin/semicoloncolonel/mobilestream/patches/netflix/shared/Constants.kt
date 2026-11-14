package semicoloncolonel.mobilestream.patches.netflix.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility
import semicoloncolonel.mobilestream.patches.shared.misc.devTarget

object Constants {
    // Netflix ships its own native tamper guard, so every Netflix patch is
    // experimental: it only applies to a build that has been re-signed, and
    // the guard's self-kill is blocked in-process by the bundled native hook.
    // The target is pinned to the build the bypass was developed against; the
    // Application class and the guard's kill path are re-located per version.
    val COMPATIBILITY = Compatibility(
        name = "Netflix",
        packageName = "com.netflix.mediaclient",
        description = "The Netflix app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xE50914,
        // Netflix embeds the build number and version code in its version
        // name, so the full string must match exactly: "9.93.0 build 7 74878".
        targets = listOf(
            devTarget("9.93.0 build 7 74878", versionCode = 74878),
        ),
    )
}
