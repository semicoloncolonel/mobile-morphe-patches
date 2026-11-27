package semicoloncolonel.mobilestream.extension.nytimes.misc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * "Library pass reminder" for The New York Times (opt-in, off by default).
 *
 * The patron reads the NYT through a library pass that attaches to their NYT
 * account and lasts 72 hours, so it must be re-redeemed every three days at
 * the library's EZproxy link. When the pass lapses the app shows its gateway
 * (its paywall / meter / regiwall card). The patch calls {@link #onGatewayShown}
 * from that gateway, and this shows a short dialog whose button opens the
 * library's redeem link in a browser, so the patron re-runs the proper redeem
 * flow in one tap instead of hunting for the URL.
 *
 * This does NOT remove, suppress, or weaken the paywall: the gateway still
 * shows and the article stays locked until the patron completes the real
 * redeem. The dialog is an additional convenience overlay and nothing more.
 *
 * It fires only when: a library URL is configured, the device is online (the
 * browser would fail offline anyway), and the reminder has not been shown
 * within the cooldown window. The fragment is passed as an Object and its
 * activity is read by reflection, so the extension needs no AndroidX
 * dependency and does not break when the app's Fragment subclass is renamed.
 */
@SuppressWarnings("unused")
public final class LibraryPassReminder {
    private static final String TAG = "MSP-NYT-LIBPASS";
    private static final String PREFS = "msp_nyt_library_pass";
    private static final String KEY_LAST_SHOWN = "last_shown";
    // At most one reminder every six hours, so hitting the wall repeatedly
    // does not stack dialogs.
    private static final long COOLDOWN_MS = 6L * 60L * 60L * 1000L;

    private LibraryPassReminder() {
    }

    /**
     * The library redeem URL. Overwritten by the patch with the value from the
     * "Library auth URL" option. Blank (the default here) leaves the feature
     * inert, which is why the patch is safe to apply even before the URL is set.
     */
    public static String renewUrl() {
        return "";
    }

    /** Injection point: GatewayFragmentCard.onActivityCreated. The argument is the fragment. */
    public static void onGatewayShown(Object fragment) {
        try {
            final String url = renewUrl();
            if (url == null || url.trim().isEmpty()) {
                return;
            }

            final Activity activity = activityOf(fragment);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            // An offline gateway means no network; opening the redeem page
            // would fail, so say nothing.
            if (!isOnline(activity)) {
                return;
            }

            final SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            final long now = System.currentTimeMillis();
            if (now - prefs.getLong(KEY_LAST_SHOWN, 0L) < COOLDOWN_MS) {
                return;
            }
            prefs.edit().putLong(KEY_LAST_SHOWN, now).apply();

            final String target = url.trim();
            // A short delay lets the gateway finish starting, so the dialog
            // uses the right light or dark theme.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle("Library pass")
                        .setMessage("Your library pass may have lapsed. Renew it to keep reading.")
                        .setPositiveButton("Renew", (dialog, which) -> open(activity, target))
                        .setNegativeButton("Not now", null)
                        .setCancelable(true)
                        .show();
            }, 150L);
        } catch (Throwable t) {
            Log.i(TAG, "Library pass reminder skipped", t);
        }
    }

    private static Activity activityOf(Object fragment) {
        if (fragment == null) {
            return null;
        }
        try {
            Method getActivity = fragment.getClass().getMethod("getActivity");
            Object activity = getActivity.invoke(fragment);
            return (activity instanceof Activity) ? (Activity) activity : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return true;
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Throwable t) {
            // If the check itself fails, do not block the reminder.
            return true;
        }
    }

    private static void open(Activity activity, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            Log.i(TAG, "Could not open the renew URL", t);
        }
    }
}
