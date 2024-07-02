package com.android.server.policy;

import android.R;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Property;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.server.display.RampRateController;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.input.pocketmode.MiuiPocketModeManager;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.abtest.BuildConfig;
import miui.hardware.input.InputFeature;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiScreenOnProximityLock {
    private static final boolean DEBUG = true;
    private static final int EVENT_PREPARE_VIEW = 1;
    private static final int EVENT_RELEASE = 0;
    private static final int EVENT_RELEASE_VIEW = 3;
    private static final int EVENT_SET_HINT_CONTAINER = 4;
    private static final int EVENT_SHOW_VIEW = 2;
    private static final int FIRST_CHANGE_TIMEOUT = 1500;
    public static final boolean IS_JP_KDDI = InputFeature.IS_SUPPORT_KDDI;
    private static final String LOG_TAG = "MiuiScreenOnProximityLock";
    public static final String SKIP_AQUIRE_FINGER_WAKE_UP_DETAIL = "android.policy:FINGERPRINT";
    public static final String SKIP_AQUIRE_UNFOLD_WAKE_UP_DETAIL = "server.display:unfold";
    private Context mContext;
    private Handler mHandler;
    private boolean mHideNavigationBarWhenForceShow;
    protected ViewGroup mHintContainer;
    protected View mHintView;
    protected MiuiKeyguardServiceDelegate mKeyguardDelegate;
    private MiuiPocketModeManager mMiuiPocketModeManager;
    private long mAquiredTime = 0;
    private final MiuiPocketModeSensorWrapper.ProximitySensorChangeListener mSensorListener = new MiuiPocketModeSensorWrapper.ProximitySensorChangeListener() { // from class: com.android.server.policy.MiuiScreenOnProximityLock.1
        @Override // com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper.ProximitySensorChangeListener
        public void onSensorChanged(boolean tooClose) {
            if (tooClose) {
                MiuiScreenOnProximityLock.this.mHandler.removeMessages(0);
            } else {
                MiuiScreenOnProximityLock.this.mHandler.sendEmptyMessage(0);
            }
        }
    };
    protected boolean mFrontFingerprintSensor = FeatureParser.getBoolean("front_fingerprint_sensor", false);

    public MiuiScreenOnProximityLock(Context context, MiuiKeyguardServiceDelegate keyguardDelegate, Looper looper) {
        this.mContext = context;
        this.mKeyguardDelegate = keyguardDelegate;
        this.mHandler = new Handler(looper) { // from class: com.android.server.policy.MiuiScreenOnProximityLock.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                synchronized (MiuiScreenOnProximityLock.this) {
                    switch (msg.what) {
                        case 0:
                            Slog.d(MiuiScreenOnProximityLock.LOG_TAG, "far from the screen for a certain time, release proximity sensor...");
                            MiuiScreenOnProximityLock.this.release(false);
                            break;
                        case 1:
                            MiuiScreenOnProximityLock.this.prepareHintWindow();
                            break;
                        case 2:
                            MiuiScreenOnProximityLock.this.showHint();
                            break;
                        case 3:
                            MiuiScreenOnProximityLock.this.releaseHintWindow(((Boolean) msg.obj).booleanValue());
                            break;
                        case 4:
                            MiuiScreenOnProximityLock.this.setHintContainer();
                            break;
                    }
                }
            }
        };
        this.mMiuiPocketModeManager = new MiuiPocketModeManager(this.mContext);
    }

    public synchronized boolean isHeld() {
        return this.mAquiredTime != 0;
    }

    public synchronized void aquire() {
        if (!isHeld()) {
            Slog.d(LOG_TAG, "aquire");
            this.mAquiredTime = System.currentTimeMillis();
            this.mHandler.sendEmptyMessage(1);
            this.mHandler.sendEmptyMessageDelayed(0, 1500L);
            this.mMiuiPocketModeManager.registerListener(this.mSensorListener);
        }
    }

    public synchronized boolean release(boolean isNowRelease) {
        if (!isHeld()) {
            return false;
        }
        Slog.d(LOG_TAG, BuildConfig.BUILD_TYPE);
        this.mAquiredTime = 0L;
        this.mMiuiPocketModeManager.unregisterListener();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        Message releaseViewMessage = this.mHandler.obtainMessage(3);
        releaseViewMessage.obj = Boolean.valueOf(isNowRelease);
        this.mHandler.sendMessage(releaseViewMessage);
        return true;
    }

    public boolean shouldBeBlocked(boolean ScreenOnFully, KeyEvent event) {
        if (shouldBeBlockedInternal(event, ScreenOnFully)) {
            forceShow();
            return true;
        }
        return false;
    }

    private boolean shouldBeBlockedInternal(KeyEvent event, boolean ScreenOnFully) {
        if (event == null || !isHeld() || !ScreenOnFully || event.getAction() == 1) {
            return false;
        }
        switch (event.getKeyCode()) {
            case 3:
                return !this.mFrontFingerprintSensor;
            case 24:
            case 25:
                AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
                return !audioManager.isMusicActive();
            case 26:
            case UsbKeyboardUtil.PACKET_64 /* 79 */:
            case 85:
            case 86:
            case 87:
            case 126:
            case RampRateController.RateStateRecord.MODIFIER_RATE_ALL /* 127 */:
                return false;
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHintContainer() {
        ViewGroup viewGroup = this.mHintContainer;
        if (viewGroup != null && (viewGroup.getSystemUiVisibility() & 4) == 0) {
            this.mHintContainer.setSystemUiVisibility(3842);
            this.mHideNavigationBarWhenForceShow = true;
        }
        this.mHandler.sendEmptyMessageDelayed(2, this.mMiuiPocketModeManager.getStateStableDelay());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forceShow() {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(4));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareHintWindow() {
        FrameLayout frameLayout = new FrameLayout(new ContextThemeWrapper(this.mContext, R.style.Theme.Holo));
        this.mHintContainer = frameLayout;
        frameLayout.setOnTouchListener(new View.OnTouchListener() { // from class: com.android.server.policy.MiuiScreenOnProximityLock.3
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View v, MotionEvent event) {
                MiuiScreenOnProximityLock.this.forceShow();
                return true;
            }
        });
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2018, 25366784, -3);
        lp.inputFeatures |= 2;
        lp.layoutInDisplayCutoutMode = 1;
        lp.gravity = 17;
        lp.setTitle("ScreenOnProximitySensorGuide");
        WindowManager wm = (WindowManager) this.mContext.getSystemService(DumpSysInfoUtil.WINDOW);
        wm.addView(this.mHintContainer, lp);
        this.mKeyguardDelegate.enableUserActivity(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseHintWindow(boolean isNowRelease) {
        final View container = this.mHintContainer;
        if (container == null) {
            return;
        }
        View view = this.mHintView;
        if (view == null) {
            WindowManager wm = (WindowManager) this.mContext.getSystemService(DumpSysInfoUtil.WINDOW);
            wm.removeView(container);
        } else if (isNowRelease) {
            releaseReset(container, view);
            this.mHintView = null;
        } else {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, 1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            animator.setDuration(500L);
            animator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.policy.MiuiScreenOnProximityLock.4
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    View hintView = (View) ((ObjectAnimator) animation).getTarget();
                    MiuiScreenOnProximityLock.this.releaseReset(container, hintView);
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                }
            });
            animator.start();
            this.mHintView = null;
        }
        if (!this.mKeyguardDelegate.isShowingAndNotHidden()) {
            this.mKeyguardDelegate.enableUserActivity(true);
        }
        this.mHintContainer = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseReset(View container, View hintView) {
        if (hintView != null) {
            hintView.setVisibility(8);
            hintView.clearAnimation();
        }
        if (this.mHideNavigationBarWhenForceShow) {
            container.setSystemUiVisibility(3840);
            this.mHideNavigationBarWhenForceShow = false;
        }
        WindowManager wm = (WindowManager) this.mContext.getSystemService(DumpSysInfoUtil.WINDOW);
        wm.removeView(container);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHint() {
        if (!isHeld() || this.mHintView != null) {
            return;
        }
        Slog.d(LOG_TAG, "show hint...");
        this.mHintView = View.inflate(new ContextThemeWrapper(this.mContext, R.style.Theme.Holo), 285999159, this.mHintContainer);
        modTipsForKddi();
        ObjectAnimator animator = ObjectAnimator.ofFloat(this.mHintView, (Property<View, Float>) View.ALPHA, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 1.0f);
        animator.setDuration(500L);
        animator.start();
        Animation animation = new AlphaAnimation(1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        animation.setDuration(500L);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(2);
        animation.setStartOffset(500L);
        View animationView = this.mHintView.findViewById(285868222);
        animationView.startAnimation(animation);
    }

    private void modTipsForKddi() {
        if (!IS_JP_KDDI) {
            return;
        }
        TextView summaryTextView = (TextView) this.mHintView.findViewById(285868224);
        if (summaryTextView != null) {
            summaryTextView.setText(286196534);
        }
        TextView hintHasNavigationBarTextView = (TextView) this.mHintView.findViewById(285868223);
        if (hintHasNavigationBarTextView != null) {
            hintHasNavigationBarTextView.setText(286196532);
        }
    }
}
