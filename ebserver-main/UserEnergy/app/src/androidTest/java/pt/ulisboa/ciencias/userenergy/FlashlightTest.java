package pt.ulisboa.ciencias.userenergy;

import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ulisboa.ciencias.userenergy.experiments.FlashlightActivity;

@RunWith(AndroidJUnit4.class)
public class FlashlightTest {

    private Handler flashlightHandler;

    enum Duration {
        S15(15 * 1000), S30(30 * 1000), S60(60 * 1000);

        final int value;

        Duration(int i) {
            this.value = i;
        }
    }

    enum FlashPower {
        MAX(1), HALF(0.5f), QUARTER(0.25f), MIN(0.1f), OFF(-1f);
        final float value;

        FlashPower(float i) {
            this.value = i;
        }
    }

    private int calcIntensity(FlashPower flashPower, int max_brightness) {
        int force = (int) Math.ceil(flashPower.value * max_brightness);
        return Math.max(force, 1);
    }

    @Rule
    public ActivityScenarioRule<FlashlightActivity> activityScenarioRule =
            new ActivityScenarioRule<>(FlashlightActivity.class);

    @Before
    public void intentsInit() {
        Intents.init();
        flashlightHandler = new Handler(Looper.getMainLooper());
    }

    @After
    public void intentsTeardown() throws InterruptedException {
        Intents.release();
        try {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(100);
    }

    @Test
    public void runtest() {

        String duration = InstrumentationRegistry.getArguments().getString("duration");
        String power = InstrumentationRegistry.getArguments().getString("flashpower");
        //String duration = "S15";
        //String power = "half";
        FlashPower powerEnum = null;
        Duration d = null;
        try{
            d = Duration.valueOf(duration.toUpperCase());
            powerEnum = FlashPower.valueOf(power.toUpperCase());
        }catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Test failed due to exception: " + e.getMessage(), e);
        }



        if(powerEnum != FlashPower.OFF){
            try {
                FlashlightActivity flashlightActivity = startFlashlightTestActivity();
                turnOnFlashlightForTest(flashlightActivity, powerEnum, d.value);

                flashlightActivity.finish();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }else{
            FlashlightActivity activity = startFlashlightTestActivity();

            try {
                CameraManager cameraManager =
                        (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                turnOffFlashlightForTest(cameraManager);
                Thread.sleep(d.value);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            activity.finish();
        }
    }


    public void turnOnFlashlightForTest(FlashlightActivity activity,
                                        FlashPower flashPower,
                                        long duration) throws CameraAccessException {
        try {
            CameraManager cameraManager =
                    (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            int maxBrightness =
                    cameraManager.getCameraCharacteristics(
                            cameraId).get(FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
            int selectedIntensity = calcIntensity(flashPower, maxBrightness);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    // Try normal Camera2 torch first
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, selectedIntensity);
                    Thread.sleep(duration);

                    flashlightHandler.postDelayed(() ->
                            turnOffFlashlightForTest(cameraManager), duration);
                    return;
                } catch (IllegalArgumentException iae) {
                    // UI path if Camera2 reports "no flashlight"
                    toggleFlashlightViaQuickSettings(duration);
                    return;
                }
            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void turnOffFlashlightForTest(CameraManager cameraManager) {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public FlashlightActivity startFlashlightTestActivity() {
        final FlashlightActivity[] activity = new FlashlightActivity[1];
        activityScenarioRule.getScenario().onActivity(activityInstance ->
                activity[0] = activityInstance);
        return activity[0];
    }

    private void toggleFlashlightViaQuickSettings(long onDurationMs) {
        UiDevice device = UiDevice.getInstance(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation());
        try {
            int w = device.getDisplayWidth();
            int h = device.getDisplayHeight();

            // Open Quick Settings fully (two pulls)
            device.swipe(w / 2, 0, w / 2, h / 2, 20);
            Thread.sleep(150);
            device.swipe(w / 2, 0, w / 2, h / 2, 20);
            Thread.sleep(250);

            //  look for "Flash" in text or content-desc
            UiObject tile = device.findObject(new UiSelector().textContains("Flash"));
            if (!tile.exists()) {
                tile = device.findObject(new UiSelector().descriptionContains("Flash"));
            }

            if (tile.exists()) {
                tile.click();                 // ON
                Thread.sleep(onDurationMs);   // keep it on for your duration
                tile.click();                 // OFF
            } else {

                device.click(w / 2, h / 2);
                Thread.sleep(onDurationMs);
            }

        } catch (UiObjectNotFoundException | InterruptedException ignored) {

        } finally {
            try { device.pressBack(); } catch (Exception ignored) {}
            try { device.pressBack(); } catch (Exception ignored) {}
        }
    }

}
