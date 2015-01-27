package day.cloudy.apps.overlaylibrary;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

abstract class BaseOverlayService extends Service {

    private static final String TAG = "BaseOverlayService";
    private static final int BOUNCE_SPACE = (int) (Resources.getSystem().getDisplayMetrics().density * 16);
    public static final int DEFAULT_TIMEOUT = 4000;

    private AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
    private AnticipateInterpolator anticipateInterpolator = new AnticipateInterpolator();
    private WindowManager mWM;
    private boolean mViewAdded;
    private boolean mVisible;
    private boolean mShouldStop;
    private RelativeLayout mView;
    private View mContentView;
    private Timer mTimer;
    private TimerTask mTimerTask;

    protected enum SwipeDirection {
        LEFT, TOP, RIGHT, BOTTOM;
    }

    private OnSwipeTouchListener mOnSwipeTouchListener = new OnSwipeTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return super.onTouch(v, event);
        }

        @Override
        public boolean onSwipeLeft(View view) {
            return onSwiped(SwipeDirection.LEFT);
        }

        @Override
        public boolean onSwipeTop(View view) {
            return onSwiped(SwipeDirection.TOP);
        }

        @Override
        public boolean onSwipeRight(View view) {
            return onSwiped(SwipeDirection.RIGHT);
        }

        @Override
        public boolean onSwipeBottom(View view) {
            return onSwiped(SwipeDirection.BOTTOM);
        }
    };
    private SimpleAnimatorListener mAnimateOutListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            cancelTimeoutTimer();
            mVisible = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mView.setVisibility(View.GONE);
            mContentView.setTranslationX(0.0f);
            mContentView.setTranslationY(-getCalculatedTranslationY());
            mContentView.setScaleX(1.0f);
            mContentView.setScaleY(1.0f);
            if (mShouldStop) {
                stopSelf();
            }
        }
    };

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
    public boolean stopService(Intent name) {
        if (mVisible) {
            mShouldStop = true;
            animateOutTop(false);
            return false;
        }
        removeFromWindow();
        return super.stopService(name);
    }

    private void initContentView() {
        Log.d(TAG, "initContentView");
        if (null == mContentView) {
            mContentView = getView(mView);
            mView.removeAllViews();
            mView.addView(mContentView);
            mContentView.setTranslationY(-getCalculatedTranslationY());
            mWM.updateViewLayout(mView, getLayoutParams());
        }
    }

    private void addToWindow() {
        Log.d(TAG, "addToWindow");
        if (!mViewAdded) {
            mView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.base_overlay_wrapper, new RelativeLayout(this));
            if (isSwipeEnabled())
                mView.setOnTouchListener(mOnSwipeTouchListener);
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

    private ViewGroup.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, getCalculatedTranslationY(),
                showAboveStatusBar() ? WindowManager.LayoutParams.TYPE_SYSTEM_ERROR : WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER;
        return layoutParams;
    }

    protected void restartTimeoutTimer() {
        cancelTimeoutTimer();
        mTimer = new Timer();
        mTimer.schedule(mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mView.post(new Runnable() {
                    @Override
                    public void run() {
                        animateOutTop(false);
                    }
                });
            }
        }, getTimeout());
    }

    protected void cancelTimeoutTimer() {
        if (null != mTimerTask)
            mTimerTask.cancel();
        if (null != mTimer)
            mTimer.cancel();
    }

    protected abstract View getView(ViewGroup parent);

    protected abstract int getTranslationY();

    private int getCalculatedTranslationY() {
        int i = getTranslationY() + BOUNCE_SPACE;
        if (null != mContentView) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mContentView.getLayoutParams();
            return i + params.topMargin + params.bottomMargin;
        }
        return i;
    }

    protected int getTimeout() {
        return DEFAULT_TIMEOUT;
    }

    protected boolean onSwiped(SwipeDirection direction) {
        return false;
    }

    /*
    * true = TYPE_SYSTEM_ERROR; false = TYPE_PRIORITY_PHONE
    * If set to true HOME button becomes disabled while the View is added to Window
    * In either case the BACK button is disabled while the View is added to Window
    * Overcome these limitations by implementing an AccessibilityService and listening for the key presses
     */
    protected boolean showAboveStatusBar() {
        return false;
    }

    protected boolean isSwipeEnabled() {
        return false;
    }

    private void animateIn() {
        Log.d(TAG, "animateIn");
        if (!mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", 0);
            animator.setDuration(250);
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

    protected void animateOutLeft() {
        Log.d(TAG, "animateOutLeft");
        if (mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationX", -mContentView.getWidth());
            animator.setDuration(250);
            animator.setInterpolator(accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

    protected void animateOutTop(boolean anticipate) {
        Log.d(TAG, "animateOutTop");
        if (mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", -getCalculatedTranslationY());
            animator.setDuration(anticipate ? 300 : 250);
            animator.setInterpolator(anticipate ? anticipateInterpolator : accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

    protected void animateOutRight() {
        Log.d(TAG, "animateOutRight");
        if (mVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationX", mContentView.getWidth());
            animator.setDuration(250);
            animator.setInterpolator(accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

}
