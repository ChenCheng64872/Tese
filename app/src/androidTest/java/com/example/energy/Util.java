package com.example.energy;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.example.energy.enums.AirplaneMode;
import com.example.energy.enums.BatterySaving;
import com.example.energy.enums.Bright;
import com.example.energy.enums.Refresh;
import com.example.energy.enums.Theme;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;


/**
 * Utility class for automated UI tests and system settings modifications.
 * Includes methods for adjusting refresh rates, enabling/disabling airplane mode,
 * toggling battery-saving mode, and adjusting screen brightness dynamically.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Util {

    /**
     * Test: Set refresh rate to 30 FPS.
     */
    @Test
    public void refresh_FPS30() throws Exception {
        setRefreshRate(Refresh.FPS30);
    }

    /**
     * Test: Set refresh rate to 60 FPS.
     */
    @Test
    public void refresh_FPS60() throws Exception {
        setRefreshRate(Refresh.FPS60);
    }

    /**
     * Test: Set refresh rate to 120 FPS.
     */
    @Test
    public void refresh_FPS120() throws Exception {
        setRefreshRate(Refresh.FPS120);
    }

    /**
     * Dynamically sets the screen refresh rate.
     *
     * @param refreshRate The desired refresh rate (e.g., 30.0, 60.0, 120.0).
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws IOException if there is an error executing the shell command.
     */
    public static void setRefreshRate(Refresh refreshRate) throws InterruptedException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.executeShellCommand("settings put system peak_refresh_rate " + refreshRate.value);
        device.executeShellCommand("settings put system min_refresh_rate " + refreshRate.value);
    }

    /**
     * Test: Enable airplane mode and Wi-Fi.
     */
    @Test
    public void airplane_AirOn() throws Exception {
        setAirplaneMode(AirplaneMode.ON);
    }

    /**
     * Test: Disable airplane mode and enable Wi-Fi.
     */
    @Test
    public void airplane_AirOff() throws Exception {
        setAirplaneMode(AirplaneMode.OFF);
    }

    /**
     * Toggles airplane mode and Wi-Fi settings.
     *
     * @param airplane True to enable airplane mode and Wi-Fi; false to disable airplane mode and enable Wi-Fi.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws IOException if there is an error executing the shell command.
     */
    public static void setAirplaneMode(AirplaneMode airplane) throws InterruptedException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (airplane == AirplaneMode.ON) {
            device.executeShellCommand("settings put global airplane_mode_on 1");
            device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true");
            device.executeShellCommand("svc wifi enable");
        } else {
            device.executeShellCommand("settings put global airplane_mode_on 0");
            device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false");
            device.executeShellCommand("svc wifi enable");
        }
    }

    /**
     * Test: Enable battery-saving mode.
     */
    @Test
    public void batterySaving_SavOn() throws Exception {
        setBatterySaving(BatterySaving.ON);
    }

    /**
     * Test: Disable battery-saving mode.
     */
    @Test
    public void batterySaving_SavOff() throws Exception {
        setBatterySaving(BatterySaving.OFF);
    }

    /**
     * Toggles the battery-saving mode.
     *
     * @param saving True to enable battery-saving mode; false to disable it.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws IOException if there is an error executing the shell command.
     */
    public static void setBatterySaving(BatterySaving saving) throws InterruptedException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (saving == BatterySaving.ON) {
            device.executeShellCommand("settings put global low_power 1");
        } else {
            device.executeShellCommand("settings put global low_power 0");
        }
    }

    /**
     * Test: Enable dark mode.
     */
    @Test
    public void setTheme_dark() throws Exception {
        setTheme(Theme.DARK);
    }

    /**
     * Test: Enable light mode.
     */
    @Test
    public void setTheme_light() throws Exception {
        setTheme(Theme.LIGHT);
    }

    public static void setTheme(Theme theme) throws InterruptedException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (theme == Theme.DARK) {
            device.executeShellCommand("cmd uimode night yes");
        } else {
            device.executeShellCommand("cmd uimode night no");
        }
    }




    /**
     * Test: Set screen brightness to full.
     */
    @Test
    public void brightness_FullB() throws IOException, InterruptedException {
        setBrightness(Bright.FULL);
    }

    /**
     * Test: Set screen brightness to 75% (three-quarters).
     */
    @Test
    public void brightness_ThreeQ() throws IOException, InterruptedException {
        setBrightness(Bright.THREEQ);
    }

    /**
     * Test: Set screen brightness to 50% (half).
     */
    @Test
    public void brightness_HalfB() throws IOException, InterruptedException {
        setBrightness(Bright.HALF);
    }

    /**
     * Test: Set screen brightness to 25% (quarter).
     */
    @Test
    public void brightness_QuartB() throws IOException, InterruptedException {
        setBrightness(Bright.QUART);
    }

    /**
     * Test: Set screen brightness to minimum (0%).
     */
    @Test
    public void brightness_ZeroB() throws IOException, InterruptedException {
        setBrightness(Bright.ZERO);
    }

    /**
     * Sets the screen brightness level.
     *
     * @param brightness The desired brightness level (0-255).
     * @throws IOException if there is an error executing the shell command.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     */
    public static void setBrightness(Bright brightness) throws IOException, InterruptedException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.executeShellCommand("settings put system screen_brightness " + brightness.value);
    }

    /**
     * General parameter setup test.
     * This test configures the environment based on the provided parameters.
     *
     */
    @Test
    public void setupGeneralParameters() {
        try {
            int count = 0;
            // Parse parameters from adb arguments
            String theme = InstrumentationRegistry.getArguments().getString("theme");
            String bLevel = InstrumentationRegistry.getArguments().getString("brightness");
            String plane = InstrumentationRegistry.getArguments().getString("airplane");
            String saving = InstrumentationRegistry.getArguments().getString("saving");
            String refresh = InstrumentationRegistry.getArguments().getString("refresh");

            if (bLevel != null) {
                Bright brightEnum = Bright.valueOf(bLevel.toUpperCase());
                // Call the method to configure brightness
                Util.setBrightness(brightEnum);
                count++;
            }

            if (theme != null) {
                Theme themeEnum = Theme.valueOf(theme.toUpperCase());
                // Call the method to configure the refresh rate
                Util.setTheme(themeEnum);
                count++;
            }

            if (refresh != null) {
                Refresh refreshEnum = Refresh.valueOf(refresh.toUpperCase());
                // Call the method to configure the refresh rate
                Util.setRefreshRate(refreshEnum);
                count++;
            }

            if (plane != null) {
                AirplaneMode airplaneEnum = AirplaneMode.valueOf(plane.toUpperCase());
                // Call the method to configure airplane mode
                Util.setAirplaneMode(airplaneEnum);
                count++;
            }

            if (saving != null) {
                BatterySaving savingEnum = BatterySaving.valueOf(saving.toUpperCase());
                // Call the method to configure battery saving mode
                Util.setBatterySaving(savingEnum);
                count++;
            }

            Thread.sleep(600*count);


        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Test failed due to exception: " + e.getMessage(), e);
        }
    }

    @Test
    public void resetDefaultValues() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            device.executeShellCommand("settings delete secure ui_night_mode");
            device.executeShellCommand("settings delete system min_refresh_rate");
            device.executeShellCommand("settings delete system peak_refresh_rate");
            device.executeShellCommand("settings delete system screen_brightness");
            device.executeShellCommand("settings delete global airplane_mode_on");
            device.executeShellCommand("settings delete global low_power");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
