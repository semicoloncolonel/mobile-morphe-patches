package semicoloncolonel.mobilestream.extension.yttv;

import android.view.MotionEvent;

import java.lang.reflect.Method;

/**
 * Press and hold to speed up the YouTube TV player.
 *
 * The active player controller is captured as it is handed to the media layer.
 * A long press on the player raises the playback rate to the chosen speed, and
 * letting go returns it to normal. The controller's setPlaybackRate is called
 * by reflection, because its class is part of the app and not on this module's
 * classpath.
 */
public final class YttvPlaybackSpeed {

    private static volatile Object controller;
    private static volatile Method setRate;
    private static volatile boolean holding;

    private YttvPlaybackSpeed() {}

    // Called from the media layer when the playback controller is set.
    public static void setController(Object playbackController) {
        controller = playbackController;
        Method method = null;
        if (playbackController != null) {
            try {
                method = playbackController.getClass().getMethod("setPlaybackRate", float.class);
            } catch (Throwable t) {
                method = null;
            }
        }
        setRate = method;
    }

    // Called from the player gesture handler's onLongPress.
    public static void onLongPress(float rate) {
        holding = true;
        apply(rate);
    }

    // Called from the player gesture detector's onTouchEvent, for every event.
    public static void onTouch(MotionEvent event) {
        if (!holding || event == null) return;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            holding = false;
            apply(1f);
        }
    }

    private static void apply(float rate) {
        try {
            Object c = controller;
            Method m = setRate;
            if (c != null && m != null) m.invoke(c, Float.valueOf(rate));
        } catch (Throwable t) {
            // The controller went away or changed shape; ignore.
        }
    }

    // Called from the media layer when a playback controller is set, to start
    // every video at a chosen speed. Used by "Set playback speed".
    public static void applyDefault(Object playbackController, float rate) {
        try {
            if (playbackController != null && rate > 0f) {
                playbackController.getClass()
                    .getMethod("setPlaybackRate", float.class)
                    .invoke(playbackController, Float.valueOf(rate));
            }
        } catch (Throwable t) {
            // The controller is not the shape we expect; ignore.
        }
    }
}
