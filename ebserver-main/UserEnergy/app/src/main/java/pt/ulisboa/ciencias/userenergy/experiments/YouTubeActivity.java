package pt.ulisboa.ciencias.userenergy.experiments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import pt.ulisboa.ciencias.userenergy.R;


public class YouTubeActivity extends AppCompatActivity {

    public static final String YOUTUBE_VIDEO_ID = "xOMMV_qXcQ8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_drain);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public Intent openYouTubeVideo() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("vnd.youtube:" + YouTubeActivity.YOUTUBE_VIDEO_ID));
        startActivity(intent);
        return intent;
    }
}