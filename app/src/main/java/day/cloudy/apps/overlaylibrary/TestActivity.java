package day.cloudy.apps.overlaylibrary;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Gaelan on 1/26/2015.
 */
public class TestActivity extends AppCompatActivity {

    private static final int REQ_DRAW_OVERLAYS = 1337;

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.test1).setOnClickListener(v -> {
            Intent intent = new Intent(TestActivity.this, TestOverlayService.class);
            if (canDrawOverlays()) {
                startService(intent);
            } else {
                mIntent = intent;
                requestDrawOverlaysPermission();
            }
        });
        findViewById(R.id.test2).setOnClickListener(v -> {
            Intent intent = new Intent(TestActivity.this, TestOverlayService2.class);
            if (canDrawOverlays()) {
                startService(intent);
            } else {
                mIntent = intent;
                requestDrawOverlaysPermission();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(TestActivity.this, TestOverlayService.class));
        stopService(new Intent(TestActivity.this, TestOverlayService2.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DRAW_OVERLAYS && canDrawOverlays() && mIntent != null) {
            startService(mIntent);
            mIntent = null;
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestDrawOverlaysPermission() {
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), REQ_DRAW_OVERLAYS);
    }
}
