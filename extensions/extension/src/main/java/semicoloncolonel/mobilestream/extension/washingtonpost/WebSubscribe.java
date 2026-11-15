package semicoloncolonel.mobilestream.extension.washingtonpost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The Washington Post: "Subscribe on the web".
 *
 * A patched app cannot buy through Google Play, which only bills the copy it
 * installed. The paywall's purchase buttons call this instead, which opens the
 * Post's own subscription page in the browser, where buying works.
 */
@SuppressWarnings("unused")
public final class WebSubscribe {
    private static final String TAG = "MSP-SUBSCRIBE";
    private static final String SUBSCRIBE_URL = "https://www.washingtonpost.com/subscribe/";

    private WebSubscribe() {
    }

    /**
     * Called at the start of each purchase method with the Activity and the
     * purchase callback. Opens the page, then tells the callback the purchase
     * was cancelled, so the paywall resets instead of waiting.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void open(Activity activity, Object callback) {
        open(activity);
        if (callback == null) {
            return;
        }
        try {
            // The callback has one method taking the result, whose constructor
            // takes a result code (an enum with RESULT_CANCELED) and a message.
            for (Method method : callback.getClass().getMethods()) {
                if (method.getParameterTypes().length != 1) continue;
                for (Constructor<?> constructor : method.getParameterTypes()[0].getDeclaredConstructors()) {
                    Class<?>[] types = constructor.getParameterTypes();
                    if (types.length != 2 || !types[0].isEnum() || types[1] != String.class) continue;
                    Object cancelled = Enum.valueOf((Class) types[0], "RESULT_CANCELED");
                    constructor.setAccessible(true);
                    method.invoke(callback, constructor.newInstance(cancelled, "Opened the subscription page"));
                    return;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "could not report the purchase as cancelled: " + t);
        }
    }

    public static void open(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUBSCRIBE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            Log.w(TAG, "could not open the subscription page: " + t);
        }
    }
}
