package semicoloncolonel.mobilestream.patches.kouponai.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch

// ─────────────────────────────────────────────────────────────────────────────
// Koupon 3.1.6 is a React Native app, and its native libraries
// (libreactnative.so, libhermes.so, and the rest) are in the ABI split APK
// (split_config.arm64_v8a.apk), not in base.apk. Patching base.apk alone, as
// "Use installed app" does with a split install, drops them, and the patched
// app crashes at launch with "couldn't find DSO to load: libreactnative.so".
// Every Koupon patch depends on this check, which refuses such an input with
// a clear message instead of building an app that cannot start.
// ─────────────────────────────────────────────────────────────────────────────

private val ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

internal val requireNativeLibsPatch = resourcePatch {
    execute {
        val present = ABIS.any { abi ->
            try {
                get("lib/$abi/libreactnative.so", true).exists()
            } catch (e: Exception) {
                false
            }
        }
        if (!present) {
            throw PatchException(
                "Koupon's native libraries are missing from this input. Patch the full APKM bundle " +
                    "(base plus the ABI split) from APKMirror. Do not use \"Use installed app\" or a " +
                    "base.apk alone: the libraries are in the split, and the patched app would crash at launch.",
            )
        }
    }
}
