package day.cloudy.apps.overlaylibrary;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

/**
 * Created by Gaelan on 1/26/2015.
 */
public class TestActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TestActivity.this, TestOverlayService.class);
                startService(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(TestActivity.this, TestOverlayService.class));
    }
}
