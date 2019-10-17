package day.cloudy.apps.overlaylibrary;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
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

import de.greenrobot.event.EventBus;

public abstract class BaseOverlayService extends Service {

    private static final String TAG = "BaseOverlayService";
    private static final int BOUNCE_SPACE = (int) (Resources.getSystem().getDisplayMetrics().density * 16);
    public static final int DEFAULT_TIMEOUT = 4000;
    public static final int DEFAULT_SLIDE_DURATION = 300;

    protected Handler mHandler = new Handler();
    private AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
    private AnticipateInterpolator anticipateInterpolator = new AnticipateInterpolator();
    private WindowManager mWM;
    private boolean mViewAdded;
    private boolean mViewVisible;
    private boolean mShouldShow;
    private boolean mShouldStop;
    private RelativeLayout mView;
    private View mContentView;
    private Timer mTimer;
    private TimerTask mTimerTask;

    protected enum SwipeDirection {
        LEFT, TOP, RIGHT, BOTTOM
    }

    private OnSwipeTouchListener mOnSwipeTouchListener = new OnSwipeTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return super.onTouch(v, event);
        }

        @Override
        public boolean onSwipeLeft(View view) {
            mShouldShow = false;
            animateOutLeft();
            return onSwiped(SwipeDirection.LEFT);
        }

        @Override
        public boolean onSwipeTop(View view) {
            mShouldShow = false;
            animateOutTop(false);
            return onSwiped(SwipeDirection.TOP);
        }

        @Override
        public boolean onSwipeRight(View view) {
            mShouldShow = false;
            animateOutRight();
            return onSwiped(SwipeDirection.RIGHT);
        }

        @Override
        public boolean onSwipeBottom(View view) {
            mShouldShow = false;
            animateOutTop(true);
            return onSwiped(SwipeDirection.BOTTOM);
        }
    };

    protected abstract View getView(ViewGroup parent);

    protected abstract int getTranslationY();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        EventBus.getDefault().register(this);
        mWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        addToWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        initContentView();
        animateIn();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mViewVisible) {
            mShouldStop = true;
            animateOutTop(false);
            return;
        }
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(NavigationBarEvent event) {
        animateOutTop(false);
        switch (event.which) {
            case NavigationBarEvent.BACK:
                // Do nothing extra
                break;
            case NavigationBarEvent.HOME:
                // Seems to work pretty well
                Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No HOME activity found", e);
                }
                break;
            case NavigationBarEvent.RECENTS:
                // Do nothing extra
                break;
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

    private void initContentView() {
        Log.d(TAG, "initContentView");
        mView.removeAllViews();
        mContentView = getView(mView);
        mView.addView(mContentView);
        mContentView.setTranslationY(-getCalculatedTranslationY());
        mWM.updateViewLayout(mView, getLayoutParams());
    }

    private ViewGroup.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, getCalculatedTranslationY(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : (showAboveStatusBar() ? WindowManager.LayoutParams.TYPE_SYSTEM_ERROR : WindowManager.LayoutParams.TYPE_PRIORITY_PHONE),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER;
        return layoutParams;
    }

    private int getCalculatedTranslationY() {
        int i = getTranslationY() + BOUNCE_SPACE;
        if (null != mContentView) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mContentView.getLayoutParams();
            i += params.topMargin + params.bottomMargin;
        }
        return i;
    }

    protected void restartTimeoutTimer() {
        cancelTimeoutTimer();
        int timeout = getTimeout();
        if (timeout > 0) {
            mTimer = new Timer();
            mTimer.schedule(mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mView.post(new Runnable() {
                        @Override
                        public void run() {
                            mShouldShow = false;
                            animateOutTop(false);
                        }
                    });
                }
            }, getTimeout());
        }
    }

    protected void cancelTimeoutTimer() {
        if (null != mTimerTask)
            mTimerTask.cancel();
        if (null != mTimer)
            mTimer.cancel();
    }

    protected int getTimeout() {
        return DEFAULT_TIMEOUT;
    }

    protected int getNotificationWidth() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    protected int getSlideDuration() {
        return DEFAULT_SLIDE_DURATION;
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

    /*
     * Whether or not to accept swipe events
     */
    protected boolean isSwipeEnabled() {
        return false;
    }

    /*
     * Callback for swipe actions
     */
    protected boolean onSwiped(SwipeDirection direction) {
        return false;
    }

    private SimpleAnimatorListener mAnimateInListener = new SimpleAnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
            mViewVisible = true;
            mView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            restartTimeoutTimer();
        }
    };

    private SimpleAnimatorListener mAnimateOutListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            cancelTimeoutTimer();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mViewVisible = false;
            if (null != mView) {
                mView.setVisibility(View.INVISIBLE);
                mContentView.setTranslationX(0);
                mContentView.setTranslationY(-getCalculatedTranslationY());
                if (mShouldShow) {
                    Log.d(TAG, "should show");
                    animateIn();
                }
            }
            mShouldShow = false;
            if (mShouldStop) {
                Log.d(TAG, "should stop");
                mShouldStop = false;
                removeFromWindow();
                onDestroy();
            }
        }
    };

    protected void animateIn() {
        Log.d(TAG, "animateIn");
        if (!mViewVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", 0);
            animator.setDuration(getSlideDuration());
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addListener(mAnimateInListener);
            animator.start();
        } else {
            mShouldShow = true;
        }
    }

    protected void animateOutLeft() {
        Log.d(TAG, "animateOutLeft");
        if (mViewVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationX", -mContentView.getWidth());
            animator.setDuration(getSlideDuration());
            animator.setInterpolator(accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

    protected void animateOutTop(boolean anticipate) {
        Log.d(TAG, "animateOutTop");
        if (mViewVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationY", -getCalculatedTranslationY());
            animator.setDuration(getSlideDuration());
            animator.setInterpolator(anticipate ? anticipateInterpolator : accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

    protected void animateOutRight() {
        Log.d(TAG, "animateOutRight");
        if (mViewVisible) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mContentView, "translationX", mContentView.getWidth());
            animator.setDuration(getSlideDuration());
            animator.setInterpolator(accelerateInterpolator);
            animator.addListener(mAnimateOutListener);
            animator.start();
        }
    }

    public static class NavigationBarEvent {

        public static final int BACK = 0;
        public static final int HOME = 1;
        public static final int RECENTS = 2;

        public int which;

        public NavigationBarEvent(int which) {
            this.which = which;
        }

    }
}