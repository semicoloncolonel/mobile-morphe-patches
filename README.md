# Mobile Stream Patches

Morphe patches that remove ads and block tracking in the phone versions of streaming apps.

These patches are ports of [ajstrick81's Android TV patches](https://github.com/ajstrick81/morphe-androidtv-patches). The original hook designs are their work. This project adapts them to the phone builds of the same apps. For the Android TV apps, use the original project.

## Patch status

| App | Patches | Tested version |
|-----|---------|----------------|
| Tubi | Skip ads, Remove social sign-in, Hide sign-in prompts, Block analytics & tracking (opt-in) | `10.43.1` |
| Disney+ | Skip ads, Block analytics & tracking (opt-in) | `26.20.0+rc1-2026.10.22` |
| Pluto TV | Skip ads, Mask live ad breaks, Block analytics & tracking (opt-in) | `17.2.0` |
| Paramount+ | Skip ads, Block analytics & tracking (opt-in) | `17.2.0` |
| HBO Max | Skip ads, Remove social sign-in, Block analytics & tracking (opt-in) | `7.14.0.530` |
| Peacock | Skip ads (experimental), Disable Xfinity auto sign-in, Block analytics & tracking (opt-in) | `7.10.20` |
| Prime Video | Skip ads (experimental), Block analytics & tracking (opt-in) | `3.0.477.1447` |
| MGM+ | Skip ads (experimental), Skip pre-roll promo (experimental), Skip lion intro, Disable MGM+ AI (opt-in), Remove social sign-in, Block analytics & tracking (opt-in) | `240.0.2026240000` |
| Fox One | Skip ads (experimental), Disable Pairip license check, Block analytics & tracking (opt-in) | `1.24.0` |
| The New York Times | Skip ads (experimental), Hide upsell pop-ups (experimental), Remove social sign-in, Block analytics & tracking (opt-in) | `11.98.0` |
| The Washington Post | Skip ads (experimental), Hide marketing pop-ups (experimental), Disable AI (opt-in), Block analytics & tracking (opt-in) | `7.18.1` |
| The Wall Street Journal | Skip ads (experimental), Hide marketing pop-ups (experimental), Block analytics & tracking (opt-in) | `15.12.0.42675` |
| AMC+ | Skip ads (experimental), Block analytics & tracking (opt-in) | `1.8.62.1` |
| Apple TV | Skip ads (experimental) | `3.0.0` |
| Hallmark+ | Skip ads (experimental), Disable Pairip license check, Block analytics & tracking (opt-in) | `8.108.2` |
| Starz | Skip pre-roll promo (experimental), Remove social sign-in, Block analytics & tracking (opt-in) | `6.11.0` |
| PBS | Skip ads (experimental), Block analytics & tracking (opt-in) | `5.27.0` |
| Samsung TV Plus | Block analytics & tracking (opt-in) | `1.0.30.8` |
| Fox News | Skip ads (experimental), Opt out of data sale & sharing, Block analytics & tracking (opt-in) | `5.49.2` |
| MS NOW | Opt out of data sale & sharing, Block analytics & tracking (opt-in) | `14.6.0` |
| CNN | Skip ads (experimental), Opt out of data sale & sharing, Remove social sign-in, Block analytics & tracking (opt-in) | `26.21.0` |

## How to install

1. Download the phone build of the app from APKMirror. Match the exact version in the table above, and download the `.apkm` bundle (Prime Video and The Wall Street Journal ship as a single `.apk`). Fox One is not on APKMirror. Install it from the Play Store on your phone and use the installed copy. The same works for the news apps.
2. In Morphe Manager, add this project's `.mpp` file as a patch source.
3. Select the `.apkm` file and apply the patches.

## What the patches do

**Skip ads** stops the app from loading ad breaks, server-stitched ad streams, and pause-screen ads. On Pluto TV, it removes on-demand ads only. Live TV ads are part of the broadcast. On Paramount+, live sports breaks are replaced with Paramount's "Commercial in Progress" card. On AMC+, it plays videos without the stitched-in ad breaks, which also removes the interactive true[X] and Brightline ads. On Apple TV, it stops the ad interstitials in on-demand videos, along with any promos delivered the same way. On Hallmark+, it stops FreeWheel ad breaks and pause-screen ads, but ads that the server stitches into a stream remain. On PBS, it stops the sponsor messages before and during videos. On Fox News, it removes banner, feed, sponsored, and full-screen ads, and plays videos without inserted ad breaks. On CNN, it removes banner and sponsored-content ads and plays article videos and clips without ads. All Access streaming is not changed. Samsung TV Plus has no Skip ads patch: its ads are stitched into the stream with no marker the app can detect.

**Mask live ad breaks** (Pluto TV) covers live ads with a black screen and mutes them, then restores the show when the break ends. Live ads are part of the broadcast, so they cannot be cut out.

**Block analytics & tracking** is off by default. Crash reporting, sign-in, and playback are left alone. In every app, it also blocks Meta app events and the Meta Pixel, Google Ads conversion tracking, Google Analytics, Optimizely event logging, Bitmovin player analytics, and Mixpanel. Facebook sign-in and sharing keep working.

- On Tubi, it blocks Adjust, Branch, Braze, Nielsen, Conviva, NPAW/Youbora, Mux, Firebase/Google Analytics, Meta app events, and Tubi's own analytics endpoint.
- On Disney+, it blocks Braze, Conviva, Nielsen, and Firebase/Google Analytics. Disney's own analytics use the same servers as sign-in and playback, so they are not blocked.
- On Paramount+, it blocks Braze, Conviva, Nielsen, comScore, Kochava, Adobe Analytics, New Relic, Tealium, and Branch.
- On HBO Max, it blocks Branch, Braze, comScore, Firebase/Google Analytics, AppsFlyer, and MediaMelon.
- On Peacock, it blocks Braze, Conviva, Nielsen, comScore, Adobe Analytics, New Relic, Firebase/Google Analytics, mParticle, and Branch.
- On Prime Video, it stops Amazon's Minerva metrics uploads and Firebase/Google Analytics.
- On Pluto TV, it blocks Kochava, comScore, Nielsen, Adobe Analytics, Conviva, New Relic, Salesforce Marketing Cloud, and Pluto's Tealium tag config.
- On MGM+, it blocks Braze, Kochava, Segment, Nielsen, and Firebase/Google Analytics.
- On Fox One, it blocks AppsFlyer, Braze, Conviva, comScore, Adobe Analytics, Nielsen, Mux, Datadog, Segment, Contentsquare session replay, and Firebase/Google Analytics.
- On The New York Times, it blocks AppsFlyer, Chartbeat, comScore, Datadog, Iterate surveys, and Firebase/Google Analytics. Statsig, which runs the app's feature settings, is left alone.
- On The Washington Post, it blocks AppsFlyer, Chartbeat, comScore, Permutive, Iterable, Airship analytics, and Firebase/Google Analytics. Push notifications are left alone.
- On Hallmark+, it blocks Kochava, Datazoom, and Firebase/Google Analytics.
- On Starz, it blocks AppsFlyer, CleverTap, Leanplum, Braze, Ethnio surveys, and Firebase/Google Analytics.
- On PBS, it blocks AppsFlyer, Mux playback analytics, and Firebase/Google Analytics.
- On Samsung TV Plus, it blocks Braze, Firebase/Google Analytics, and Samsung's own usage and diagnostics logging. Channels and sign-in are left alone.
- On Fox News, it blocks Adobe, AppsFlyer, Chartbeat, comScore, Nielsen, Firebase/Google Analytics, and Fox's own ad telemetry and ad-ID graph. Push notifications are left alone.
- On MS NOW, it blocks Adobe, Kochava, Nielsen, comScore, Amplitude, Parse.ly, Iterate, and Firebase/Google Analytics.
- On CNN, it blocks Adobe, AppsFlyer, Chartbeat, comScore, Firebase/Google Analytics, and CNN's own data collection. Push notifications are left alone.
- On AMC+, it blocks AppsFlyer, Braze, mParticle, Nielsen, New Relic, NPAW/Youbora, and Firebase/Google Analytics. TV-provider sign-in through Adobe Pass is left alone.
- On The Wall Street Journal, it blocks Adobe Analytics, AppsFlyer, Braze, comScore, LiveRamp, Parse.ly, Permutive, Snowplow, and Firebase/Google Analytics. Podcasts are left alone.

**Disable Pairip license check** (Fox One and Hallmark+) lets the patched app start. Google's Pairip license check blocks any copy that was not installed from the Play Store, and every patched app counts as one.

**Hide upsell pop-ups** (The New York Times) stops the full-screen subscription takeover and the promo banner at the top of the app.

**Hide marketing pop-ups** (The Washington Post and The Wall Street Journal) stops in-app marketing messages, such as subscription offers. Push notifications still arrive.

**Disable AI** (The Washington Post) is off by default. It turns off Ask The Post AI, AI article summaries, and the Ask The Post questions shown under articles.

**Opt out of data sale & sharing** (Fox News, MS NOW, and CNN) sets the app's privacy choice to "do not sell or share" and opts out of targeted advertising, as a second layer behind Block analytics & tracking. Ad and measurement partners receive the US Privacy opt-out signal. On Fox News and MS NOW, this is the starting choice and you can change it in the app's privacy settings. On MS NOW, it replaces the "Allow All" that the app saves for you after onboarding. On CNN, the opt-out stays on.

**Hide device identifiers** is off by default and works on every app here. It gives the app a random Android ID, generated once per install, instead of your device's. It also reports a zeroed advertising ID with ad tracking limited, the same as deleting your ad ID in Android settings. The video DRM ID is left alone so playback keeps working.

**Disable auto-updates** is off by default and works on every app here except The Washington Post, whose resources the patcher cannot rebuild. It stops the Play Store from silently replacing the patched app with the official version. On Tubi, it is always applied with Skip ads.

**Skip pre-roll promo** removes the app's own promo that plays before a title. On MGM+, it jumps past the 20-30 second self-promo to the start of the main content, and only while that promo is playing, so normal titles and next-episode binge are left alone. On Starz, titles start without the promo for other Starz shows, so there is nothing to skip.

**Skip lion intro** (MGM+ only) opens the app straight after the MGM+ logo, without the lion video.

**Remove social sign-in** removes the options to sign in with a Google, Facebook, or Apple account, so the app cannot link to one. Email sign-in, TV-provider sign-in (including YouTube TV as your provider), and subscriber sign-in keep working. It covers Tubi, Starz, MGM+, CNN, HBO Max, and The New York Times. On MGM+, it also stops Google's sign-in sheet from opening on its own at startup, and Login with Amazon stays for Amazon-billed subscribers. An account that was created with Google or Facebook needs a password reset to sign in by email. PBS, Fox News, The Wall Street Journal, and The Washington Post show their social options on their own sign-in web pages, which a patch cannot change.

**Hide sign-in prompts** (Tubi only) stops the request to sign in to save your progress when you pause or leave a title. You can still sign in from the account screen.

**Disable Xfinity auto sign-in** (Peacock only) stops the app from signing you in on its own when it detects an Xfinity home network. Signing in with your own Peacock account still works.

**Disable MGM+ AI** (MGM+ only) is off by default. It turns off the "MGM+ AI" chat assistant: the "Get Personalized Pick" and "Not sure what to watch?" prompts, the AI chat itself, and the AI-picks rows on the Home screen. It forces the feature's flags off whatever the server sends, and changes nothing else.

Everything runs on the device, inside the patched app. The patches do not send data anywhere.

## License

GPL-3.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for credits.
