package semicoloncolonel.mobilestream.patches.cnn.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import semicoloncolonel.mobilestream.patches.cnn.shared.Constants

// ─────────────────────────────────────────────────────────────────────────────
// CNN 26.21.0 and 26.23.0 — Faster launch.
//
// The splash screen waits for CNN's feature settings before it opens the app.
// Where that wait happens moved between the two builds, so this patch addresses
// both, and each half is optional: the half that does not apply finds nothing
// and leaves that build alone.
//
// 26.21.0 — Optimizely.
//   The splash waits for Optimizely (CNN's feature-flag service) to start, and
//   OptimizelyClientFactory starts it with the SDK's asynchronous initialize,
//   which downloads a fresh datafile first. When cdn.optimizely.com is blocked
//   or slow, the SDK retries with waits of 2, 4 and 8 seconds, so the splash
//   stays up for about 14 seconds. The initialize call is redirected to
//   OptimizelyQuickStart, which starts the client at once from the datafile
//   already on the phone and refreshes in the background.
//
// 26.23.0 — the startup-config gate.
//   26.23.0 rewrote OptimizelyClientFactory to start from the local datafile
//   itself, so the asynchronous initialize call is gone and the Optimizely half
//   finds nothing. The splash's wait moved to StartupManager: SplashFragment
//   collects StartupManager.isReady, a StateFlow<Boolean> that starts false and
//   turns true only after the remote startup config is fetched, and does not
//   open the app until it sees true. When that fetch is blocked or slow the flag
//   never turns true and the splash hangs. (The Spoof network state patch makes
//   this worse: it tells CNN it is online, so the splash's own "no internet"
//   screen, which has a Retry button, never shows.)
//
//   The wait is FlowKt.first(isReady) { it }. Its predicate is forced to accept
//   the first value, which a StateFlow always holds, so the splash opens the app
//   at once with the last-known settings and the fetch finishes in the
//   background. Only the splash's own predicate is changed, so nothing else that
//   reads the flag, such as the home-screen widget, is affected.
// ─────────────────────────────────────────────────────────────────────────────

private const val FACTORY = "Lcom/cnn/mobile/android/phone/features/featurestateprovider/optimizely/OptimizelyClientFactory;"
private const val QUICK_START = "Lsemicoloncolonel/mobilestream/extension/cnn/OptimizelyQuickStart;"

// The suspend lambda passed to FlowKt.first that holds the splash's wait on the
// startup-config flag. Its name is tied to the lambda's position in
// SplashFragment, so a build that reorders it is left unpatched rather than
// patched wrongly.
private const val STARTUP_GATE_PREDICATE =
    "Lcom/cnn/mobile/android/phone/features/splash/SplashFragment\$checkConfigInitialized\$2\$1;"

@Suppress("unused")
val fasterLaunchPatch = bytecodePatch(
    name = "Faster launch",
    description = "Opens the app without waiting to download its feature settings.",
) {
    category("Setup")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        // 26.21.0: redirect Optimizely's asynchronous initialize to the local
        // quick start.
        var replaced = 0
        mutableClassDefBy(FACTORY).methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                val reference = (instruction as ReferenceInstruction).reference as MethodReference
                // OptimizelyManager.initialize(Context, Integer datafileRes, listener)
                if (reference.returnType != "V" || reference.parameterTypes.size != 3 ||
                    reference.parameterTypes[0] != "Landroid/content/Context;" ||
                    reference.parameterTypes[1] != "Ljava/lang/Integer;"
                ) {
                    return@forEachIndexed
                }
                val r = instruction as FiveRegisterInstruction
                method.replaceInstruction(
                    index,
                    "invoke-static { v${r.registerC}, v${r.registerD}, v${r.registerE}, v${r.registerF} }, " +
                        "$QUICK_START->start(Ljava/lang/Object;Landroid/content/Context;Ljava/lang/Integer;Ljava/lang/Object;)V",
                )
                replaced++
            }
        }
        // 26.21.0 has one asynchronous initialize call to replace. 26.23.0 and
        // later start from the local datafile already, so finding none is fine.
        if (replaced > 1) throw PatchException("Optimizely initialize call: expected 0 or 1, found $replaced")

        // 26.23.0: make the splash's wait on the startup-config flag return at
        // once by forcing its predicate to accept the first value.
        var splashGateFound = false
        if (classDefByOrNull(STARTUP_GATE_PREDICATE) != null) {
            splashGateFound = true
            val predicate = mutableClassDefBy(STARTUP_GATE_PREDICATE)
            val invokeSuspend = predicate.methods.single { it.name == "invokeSuspend" }
            val instructions = invokeSuspend.implementation?.instructions?.toList()
                ?: throw PatchException("Startup-config predicate has no body")
            // Where the predicate reads the emitted Boolean (a single
            // iget-boolean) and returns Boolean.valueOf of it, set that register
            // to 1 so it always returns true. Some 26.23.0 builds already
            // short-circuit it to a constant true (first { true } on the isReady
            // StateFlow is instant and does not hang), so there is no boolean
            // read and nothing to do — leave it rather than failing.
            val index = instructions.indexOfFirst { it.opcode == Opcode.IGET_BOOLEAN }
            if (index >= 0) {
                val register = (instructions[index] as OneRegisterInstruction).registerA
                invokeSuspend.replaceInstruction(index, "const/16 v$register, 0x1")
            }
        }

        // Fail only on a build where neither surface is recognizable at all.
        if (replaced == 0 && !splashGateFound) {
            throw PatchException("Faster launch found neither the Optimizely call nor the startup-config gate")
        }
    }
}
