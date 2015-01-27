package day.cloudy.apps.overlaylibrary;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Gaelan on 1/26/2015.
 */
public class TestOverlayService extends BaseOverlayService {

    private static final String TAG = "TestOverlayService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected View getView(ViewGroup parent) {
        Log.d(TAG, "getView");
        CardView view = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.notification_test, parent, false);
        return view;
    }

    @Override
    protected int getTranslationY() {
        return getResources().getDimensionPixelSize(R.dimen.notif_test_top_margin);
    }

    @Override
    protected int getTimeout() {
        return 4000;
    }

    @Override
    protected boolean showAboveStatusBar() {
        return false;
    }

    @Override
    protected boolean dismissOnTouch() {
        return true;
    }
}
