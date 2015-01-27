package day.cloudy.apps.overlaylibrary;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

abstract class BaseOverlayService extends Service {

    private static final String TAG = "BaseOverlayService";
    private static final long SLIDE_DURATION = 300L;

    private WindowManager mWM;
    private boolean mViewAdded;
    private boolean mVisible;
    private RelativeLayout mView;
    private View mContentView;
    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        addToWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initContentView();
        animateIn();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        animateOut();
        removeFromWindow();
    }

    private void animateIn() {
        Log.d(TAG, "animateIn");
        if (!mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", getStatusBarHeight());
            animator.setDuration(SLIDE_DURATION);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addListener(new SimpleAnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mView.setVisibility(View.VISIBLE);
                    mVisible = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    restartTimeoutTimer();
                }
            });
            animator.start();
        }
    }

    private void animateOut() {
        Log.d(TAG, "animateOut");
        if (mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", -getTranslationY());
            animator.setDuration(SLIDE_DURATION);
            animator.setInterpolator(new AnticipateInterpolator());
            animator.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mVisible = false;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mView.setVisibility(View.GONE);
                }
            });
            animator.start();
        }
    }

    private void addToWindow() {
        Log.d(TAG, "addToWindow");
        if (!mViewAdded) {
            mView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.base_overlay_wrapper, new RelativeLayout(this));
            mWM.addView(mView, getLayoutParams());
            mViewAdded = true;
        }
    }

    private void removeFromWindow() {
        Log.d(TAG, "removeFromWindow");
        if (null != mView && mViewAdded) {
            mWM.removeView(mView);
            mViewAdded = false;
        }
    }

    private void initContentView() {
        Log.d(TAG, "initContentView");
        if (null == mContentView) {
            mContentView = getView(mView);
            if (dismissOnTouch()) {
                mContentView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                animateOut();
                                return true;
                        }
                        return false;
                    }
                });
            }
            mView.removeAllViews();
            mView.addView(mContentView);
            mContentView.setTranslationY(-getTranslationY());
        }
    }

    private ViewGroup.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, getTranslationY() + getStatusBarHeight(),
                WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER;
        return layoutParams;
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void restartTimeoutTimer() {
        cancelTimeoutTimer();
        mTimer = new Timer();
        mTimer.schedule(mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mView.post(new Runnable() {
                    @Override
                    public void run() {
                        animateOut();
                    }
                });
            }
        }, getTimeout());
    }

    private void cancelTimeoutTimer() {
        if (null != mTimerTask)
            mTimerTask.cancel();
        if (null != mTimer)
            mTimer.cancel();
    }

    protected abstract View getView(ViewGroup parent);

    protected abstract int getTranslationY();

    protected abstract int getTimeout();

    protected abstract boolean dismissOnTouch();

    protected abstract boolean showAboveStatusBar();

}
