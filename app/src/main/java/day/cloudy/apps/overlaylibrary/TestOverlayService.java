package day.cloudy.apps.overlaylibrary;

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
    protected View getView(ViewGroup parent) {
        Log.d(TAG, "getView");
        return LayoutInflater.from(this).inflate(R.layout.notification_test, parent, false);
    }

    @Override
    protected int getTranslationY() {
        return getResources().getDimensionPixelSize(R.dimen.notif_test_height);
    }

    @Override
    protected boolean showAboveStatusBar() {
        return true;
    }
}
