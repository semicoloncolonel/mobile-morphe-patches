package semicoloncolonel.mobilestream.extension.paramount.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import semicoloncolonel.mobilestream.extension.shared.misc.AdBreakText;

/**
 * Paramount+ phone and TV: masks live ad breaks with a black screen and
 * muted audio, the same design as Pluto TV's mask.
 *
 * Live channels (CBS News, CBS Sports, local stations) use the Avia player's
 * ID3 ad tags to signal ad segments. Each tag is a tick: the first raises
 * the mask, later ones keep it up, and the mask lifts when ticks stop.
 *
 * Muting uses Avia's own mute(boolean), which is un-obfuscated. The media
 * stream volume is zeroed separately so the user hears nothing even if Avia
 * unmutes for some reason.
 *
 * Runtime switches (in the app's external files dir):
 *   "slate_off"            — disables the mask entirely
 *   "paramount_slate_mode" — "black" for cover only, "mute" for mute only
 */
@SuppressWarnings("unused")
public final class ParamountLiveMask {
    private static final String TAG = "MSP-P+-LIVE";
    private static final String DISABLE_MARKER = "slate_off";
    private static final String MODE_MARKER = "paramount_slate_mode";
    private static final long HIDE_DELAY_MS = 6_500L;
    private static final long AD_END_GRACE_MS = 2_500L;
    private static final java.util.regex.Pattern TAG_FIELDS = java.util.regex.Pattern.compile(
            "creativeId=([^,]*), currentIndexSec=(\\d+), maximumIndexSec=(\\d+), typeFlags=\\[([^\\]]*)\\]");
    private static final long FAILSAFE_MS = 15L * 60L * 1000L;

    private enum Mode { BOTH, BLACK, MUTE }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static boolean callbacksRegistered;
    private static volatile Object aviaPlayer;

    private static FrameLayout cover;
    private static boolean shown;
    private static boolean aviaMuted;
    private static boolean streamMuted;
    private static int savedStreamVolume = -1;

    private static final class Lift implements Runnable {
        private final String reason;

        Lift(String reason) {
            this.reason = reason;
        }

        @Override
        public void run() {
            lift(reason);
        }
    }

    private static final class Show implements Runnable {
        private final long hideAfterMs;

        Show(long hideAfterMs) {
            this.hideAfterMs = hideAfterMs;
        }

        @Override
        public void run() {
            showOrKeep(hideAfterMs);
        }
    }

    private static final Runnable HIDE = new Lift("no ad ticks, break ended");
    private static final Runnable FAILSAFE = new Lift("failsafe");

    private ParamountLiveMask() {
    }

    public static void registerActivity(Activity activity) {
        if (activity == null) return;
        activityRef = new WeakReference<>(activity);
        if (!callbacksRegistered) {
            try {
                activity.getApplication().registerActivityLifecycleCallbacks(new Tracker());
                callbacksRegistered = true;
            } catch (Throwable t) {
                Log.w(TAG, "could not track activities: " + t);
            }
        }
    }

    private static final class Tracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (activityRef.get() == activity) MAIN.post(new Lift("activity paused"));
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (activityRef.get() == activity) activityRef = new WeakReference<>(null);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    public static void onPlayerPause(Activity activity) {
        MAIN.post(new Lift("player paused"));
    }

    public static void unregisterActivity(Activity activity) {
        if (activityRef.get() == activity) {
            MAIN.post(new Lift("player closed"));
            activityRef = new WeakReference<>(null);
        }
    }

    public static void setAviaPlayer(Object player) {
        aviaPlayer = player;
    }

    public static void onAdTick(Object id3Tag) {
        long keepMs = HIDE_DELAY_MS;
        try {
            java.util.regex.Matcher m = TAG_FIELDS.matcher(String.valueOf(id3Tag));
            if (!m.find() || m.group(1).trim().isEmpty() || "null".equals(m.group(1).trim())) return;
            long current = Long.parseLong(m.group(2));
            long maximum = Long.parseLong(m.group(3));
            String flags = m.group(4);
            if (flags.contains("QUARTILE_END") || flags.contains("END_OF_MEDIA")) {
                keepMs = AD_END_GRACE_MS;
            } else if (maximum > current) {
                keepMs = Math.max(HIDE_DELAY_MS, (maximum - current) * 1000 + AD_END_GRACE_MS);
            }
        } catch (Throwable t) {
            Log.w(TAG, "could not read ad tag timing: " + t);
        }
        MAIN.post(new Show(keepMs));
    }

