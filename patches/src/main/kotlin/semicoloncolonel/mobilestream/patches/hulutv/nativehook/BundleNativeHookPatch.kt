package semicoloncolonel.mobilestream.patches.hulutv.nativehook

import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

// Writes the prebuilt native ad-strip library and its ShadowHook dependency
// into the APK's native lib dir. Adapted from ajstrick81's BundleNativeHookPatch
// (native-adstrip-toolkit, GPL-3.0). The Hulu TV app is armeabi-v7a.
//
// The prebuilt libraries are bundled as patch resources (built from
// extensions-native/hulutv; see that module's README):
//   patches/src/main/resources/native/armeabi-v7a/libhuluhook.so
//   patches/src/main/resources/native/armeabi-v7a/libshadowhook.so
//   patches/src/main/resources/native/armeabi-v7a/libshadowhook_nothing.so
//
// A dependency of Skip ads.
private const val ABI = "armeabi-v7a"
private val LIBS = listOf("libhuluhook.so", "libshadowhook.so", "libshadowhook_nothing.so")

val bundleNativeHookPatch = resourcePatch {
    execute {
        for (lib in LIBS) {
            val bytes = object {}.javaClass.getResourceAsStream("/native/$ABI/$lib")
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("bundled native library /native/$ABI/$lib missing")
            val target = resourceFile("lib/$ABI/$lib", false)
            target.parentFile?.mkdirs()
            target.outputStream().use { it.write(bytes) }
        }

        // The TV app ships android:extractNativeLibs="false", which mmaps libs
        // straight from the (compressed, unaligned) APK and would throw
        // UnsatisfiedLinkError on an injected library. Force extraction so the
        // linker loads our libs from nativeLibraryDir instead.
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:extractNativeLibs", "true")
        }
    }
}
