package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import pt.ulisboa.ciencias.userenergy.experiments.SignalActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignalTest extends BaseWriteTest {

    private static final String SIGNAL_PACKAGE_NAME = "org.thoughtcrime.securesms";
    private static final long AUTOMATION_DELAY = 2 * 1000;

    @Rule
    public ActivityScenarioRule<SignalActivity> activityScenarioRule =
            new ActivityScenarioRule<>(SignalActivity.class);

    @Test
    public void sendmessage() throws InterruptedException, UiObjectNotFoundException {
        TextMessage msg = parseMessageArg();

        ActivityScenario<SignalActivity> scenario = activityScenarioRule.getScenario();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        scenario.onActivity(activity -> {
            Intent launchIntent = activity.getPackageManager()
                    .getLaunchIntentForPackage(SIGNAL_PACKAGE_NAME);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
            }
        });

        device.waitForIdle(AUTOMATION_DELAY);

        UiScrollable chatList = new UiScrollable(new UiSelector().scrollable(true).instance(0));
        if (!chatList.waitForExists(AUTOMATION_DELAY)) {
            throw new UiObjectNotFoundException("Chat list not found in " + SIGNAL_PACKAGE_NAME);
        }
        chatList.getChildByInstance(new UiSelector().clickable(true), 0).click();

        UiObject messageField = device.findObject(new UiSelector().className("android.widget.EditText"));
        if (messageField.waitForExists(5000)) {
            typeMessage(device, messageField, msg);

            UiObject sendButton = device.findObject(new UiSelector().descriptionMatches("(?i).*send.*"));
            if (!sendButton.exists()) {
                sendButton = device.findObject(new UiSelector().resourceIdMatches("(?i).*send.*").clickable(true));
            }
            if (sendButton.exists()) {
                sendButton.click();
            }
        }

        Thread.sleep(250);

        try {
            device.executeShellCommand("am force-stop " + SIGNAL_PACKAGE_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(1000);
    }
}
