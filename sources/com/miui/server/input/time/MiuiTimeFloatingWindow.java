package com.miui.server.input.time;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.server.input.MiuiInputThread;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/* loaded from: classes.dex */
public class MiuiTimeFloatingWindow {
    private static final long LOG_PRINT_TIME_DIFF = 300000;
    private static final int REASON_SCREEN_STATE_CHANGE = 0;
    private static final int REASON_SETTINGS_CHANGE = 1;
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "MiuiTimeFloatingWindow";
    private Choreographer mChoreographer;
    private final Context mContext;
    private DateTimeFormatter mDateTimeFormatter;
    private Handler mHandler;
    private boolean mIsFirstScreenOn;
    private boolean mIsInit;
    private boolean mIsScreenOn;
    private boolean mIsTimeFloatingWindowOn;
    private WindowManager.LayoutParams mLayoutParams;
    private PackageMonitor mPackageMonitor;
    private View mRootView;
    private volatile boolean mShowTime;
    private TextView mTimeTextView;
    private IntentFilter mTimezoneChangedIntentFilter;
    private BroadcastReceiver mTimezoneChangedReceiver;
    private WindowManager mWindowManager;
    private long mTimerForLog = 0;
    private final Runnable mDrawCallBack = new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.1
        @Override // java.lang.Runnable
        public void run() {
            MiuiTimeFloatingWindow.this.mChoreographer.removeCallbacks(3, MiuiTimeFloatingWindow.this.mDrawCallBack, null);
            if (!MiuiTimeFloatingWindow.this.mShowTime) {
                return;
            }
            MiuiTimeFloatingWindow.this.updateTime();
            MiuiTimeFloatingWindow.this.mChoreographer.postCallback(3, MiuiTimeFloatingWindow.this.mDrawCallBack, null);
        }
    };
    private String mLastTimeText = "";

    public MiuiTimeFloatingWindow(Context context) {
        this.mContext = context;
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(MiuiInputThread.getHandler());
        miuiSettingsObserver.observe();
    }

    private int getLayoutParamsY() {
        Paint textPaint = new Paint();
        Paint.FontMetricsInt textMetrics = new Paint.FontMetricsInt();
        textPaint.setTextSize(this.mContext.getResources().getDisplayMetrics().density * 10.0f);
        textPaint.getFontMetricsInt(textMetrics);
        int textMetricsHeight = (textMetrics.descent - textMetrics.ascent) + 2;
        WindowInsets insets = this.mWindowManager.getMaximumWindowMetrics().getWindowInsets();
        if (insets == null) {
            return textMetricsHeight;
        }
        int headerPaddingTop = 0;
        RoundedCorner topLeftRounded = insets.getRoundedCorner(0);
        if (topLeftRounded != null) {
            headerPaddingTop = topLeftRounded.getRadius();
        }
        RoundedCorner topRightRounded = insets.getRoundedCorner(1);
        if (topRightRounded != null) {
            headerPaddingTop = Math.max(headerPaddingTop, topRightRounded.getRadius());
        }
        if (insets.getDisplayCutout() != null) {
            headerPaddingTop = Math.max(headerPaddingTop, insets.getDisplayCutout().getSafeInsetTop());
        }
        return headerPaddingTop + textMetricsHeight;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initLayoutParams() {
        this.mLayoutParams.flags = 296;
        this.mLayoutParams.layoutInDisplayCutoutMode = 1;
        this.mLayoutParams.type = 2018;
        this.mLayoutParams.setTrustedOverlay();
        this.mLayoutParams.format = -3;
        this.mLayoutParams.gravity = 8388659;
        this.mLayoutParams.x = 0;
        this.mLayoutParams.y = getLayoutParamsY();
        this.mLayoutParams.width = -2;
        this.mLayoutParams.height = -2;
        this.mLayoutParams.setTitle(TAG);
        if (ActivityManager.isHighEndGfx()) {
            this.mLayoutParams.flags |= BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT;
            this.mLayoutParams.privateFlags |= 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService(LayoutInflater.class);
        View inflate = layoutInflater.inflate(285999169, (ViewGroup) null);
        this.mRootView = inflate;
        this.mTimeTextView = (TextView) inflate.findViewById(285868108);
        this.mRootView.setOnTouchListener(new FloatingWindowOnTouchListener());
        this.mRootView.setForceDarkAllowed(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: addRootViewToWindow, reason: merged with bridge method [inline-methods] */
    public void lambda$showTimeFloatWindow$1(int reason) {
        if (reason == 1) {
            Slog.i(TAG, "Because settings state change, add window");
            this.mWindowManager.addView(this.mRootView, this.mLayoutParams);
        }
        if (this.mShowTime || !this.mIsScreenOn) {
            return;
        }
        this.mRootView.setVisibility(0);
        this.mShowTime = true;
        this.mChoreographer.postCallback(3, this.mDrawCallBack, null);
        Slog.i(TAG, "Time floating window show");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: removeRootViewFromWindow, reason: merged with bridge method [inline-methods] */
    public void lambda$hideTimeFloatWindow$2(int reason) {
        if (reason == 1) {
            Slog.i(TAG, "Because settings state change, remove window");
            this.mWindowManager.removeViewImmediate(this.mRootView);
        }
        if (!this.mShowTime) {
            return;
        }
        this.mRootView.setVisibility(8);
        this.mShowTime = false;
        this.mTimerForLog = 0L;
        Slog.i(TAG, "Time floating window hide");
    }

    private void init() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        this.mLayoutParams = new WindowManager.LayoutParams();
        HandlerThread handlerThread = new HandlerThread("TimeFloatingWindow");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.initLayoutParams();
            }
        });
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.initView();
            }
        });
        updateDateTimeFormatter();
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.lambda$init$0();
            }
        });
        this.mIsInit = true;
        this.mPackageMonitor = new PackageMonitor() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.2
            public void onPackageDataCleared(String str, int i) {
                if (!MiuiTimeFloatingWindow.SETTINGS_PACKAGE_NAME.equals(str)) {
                    return;
                }
                Slog.i(MiuiTimeFloatingWindow.TAG, "settings data was cleared, write current value.");
                Settings.System.putIntForUser(MiuiTimeFloatingWindow.this.mContext.getContentResolver(), "miui_time_floating_window", MiuiTimeFloatingWindow.this.mIsTimeFloatingWindowOn ? 1 : 0, -2);
            }
        };
        this.mTimezoneChangedIntentFilter = new IntentFilter("android.intent.action.TIMEZONE_CHANGED");
        this.mTimezoneChangedReceiver = new BroadcastReceiver() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                MiuiTimeFloatingWindow.this.updateDateTimeFormatter();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$0() {
        this.mChoreographer = Choreographer.getInstance();
    }

    public void showTimeFloatWindow(final int reason) {
        if (!this.mIsInit) {
            init();
        }
        Slog.i(TAG, "Request show time floating window because " + reasonToString(reason) + " mShowTime = " + this.mShowTime + " mIsScreenOn = " + this.mIsScreenOn);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.lambda$showTimeFloatWindow$1(reason);
            }
        });
    }

    public void hideTimeFloatWindow(final int reason) {
        Slog.i(TAG, "Request hide time floating window because " + reasonToString(reason) + " mShowTime = " + this.mShowTime + " mIsScreenOn = " + this.mIsScreenOn);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.lambda$hideTimeFloatWindow$2(reason);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTime() {
        String time = this.mDateTimeFormatter.format(Instant.now());
        if (!this.mLastTimeText.equals(time)) {
            this.mTimeTextView.setText(time);
            this.mLastTimeText = time;
        }
        logForIsRunning();
    }

    private void logForIsRunning() {
        long now = System.currentTimeMillis();
        if (now > this.mTimerForLog) {
            Slog.i(TAG, "Time floating window is running.");
            this.mTimerForLog = LOG_PRINT_TIME_DIFF + now;
        }
    }

    public void updateTimeFloatWindowState() {
        int newValue = Settings.System.getIntForUser(this.mContext.getContentResolver(), "miui_time_floating_window", 0, -2);
        boolean newState = newValue != 0;
        if (this.mIsTimeFloatingWindowOn == newState) {
            Slog.w(TAG, "The setting value was not change, but receive the notify, new state is " + newState);
            return;
        }
        this.mIsTimeFloatingWindowOn = newState;
        if (newState) {
            showTimeFloatWindow(1);
            registerReceivers();
        } else {
            hideTimeFloatWindow(1);
            unregisterReceivers();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDateTimeFormatter() {
        this.mDateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(TimeZone.getDefault().toZoneId());
    }

    private void registerReceivers() {
        try {
            this.mContext.registerReceiver(this.mTimezoneChangedReceiver, this.mTimezoneChangedIntentFilter);
            this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.CURRENT, true);
            Slog.i(TAG, "Register time zone and package monitor.");
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    private void unregisterReceivers() {
        try {
            this.mContext.unregisterReceiver(this.mTimezoneChangedReceiver);
            this.mPackageMonitor.unregister();
            Slog.i(TAG, "Unregister time zone and package monitor.");
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    public void updateScreenState(boolean isScreenOn) {
        if (this.mIsScreenOn == isScreenOn) {
            Slog.w(TAG, "The screen state not change, but receive the notify, now isScreenOn = " + isScreenOn);
            return;
        }
        this.mIsScreenOn = isScreenOn;
        if (isScreenOn && !this.mIsFirstScreenOn) {
            Slog.i(TAG, "First screen on let's read setting value");
            updateTimeFloatWindowState();
            this.mIsFirstScreenOn = true;
        } else if (this.mIsTimeFloatingWindowOn) {
            if (isScreenOn) {
                showTimeFloatWindow(0);
            } else {
                hideTimeFloatWindow(0);
            }
        }
    }

    /* loaded from: classes.dex */
    private class MiuiSettingsObserver extends ContentObserver {
        public MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = MiuiTimeFloatingWindow.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("miui_time_floating_window"), false, this, -2);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            MiuiTimeFloatingWindow.this.updateTimeFloatWindowState();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FloatingWindowOnTouchListener implements View.OnTouchListener {
        private static final int MOVE_THRESHOLD = 3;
        private final int[] mLocationTemp;
        protected float mStartRawX;
        protected float mStartRawY;
        protected float mStartX;
        protected float mStartY;

        private FloatingWindowOnTouchListener() {
            this.mStartRawX = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.mStartRawY = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.mStartX = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.mStartY = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.mLocationTemp = new int[2];
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:10:0x003d, code lost:
        
            return true;
         */
        @Override // android.view.View.OnTouchListener
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public boolean onTouch(android.view.View r4, android.view.MotionEvent r5) {
            /*
                r3 = this;
                int r0 = r5.getAction()
                r1 = 1
                switch(r0) {
                    case 0: goto L1e;
                    case 1: goto L14;
                    case 2: goto L9;
                    default: goto L8;
                }
            L8:
                goto L3d
            L9:
                boolean r0 = r3.isClick(r5)
                if (r0 == 0) goto L10
                goto L3d
            L10:
                r3.actionMoveEvent(r5)
                goto L3d
            L14:
                boolean r0 = r3.isClick(r5)
                if (r0 == 0) goto L3d
                r4.performClick()
                goto L3d
            L1e:
                float r0 = r5.getRawX()
                r3.mStartRawX = r0
                float r0 = r5.getRawY()
                r3.mStartRawY = r0
                int[] r0 = r3.mLocationTemp
                r4.getLocationOnScreen(r0)
                int[] r0 = r3.mLocationTemp
                r2 = 0
                r2 = r0[r2]
                float r2 = (float) r2
                r3.mStartX = r2
                r0 = r0[r1]
                float r0 = (float) r0
                r3.mStartY = r0
            L3d:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.miui.server.input.time.MiuiTimeFloatingWindow.FloatingWindowOnTouchListener.onTouch(android.view.View, android.view.MotionEvent):boolean");
        }

        protected void actionMoveEvent(MotionEvent event) {
            int newX = (int) (this.mStartX + (event.getRawX() - this.mStartRawX));
            int newY = (int) (this.mStartY + (event.getRawY() - this.mStartRawY));
            MiuiTimeFloatingWindow.this.updateLocation(newX, newY);
        }

        private boolean isClick(MotionEvent event) {
            return Math.abs(this.mStartRawX - event.getRawX()) <= 3.0f && Math.abs(this.mStartRawY - event.getRawY()) <= 3.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLocation(int x, int y) {
        if ((this.mLayoutParams.x == x && this.mLayoutParams.y == y) || !this.mShowTime) {
            return;
        }
        this.mLayoutParams.x = x;
        this.mLayoutParams.y = y;
        this.mWindowManager.updateViewLayout(this.mRootView, this.mLayoutParams);
    }

    private String reasonToString(int reason) {
        switch (reason) {
            case 0:
                return "SCREEN_STATE_CHANGE";
            case 1:
                return "SETTINGS_CHANGE";
            default:
                return "UNKNOWN";
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.println(prefix + "mShowTime = " + this.mShowTime);
        pw.println(prefix + "mIsTimeFloatingWindowOn = " + this.mIsTimeFloatingWindowOn);
        pw.println(prefix + "mIsScreenOn = " + this.mIsScreenOn);
    }
}
