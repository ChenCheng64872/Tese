package pt.ulisboa.ciencias.userenergy;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChangeTextBehaviorTest {

    @After
    public void tearDown() throws InterruptedException {
        Intents.release();
        try {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(100);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().clearCache();
    }

    public static final String STRING_TO_BE_TYPED =
            "Esta é a primeira linha do texto.\n" +
                    "Esta é a segunda linha do texto.\n" +
                    "Esta é a terceira linha do texto.\n" +
                    "Esta é a quarta linha do texto.\n" +
                    "Esta é a quinta linha do texto.\n" +
                    "Esta é a sexta linha do texto.\n" +
                    "Esta é a sétima linha do texto.\n" +
                    "Esta é a oitava linha do texto.\n" +
                    "Esta é a nona linha do texto.\n" +
                    "Finalmente, esta é a décima linha.";

    /**
     * Use {@link ActivityScenarioRule} to create and launch the activity under test, and close it
     * after test completes. This is a replacement for {@link androidx.test.rule.ActivityTestRule}.
     */
    @Rule public ActivityScenarioRule<MainActivity> activityScenarioRule
            = new ActivityScenarioRule<>(MainActivity.class);

    private void typeTextSlowly(final String text, final int delay) {
        StringBuilder typedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            typedText.append(c);
            //onView(withId(R.id.editTextUserInput)).perform(replaceText(typedText.toString()));
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //onView(withId(R.id.editTextUserInput)).perform(closeSoftKeyboard());
    }

    @Test
    public void changeText_sameActivity() {
        // Type text slowly and then press the button.
        typeTextSlowly(STRING_TO_BE_TYPED, 60000 / STRING_TO_BE_TYPED.length());
        onView(withId(R.id.show_text_view)).perform(click());

        // Check that the text was changed.
        onView(withId(R.id.show_text_view)).check(matches(withText(STRING_TO_BE_TYPED)));
    }

    @Test
    public void changeText_newActivity() {
        // Type text slowly and then press the button.
        typeTextSlowly(STRING_TO_BE_TYPED, 60000 / STRING_TO_BE_TYPED.length());
        onView(withId(R.id.show_text_view)).perform(click());

        // This view is in a different Activity, no need to tell Espresso.
        onView(withId(R.id.show_text_view)).check(matches(withText(STRING_TO_BE_TYPED)));
    }
}





