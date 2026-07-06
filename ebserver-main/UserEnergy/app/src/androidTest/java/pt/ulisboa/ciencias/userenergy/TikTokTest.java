package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import pt.ulisboa.ciencias.userenergy.experiments.TikTokActivity;

@RunWith(AndroidJUnit4.class)
public class TikTokTest extends BaseWriteTest {

    private static final String TIKTOK_PACKAGE = "com.zhiliaoapp.musically";

    // Defaults for local testing
    private static final long WATCH_DURATION_MS = 2 * 60 * 1000; // 2 minutes
    private static final boolean AUTOSCROLL = true;              // enable auto scroll
    private static final long AUTOSCROLL_INTERVAL_MS = 15000;    // every 15 seconds

    @Rule
    public ActivityScenarioRule<TikTokActivity> activityScenarioRule =
            new ActivityScenarioRule<>(TikTokActivity.class);

    @Test
    public void runtest() throws Exception {

        final long watchDurationMs   = getLongArg("duration_ms", WATCH_DURATION_MS);
        final boolean autoScroll     = getBoolArg("autoscroll", AUTOSCROLL);
        final long autoScrollEveryMs = getLongArg("autoscroll_interval_ms", AUTOSCROLL_INTERVAL_MS);

        ActivityScenario<TikTokActivity> scenario = activityScenarioRule.getScenario();

        scenario.onActivity(activity -> {
            activity.setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // Open TikTok via helper
            try {
                activity.getClass().getMethod("openTikTok").invoke(activity);
            } catch (Exception ignored) { }

            Handler h = new Handler();

            // Helper lambda for center tap (used for mid-run pause/play)
            Runnable tapCenter = () -> {
                UiDevice device = UiDevice.getInstance(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());
                Point size = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(size);
                device.click(size.x / 2, size.y / 2);
            };

            // ⏸️ Mid-run pause/play once: pause at half time, resume autoScrollEveryMs/2s later
            final long midPoint = watchDurationMs / 2;
            h.postDelayed(() -> {
                // Pause
                tapCenter.run();
                // Resume after autoScrollEveryMs/2 seconds
                h.postDelayed(tapCenter, Math.round(autoScrollEveryMs/2));
            }, midPoint);

            // Auto-scroll every Xs
            if (autoScroll) {
                Runnable scroller = new Runnable() {
                    @Override public void run() {
                        try {
                            UiDevice device = UiDevice.getInstance(
                                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());
                            Point size = new Point();
                            activity.getWindowManager().getDefaultDisplay().getSize(size);
                            int startX = size.x / 2;
                            int startY = (int) (size.y * 0.75);
                            int endY   = (int) (size.y * 0.25);
                            device.swipe(startX, startY, startX, endY, 20);
                        } catch (Exception ignored) { }
                        new Handler().postDelayed(this, autoScrollEveryMs);
                    }
                };
                h.postDelayed(scroller, autoScrollEveryMs);
            }
        });

        // Let the test run for the watch duration
        Thread.sleep(watchDurationMs);

        // Bring app back to foreground
        scenario.onActivity(activity -> {
            Intent intent = new Intent(activity, TikTokActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
        });

        // Stop TikTok when done
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            device.executeShellCommand("am force-stop " + TIKTOK_PACKAGE);
            
        } catch (IOException ignored) { }
    }

    // Arg helpers kept for future externalized runs
    private long getLongArg(String key, long def) {
        try {
            String v = InstrumentationRegistry.getArguments().getString(key, null);
            return (v == null || v.isEmpty()) ? def : Long.parseLong(v);
        } catch (Exception e) { return def; }
    }

    private boolean getBoolArg(String key, boolean def) {
        try {
            String v = InstrumentationRegistry.getArguments().getString(key, null);
            if (v == null) return def;
            v = v.trim().toLowerCase();
            if (v.equals("1") || v.equals("true") || v.equals("yes")) return true;
            if (v.equals("0") || v.equals("false") || v.equals("no")) return false;
            return def;
        } catch (Exception e) { return def; }
    }
}
