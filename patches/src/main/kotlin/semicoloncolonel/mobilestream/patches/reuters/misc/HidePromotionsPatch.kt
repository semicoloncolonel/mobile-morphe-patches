package semicoloncolonel.mobilestream.patches.reuters.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import semicoloncolonel.mobilestream.patches.reuters.shared.Constants
import semicoloncolonel.mobilestream.patches.shared.misc.HERMES_BUNDLE
import semicoloncolonel.mobilestream.patches.shared.misc.HermesBundle

// Reuters 7.51.0.
//
//   SubscribeButton,              the Subscribe button in the header and the
//   useEnableSubscribeButton      pull-up it opens. The button's remote switch
//                                 (enable_today_subscribe) also answers false.
//   PreferenceCard,               the "Turn on notifications" and topic cards
//   TopicEngagementBlock          placed inside articles.
//   NewsletterArticleCard,        the newsletter sign-ups inside articles. The
//   NewsletterCard                NewsletterCard that subscribes (it loads
//                                 SUBSCRIBE_TO_NEWSLETTERS) is changed; the one
//                                 in the newsletter settings list is not.
//   parseArticleNotificationData  the notification block at the end of an
//                                 article, now always absent.
//   parseArticleDetail            the newsletter list at the end of an article
//                                 is dropped (config.newsletters reads as off),
//                                 and the newsletter card is not copied.
//
//   Breakingviews banner          the "breaking-views" promo in the Today and
//                                 section feeds and in topic feeds: the jsx
//                                 call that renders it becomes LoadConstNull.
//   Braze in-app messages         marketing pop-ups. Braze's default listener
//                                 now discards each message; Braze still
//                                 delivers push alerts.
//
// The cards render nothing. Topic tags and Read next stay.

private const val VERSION = 96

private fun ByteArray.of(vararg values: Int) = values.map { it.toByte() }.toByteArray()

private const val IN_APP_OPERATION = "Lcom/braze/ui/inappmessage/InAppMessageOperation;"

internal object BrazeBeforeInAppMessageDisplayedFingerprint : Fingerprint(
    definingClass = "Lcom/braze/ui/inappmessage/listeners/DefaultInAppMessageManagerListener;",
    name = "beforeInAppMessageDisplayed",
    returnType = IN_APP_OPERATION,
)

private val discardBrazeMessagesPatch = bytecodePatch {
    execute {
        BrazeBeforeInAppMessageDisplayedFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $IN_APP_OPERATION->DISCARD:$IN_APP_OPERATION
                return-object v0
            """,
        )
    }
}

// The Breakingviews banner branch: LoadConstString "breaking-views",
// PutNewOwnById "variant", CreateClosure onPress, PutNewOwnByIdShort
// "onPress", then Call3 jsx and Ret. Only the Call3 changes, to three
// LoadConstNull into its result register.
private fun bannerEdit(prefix: List<Int>, register: Int, jsx: List<Int>) =
    byteArrayOf().of(*(prefix + listOf(84, register) + jsx + listOf(92, register)).toIntArray()) to
        byteArrayOf().of(*(prefix + listOf(119, register, 119, register, 119, register, 92, register)).toIntArray())

@Suppress("unused")
val hidePromotionsPatch = rawResourcePatch(
    name = "Hide promotions",
    description = "Removes the Subscribe button, the Breakingviews promo banners, marketing pop-ups, and the " +
        "newsletter sign-ups and notification prompts in articles and feeds.",
) {
    category("Interface")
    compatibleWith(Constants.COMPATIBILITY)

    dependsOn(discardBrazeMessagesPatch)

    execute {
        val file = get(HERMES_BUNDLE)
        val bundle = HermesBundle(file.readBytes())
        bundle.returnAtStart("SubscribeButton", HermesBundle.Value.NULL)
        bundle.returnAtStart("useEnableSubscribeButton", HermesBundle.Value.FALSE)
        bundle.returnAtStart("PreferenceCard", HermesBundle.Value.NULL)
        bundle.returnAtStart("TopicEngagementBlock", HermesBundle.Value.NULL)
        bundle.returnAtStart("NewsletterArticleCard", HermesBundle.Value.NULL)
        bundle.returnAtStart("NewsletterCard", "SUBSCRIBE_TO_NEWSLETTERS", HermesBundle.Value.NULL)
        bundle.returnAtStart("parseArticleNotificationData", HermesBundle.Value.UNDEFINED)

        val article = listOf("parseArticleNotificationData")
        // GetById r14, r10, "newsletters"; Neq r16, r14, r4 -> LoadConstFalse r16 (twice),
        // so the following JmpFalse leaves newsletters undefined.
        val newsletters = bundle.stringId("newsletters")
        bundle.replaceInstructions(
            "parseArticleDetail", article, VERSION,
            byteArrayOf().of(55, 14, 10, 44, newsletters and 0xFF, newsletters shr 8, 16, 16, 14, 4),
            byteArrayOf().of(55, 14, 10, 44, newsletters and 0xFF, newsletters shr 8, 121, 16, 121, 16),
        )
        // GetById r3, r1, "newsletterCard" -> LoadConstUndefined r3 (three times).
        val newsletterCard = bundle.stringId("newsletterCard")
        bundle.replaceInstructions(
            "parseArticleDetail", article, VERSION,
            byteArrayOf().of(55, 3, 1, 33, newsletterCard and 0xFF, newsletterCard shr 8),
            byteArrayOf().of(118, 3, 118, 3, 118, 3),
        )
        val banners = listOf("breakingviews-promo-banner", "breaking-views")
        // Today and section feeds.
        val (feedFind, feedReplace) = bannerEdit(
            listOf(0x73, 0x0b, 0xcc, 0xaf, 0x40, 0x04, 0x0b, 0x5b, 0x67, 0x64, 0x0b, 0x01, 0xef, 0x7e, 0x3f, 0x04, 0x0b, 0xc9),
            4, listOf(6, 3, 5, 4),
        )
        bundle.replaceInstructions("", banners + "commentary.highlight.click", VERSION, feedFind, feedReplace)
        // Topic feeds.
        val (topicFind, topicReplace) = bannerEdit(
            listOf(0x73, 0x0d, 0xcc, 0xaf, 0x40, 0x05, 0x0d, 0x5b, 0x67, 0x64, 0x0d, 0x09, 0x4d, 0x85, 0x3f, 0x05, 0x0d, 0xc9),
            5, listOf(8, 3, 7, 5),
        )
        bundle.replaceInstructions("", banners + "ExploreLinks", VERSION, topicFind, topicReplace)
        file.writeBytes(bundle.toByteArray())
    }
}
