package com.miui.server.input.gesture.multifingergesture;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.animation.DeviceHelper;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerDownGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerLongPressGesture;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MiuiMultiFingerGestureManager implements MiuiGestureListener {
    private static final Set<String> DISABLE_GESTURE_APPS = Set.of("com.android.cts.verifier");
    private static final String TAG = "MiuiMultiFingerGestureManager";
    private boolean mBootCompleted;
    private final Context mContext;
    private boolean mContinueSendEvent;
    private WindowManagerPolicy.WindowState mCurrentFocusedWindow;
    private int mCurrentUserId;
    private boolean mDeviceProvisioned;
    private boolean mEnable;
    private BooleanSupplier mGetKeyguardActiveFunction;
    private final Handler mHandler;
    private boolean mIsFolded;
    private boolean mIsRegister;
    private boolean mIsScreenOn;
    private final MiuiGestureMonitor mMiuiGestureMonitor;
    private final MiuiSettingsObserver mMiuiSettingsObserver;
    private boolean mNeedCancel;
    private final Set<String> mNotNeedHapticFeedbackFunction = new HashSet();
    private final List<BaseMiuiMultiFingerGesture> mAllMultiFingerGestureList = new ArrayList();

    public boolean getIsFolded() {
        return this.mIsFolded;
    }

    public MiuiMultiFingerGestureManager(Context context, Handler mHandler) {
        this.mContext = context;
        this.mHandler = mHandler;
        this.mMiuiGestureMonitor = MiuiGestureMonitor.getInstance(context);
        initAllGesture();
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        updateSettings();
        registerOnConfigBroadcast();
        initNotNeedHapticFeedbackFunction();
    }

    private void initNotNeedHapticFeedbackFunction() {
        this.mNotNeedHapticFeedbackFunction.add("screen_shot");
        this.mNotNeedHapticFeedbackFunction.add("partial_screen_shot");
        this.mNotNeedHapticFeedbackFunction.add("dump_log");
    }

    private void registerOnConfigBroadcast() {
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                MiuiMultiFingerGestureManager.this.onConfigChange();
            }
        }, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    private void initAllGesture() {
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerDownGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerLongPressGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture(this.mContext, this.mHandler, this));
        if (DeviceHelper.isTablet(this.mContext) || DeviceHelper.isFoldDevice()) {
            this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture.MiuiThreeFingerHorizontalLTRGesture(this.mContext, this.mHandler, this));
            this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture.MiuiThreeFingerHorizontalRTLGesture(this.mContext, this.mHandler, this));
        }
    }

    private void updateAllGestureStatus(final MiuiMultiFingerGestureStatus newStatus, final BaseMiuiMultiFingerGesture except) {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$updateAllGestureStatus$0(BaseMiuiMultiFingerGesture.this, newStatus, (BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateAllGestureStatus$0(BaseMiuiMultiFingerGesture except, MiuiMultiFingerGestureStatus newStatus, BaseMiuiMultiFingerGesture multiFingerGesture) {
        MiuiMultiFingerGestureStatus status;
        if (multiFingerGesture == except || (status = multiFingerGesture.getStatus()) == newStatus || status == MiuiMultiFingerGestureStatus.NONE) {
            return;
        }
        multiFingerGesture.changeStatus(newStatus);
    }

    private void initAllGestureStatus() {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$initAllGestureStatus$1((BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$initAllGestureStatus$1(BaseMiuiMultiFingerGesture gesture) {
        MiuiMultiFingerGestureStatus status = gesture.getStatus();
        if (status == MiuiMultiFingerGestureStatus.NONE) {
            return;
        }
        gesture.changeStatus(MiuiMultiFingerGestureStatus.READY);
    }

    @Override // com.miui.server.input.gesture.MiuiGestureListener
    public void onPointerEvent(MotionEvent event) {
        switch (event.getAction()) {
            case 0:
                boolean canPerformGesture = canPerformGesture(event);
                this.mContinueSendEvent = canPerformGesture;
                if (canPerformGesture) {
                    initAllGestureStatus();
                    break;
                }
                break;
            case 3:
                Slog.d(TAG, "Receive a cancel event, all gesture will fail.");
                updateAllGestureStatus(MiuiMultiFingerGestureStatus.FAIL, null);
                this.mContinueSendEvent = false;
                return;
        }
        if (!this.mContinueSendEvent) {
            return;
        }
        sendEventToGestures(event);
    }

    private boolean canPerformGesture(MotionEvent event) {
        boolean isGestureAvailable = this.mDeviceProvisioned && this.mIsScreenOn && this.mEnable;
        if (!isGestureAvailable || event.getDeviceId() == -1) {
            return false;
        }
        WindowManagerPolicy.WindowState windowState = this.mCurrentFocusedWindow;
        if (windowState == null) {
            return true;
        }
        return true ^ DISABLE_GESTURE_APPS.contains(windowState.getOwningPackage());
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0037  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0036 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void sendEventToGestures(android.view.MotionEvent r7) {
        /*
            r6 = this;
            r0 = 0
            r1 = 1
            r6.mNeedCancel = r1
            java.util.List<com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture> r1 = r6.mAllMultiFingerGestureList
            java.util.Iterator r1 = r1.iterator()
        La:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L42
            java.lang.Object r2 = r1.next()
            com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture r2 = (com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture) r2
            com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus r3 = r2.getStatus()
            int[] r4 = com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager.AnonymousClass2.$SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus
            int r5 = r3.ordinal()
            r4 = r4[r5]
            switch(r4) {
                case 1: goto L2a;
                case 2: goto L26;
                default: goto L25;
            }
        L25:
            goto L2e
        L26:
            r6.gestureStatusIsDetecting(r7, r2)
            goto L2e
        L2a:
            r6.gestureStatusIsReady(r7, r2)
        L2e:
            com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus r3 = r2.getStatus()
            com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus r4 = com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus.SUCCESS
            if (r3 != r4) goto L37
            return
        L37:
            com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus r4 = com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus.FAIL
            if (r3 == r4) goto L3f
            com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus r4 = com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus.NONE
            if (r3 != r4) goto L41
        L3f:
            int r0 = r0 + 1
        L41:
            goto La
        L42:
            java.util.List<com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture> r1 = r6.mAllMultiFingerGestureList
            int r1 = r1.size()
            if (r0 != r1) goto L4d
            r1 = 0
            r6.mContinueSendEvent = r1
        L4d:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager.sendEventToGestures(android.view.MotionEvent):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus;

        static {
            int[] iArr = new int[MiuiMultiFingerGestureStatus.values().length];
            $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus = iArr;
            try {
                iArr[MiuiMultiFingerGestureStatus.READY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus[MiuiMultiFingerGestureStatus.DETECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void checkSuccess(BaseMiuiMultiFingerGesture gesture) {
        this.mContinueSendEvent = false;
        updateAllGestureStatus(MiuiMultiFingerGestureStatus.FAIL, gesture);
        if (!checkBootCompleted()) {
            return;
        }
        triggerFunction(gesture);
    }

    private void gestureStatusIsDetecting(MotionEvent event, BaseMiuiMultiFingerGesture gesture) {
        if (event.getPointerCount() == gesture.getFunctionNeedFingerNum()) {
            gesture.onTouchEvent(event);
        } else {
            gesture.changeStatus(MiuiMultiFingerGestureStatus.FAIL);
        }
    }

    private void gestureStatusIsReady(MotionEvent event, BaseMiuiMultiFingerGesture gesture) {
        if (event.getPointerCount() != gesture.getFunctionNeedFingerNum()) {
            return;
        }
        if (!gesture.preCondition()) {
            gesture.changeStatus(MiuiMultiFingerGestureStatus.FAIL);
            Slog.i(TAG, gesture.getGestureKey() + " init fail, because pre condition.");
            return;
        }
        gesture.initGesture(event);
        if (gesture.getStatus() == MiuiMultiFingerGestureStatus.DETECTING) {
            pilferPointers();
            gesture.onTouchEvent(event);
        }
    }

    private boolean checkBootCompleted() {
        if (!this.mBootCompleted) {
            boolean z = SystemProperties.getBoolean("sys.boot_completed", false);
            this.mBootCompleted = z;
            return z;
        }
        return true;
    }

    private void triggerFunction(BaseMiuiMultiFingerGesture gesture) {
        boolean notNeedHapticFeedback = this.mNotNeedHapticFeedbackFunction.contains(gesture.getGestureFunction());
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(gesture.getGestureFunction(), gesture.getGestureKey(), null, !notNeedHapticFeedback);
    }

    private void pilferPointers() {
        if (!this.mNeedCancel) {
            return;
        }
        this.mMiuiGestureMonitor.pilferPointers();
        Slog.d(TAG, "Pilfer pointers because gesture detected");
        this.mNeedCancel = false;
    }

    public void updateScreenState(boolean screenOn) {
        this.mIsScreenOn = screenOn;
    }

    public void onUserSwitch(int newUserId) {
        if (this.mCurrentUserId != newUserId) {
            this.mCurrentUserId = newUserId;
            updateSettings();
        }
    }

    public void initKeyguardActiveFunction(BooleanSupplier getKeyguardActiveFunction) {
        this.mGetKeyguardActiveFunction = getKeyguardActiveFunction;
    }

    public boolean isKeyguardActive() {
        BooleanSupplier booleanSupplier = this.mGetKeyguardActiveFunction;
        if (booleanSupplier == null) {
            return false;
        }
        return booleanSupplier.getAsBoolean();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEmpty(String function) {
        return TextUtils.isEmpty(function) || "none".equals(function);
    }

    private void updateSettings() {
        Settings.System.putInt(this.mContext.getContentResolver(), "enable_three_gesture", 1);
        MiuiSettingsObserver miuiSettingsObserver = this.mMiuiSettingsObserver;
        if (miuiSettingsObserver == null) {
            return;
        }
        miuiSettingsObserver.onChange(false, Settings.System.getUriFor("enable_three_gesture"));
        this.mMiuiSettingsObserver.onChange(false, Settings.Global.getUriFor("device_provisioned"));
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.this.lambda$updateSettings$2((BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateSettings$2(BaseMiuiMultiFingerGesture gesture) {
        this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor(gesture.getGestureKey()));
    }

    public void dump(final String prefix, final PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$dump$3(pw, prefix, (BaseMiuiMultiFingerGesture) obj);
            }
        });
        pw.println(prefix + "mBootCompleted = " + this.mBootCompleted);
        pw.println(prefix + "mDeviceProvisioned = " + this.mDeviceProvisioned);
        pw.println(prefix + "mIsScreenOn = " + this.mIsScreenOn);
        pw.println(prefix + "mEnable = " + this.mEnable);
        pw.println(prefix + "mIsFolded = " + this.mIsFolded);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$3(PrintWriter pw, String prefix, BaseMiuiMultiFingerGesture gesture) {
        pw.print(prefix);
        pw.println("gestureName=" + gesture.getClass().getSimpleName());
        pw.print(prefix);
        pw.println("gestureKey=" + gesture.getGestureKey());
        pw.print(prefix);
        pw.println("gestureFunction=" + gesture.getGestureFunction());
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConfigChange() {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((BaseMiuiMultiFingerGesture) obj).onConfigChange();
            }
        });
    }

    public void onFocusedWindowChanged(WindowManagerPolicy.WindowState newFocus) {
        this.mCurrentFocusedWindow = newFocus;
    }

    public void notifyFoldStatus(boolean folded) {
        this.mIsFolded = folded;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            final ContentResolver resolver = MiuiMultiFingerGestureManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("enable_three_gesture"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this, -1);
            MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$MiuiSettingsObserver$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    MiuiMultiFingerGestureManager.MiuiSettingsObserver.this.lambda$observe$0(resolver, (BaseMiuiMultiFingerGesture) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$observe$0(ContentResolver resolver, BaseMiuiMultiFingerGesture baseMiuiMultiFingerGesture) {
            resolver.registerContentObserver(Settings.System.getUriFor(baseMiuiMultiFingerGesture.getGestureKey()), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, final Uri uri) {
            if (Settings.System.getUriFor("enable_three_gesture").equals(uri)) {
                MiuiMultiFingerGestureManager miuiMultiFingerGestureManager = MiuiMultiFingerGestureManager.this;
                miuiMultiFingerGestureManager.mEnable = Settings.System.getInt(miuiMultiFingerGestureManager.mContext.getContentResolver(), "enable_three_gesture", 1) == 1;
                Slog.d(MiuiMultiFingerGestureManager.TAG, "enable_three_gesture_key :" + MiuiMultiFingerGestureManager.this.mEnable);
            } else if (Settings.Global.getUriFor("device_provisioned").equals(uri)) {
                MiuiMultiFingerGestureManager miuiMultiFingerGestureManager2 = MiuiMultiFingerGestureManager.this;
                miuiMultiFingerGestureManager2.mDeviceProvisioned = Settings.Global.getInt(miuiMultiFingerGestureManager2.mContext.getContentResolver(), "device_provisioned", 0) != 0;
            } else {
                MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList.stream().filter(new Predicate() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$MiuiSettingsObserver$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean equals;
                        equals = Settings.System.getUriFor(((BaseMiuiMultiFingerGesture) obj).getGestureKey()).equals(uri);
                        return equals;
                    }
                }).findFirst().ifPresent(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$MiuiSettingsObserver$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        MiuiMultiFingerGestureManager.MiuiSettingsObserver.this.lambda$onChange$2((BaseMiuiMultiFingerGesture) obj);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$2(BaseMiuiMultiFingerGesture gesture) {
            String function = MiuiSettings.Key.getKeyAndGestureShortcutFunction(MiuiMultiFingerGestureManager.this.mContext, gesture.getGestureKey());
            gesture.setGestureFunction(function);
            Slog.d(MiuiMultiFingerGestureManager.TAG, gesture.getGestureKey() + " :" + function);
            updateGestures(gesture);
        }

        private void updateGestures(BaseMiuiMultiFingerGesture gesture) {
            gesture.changeStatus(MiuiMultiFingerGestureManager.this.isEmpty(gesture.getGestureFunction()) ? MiuiMultiFingerGestureStatus.NONE : MiuiMultiFingerGestureStatus.READY);
            byte num = 0;
            for (BaseMiuiMultiFingerGesture baseMiuiMultiFingerGesture : MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList) {
                if (!MiuiMultiFingerGestureManager.this.isEmpty(baseMiuiMultiFingerGesture.getGestureFunction())) {
                    num = (byte) (num + 1);
                }
            }
            if (!MiuiMultiFingerGestureManager.this.mIsRegister && num > 0) {
                Slog.d(MiuiMultiFingerGestureManager.TAG, "The gesture has been add,register pointer event listener.");
                MiuiMultiFingerGestureManager.this.mMiuiGestureMonitor.registerPointerEventListener(MiuiMultiFingerGestureManager.this);
                MiuiMultiFingerGestureManager.this.mIsRegister = true;
            } else if (MiuiMultiFingerGestureManager.this.mIsRegister && num == 0) {
                Slog.d(MiuiMultiFingerGestureManager.TAG, "The gestures has been all removed, unregister pointer event listener.");
                MiuiMultiFingerGestureManager.this.mMiuiGestureMonitor.unregisterPointerEventListener(MiuiMultiFingerGestureManager.this);
                MiuiMultiFingerGestureManager.this.mIsRegister = false;
            }
        }
    }
}
