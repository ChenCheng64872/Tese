package pt.ulisboa.ciencias.userenergy.experiments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.ulisboa.ciencias.userenergy.R;

public class InstagramActivity extends AppCompatActivity {
    private static final String TAG = "InstagramActivity";

    private static final String PKG_INSTAGRAM = "com.instagram.android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automation); // same blank layout
    }

    /** Open Instagram home/feed if installed. */
    public void openInstagram() {
        Intent i = getLaunchIntentForPackage(PKG_INSTAGRAM);
        if (i != null) {
            startActivity(i);
        } else {
            Toast.makeText(this, "Instagram not installed", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Instagram not installed (getLaunchIntentForPackage returned null).");
        }
    }

    private Intent getLaunchIntentForPackage(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            Intent i = pm.getLaunchIntentForPackage(pkg);
            if (i != null) return i;
        } catch (Exception ignored) { }
        return null;
    }
}
