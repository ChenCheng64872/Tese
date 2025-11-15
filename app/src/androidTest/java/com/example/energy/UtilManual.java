package com.example.energy;

import android.content.Intent;
import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import pt.ulisboa.ciencias.userenergy.enums.Bright;

/**
 * This class provides UI-based methods for modifying system settings manually as a safeguard
 * when ADB commands fail or require root access.
 * It uses UiAutomator to interact with system settings, ensuring compatibility across devices
 * where direct ADB modifications are not permitted.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UtilManual {

    /**
     * Test: Enable dynamic refresh rate (e.g., Adaptive mode).
     */
    @Test
    public void refresh_NorR() throws Exception {
        setRefreshDynamic(true);
    }

    /**
     * Test: Set refresh rate to standard mode (e.g., Static mode).
     */
    @Test
    public void refresh_DynR() throws Exception {
        setRefreshDynamic(false);
    }

    /**
     * Sets the refresh rate dynamically through UI interaction.
     *
     * @param turnOn True for Adaptive (dynamic) refresh rate; false for Standard refresh rate.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws UiObjectNotFoundException if a required UI element is not found.
     * @throws IOException if there is an error starting the settings activity.
     */
    private void setRefreshDynamic(boolean turnOn) throws InterruptedException, UiObjectNotFoundException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Open display settings
        Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent);

        Thread.sleep(1000); // Allow the settings page to load

        // Locate and select the "Motion smoothness" option
        UiObject refreshRateOption = device.findObject(new UiSelector().textContains("Motion smoothness"));
        if (refreshRateOption.exists()) {
            refreshRateOption.click();

            Thread.sleep(1000);

            // Choose between Adaptive and Standard options
            UiObject dynamicOption = device.findObject(new UiSelector().textContains(turnOn ? "Adaptive" : "Standard"));
            if (dynamicOption.exists()) {
                dynamicOption.click();
            } else {
                throw new UiObjectNotFoundException("Refresh rate option not found");
            }

            // Apply changes if necessary
            UiObject applyButton = device.findObject(new UiSelector().text("Apply"));
            if (applyButton.exists()) {
                applyButton.click();
            }
        } else {
            throw new UiObjectNotFoundException("Refresh rate settings not found");
        }

        device.pressBack();
        device.pressBack();

        Thread.sleep(1000); // Allow changes to take effect
    }

    /**
     * Test: Enable airplane mode and Wi-Fi.
     */
    @Test
    public void airplane_AirOn() throws Exception {
        airplaneAndWifi(true);
    }

    /**
     * Test: Disable airplane mode and Wi-Fi.
     */
    @Test
    public void airplane_AirOff() throws Exception {
        airplaneAndWifi(false);
    }

    /**
     * Toggles airplane mode and Wi-Fi settings through Quick Settings.
     *
     * @param turnOn True to enable airplane mode and Wi-Fi; false to disable airplane mode and enable Wi-Fi.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws UiObjectNotFoundException if a required UI element is not found.
     * @throws IOException if there is an error interacting with Quick Settings.
     */
    private void airplaneAndWifi(boolean turnOn) throws InterruptedException, UiObjectNotFoundException, IOException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.openQuickSettings();

        Thread.sleep(2000); // Wait for Quick Settings to open

        // Locate and toggle airplane mode and Wi-Fi
        UiObject wifiButton = device.findObject(new UiSelector().descriptionContains("Wi-Fi"));
        UiObject airplaneModeButton = device.findObject(new UiSelector().descriptionContains("Airplane mode"));
        if (wifiButton.exists() && airplaneModeButton.exists()) {
            airplaneModeButton.click(); // Toggle airplane mode
            Thread.sleep(5000);
            if (turnOn && wifiButton.isChecked()) wifiButton.click(); // Enable Wi-Fi if airplane mode is turned on
        }
        device.pressBack();
        device.pressBack();
    }

    /**
     * Test: Enable battery-saving mode.
     */
    @Test
    public void batterySaving_SavOn() throws Exception {
        batterySaving(true);
    }

    /**
     * Test: Disable battery-saving mode.
     */
    @Test
    public void batterySaving_SavOff() throws Exception {
        batterySaving(false);
    }

    /**
     * Toggles the battery-saving mode through system settings.
     *
     * @param turnOn True to enable battery-saving mode; false to disable it.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws UiObjectNotFoundException if a required UI element is not found.
     */
    private void batterySaving(boolean turnOn) throws InterruptedException, UiObjectNotFoundException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Open battery saver settings
        Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent);

        Thread.sleep(2000); // Wait for the settings page to load

        // Locate and toggle the battery saver switch
        UiObject toggleButton = device.findObject(new UiSelector().className("android.widget.Switch"));
        if (toggleButton.exists()) {
            boolean isCurrentlyEnabled = toggleButton.isChecked();
            if (turnOn != isCurrentlyEnabled) {
                toggleButton.click(); // Toggle the switch
            }
        } else {
            System.out.println("Battery saver toggle not found.");
        }

        device.pressBack(); // Return to the app
        Thread.sleep(1000);
    }

    /**
     * Test: Set brightness to full.
     */
    @Test
    public void brightness_FullB() {
        setBrightness(Bright.FULL.value);
    }

    // Additional brightness tests are similarly documented...

    /**
     * Adjusts the screen brightness manually through Quick Settings.
     *
     * @param brightnessValue The desired brightness level (0-255).
     */
    public void setBrightness(int brightnessValue) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        try {
            // Open Quick Settings
            device.openQuickSettings();

            Thread.sleep(1000); // Allow Quick Settings to load

            // Locate and interact with the brightness slider
            UiObject brightnessSlider = device.findObject(new UiSelector().descriptionContains("Brightness"));
            if (brightnessSlider.exists()) {
                // Swipe to adjust brightness
                int y = brightnessSlider.getBounds().centerY();
                int xStart = brightnessSlider.getBounds().left;
                int xEnd = brightnessSlider.getBounds().right * brightnessValue / 255;
                device.swipe(xStart, y, xEnd, y, 50);
            } else {
                throw new UiObjectNotFoundException("Brightness slider not found.");
            }

            device.pressBack();
            Thread.sleep(1000);
        } catch (UiObjectNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
