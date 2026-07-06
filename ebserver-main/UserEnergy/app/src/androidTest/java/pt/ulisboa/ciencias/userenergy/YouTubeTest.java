package pt.ulisboa.ciencias.userenergy;

import android.graphics.Rect;
import android.os.RemoteException;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import pt.ulisboa.ciencias.userenergy.experiments.YouTubeActivity;

@RunWith(AndroidJUnit4.class)
public class YouTubeTest extends BaseWriteTest {

    private static final String YOUTUBE_PACKAGE_NAME = "com.google.android.youtube";

    enum Duration {
        S15(15 * 1000), S30(30 * 1000), S60(60 * 1000);
        final int value;
        Duration(int i) { this.value = i; }
    }

    enum Resolution {
        HIGHEST("1440p"), HIGH("1080p"), MEDIUM("720p"), LOW("240p");
        final String value;
        Resolution(String i) { this.value = i; }
    }

    @Rule
    public ActivityScenarioRule<YouTubeActivity> activityScenarioRule =
            new ActivityScenarioRule<>(YouTubeActivity.class);

    @Test
    public void play_video() throws InterruptedException, RemoteException {
        String durationArg = InstrumentationRegistry.getArguments().getString("duration");
        String resolutionArg = InstrumentationRegistry.getArguments().getString("resolution");

        Duration d = Duration.valueOf(durationArg != null ? durationArg.toUpperCase() : "S30");
        Resolution r = Resolution.valueOf(resolutionArg != null ? resolutionArg.toUpperCase() : "HIGHEST");

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Setup
        device.setOrientationNatural();
        ActivityScenario<YouTubeActivity> scenario = activityScenarioRule.getScenario();
        scenario.onActivity(activity -> activity.openYouTubeVideo());

        device.setOrientationLeft(); // Force Landscape
        Thread.sleep(2000);

        // 0. Set volume to ~25% (Stream 3 is Media, max is usually 15. 4 is ~26%)
        try {
            device.executeShellCommand("media volume --show --stream 3 --set 4");
            System.out.println("Set media volume to 4 (~25%).");
        } catch (Exception e) {
            System.out.println("Set Volume Failed: " + e.getMessage());
        }

        // 1. Set Quality
        try {
            setVideoQuality(device, r);
        } catch (Exception e) {
            System.out.println("Set Quality Failed: " + e.getMessage());
        }

        // 2. Skip to 2/3 of the video
        skipToTwoThirds(device);

        // 3. Ensure playing (seeking sometimes pauses)
        ensureVideoPlaying(device);

        // 4. Start Measuring (Sleep for duration)
        System.out.println("Starting measurement for " + d.value + "ms");
        Thread.sleep(d.value);

        // Cleanup
        try {
            device.executeShellCommand("am force-stop " + YOUTUBE_PACKAGE_NAME);
            device.setOrientationNatural();
            device.unfreezeRotation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void skipToTwoThirds(UiDevice device) {
        try {
            // Wake up controls first
            int width = device.getDisplayWidth();
            int height = device.getDisplayHeight();
            device.click(width / 2, height / 2);
            Thread.sleep(500); // Give controls time to fade in

            // Find the SeekBar (Time Bar)
            UiObject seekBar = device.findObject(new UiSelector().resourceId("com.google.android.youtube:id/time_bar_view"));

            if (!seekBar.exists()) {
                seekBar = device.findObject(new UiSelector().className("android.widget.SeekBar"));
            }

            if (seekBar.exists()) {
                Rect bounds = seekBar.getBounds();

                // Start from the beginning of the BAR, not the screen edge (0)
                // Using 0 might trigger the "Back" gesture on Android.
                int startX = bounds.left + 5; // +5 to be safely inside the bar
                int startY = bounds.centerY();

                // Calculate position: Left + (TotalWidth * 0.66)
                int endX = bounds.left + (int)(bounds.width() * 0.66);
                int endY = bounds.centerY();

                // Perform a drag/swipe
                // Steps: 50 makes it slower/smoother (simulates a drag), 20 is a fast flick
                device.swipe(startX, startY, endX, endY, 50);

                System.out.println("Skipped to 2/3 of video.");
                Thread.sleep(2000); // Wait for buffer after seek
            } else {
                System.out.println("Seek bar not found, could not skip.");
            }
        } catch (Exception e) {
            System.out.println("Error skipping video: " + e.getMessage());
        }
    }

    private void setVideoQuality(UiDevice device, Resolution resolution) throws UiObjectNotFoundException, InterruptedException {
        // --- Step 1: Open Settings ---
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        //device.click(width / 2, height / 2);

        UiObject settingsButton = device.findObject(new UiSelector().descriptionContains("Settings"));
        if (!settingsButton.exists()) settingsButton = device.findObject(new UiSelector().descriptionContains("More options"));

        if (!settingsButton.exists()) {
            device.click(width / 2, height / 2);
            Thread.sleep(500);
        }

        if (settingsButton.exists()) {
            settingsButton.click();
            Thread.sleep(1000);
        } else {
            return;
        }

        // --- Step 2: Click 'Quality' (Index 0) ---
        UiObject menuList = device.findObject(new UiSelector().classNameMatches(".*RecyclerView"));

        if (menuList.waitForExists(1000)) {
            UiObject firstItem = menuList.getChild(new UiSelector().index(0));
            if (firstItem.exists()) {
                firstItem.click();
                Thread.sleep(500);
            } else {
                device.pressBack(); return;
            }
        } else {
            device.pressBack(); return;
        }

        // --- Step 3: Click 'Advanced' ---
        UiObject advancedOption = device.findObject(new UiSelector().textContains("Advanced"));
        if (!advancedOption.exists()) advancedOption = device.findObject(new UiSelector().textContains("Avança"));

        if (!advancedOption.exists()) {
            if (menuList.exists()) advancedOption = menuList.getChild(new UiSelector().index(3));
        }

        if (advancedOption.exists()) {
            advancedOption.click();
            Thread.sleep(1000);

            // --- Step 4: Click Resolution by Position ---
            UiScrollable resolutionList = new UiScrollable(new UiSelector().scrollable(true));

            switch (resolution) {
                case HIGHEST:
                    resolutionList.getChild(new UiSelector().index(1)).click();
                    System.out.println("Clicked HIGHEST (Index 1)");
                    break;

                case HIGH:
                    resolutionList.getChild(new UiSelector().index(2)).click();
                    System.out.println("Clicked HIGH (Index 2)");
                    break;

                case MEDIUM:
                    UiObject medItem = resolutionList.getChild(new UiSelector().index(3));
                    if (medItem.exists()) {
                        medItem.click();
                    } else {
                        resolutionList.scrollForward();
                        Thread.sleep(1000);
                        resolutionList.getChild(new UiSelector().index(0)).click();
                    }
                    System.out.println("Clicked MEDIUM");
                    break;

                case LOW:
                    resolutionList.flingToEnd(4);
                    Thread.sleep(1000);
                    int count = resolutionList.getChildCount();
                    if (count > 0) {
                        resolutionList.getChild(new UiSelector().index(count - 1)).click();
                        System.out.println("Clicked LOW (Last Item)");
                    }
                    break;
            }

        } else {
            UiObject highQuality = device.findObject(new UiSelector().textContains("HIGHEST"));
            if (!highQuality.exists()) highQuality = device.findObject(new UiSelector().textContains("Alta"));
            if (highQuality.exists()) highQuality.click();
        }

        Thread.sleep(5000);
    }

    private void ensureVideoPlaying(UiDevice device) {
        UiObject playButton = device.findObject(new UiSelector().descriptionContains("Play"));
        if (!playButton.exists()) playButton = device.findObject(new UiSelector().descriptionContains("Reproduzir"));
        if (playButton.exists()) {
            try { playButton.click(); } catch (Exception e) {}
        }
    }
}