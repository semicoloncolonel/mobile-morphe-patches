package semicoloncolonel.mobilestream.extension.kouponai.web;

import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import semicoloncolonel.mobilestream.extension.shared.privacy.TrackerBlocker;

/**
 * Koupon: filter for the requests made by the app's web pages.
 *
 * A deal page in Koupon 3.1.6 is a web page (cross-end.koupon.ai/product/detail)
 * in React Native's web view, with the account's Firebase UID in its URL. The
 * page's own requests never reach React Native's networking, so the web view
 * client gets a shouldInterceptRequest that asks {@link #intercept} first.
 *
 * Three switches, each turned on by its own patch (their bodies are replaced
 * to return true):
 *
 *   {@link #blockTrackers()}   the shared tracker list, and Intercom's
 *                              messenger metrics.
 *   {@link #stopHistory()}     POST /api/browse-history/add-history, which
 *                              records each deal you open, with its categories
 *                              and title keywords, for your History and for
 *                              personalized ranking.
 *   {@link #hideAi()}          /api/deal/ai-evaluation and
 *                              /api/deal/ai-evaluation-enabled, the AI
 *                              evaluation section of a deal page.
 *
 * A blocked request gets an empty 404, which the page treats like a failed
 * request. Everything is decided on the device.
 */
@SuppressWarnings("unused")
public final class KouponWebFilter {
    private static final String TAG = "MSP-KOUPON";

    private static final Set<String> logged = Collections.synchronizedSet(new HashSet<>());

    private KouponWebFilter() {
    }

    /** Overwritten to return true by "Block analytics & tracking". */
    public static boolean blockTrackers() {
        return false;
    }

    /** Overwritten to return true by "Stop behavioral tracking". */
    public static boolean stopHistory() {
        return false;
    }

    /** Overwritten to return true by "Disable AI". */
    public static boolean hideAi() {
        return false;
    }

    private static boolean isKoupon(String host) {
        return host.equals("koupon.ai") || host.endsWith(".koupon.ai");
    }

    private static String reason(String host, String path) {
        if (blockTrackers()) {
            if (TrackerBlocker.isBlocked(host, path)) return "tracker";
            if (host.endsWith("intercom.io") && path.startsWith("/messenger/web/metrics")) return "tracker";
        }
        if (isKoupon(host)) {
            if (stopHistory() && path.contains("/browse-history/")) return "history";
            if (hideAi() && path.contains("/deal/ai-evaluation")) return "AI";
        }
        return null;
    }

    /** Called from the web view client's added shouldInterceptRequest. */
    public static WebResourceResponse intercept(WebResourceRequest request) {
        try {
            if (request == null || request.getUrl() == null) return null;
            Uri url = request.getUrl();
            String host = url.getHost() == null ? "" : url.getHost().toLowerCase();
            String path = url.getPath() == null ? "" : url.getPath();
            String reason = reason(host, path);
            if (reason == null) return null;
            if (logged.add(host + path)) {
                Log.i(TAG, "blocked " + reason + " request -> " + host + path);
            }
            return new WebResourceResponse("application/json", StandardCharsets.UTF_8.name(), 404, "Not Found",
                    Collections.singletonMap("Access-Control-Allow-Origin", "*"),
                    new ByteArrayInputStream(new byte[0]));
        } catch (Throwable t) {
            return null;
        }
    }
}
