package com.android.server.policy;

import android.app.ActivityThread;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.input.shortcut.ShortcutOneTrackHelper;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.miui.server.input.PadManager;
import com.xiaomi.mirror.MirrorManager;
import java.util.ArrayList;
import java.util.List;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.util.HapticFeedbackUtil;

/* loaded from: classes.dex */
public class PhoneWindowManagerStubImpl implements PhoneWindowManagerStub {
    private static final List<String> DELIVE_META_APPS;
    private static final boolean SUPPORT_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    static final String TAG = "PhoneWindowManagerStubImpl";
    private int SUB_DISPLAY_ID = 2;
    private DisplayTurnoverManager mDisplayTurnoverManager;
    private WindowManagerPolicy.WindowState mFocusedWindow;
    HapticFeedbackUtil mHapticFeedbackUtil;
    private MiuiKeyShortcutRuleManager mMiuiKeyShortcutRuleManager;
    private boolean mPhotoHandleConnection;
    private int mPhotoHandleEventDeviceId;
    private PowerManagerServiceStub mPowerManagerServiceImpl;
    private boolean mSupportAOD;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PhoneWindowManagerStubImpl> {

        /* compiled from: PhoneWindowManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PhoneWindowManagerStubImpl INSTANCE = new PhoneWindowManagerStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PhoneWindowManagerStubImpl m2248provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PhoneWindowManagerStubImpl m2247provideNewInstance() {
            return new PhoneWindowManagerStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        DELIVE_META_APPS = arrayList;
        arrayList.add("com.ss.android.lark.kami");
    }

    public void init(Context context) {
        this.mSupportAOD = context.getResources().getBoolean(17891627);
        this.mPowerManagerServiceImpl = PowerManagerServiceStub.get();
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            this.mDisplayTurnoverManager = new DisplayTurnoverManager(context);
        }
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, true);
    }

    public boolean shouldDispatchInputWhenNonInteractive(int keyCode) {
        if (this.mSupportAOD && keyCode == 354) {
            return true;
        }
        return SUPPORT_FOD && (keyCode == 0 || keyCode == 354);
    }

    public boolean shouldMoveDisplayToTop(int keyCode) {
        if (DeviceFeature.IS_FOLD_DEVICE && this.mSupportAOD && keyCode == 354) {
            return false;
        }
        return true;
    }

    public boolean isInHangUpState() {
        return this.mPowerManagerServiceImpl.isInHangUpState();
    }

    public void setForcedDisplayDensityForUser(IWindowManager windowManager) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            try {
                windowManager.setForcedDisplayDensityForUser(this.SUB_DISPLAY_ID, 240, -2);
            } catch (RemoteException e) {
            }
        }
    }

    public void systemBooted() {
        DisplayTurnoverManager displayTurnoverManager;
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && (displayTurnoverManager = this.mDisplayTurnoverManager) != null) {
            displayTurnoverManager.systemReady();
        }
    }

    public boolean interceptKeyBeforeQueueing(KeyEvent event) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && this.mDisplayTurnoverManager != null && event != null && event.isWakeKey() && event.getDisplayId() == this.SUB_DISPLAY_ID) {
            if (event.getAction() == 0) {
                this.mDisplayTurnoverManager.switchSubDisplayPowerState(true, "DOUBLE_CLICK");
            }
            return true;
        }
        return false;
    }

    public VibrationEffect convertToMiuiHapticFeedback(int hapticFeedbackConstantId) {
        return this.mHapticFeedbackUtil.convertToMiuiHapticFeedback(hapticFeedbackConstantId);
    }

    public boolean interceptWakeKey(KeyEvent event) {
        if (MirrorManager.get().isWorking() && event.isWakeKey()) {
            return event.getKeyCode() == 82;
        }
        return event.isCanceled();
    }

    public void setFocusedWindow(WindowManagerPolicy.WindowState focusedWindow) {
        this.mFocusedWindow = focusedWindow;
    }

    public boolean isPad() {
        return PadManager.getInstance().isPad();
    }

    public boolean interceptKeyWithMeta() {
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        return windowState == null || !DELIVE_META_APPS.contains(windowState.getOwningPackage());
    }

    public boolean hasMiuiPowerMultiClick() {
        if (this.mMiuiKeyShortcutRuleManager == null) {
            this.mMiuiKeyShortcutRuleManager = (MiuiKeyShortcutRuleManager) LocalServices.getService(MiuiKeyShortcutRuleManager.class);
        }
        String doubleClick = this.mMiuiKeyShortcutRuleManager.getFunction("double_click_power_key");
        return (TextUtils.isEmpty(doubleClick) || "none".equals(doubleClick)) ? false : true;
    }

    public boolean triggerModifierShortcut(Context context) {
        return (!MiuiKeyInterceptExtend.getInstance(context).getKeyboardShortcutEnable() && MiuiKeyInterceptExtend.getInstance(context).getAospKeyboardShortcutEnable()) || !PadManager.getInstance().isPad();
    }

    public boolean isEnableCombinationPowerVolumeDownScreenShot() {
        if (this.mMiuiKeyShortcutRuleManager == null) {
            this.mMiuiKeyShortcutRuleManager = (MiuiKeyShortcutRuleManager) LocalServices.getService(MiuiKeyShortcutRuleManager.class);
        }
        String powerVolumeDownFunction = this.mMiuiKeyShortcutRuleManager.getFunction("key_combination_power_volume_down");
        return "screen_shot".equals(powerVolumeDownFunction);
    }

    public void trackGlobalActions(String action) {
        String trackFunction;
        if ("key_combination_power_volume_down".equals(action)) {
            trackFunction = "screen_shot";
        } else {
            trackFunction = "show_global_action";
        }
        ShortcutOneTrackHelper.getInstance(ActivityThread.currentActivityThread().getSystemContext()).trackShortcutEventTrigger(action, trackFunction);
    }

    public boolean noConfirmForShutdown() {
        if ("zhuque".equals(Build.DEVICE)) {
            return true;
        }
        return false;
    }

    public void logMessageRemoved(Handler handler, int what, String reason) {
        if (handler.hasMessages(what)) {
            MiuiInputLog.defaults("remove message what= " + what + " reason= " + reason);
        }
    }

    public boolean skipKeyGesutre(KeyEvent event) {
        if (this.mMiuiKeyShortcutRuleManager == null) {
            this.mMiuiKeyShortcutRuleManager = (MiuiKeyShortcutRuleManager) LocalServices.getService(MiuiKeyShortcutRuleManager.class);
        }
        return this.mMiuiKeyShortcutRuleManager.skipKeyGesutre(event);
    }

    public boolean interceptUnhandledKey(KeyEvent event) {
        InputDevice device;
        int keyCode = event.getKeyCode();
        if (keyCode == 193 && (device = event.getDevice()) != null && device.isXiaomiStylus() >= 3) {
            return true;
        }
        return false;
    }

    public void notifyPhotoHandleConnectionStatus(boolean connection, int deviceId) {
        this.mPhotoHandleConnection = connection;
        this.mPhotoHandleEventDeviceId = deviceId;
    }

    public boolean skipInterceptMacroEvent(KeyEvent event) {
        WindowManagerPolicy.WindowState windowState;
        return 313 == event.getKeyCode() && this.mPhotoHandleConnection && event.getDeviceId() == this.mPhotoHandleEventDeviceId && (windowState = this.mFocusedWindow) != null && AccessController.PACKAGE_CAMERA.equals(windowState.getOwningPackage());
    }
}
