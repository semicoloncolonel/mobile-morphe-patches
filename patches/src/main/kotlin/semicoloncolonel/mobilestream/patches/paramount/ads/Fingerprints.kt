package semicoloncolonel.mobilestream.patches.paramount.ads

import app.morphe.patcher.Fingerprint

// Paramount+ phone 17.3.0. The Avia player library keeps its names
// un-obfuscated, just like Pluto TV, so the same seams match by name.

// The ad detector: Avia's ID3Tag model. Its toString() labels the fields
// "ID3Tag(creativeId=..." etc. The stitcher / DAI provider builds one of
// these for every ad segment it sees. toString() survives R8.
internal object Id3TagToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("ID3Tag(creativeId="),
    custom = { method, _ -> method.name == "toString" }
)

// AviaPlayer.startExoplayer: where the player starts, so the mask can
// grab a reference for muting.
internal object AviaStartExoplayerFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        classDef.type == "Lcom/paramount/android/avia/player/player/core/AviaPlayer;" &&
            method.name == "startExoplayer" &&
            method.parameterTypes.size == 1
    }
)
