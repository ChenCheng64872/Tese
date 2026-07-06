package pt.ulisboa.ciencias.userenergy;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.KeyEvent;

import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;

public abstract class BaseWriteTest {

    @Before
    public void setUp() throws InterruptedException, UiObjectNotFoundException {
        Intents.init();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(intent);

                UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

                Thread.sleep(2000);

                UiObject toggleSwitch = device.findObject(new UiSelector().className("android.widget.Switch"));
                if (toggleSwitch.exists() && !toggleSwitch.isChecked()) {
                    toggleSwitch.click();
                }

                Thread.sleep(500);
            }
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        Intents.release();
        try {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(100);
    }

    protected static TextMessage parseMessageArg() {
        String messageSize = InstrumentationRegistry.getArguments().getString("message");
        if (messageSize == null) {
            throw new AssertionError("Required test argument 'message' was not provided. Pass --e message SHORT|MEDIUM|LONG|ACK");
        }
        return TextMessage.valueOf(messageSize.toUpperCase());
    }

    protected static void typeMessage(UiDevice device, UiObject messageField, TextMessage msg)
            throws UiObjectNotFoundException, InterruptedException {
        messageField.click();
        for (char c : msg.value.toCharArray()) {
            int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + Character.toUpperCase(c));
            if (Character.isSpaceChar(c)) {
                keyCode = KeyEvent.KEYCODE_SPACE;
            } else if (c == '.') {
                keyCode = KeyEvent.KEYCODE_PERIOD;
            } else if (c == ',') {
                keyCode = KeyEvent.KEYCODE_COMMA;
            }
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                device.pressKeyCode(keyCode);
                Thread.sleep(50);
            }
        }
    }
}
