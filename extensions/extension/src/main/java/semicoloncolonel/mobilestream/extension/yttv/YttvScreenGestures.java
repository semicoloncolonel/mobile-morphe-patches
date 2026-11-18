package semicoloncolonel.mobilestream.extension.yttv;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Volume and brightness swipe sliders for the YouTube TV player.
 *
 * Called from the player gesture handler's onScroll. A vertical drag on the
 * right half of the player changes the media volume; a vertical drag on the
 * left half changes the screen brightness. Brightness is set on the app's own
 * window only, so it does not change the system setting or any other app. All
 * the way down means mute for volume and automatic (follow the system) for
 * brightness.
 *
 * Returns true when it has handled the drag, so the caller consumes it and does
 * not run its normal behavior. Horizontal and near-horizontal drags are left
 * for the player's own gestures.
 */
public final class YttvScreenGestures {

    // Fraction of the view height a drag must cover before it counts as a
    // deliberate vertical gesture rather than a tap or a scrub.
    private static final float SLOP = 0.02f;

    private static long gestureId = -1;
    private static boolean committed;
    private static boolean handled;
    private static boolean leftSide;
    private static float level; // 0..1 working value for the side in use

    private static AudioManager audioManager;
    private static int maxVolume;
    private static Activity activity;

    private YttvScreenGestures() {}

    public static boolean onScroll(View view, MotionEvent down, MotionEvent current,
                                   float distanceX, float distanceY) {
        try {
            if (view == null || down == null || current == null) return false;

            long id = down.getDownTime();
            if (id != gestureId) {
                gestureId = id;
                committed = false;
                handled = false;
            }

            if (!committed) {
                committed = true;
                float dx = Math.abs(current.getX() - down.getX());
                float dy = Math.abs(current.getY() - down.getY());
                int height = view.getHeight();
                if (height <= 0 || dy < height * SLOP || dy <= dx) {
                    handled = false; // horizontal or too small: not ours
                    return false;
                }
                handled = true;
                leftSide = down.getX() < view.getWidth() / 2f;

                Context context = view.getContext();
                activity = findActivity(context);
                if (leftSide) {
                    if (activity == null) { handled = false; return false; }
                    level = currentBrightness(activity);
                } else {
                    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager == null) { handled = false; return false; }
                    maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    level = maxVolume > 0
                        ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) maxVolume
                        : 0f;
                }
            }

            if (!handled) return false;

            int height = view.getHeight();
            if (height <= 0) return true;
            // distanceY is positive when the finger moves up; moving up raises the level.
            level += distanceY / height;
            if (level < 0f) level = 0f;
            if (level > 1f) level = 1f;

            if (leftSide) {
                applyBrightness(activity, level);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    Math.round(level * maxVolume), AudioManager.FLAG_SHOW_UI);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void applyBrightness(Activity activity, float level) {
        if (activity == null) return;
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        // All the way down follows the system (automatic) brightness.
        lp.screenBrightness = level <= 0f
            ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            : level;
        activity.getWindow().setAttributes(lp);
    }

    private static float currentBrightness(Activity activity) {
        float b = activity.getWindow().getAttributes().screenBrightness;
        return b < 0f ? 0f : b; // treat "follow system" as the bottom of the range
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
