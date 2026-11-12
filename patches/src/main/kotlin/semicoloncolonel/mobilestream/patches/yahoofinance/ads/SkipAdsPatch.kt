package semicoloncolonel.mobilestream.patches.yahoofinance.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import semicoloncolonel.mobilestream.patches.yahoofinance.shared.Constants
import semicoloncolonel.mobilestream.patches.yahoofinance.shared.offFlags

// ─────────────────────────────────────────────────────────────────────────────
// Yahoo Finance 26.22.2 — Skip ads.
//
// Every ad slot (the Home pencil, the Markets and quote page slots, Key Stats,
// More News, the Inception and React Native slots, and the Taboola stream ads)
// gets its ad from the Sponsored Moments SDK's fetcher (sponsoredmoments
// fetcher/g, R8-renamed):
//
//   a(f, String, h)  a slot asks for an ad and waits. It now gets an ad error
//                    (100, the code the SDK uses for a failed Taboola fetch)
//                    at once, so the slot shows its empty state.
//   h(String, h)     the Google Ad Manager request, and
//   i(String, I, h)  the Taboola request: both return at once.
//   v(String, h)     "is an ad ready" answers false, and
//   m(String, h)     the cache serves no ad.
//
// Video: the Video 2.0 pencil ad is turned off in VideoKitAdsConfig, and Yahoo's
// video player (SapiMediaItemProviderConfig) is told the ads opt-out is on, so it
// sends dnt=true and no device ID, and its ad-break addresses point at an
// address that does not resolve. The flag inceptionQspNewsListAds reads as off,
// so More News on quote pages has no ad rows.
//
// The ad-free entitlement of Yahoo Finance Plus is not touched.
// ─────────────────────────────────────────────────────────────────────────────

private const val FETCHER = "Lcom/oath/mobile/ads/sponsoredmoments/fetcher/g;"
private const val LISTENER = "Lcom/oath/mobile/ads/sponsoredmoments/fetcher/f;"
private const val CALLBACK = "Lcom/oath/mobile/ads/sponsoredmoments/fetcher/h;"
private const val SAPI_CONFIG =
    "Lcom/verizondigitalmedia/mobile/client/android/sapiMediaItemProvider/config/SapiMediaItemProviderConfig;"
private const val NO_ADS_URL = "https://video-api.invalid/"

internal object AwaitAdFingerprint : Fingerprint(
    definingClass = FETCHER,
    name = "a",
    parameters = listOf(LISTENER, "Ljava/lang/String;", CALLBACK),
    returnType = "V",
)

internal object GamRequestFingerprint : Fingerprint(
    definingClass = FETCHER,
    name = "h",
    parameters = listOf("Ljava/lang/String;", CALLBACK),
    returnType = "V",
)

internal object TaboolaRequestFingerprint : Fingerprint(
    definingClass = FETCHER,
    name = "i",
    parameters = listOf("Ljava/lang/String;", "I", CALLBACK),
    returnType = "V",
)

internal object AdReadyFingerprint : Fingerprint(
    definingClass = FETCHER,
    name = "v",
    parameters = listOf("Ljava/lang/String;", CALLBACK),
    returnType = "Z",
)

internal object CachedAdFingerprint : Fingerprint(
    definingClass = FETCHER,
    name = "m",
    parameters = listOf("Ljava/lang/String;", CALLBACK),
    returnType = "Lcom/oath/mobile/ads/sponsoredmoments/models/s;",
)

internal object VideoKitAdsConfigFingerprint : Fingerprint(
    definingClass = "Lcom/vzmedia/android/videokit/config/VideoKitAdsConfig;",
    name = "<init>",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"),
    returnType = "V",
)

internal object AdsOptOutFingerprint : Fingerprint(
    definingClass = SAPI_CONFIG,
    name = "isAdsOptOut",
    parameters = listOf(),
    returnType = "Z",
)

internal object AdBreaksEndpointFingerprint : Fingerprint(
    definingClass = SAPI_CONFIG,
    name = "getThunderballAdbreaksEndpoint",
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
)

internal object AdsEndpointFingerprint : Fingerprint(
    definingClass = SAPI_CONFIG,
    name = "getThunderballAdsEndpoint",
    parameters = listOf(),
    returnType = "Ljava/lang/String;",
)

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Removes the ads on Home, Markets, quote pages, and news, and the ads in videos.",
) {
    category("Ads")
    compatibleWith(Constants.COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        AwaitAdFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, 0x64
                invoke-interface { p1, v0 }, $LISTENER->a(I)V
                return-void
            """,
        )
        GamRequestFingerprint.method.addInstructions(0, "return-void")
        TaboolaRequestFingerprint.method.addInstructions(0, "return-void")
        AdReadyFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
        CachedAdFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        VideoKitAdsConfigFingerprint.method.addInstructions(0, "const/4 p3, 0x0")
        AdsOptOutFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
        listOf(AdBreaksEndpointFingerprint, AdsEndpointFingerprint).forEach {
            it.method.addInstructions(
                0,
                """
                    const-string v0, "$NO_ADS_URL"
                    return-object v0
                """,
            )
        }

        offFlags("adsOff")
    }
}
