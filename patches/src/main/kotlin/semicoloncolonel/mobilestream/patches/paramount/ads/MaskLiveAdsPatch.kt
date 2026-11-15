package semicoloncolonel.mobilestream.patches.paramount.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import semicoloncolonel.mobilestream.patches.paramount.shared.Constants

private const val LIVE_MASK = "Lsemicoloncolonel/mobilestream/extension/paramount/ads/ParamountLiveMask;"

// Paramount+ phone and TV builds share the same Avia player library as
// Pluto TV. Live TV channels (CBS News, CBS Sports, local stations) use
// ID3 ad tags in the stream, the same mechanism Pluto uses for its stitched
// channels. Each tag is a tick: the first one raises the mask, later ones
// keep it up, and the mask lifts when ticks stop (the show is back).
//
// Player activities from the Avia SDK (a build contains only its own hosts;
// the others are skipped):
//   Phone — VideoPlayerActivity (on-demand), MainActivity (live TV inline)
//   TV    — TV VideoPlayerActivity, LiveTvFullScreenActivity, HomeActivity
private val PLAYER_HOSTS = listOf(
    "Lcom/paramount/android/pplus/player/mobile/integration/ui/VideoPlayerActivity;",
    "Lcom/cbs/app/screens/main/MainActivity;",
    "Lcom/paramount/android/pplus/player/tv/impl/integration/ui/VideoPlayerActivity;",
    "Lcom/paramount/android/pplus/livetv/tv/channel/LiveTvFullScreenActivity;",
    "Lcom/cbs/app/tv/ui/activity/HomeActivity;",
)

@Suppress("unused")
val maskLiveAdsPatch = bytecodePatch(
    name = "Mask live ad breaks",
    description = "Covers live ad breaks with a \"We'll be right back!\" screen and mutes them.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        // 1) Every ad tag is a tick. Call the helper with the finished tag
        //    (p0) before each return in the constructor.
        Id3TagToStringFingerprint.classDef.methods
            .filter { it.name == "<init>" }
            .forEach { constructor ->
                val returns = constructor.implementation!!.instructions
                    .withIndex()
                    .filter { it.value.opcode == Opcode.RETURN_VOID }
                    .map { it.index }
                returns.reversed().forEach { index ->
                    constructor.addInstructions(
                        index,
                        "invoke-static/range { p0 .. p0 }, $LIVE_MASK->onAdTick(Ljava/lang/Object;)V",
                    )
                }
            }

        // 2) The activity the cover goes over: register on resume, lift on
        //    pause, forget on destroy.
        fun hook(method: com.android.tools.smali.dexlib2.iface.Method?, helper: String): Boolean {
            val target = method as? app.morphe.patcher.util.proxy.mutableTypes.MutableMethod ?: return false
            target.addInstructions(
                0,
                "invoke-static/range { p0 .. p0 }, $LIVE_MASK->$helper(Landroid/app/Activity;)V",
            )
            return true
        }
        var registered = 0
        PLAYER_HOSTS.forEach { host ->
            if (classDefByOrNull(host) == null) return@forEach
            val methods = mutableClassDefBy(host).methods
            fun lifecycle(name: String) = methods.firstOrNull {
                it.name == name && it.parameterTypes.isEmpty() && it.returnType == "V"
            }
            val onCreate = methods.firstOrNull {
                it.name == "onCreate" && it.returnType == "V" &&
                    it.parameterTypes.map { type -> type.toString() } == listOf("Landroid/os/Bundle;")
            }
            if (hook(lifecycle("onResume"), "registerActivity") || hook(onCreate, "registerActivity")) {
                registered++
            } else {
                error("Paramount+ live mask: $host has no onResume() or onCreate(Bundle)")
            }
            hook(lifecycle("onPause"), "onPlayerPause")
            hook(lifecycle("onDestroy"), "unregisterActivity")
        }
        if (registered == 0) error("Paramount+ live mask: no player activity found")

        // 3) The player to mute, through Avia's readable mute(boolean).
        AviaStartExoplayerFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $LIVE_MASK->setAviaPlayer(Ljava/lang/Object;)V",
        )
    }
}
