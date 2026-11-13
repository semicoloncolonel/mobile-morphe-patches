package semicoloncolonel.mobilestream.patches.yttvshell.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor

// YouTube TV's own Android TV app (com.google.android.youtube.tvunplugged) runs on an older,
// un-patchable classic Cobalt engine. Regular YouTube for Android TV
// (com.google.android.youtube.tv) runs on the newer Chromium-based Cobalt, which exposes
// WebContents.evaluateJavaScript, so it can be scripted. This patch set points that app's Cobalt
// start URL at the YouTube TV living-room web app (youtube.com/tv/upg) and injects an ad and
// tracking blocker, giving an ad-free YouTube TV on Android TV. It replaces the regular
// YouTube-for-TV app, which uses the same package name.
object Constants {
    val COMPATIBILITY = Compatibility(
        name = "YouTube TV (Android TV)",
        packageName = "com.google.android.youtube.tv",
        description = "The YouTube-for-Android-TV app, redirected to the YouTube TV living-room " +
            "app. This replaces the regular YouTube for Android TV app, not the YouTube TV " +
            "(unplugged) app.",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF0000,
        targets = listOf(AppTarget(version = "7.32.300", description = "Android TV build")),
        formFactor = FormFactor.TV,
    )
}
