package semicoloncolonel.mobilestream.patches.hallmark.ads

import app.morphe.patcher.Fingerprint

// Hallmark+ phone build 8.108.2 (versionCode 80011303), built on Accedo One
// with the Akamai AMP player. The Accedo One model classes keep their names;
// their getters are R8-renamed, so these anchor on the class and the
// return type instead of the method name.

private const val PLAYER_PROPERTIES = "Ltv/accedo/one/player/akamai/OneAkamaiPlayerProperties;"

// The player view creates OneAkamaiFreewheelPlugin only when this config is
// non-null, and every use of the plugin field is null-checked.
object FreewheelConfigFingerprint : Fingerprint(
    definingClass = PLAYER_PROPERTIES,
    returnType = "Ltv/accedo/one/player/akamai/OneAkamaiPlayerProperties\$Freewheel;",
    parameters = listOf(),
)

// Amazon Publisher Services config. When it is null, the app skips
// fetchAdTags and does not add the adsParams query to the stream URL.
object AmazonPublisherServicesConfigFingerprint : Fingerprint(
    definingClass = PLAYER_PROPERTIES,
    returnType = "Ltv/accedo/one/player/akamai/OneAkamaiPlayerProperties\$AmazonPublisherServices;",
    parameters = listOf(),
)

// Pause-screen ads load only when this flag is true.
object PauseScreenAdsEnabledFingerprint : Fingerprint(
    definingClass = "Ltv/accedo/one/core/model/content/AdvertisementGoogleImaDai\$PauseScreen;",
    name = "getEnabled",
    returnType = "Z",
)
