package day.cloudy.apps.overlaylibrary;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Created by Gaelan on 1/26/2015.
 */
public class TestOverlayService2 extends BaseOverlayService {

    private static final String TAG = "TestOverlayService2";

    @Override
    protected View getView(ViewGroup parent) {
        Log.d(TAG, "getView");
        CardView view = (CardView) LayoutInflater.from(this).inflate(R.layout.notification_test2, parent, false);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        layoutParams.width = getMaximumWidth();
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        return view;
    }

    @Override
    protected int getTranslationY() {
        return getResources().getDimensionPixelSize(R.dimen.notif_test_height);
    }

    @Override
    protected boolean isSwipeEnabled() {
        return true;
    }

    @Override
    protected boolean onSwiped(SwipeDirection direction) {
        Toast.makeText(this, "View swiped, " + direction, Toast.LENGTH_SHORT).show();
        switch (direction) {
            case LEFT:
                animateOutLeft();
                break;
            case TOP:
                animateOutTop(false);
                break;
            case RIGHT:
                animateOutRight();
                break;
            case BOTTOM:
                animateOutTop(true);
                break;
        }
        return true;
    }

    private int getMaximumWidth() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }
}
