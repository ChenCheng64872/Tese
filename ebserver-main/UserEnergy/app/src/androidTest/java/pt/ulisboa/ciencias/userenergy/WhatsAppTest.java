package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import pt.ulisboa.ciencias.userenergy.experiments.WhatsAppActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WhatsAppTest extends BaseWriteTest {

    private static final String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    private static final long AUTOMATION_DELAY = 2 * 1000;

    @Rule
    public ActivityScenarioRule<WhatsAppActivity> activityScenarioRule =
            new ActivityScenarioRule<>(WhatsAppActivity.class);

    @Test
    public void sendmessage() throws InterruptedException, UiObjectNotFoundException {
        TextMessage msg = parseMessageArg();

        ActivityScenario<WhatsAppActivity> scenario = activityScenarioRule.getScenario();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        scenario.onActivity(activity -> {
            Intent launchIntent = activity.getPackageManager()
                    .getLaunchIntentForPackage(WHATSAPP_PACKAGE_NAME);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
            }
        });

        Thread.sleep(AUTOMATION_DELAY);

        UiObject firstChat = device.findObject(new UiSelector()
                .resourceId("com.whatsapp:id/contact_row_container"));
        if (!firstChat.waitForExists(AUTOMATION_DELAY) || !firstChat.isClickable()) {
            throw new UiObjectNotFoundException("Could not find the first chat item.");
        }
        firstChat.click();

        UiObject messageField = device.findObject(
                new UiSelector().resourceId("com.whatsapp:id/entry"));
        if (messageField.waitForExists(5000)) {
            typeMessage(device, messageField, msg);

            UiObject sendButton = device.findObject(
                    new UiSelector().descriptionContains("Send"));
            if (sendButton.exists()) {
                sendButton.click();
            }
        }

        Thread.sleep(250);

        try {
            device.executeShellCommand("am force-stop " + WHATSAPP_PACKAGE_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(1000);
    }
}
