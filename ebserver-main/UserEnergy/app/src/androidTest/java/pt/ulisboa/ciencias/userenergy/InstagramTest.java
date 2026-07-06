package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import pt.ulisboa.ciencias.userenergy.experiments.InstagramActivity;

@RunWith(AndroidJUnit4.class)
public class InstagramTest extends BaseWriteTest {

    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";

    // Local defaults
    private static final long WATCH_DURATION_MS     = 30 * 1000; // 30s total
    private static final long SCROLL_INTERVAL_MS    = 5000;      // scroll every 5s
    private static final long LIKE_INTERVAL_MS      = 10000;     // like every 10s

    @Rule
    public ActivityScenarioRule<InstagramActivity> activityScenarioRule =
            new ActivityScenarioRule<>(InstagramActivity.class);

    @Test
    public void runtest() throws Exception {
            final long watchDurationMs  = getLongArg("duration_ms",        WATCH_DURATION_MS);
            final long scrollEveryMs    = getLongArg("scroll_interval_ms", SCROLL_INTERVAL_MS);
            final long likeEveryMs      = getLongArg("like_interval_ms",   LIKE_INTERVAL_MS);


        ActivityScenario<InstagramActivity> scenario = activityScenarioRule.getScenario();

        scenario.onActivity(activity -> {
            activity.setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // Open Instagram
            try {
                activity.getClass().getMethod("openInstagram").invoke(activity);
            } catch (Exception ignored) { }

            Handler h = new Handler();

            // Repeating scroller
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
                        device.swipe(startX, startY, startX, endY, 18);
                    } catch (Exception ignored) { }
                    new Handler().postDelayed(this, scrollEveryMs);
                }
            };
            h.postDelayed(scroller, scrollEveryMs);

            // Repeating "Like" by pressing the heart button
            Runnable liker = new Runnable() {
                @Override public void run() {
                    try {
                        UiDevice device = UiDevice.getInstance(
                                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());

                        // Only act if Instagram is foreground
                        if (!INSTAGRAM_PACKAGE.equals(device.getCurrentPackageName())) {
                            new Handler().postDelayed(this, likeEveryMs);
                            return;
                        }

                        // Try typical Instagram heart/like button resource IDs
                        UiObject likeButton = device.findObject(
                                new UiSelector().resourceIdMatches(".*(row_feed_button_like|like_button).*"));

                        // Fallbacks: content-desc and text containing "Like"
                        if (!likeButton.exists()) {
                            likeButton = device.findObject(
                                    new UiSelector().descriptionContains("Like"));
                        }
                        if (!likeButton.exists()) {
                            likeButton = device.findObject(
                                    new UiSelector().textContains("Like"));
                        }

                        // Only click if we actually found a likely like button
                        if (likeButton.exists()) {
                            likeButton.click();
                        }

                    } catch (UiObjectNotFoundException ignored) { }

                    new Handler().postDelayed(this, likeEveryMs);
                }
            };
            h.postDelayed(liker, likeEveryMs);
        });

        // Let it run for the duration
        Thread.sleep(watchDurationMs);

        // Bring host activity to foreground (deterministic end state)
        scenario.onActivity(activity -> {
            Intent intent = new Intent(activity, InstagramActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
        });

        // Force-stop Instagram
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            device.executeShellCommand("am force-stop " + INSTAGRAM_PACKAGE);
        } catch (IOException ignored) { }
    }
        // Helper to read instrumentation args with defaults
        private long getLongArg(String key, long def) {
        try {
            String v = InstrumentationRegistry.getArguments().getString(key, null);
            return (v == null || v.isEmpty()) ? def : Long.parseLong(v);
            } catch (Exception e) { return def; }
    }

}

