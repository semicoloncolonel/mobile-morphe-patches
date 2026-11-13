package semicoloncolonel.mobilestream.patches.hulutv.privacy

import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

// Writes the prebuilt analytics-blocker library (libhulutrack.so) and its
// ShadowHook dependency into the APK's native lib dir. A dependency of Block
// analytics & tracking. Independent of the ad-strip's bundle patch; the shared
// ShadowHook libraries are written by whichever runs (idempotent).
private const val ABI = "armeabi-v7a"
private val LIBS = listOf("libhulutrack.so", "libshadowhook.so", "libshadowhook_nothing.so")

val bundleTrackerHookPatch = resourcePatch {
    execute {
        for (lib in LIBS) {
            val bytes = object {}.javaClass.getResourceAsStream("/native/$ABI/$lib")
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("bundled native library /native/$ABI/$lib missing")
            val target = resourceFile("lib/$ABI/$lib", false)
            target.parentFile?.mkdirs()
            target.outputStream().use { it.write(bytes) }
        }

        // Force extraction so the injected libs load from nativeLibraryDir (the
        // app ships extractNativeLibs="false").
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:extractNativeLibs", "true")
        }
    }
}
