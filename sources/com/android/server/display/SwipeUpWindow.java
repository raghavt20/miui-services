package com.android.server.display;

import android.R;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.server.display.animation.DynamicAnimation;
import com.android.server.display.animation.FloatPropertyCompat;
import com.android.server.display.animation.SpringAnimation;
import com.android.server.display.animation.SpringForce;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.stability.DumpSysInfoUtil;

/* loaded from: classes.dex */
public class SwipeUpWindow {
    private static final float BG_INIT_ALPHA = 0.6f;
    private static final boolean DEBUG = false;
    private static final float DEFAULT_DAMPING = 0.95f;
    private static final float DEFAULT_STIFFNESS = 322.27f;
    private static final float DISTANCE_DEBOUNCE = 200.0f;
    private static final int ICON_HEIGHT = 28;
    private static final float ICON_OFFSET = 30.0f;
    private static final float ICON_TIP_SHOW_DAMPING = 1.0f;
    private static final float ICON_TIP_SHOW_STIFFNESS = 39.478416f;
    private static final float ICON_VIEW_BOTTOM = 117.0f;
    private static final int ICON_WIDTH = 28;
    private static final int LOCK_SATE_START_DELAY = 1000;
    private static final float LOCK_STATE_LONG_DAMPING = 1.0f;
    private static final float LOCK_STATE_LONG_STIFFNESS = 6.86f;
    private static final int MSG_ICON_LOCK_ANIMATION = 102;
    private static final int MSG_ICON_TIP_HIDE_ANIMATION = 103;
    private static final int MSG_LOCK_STATE_WITH_LONG_ANIMATION = 101;
    private static final int MSG_RELEASE_WINDOW = 105;
    private static final int MSG_SCREEN_OFF = 104;
    private static final int SCREEN_HEIGHT = 2520;
    private static final int SCREEN_OFF_DELAY = 6000;
    private static final int SCREEN_WIDTH = 1080;
    private static final float SCROLL_DAMPING = 0.9f;
    private static final float SCROLL_STIFFNESS = 986.96045f;
    private static final float SHOW_STATE_DAMPING = 1.0f;
    private static final float SHOW_STATE_STIFFNESS = 157.91367f;
    public static final String TAG = "SwipeUpWindow";
    private static final int TIP_HEIGHT = 25;
    private static final float TIP_INIT_ALPHA = 0.4f;
    private static final float TIP_OFFSET = 50.0f;
    private static final int TIP_TEXT_SIZE = 19;
    private static final float TIP_VIEW_BOTTOM = 63.0f;
    private BlackLinearGradientView mBlackLinearGradientView;
    private Context mContext;
    private SpringAnimation mGradientShadowSpringAnimation;
    private Handler mHandler;
    private AnimatedVectorDrawable mIconDrawable;
    private DualSpringAnimation mIconSpringAnimation;
    private ImageView mIconView;
    private PowerManager mPowerManager;
    private DynamicAnimation.OnAnimationEndListener mPreOnAnimationEndListener;
    private boolean mScrollAnimationNeedInit;
    private FrameLayout mSwipeUpFrameLayout;
    private WindowManager.LayoutParams mSwipeUpLayoutParams;
    private boolean mSwipeUpWindowShowing;
    private DualSpringAnimation mTipSpringAnimation;
    private TextView mTipView;
    private float mUnlockDistance;
    private VelocityTracker mVelocityTracker;
    private WindowManager mWindowManager;
    private int mPreTopColor = -1;
    private int mPreBottomColor = -1;
    private int mPreBlurLevel = -1;
    private float mStartPer = 1.0f;
    private DynamicAnimation.OnAnimationEndListener mWakeStateAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.3
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            Message msg = SwipeUpWindow.this.mHandler.obtainMessage(101);
            SwipeUpWindow.this.mHandler.sendMessageDelayed(msg, 1000L);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mLockStateLongAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.4
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(102);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mLockStateShortAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.5
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(104);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mUnlockStateAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.6
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(105);
        }
    };
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() { // from class: com.android.server.display.SwipeUpWindow.7
        private float startTouchY;

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            SwipeUpWindow.this.initVelocityTrackerIfNotExists();
            switch (event.getAction()) {
                case 0:
                    if (SwipeUpWindow.this.mVelocityTracker == null) {
                        SwipeUpWindow.this.mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        SwipeUpWindow.this.mVelocityTracker.clear();
                    }
                    SwipeUpWindow.this.mVelocityTracker.addMovement(event);
                    SwipeUpWindow swipeUpWindow = SwipeUpWindow.this;
                    swipeUpWindow.mStartPer = swipeUpWindow.mAnimationState.getCurrentState();
                    this.startTouchY = event.getRawY();
                    SwipeUpWindow.this.resetIconAnimation();
                    SwipeUpWindow.this.mScrollAnimationNeedInit = true;
                    return true;
                case 1:
                case 3:
                    VelocityTracker velocityTracker = SwipeUpWindow.this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000);
                    float curVelocitY = velocityTracker.getYVelocity();
                    SwipeUpWindow.this.recycleVelocityTracker();
                    if (SwipeUpWindow.this.mStartPer != SwipeUpWindow.this.mAnimationState.getPerState()) {
                        float distance = event.getRawY() - this.startTouchY;
                        if (curVelocitY < -1000.0f && (-distance) >= 200.0f) {
                            SwipeUpWindow.this.setUnlockState();
                            return true;
                        }
                        if (curVelocitY > 1000.0f && distance >= 200.0f) {
                            SwipeUpWindow.this.setLockStateWithShortAnimation();
                            return true;
                        }
                        if ((-distance) > SwipeUpWindow.this.mUnlockDistance) {
                            SwipeUpWindow.this.setUnlockState();
                            return true;
                        }
                        if (distance > SwipeUpWindow.this.mUnlockDistance) {
                            SwipeUpWindow.this.setLockStateWithShortAnimation();
                            return true;
                        }
                        SwipeUpWindow.this.setWakeState();
                        return true;
                    }
                    return true;
                case 2:
                    SwipeUpWindow.this.mHandler.removeMessages(101);
                    SwipeUpWindow.this.mVelocityTracker.addMovement(event);
                    float offsetY = event.getRawY() - this.startTouchY;
                    float per = SwipeUpWindow.this.mStartPer + (offsetY / (SwipeUpWindow.this.mScreenHeight / 3));
                    float frictionY = MathUtils.min(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, SwipeUpWindow.this.afterFrictionValue(offsetY / r5.mScreenHeight, 1.0f));
                    float alpha = MathUtils.min(1.0f, (3.0f * per) + 1.0f);
                    float tipY = SwipeUpWindow.this.mTipView.getTop() + (150.0f * frictionY);
                    float tipAlpha = alpha * SwipeUpWindow.TIP_INIT_ALPHA;
                    float iconY = SwipeUpWindow.this.mIconView.getTop() + (50.0f * frictionY);
                    if (!SwipeUpWindow.this.mScrollAnimationNeedInit) {
                        SwipeUpWindow.this.mGradientShadowSpringAnimation.animateToFinalPosition(per);
                        SwipeUpWindow.this.mIconSpringAnimation.animateToFinalPosition(iconY, alpha);
                        SwipeUpWindow.this.mTipSpringAnimation.animateToFinalPosition(tipY, tipAlpha);
                        return true;
                    }
                    SwipeUpWindow.this.mScrollAnimationNeedInit = false;
                    SwipeUpWindow.this.startGradientShadowAnimation(SwipeUpWindow.SCROLL_STIFFNESS, SwipeUpWindow.SCROLL_DAMPING, per);
                    SwipeUpWindow.this.mIconSpringAnimation.cancel();
                    SwipeUpWindow.this.mTipSpringAnimation.cancel();
                    SwipeUpWindow swipeUpWindow2 = SwipeUpWindow.this;
                    SpringAnimation creatSpringAnimation = swipeUpWindow2.creatSpringAnimation(swipeUpWindow2.mIconView, SpringAnimation.Y, SwipeUpWindow.SCROLL_STIFFNESS, SwipeUpWindow.SCROLL_DAMPING, iconY);
                    SwipeUpWindow swipeUpWindow3 = SwipeUpWindow.this;
                    swipeUpWindow2.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation, swipeUpWindow3.creatSpringAnimation(swipeUpWindow3.mIconView, SpringAnimation.ALPHA, SwipeUpWindow.SCROLL_STIFFNESS, SwipeUpWindow.SCROLL_DAMPING, alpha));
                    SwipeUpWindow swipeUpWindow4 = SwipeUpWindow.this;
                    SpringAnimation creatSpringAnimation2 = swipeUpWindow4.creatSpringAnimation(swipeUpWindow4.mTipView, SpringAnimation.Y, SwipeUpWindow.SCROLL_STIFFNESS, SwipeUpWindow.SCROLL_DAMPING, tipY);
                    SwipeUpWindow swipeUpWindow5 = SwipeUpWindow.this;
                    swipeUpWindow4.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation2, swipeUpWindow5.creatSpringAnimation(swipeUpWindow5.mTipView, SpringAnimation.ALPHA, SwipeUpWindow.SCROLL_STIFFNESS, SwipeUpWindow.SCROLL_DAMPING, tipAlpha));
                    SwipeUpWindow.this.mIconSpringAnimation.start();
                    SwipeUpWindow.this.mTipSpringAnimation.start();
                    return true;
                default:
                    return true;
            }
        }
    };
    private int mScreenHeight = Integer.MAX_VALUE;
    private int mScreenWidth = Integer.MAX_VALUE;
    private AnimationState mAnimationState = new AnimationState();

    public SwipeUpWindow(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new SwipeUpWindowHandler(looper);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(DumpSysInfoUtil.WINDOW);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        updateSettings(false);
        updateSizeInfo();
    }

    private void updateSizeInfo() {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        Display[] displays = displayManager.getDisplays("android.hardware.display.category.ALL_INCLUDING_DISABLED");
        DisplayInfo outDisplayInfo = new DisplayInfo();
        for (Display display : displays) {
            display.getDisplayInfo(outDisplayInfo);
            if (outDisplayInfo.type == 1 && this.mScreenWidth > outDisplayInfo.logicalWidth) {
                this.mScreenWidth = outDisplayInfo.logicalWidth;
                this.mScreenHeight = outDisplayInfo.logicalHeight;
            }
        }
        if (this.mScreenWidth == Integer.MAX_VALUE) {
            this.mScreenWidth = SCREEN_WIDTH;
            this.mScreenHeight = SCREEN_HEIGHT;
        }
        this.mUnlockDistance = this.mScreenHeight * 0.2f;
        Slog.i(TAG, "updateSizeInfo: (h=" + this.mScreenHeight + ", w=" + this.mScreenWidth + ")");
    }

    public void removeSwipeUpWindow() {
    }

    public void releaseSwipeWindow() {
        releaseSwipeWindow("unknow");
    }

    public void releaseSwipeWindow(String reason) {
        if (!this.mSwipeUpWindowShowing) {
            return;
        }
        Slog.i(TAG, "release swipe up window: " + reason);
        this.mSwipeUpWindowShowing = false;
        updateSettings(false);
        this.mHandler.removeCallbacksAndMessages(null);
        SpringAnimation springAnimation = this.mGradientShadowSpringAnimation;
        if (springAnimation != null) {
            springAnimation.cancel();
            this.mGradientShadowSpringAnimation = null;
        }
        FrameLayout frameLayout = this.mSwipeUpFrameLayout;
        if (frameLayout != null) {
            this.mWindowManager.removeViewImmediate(frameLayout);
            this.mSwipeUpFrameLayout = null;
        }
    }

    public void showSwipeUpWindow() {
        if (this.mSwipeUpWindowShowing) {
            return;
        }
        updateSettings(true);
        if (this.mSwipeUpFrameLayout == null) {
            initSwipeUpWindow();
        }
        this.mSwipeUpWindowShowing = true;
        this.mWindowManager.addView(this.mSwipeUpFrameLayout, this.mSwipeUpLayoutParams);
        Slog.i(TAG, "show swipe up window");
    }

    private void initSwipeUpWindow() {
        this.mSwipeUpFrameLayout = new SwipeUpFrameLayout(new ContextThemeWrapper(this.mContext, R.style.Theme.NoTitleBar.Fullscreen));
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2026, 25297156, -3);
        this.mSwipeUpLayoutParams = layoutParams;
        layoutParams.inputFeatures |= 2;
        this.mSwipeUpLayoutParams.layoutInDisplayCutoutMode = 3;
        this.mSwipeUpLayoutParams.gravity = 8388659;
        this.mSwipeUpLayoutParams.setTitle(TAG);
        this.mSwipeUpLayoutParams.screenOrientation = 1;
        this.mSwipeUpFrameLayout.setOnTouchListener(this.mOnTouchListener);
        this.mSwipeUpFrameLayout.setLongClickable(true);
        this.mSwipeUpFrameLayout.setFocusable(true);
        initBlackLinearGradientView();
        initIconView();
        initTipView();
        initGradientShadowAnimation();
    }

    private void initBlackLinearGradientView() {
        FrameLayout.LayoutParams backViewParams = new FrameLayout.LayoutParams(-1, -1, 17);
        BlackLinearGradientView blackLinearGradientView = new BlackLinearGradientView(this.mContext);
        this.mBlackLinearGradientView = blackLinearGradientView;
        blackLinearGradientView.setBackgroundResource(0);
        this.mSwipeUpFrameLayout.addView(this.mBlackLinearGradientView, backViewParams);
    }

    private void initIconView() {
        ImageView imageView = new ImageView(this.mContext);
        this.mIconView = imageView;
        imageView.setImageResource(285737371);
        this.mIconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        this.mIconView.setAlpha(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        this.mIconView.setBackgroundResource(0);
        FrameLayout.LayoutParams iconViewParams = new FrameLayout.LayoutParams(transformDpToPx(28.0f), transformDpToPx(28.0f), 49);
        iconViewParams.setMargins(0, this.mScreenHeight - transformDpToPx(145.0f), 0, 0);
        this.mSwipeUpFrameLayout.addView(this.mIconView, iconViewParams);
        if (this.mIconView.getDrawable() instanceof AnimatedVectorDrawable) {
            this.mIconDrawable = (AnimatedVectorDrawable) this.mIconView.getDrawable();
        } else {
            Slog.i(TAG, "icon drawable get incompatible class");
        }
    }

    private void initTipView() {
        TextView textView = new TextView(this.mContext);
        this.mTipView = textView;
        textView.setGravity(17);
        this.mTipView.setText(286196657);
        this.mTipView.setTextSize(2, 19.0f);
        Typeface typeface = Typeface.create("mipro-medium", 0);
        this.mTipView.setTypeface(typeface);
        this.mTipView.setTextColor(1728053247);
        this.mTipView.setAlpha(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        this.mTipView.setBackgroundResource(0);
        FrameLayout.LayoutParams tipViewParams = new FrameLayout.LayoutParams(-2, -2, 49);
        tipViewParams.setMargins(0, this.mScreenHeight - transformDpToPx(88.0f), 0, 0);
        this.mSwipeUpFrameLayout.addView(this.mTipView, tipViewParams);
    }

    private void initGradientShadowAnimation() {
        this.mGradientShadowSpringAnimation = new SpringAnimation(this.mAnimationState, new FloatPropertyCompat<AnimationState>("perState") { // from class: com.android.server.display.SwipeUpWindow.1
            @Override // com.android.server.display.animation.FloatPropertyCompat
            public float getValue(AnimationState object) {
                return object.getPerState();
            }

            @Override // com.android.server.display.animation.FloatPropertyCompat
            public void setValue(AnimationState object, float value) {
                object.setPerState(value);
            }
        });
        SpringForce springForce = new SpringForce();
        springForce.setStiffness(DEFAULT_STIFFNESS);
        springForce.setDampingRatio(DEFAULT_DAMPING);
        this.mGradientShadowSpringAnimation.setSpring(springForce);
        this.mGradientShadowSpringAnimation.setMinimumVisibleChange(0.00390625f);
        this.mGradientShadowSpringAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() { // from class: com.android.server.display.SwipeUpWindow.2
            @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationUpdateListener
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                SwipeUpWindow.this.updateBlackView();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startSwipeUpAnimation() {
        if (!this.mSwipeUpWindowShowing) {
            return;
        }
        this.mPreTopColor = -16777216;
        this.mPreBottomColor = -16777216;
        this.mPreBlurLevel = -1;
        this.mAnimationState.setPerState(1.0f);
        updateBlur(1.0f);
        setWakeState();
        this.mHandler.removeMessages(104);
        Message msg = this.mHandler.obtainMessage(104);
        this.mHandler.sendMessageDelayed(msg, 6000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setWakeState() {
        startGradientShadowAnimation(SHOW_STATE_STIFFNESS, 1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, this.mWakeStateAnimationEndListener, 0.00390625f);
        resetIconAnimation();
        if (this.mIconView.getAlpha() <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            prepareIconAndTipAnimation();
        }
        playIconAndTipShowAnimation();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLockStateWithLongAnimation() {
        startGradientShadowAnimation(LOCK_STATE_LONG_STIFFNESS, 1.0f, 1.0f, this.mLockStateLongAnimationEndListener, 0.00390625f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLockStateWithShortAnimation() {
        startGradientShadowAnimation(DEFAULT_STIFFNESS, DEFAULT_DAMPING, 1.0f, this.mLockStateShortAnimationEndListener, 0.00390625f);
        playIconAndTipHideAnimation();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUnlockState() {
        this.mHandler.removeCallbacksAndMessages(null);
        startGradientShadowAnimation(DEFAULT_STIFFNESS, DEFAULT_DAMPING, -1.0f, this.mUnlockStateAnimationEndListener, 1.0f);
        playIconAndTipHideAnimation();
    }

    private void startGradientShadowAnimation(float finalPosition) {
        startGradientShadowAnimation(DEFAULT_STIFFNESS, DEFAULT_DAMPING, finalPosition);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startGradientShadowAnimation(float stiffness, float dampingRatio, float finalPosition) {
        startGradientShadowAnimation(stiffness, dampingRatio, finalPosition, null, 0.00390625f);
    }

    private void startGradientShadowAnimation(float stiffness, float dampingRatio, float finalPosition, DynamicAnimation.OnAnimationEndListener onAnimationEndListener, float minimumVisibleChange) {
        this.mGradientShadowSpringAnimation.cancel();
        SpringForce springForce = new SpringForce();
        springForce.setStiffness(stiffness);
        springForce.setDampingRatio(dampingRatio);
        springForce.setFinalPosition(finalPosition);
        this.mGradientShadowSpringAnimation.setSpring(springForce);
        DynamicAnimation.OnAnimationEndListener onAnimationEndListener2 = this.mPreOnAnimationEndListener;
        if (onAnimationEndListener2 != null) {
            this.mGradientShadowSpringAnimation.removeEndListener(onAnimationEndListener2);
        }
        if (onAnimationEndListener != null) {
            this.mGradientShadowSpringAnimation.addEndListener(onAnimationEndListener);
        }
        this.mPreOnAnimationEndListener = onAnimationEndListener;
        this.mGradientShadowSpringAnimation.setMinimumVisibleChange(minimumVisibleChange);
        this.mGradientShadowSpringAnimation.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBlackView() {
        float per = this.mAnimationState.getPerState();
        float topAlpha = constraintAlpha(per + 1.0f);
        float bottomAlpha = constraintAlpha(0.6f + per);
        int topColor = calculateBlackAlpha(topAlpha);
        int bottomColor = calculateBlackAlpha(bottomAlpha);
        if (topColor != this.mPreTopColor || bottomColor != this.mPreBottomColor) {
            this.mPreTopColor = topColor;
            this.mPreBottomColor = bottomColor;
            this.mBlackLinearGradientView.setLinearGradientColor(new int[]{topColor, bottomColor});
        }
        float blurLevel = constraintBlur(MathUtils.min(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, per) + 1.0f);
        updateBlur(blurLevel);
    }

    private int calculateBlackAlpha(float alpha) {
        int blackAlpha = (int) (255.0f * alpha);
        return (blackAlpha << 24) | 0;
    }

    private void updateBlur(float level) {
        setBackgroudBlur((int) (100.0f * level));
    }

    private void setBackgroudBlur(int blurRadius) {
        FrameLayout frameLayout = this.mSwipeUpFrameLayout;
        if (frameLayout == null || this.mPreBlurLevel == blurRadius) {
            return;
        }
        this.mPreBlurLevel = blurRadius;
        ViewRootImpl viewRootImpl = frameLayout.getViewRootImpl();
        if (viewRootImpl == null) {
            Slog.d(TAG, "mViewRootImpl is null");
            return;
        }
        SurfaceControl surfaceControl = viewRootImpl.getSurfaceControl();
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setBackgroundBlurRadius(surfaceControl, blurRadius);
        transaction.show(surfaceControl);
        transaction.apply();
    }

    private float constraintAlpha(float alpha) {
        if (alpha > 1.0f) {
            return 1.0f;
        }
        return alpha < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : alpha;
    }

    private float constraintBlur(float level) {
        if (level > 1.0f) {
            return 1.0f;
        }
        return level < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : level;
    }

    public int transformDpToPx(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(1, dp, ctx.getResources().getDisplayMetrics());
    }

    public int transformDpToPx(float dp) {
        return (int) TypedValue.applyDimension(1, dp, this.mContext.getResources().getDisplayMetrics());
    }

    public void cancelScreenOffDelay() {
        this.mHandler.removeCallbacksAndMessages(null);
        SpringAnimation springAnimation = this.mGradientShadowSpringAnimation;
        if (springAnimation != null) {
            DynamicAnimation.OnAnimationEndListener onAnimationEndListener = this.mPreOnAnimationEndListener;
            if (onAnimationEndListener != null) {
                springAnimation.removeEndListener(onAnimationEndListener);
                this.mPreOnAnimationEndListener = null;
            }
            this.mGradientShadowSpringAnimation.cancel();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOff() {
        this.mHandler.removeCallbacksAndMessages(null);
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 3, 0);
    }

    private void updateSettings(boolean z) {
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "swipe_up_is_showing", z ? 1 : 0, -2);
    }

    /* loaded from: classes.dex */
    private final class SwipeUpWindowHandler extends Handler {
        public SwipeUpWindowHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    SwipeUpWindow.this.setLockStateWithLongAnimation();
                    return;
                case 102:
                    SwipeUpWindow.this.playIconAnimation();
                    return;
                case 103:
                    SwipeUpWindow.this.playIconAndTipHideAnimation();
                    return;
                case 104:
                    SwipeUpWindow.this.handleScreenOff();
                    return;
                case 105:
                    SwipeUpWindow.this.releaseSwipeWindow();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public float afterFrictionValue(float value, float range) {
        if (range == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        float t = value >= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? 1.0f : -1.0f;
        float per = MathUtils.min(MathUtils.abs(value) / range, 1.0f);
        return (((((per * per) * per) / 3.0f) - (per * per)) + per) * t * range;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playIconAnimation() {
        AnimatedVectorDrawable animatedVectorDrawable = this.mIconDrawable;
        if (animatedVectorDrawable == null) {
            return;
        }
        animatedVectorDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() { // from class: com.android.server.display.SwipeUpWindow.8
            @Override // android.graphics.drawable.Animatable2.AnimationCallback
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);
                SwipeUpWindow.this.mHandler.sendEmptyMessage(103);
            }
        });
        this.mIconDrawable.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetIconAnimation() {
        if (this.mIconDrawable == null) {
            return;
        }
        this.mIconDrawable.reset();
    }

    private void prepareIconAndTipAnimation() {
        this.mIconView.setY(r0.getTop() + transformDpToPx(ICON_OFFSET));
        this.mIconView.setAlpha(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        this.mTipView.setY(r0.getTop() + transformDpToPx(50.0f));
        this.mTipView.setAlpha(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
    }

    private void playIconAndTipShowAnimation() {
        DualSpringAnimation dualSpringAnimation = this.mIconSpringAnimation;
        if (dualSpringAnimation != null) {
            dualSpringAnimation.cancel();
        }
        DualSpringAnimation dualSpringAnimation2 = this.mTipSpringAnimation;
        if (dualSpringAnimation2 != null) {
            dualSpringAnimation2.cancel();
        }
        this.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mIconView, SpringAnimation.Y, ICON_TIP_SHOW_STIFFNESS, 1.0f, this.mIconView.getTop()), creatSpringAnimation(this.mIconView, SpringAnimation.ALPHA, ICON_TIP_SHOW_STIFFNESS, 1.0f, 1.0f));
        this.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mTipView, SpringAnimation.Y, ICON_TIP_SHOW_STIFFNESS, 1.0f, this.mTipView.getTop()), creatSpringAnimation(this.mTipView, SpringAnimation.ALPHA, ICON_TIP_SHOW_STIFFNESS, 1.0f, 1.0f));
        this.mIconSpringAnimation.start();
        this.mTipSpringAnimation.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playIconAndTipHideAnimation() {
        DualSpringAnimation dualSpringAnimation = this.mIconSpringAnimation;
        if (dualSpringAnimation != null) {
            dualSpringAnimation.cancel();
        }
        DualSpringAnimation dualSpringAnimation2 = this.mTipSpringAnimation;
        if (dualSpringAnimation2 != null) {
            dualSpringAnimation2.cancel();
        }
        this.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mIconView, SpringAnimation.Y, this.mIconView.getTop()), creatSpringAnimation(this.mIconView, SpringAnimation.ALPHA, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
        this.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mTipView, SpringAnimation.Y, this.mTipView.getTop()), creatSpringAnimation(this.mTipView, SpringAnimation.ALPHA, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
        this.mIconSpringAnimation.start();
        this.mTipSpringAnimation.start();
    }

    private SpringAnimation creatSpringAnimation(View view, DynamicAnimation.ViewProperty viewProperty, float finalPosition) {
        return creatSpringAnimation(view, viewProperty, DEFAULT_STIFFNESS, DEFAULT_DAMPING, finalPosition);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SpringAnimation creatSpringAnimation(View view, DynamicAnimation.ViewProperty viewProperty, float stiffness, float damping, float finalPosition) {
        SpringAnimation springAnimation = new SpringAnimation(view, viewProperty);
        SpringForce springForce = new SpringForce(finalPosition);
        springForce.setStiffness(stiffness);
        springForce.setDampingRatio(damping);
        springAnimation.setSpring(springForce);
        springAnimation.setMinimumVisibleChange(0.00390625f);
        return springAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SwipeUpFrameLayout extends FrameLayout {
        public SwipeUpFrameLayout(Context context) {
            super(context);
        }

        @Override // android.view.View
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            SwipeUpWindow.this.startSwipeUpAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AnimationState {
        public static final float STATE_LOCK = 1.0f;
        public static final float STATE_UNLOCK = -1.0f;
        public static final float STATE_WAKE = 0.0f;
        private float perState;

        public AnimationState() {
            this.perState = 1.0f;
        }

        public AnimationState(float curState) {
            this.perState = curState;
        }

        public float getPerState() {
            return this.perState;
        }

        public void setPerState(float perState) {
            this.perState = perState;
        }

        public float getCurrentState() {
            float f = this.perState;
            if (f >= 1.0f) {
                return 1.0f;
            }
            if (f > -1.0f) {
                return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            }
            return -1.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DualSpringAnimation {
        private SpringAnimation mSpringAnimationAlpha;
        private SpringAnimation mSpringAnimationY;

        public DualSpringAnimation(SpringAnimation springAnimationY, SpringAnimation springAnimationAlpha) {
            this.mSpringAnimationY = springAnimationY;
            this.mSpringAnimationAlpha = springAnimationAlpha;
        }

        public void start() {
            this.mSpringAnimationY.start();
            this.mSpringAnimationAlpha.start();
        }

        public void cancel() {
            this.mSpringAnimationY.cancel();
            this.mSpringAnimationAlpha.cancel();
        }

        public void skipToEnd() {
            this.mSpringAnimationY.skipToEnd();
            this.mSpringAnimationAlpha.skipToEnd();
        }

        public void animateToFinalPosition(float y, float alpha) {
            this.mSpringAnimationY.animateToFinalPosition(y);
            this.mSpringAnimationAlpha.animateToFinalPosition(alpha);
        }
    }
}
