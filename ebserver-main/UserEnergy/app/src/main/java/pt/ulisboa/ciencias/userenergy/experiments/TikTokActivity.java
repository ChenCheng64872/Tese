package pt.ulisboa.ciencias.userenergy.experiments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.ulisboa.ciencias.userenergy.R;

public class TikTokActivity extends AppCompatActivity {
    private static final String TAG = "TikTokActivity";

    private static final String PKG_TIKTOK = "com.zhiliaoapp.musically";
    private static final String PKG_TIKTOK_ALT = "com.ss.android.ugc.trill"; // legacy/alt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automation);
    }

    /** Open TikTok home/feed if installed. */
    public void openTikTok() {
        Intent i = getLaunchIntentForAny(PKG_TIKTOK, PKG_TIKTOK_ALT);
        if (i != null) {
            startActivity(i);
        } else {
            Toast.makeText(this, "TikTok not installed", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "TikTok not installed (getLaunchIntentForPackage returned null).");
        }
    }

    private Intent getLaunchIntentForAny(String... pkgs) {
        PackageManager pm = getPackageManager();
        for (String p : pkgs) {
            try {
                Intent i = pm.getLaunchIntentForPackage(p);
                if (i != null) return i;
            } catch (Exception ignored) { }
        }
        return null;
    }
}
