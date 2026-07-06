package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;
import android.os.Handler;
import android.view.KeyEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
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

import pt.ulisboa.ciencias.userenergy.experiments.GoogleKeepActivity;

@RunWith(AndroidJUnit4.class)
public class GoogleKeepTest extends BaseWriteTest {

    private static final String GOOGLE_KEEP_PACKAGE_NAME = "com.google.android.keep";
    private static final String GOOGLE_KEEP_MAIN_ACTIVITY = "com.google.android.keep.activities.BrowseActivity";
    private static final long AUTOMATION_DELAY = 1000; // 1 second delay before automation starts
   // private static final String MSG = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ";


    enum TEXT_MESSAGE {
        LONG("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidu"),
        MEDIUM("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non urna vitae elit tristique tincidunt. Aliquam erat volutpat. Vivamus sit amet magna vel libero pulvinar suscipit non et velit. Suspendisse potenti"),
        SHORT("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio vitae mauris hendrerit feugiat"),
        ACK("Yes");
        final String value;

        TEXT_MESSAGE(String i) {
            this.value = i;
        }
    }


    @Rule
    public ActivityScenarioRule<GoogleKeepActivity> activityScenarioRule =
            new ActivityScenarioRule<>(GoogleKeepActivity.class);

    @Test
    public void create_note() throws InterruptedException {

        String messageSize = InstrumentationRegistry.getArguments().getString("message");
        TEXT_MESSAGE msg;

        try{
            msg = TEXT_MESSAGE.valueOf(messageSize.toUpperCase());
        }catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Test failed due to exception: " + e.getMessage(), e);
        }




        ActivityScenario<GoogleKeepActivity> scenario = activityScenarioRule.getScenario();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        scenario.onActivity(activity -> {

            launchApp(GOOGLE_KEEP_PACKAGE_NAME, GOOGLE_KEEP_MAIN_ACTIVITY);

            new Handler().postDelayed(() -> {
                try {
                    // Click on the first note
                    UiObject firstNote = device.findObject(
                            new UiSelector().resourceId("com.google.android.keep:id/browse_note_interior_content").instance(0));
                    if (firstNote.exists()) {
                        firstNote.click();
                    }

                    // Type the message
                    UiObject noteBody = device.findObject(
                            new UiSelector().resourceId("com.google.android.keep:id/edit_note_text"));
                    if (noteBody.exists()) {
                        noteBody.click();
                        typeText(device, msg.value);
                    }
                } catch (UiObjectNotFoundException e) {
                    e.printStackTrace();
                }
            }, AUTOMATION_DELAY); // Adjust the delay as needed
        });

        Thread.sleep(7500 + (int) (msg.value.length()*0.6 * 1000));

        // Erase the text before finishing the test
        try {
            eraseText(device);
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(250);

        try {
            device.executeShellCommand("am force-stop " + GOOGLE_KEEP_PACKAGE_NAME);
            
          //  device.executeShellCommand("pm clear " +  GOOGLE_KEEP_PACKAGE_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void launchApp(String packageName, String mainActivity) {
        // Launch the Google Keep app using an Intent
        Intent intent = new Intent();
        intent.setClassName(packageName, mainActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent);
    }


/*
    @Test
    public void utilSetup_L(){
        openSettingsAndSetTheme(Theme.LIGHT);
    }
    @Test
    public void utilSetup_D(){
        openSettingsAndSetTheme(Theme.DARK);
    }

    private void openSettingsAndSetTheme(
            Theme theme) {
        ActivityScenario<GoogleKeepActivity> scenario = activityScenarioRule.getScenario();
        launchApp(GOOGLE_KEEP_PACKAGE_NAME, GOOGLE_KEEP_MAIN_ACTIVITY);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
        // Click on the more options. Could not find the way to do it using the UI Automator
            UiObject moreOptions = device.findObject(new UiSelector()
                    .className("android.widget.ImageButton"));
        moreOptions.click();

        Point screenSize = new Point();
        device.click(screenSize.x / 10, screenSize.y / 10);

        UiObject settings = device.findObject(new UiSelector().text("Settings"));
        if (settings.exists()) {
            settings.click();

            UiObject themeOption = device.findObject(new UiSelector().text("Theme"));
            if (themeOption.exists()) {
                themeOption.click();

                UiObject themeMode = device.findObject(
                        new UiSelector().text(theme == Theme.DARK ? "Dark" : "Light"));
                if (themeMode.exists()) {
                    themeMode.click();
                }

                UiObject okButton = device.findObject(new UiSelector().text("OK"));
                if (okButton.exists()) {
                    okButton.click();
                }
            }

            try {
                device.executeShellCommand("am force-stop " + GOOGLE_KEEP_PACKAGE_NAME);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Thread.sleep(1000);
        }
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

*/
    private void typeText(UiDevice device, String text) throws UiObjectNotFoundException {
        for (char c : text.toCharArray()) {
            int keyCode = getKeyEventCode(c);
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                device.pressKeyCode(keyCode);
            }
        }
    }

    private int getKeyEventCode(char c) {
        if (Character.isLetterOrDigit(c)) {
            return KeyEvent.keyCodeFromString("KEYCODE_" + Character.toUpperCase(c));
        } else if (Character.isSpaceChar(c)) {
            return KeyEvent.KEYCODE_SPACE;
        } else {
            switch (c) {
                case '.':
                    return KeyEvent.KEYCODE_PERIOD;
                case ',':
                    return KeyEvent.KEYCODE_COMMA;
                case '\n':
                    return KeyEvent.KEYCODE_ENTER;
                default:
                    return KeyEvent.KEYCODE_UNKNOWN;
            }
        }
    }

    private void eraseText(UiDevice device) throws UiObjectNotFoundException {
        UiObject noteBody = device.findObject(
                new UiSelector().resourceId("com.google.android.keep:id/edit_note_text"));
        if (noteBody.exists()) {
            noteBody.click();
            // Long press to select all text
            device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON);
            // Press delete key to remove all selected text
            device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        }
    }
}