    private static void showOrKeep(long hideAfterMs) {
        Mode mode = readMode();
        if (mode == null) {
            if (shown) lift("disabled by marker file");
            return;
        }
        MAIN.removeCallbacks(HIDE);
        MAIN.postDelayed(HIDE, hideAfterMs);
        if (!shown) {
            MAIN.removeCallbacks(FAILSAFE);
            MAIN.postDelayed(FAILSAFE, FAILSAFE_MS);
        }
        if (mode != Mode.BLACK) mute();
        if (mode != Mode.MUTE) showCover();
        if (!shown) Log.i(TAG, "live ad break: mask on (" + mode + ")");
        Log.d(TAG, "ad tick: holding the mask for " + hideAfterMs + "ms");
        shown = true;
    }

    private static void lift(String reason) {
        MAIN.removeCallbacks(HIDE);
        MAIN.removeCallbacks(FAILSAFE);
        if (!shown) return;
        removeCover();
        unmute();
        shown = false;
        Log.i(TAG, "live ad break: mask off (" + reason + ")");
    }

    private static void showCover() {
        if (cover != null) return;
        Activity activity = activityRef.get();
        if (activity == null) {
            Log.w(TAG, "no player activity for the cover");
            return;
        }
        try {
            ViewGroup root = activity.findViewById(android.R.id.content);
            if (root == null) return;
            FrameLayout view = new FrameLayout(activity);
            view.setBackgroundColor(Color.BLACK);
            view.setClickable(false);
            view.setFocusable(false);
            view.setElevation(1_000_000f);
            view.setTranslationZ(1_000_000f);
            TextView label = new TextView(activity);
            label.setText(AdBreakText.untimed());
            label.setTextColor(Color.LTGRAY);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            label.setGravity(Gravity.CENTER);
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 32, activity.getResources().getDisplayMetrics());
            label.setPadding(padding, padding, padding, padding);
            view.addView(label, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(view, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.bringToFront();
            cover = view;
        } catch (Throwable t) {
            Log.w(TAG, "cover failed: " + t);
        }
    }

    private static void removeCover() {
        if (cover != null && cover.getParent() instanceof ViewGroup) {
            ((ViewGroup) cover.getParent()).removeView(cover);
        }
        cover = null;
    }

    private static void mute() {
        if (!streamMuted) {
            AudioManager am = audioManager();
            if (am != null) {
                try {
                    savedStreamVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    streamMuted = true;
                } catch (Throwable t) {
                    Log.w(TAG, "stream mute failed: " + t);
                }
            }
        }
        if (!aviaMuted && !isAviaMuted() && setAviaMute(true)) aviaMuted = true;
    }

    private static void unmute() {
        if (aviaMuted) {
            setAviaMute(false);
            aviaMuted = false;
        }
        if (streamMuted) {
            AudioManager am = audioManager();
            if (am != null && savedStreamVolume >= 0) {
                try {
                    if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, savedStreamVolume, 0);
                    }
                } catch (Throwable ignored) {
                }
            }
            streamMuted = false;
            savedStreamVolume = -1;
        }
    }

    private static boolean isAviaMuted() {
        Object player = aviaPlayer;
        if (player == null) return false;
        try {
            Object muted = player.getClass().getMethod("isMuted").invoke(player);
            return Boolean.TRUE.equals(muted);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean setAviaMute(boolean mute) {
        Object player = aviaPlayer;
        if (player == null) return false;
        try {
            Method m = player.getClass().getMethod("mute", boolean.class);
            m.invoke(player, mute);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Avia mute(" + mute + ") failed: " + t);
            return false;
        }
    }

    private static AudioManager audioManager() {
        Activity activity = activityRef.get();
        if (activity == null) return null;
        try {
            return (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Mode readMode() {
        Activity activity = activityRef.get();
        File dir = null;
        try {
            if (activity != null) dir = activity.getExternalFilesDir(null);
        } catch (Throwable ignored) {
        }
        if (dir == null) return Mode.BOTH;
        try {
            if (new File(dir, DISABLE_MARKER).exists()) return null;
            File modeFile = new File(dir, MODE_MARKER);
            if (modeFile.exists()) {
                byte[] bytes = new byte[16];
                int n;
                try (java.io.FileInputStream in = new java.io.FileInputStream(modeFile)) {
                    n = in.read(bytes);
                }
                String token = n > 0 ? new String(bytes, 0, n).trim().toLowerCase() : "";
                if ("black".equals(token)) return Mode.BLACK;
                if ("mute".equals(token)) return Mode.MUTE;
            }
        } catch (Throwable ignored) {
        }
        return Mode.BOTH;
    }
}
