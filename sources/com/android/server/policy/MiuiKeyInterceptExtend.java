package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.KeyboardShortcutInfo;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.PadManager;
import com.miui.server.input.custom.InputMiuiDesktopMode;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import miui.hardware.input.MiuiKeyEventUtil;
import miui.hardware.input.MiuiKeyboardHelper;
import miui.os.Build;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiKeyInterceptExtend {
    private static final String ALIYUN_PACKAGE_NAME = "com.aliyun.wuying.enterprise";
    private static volatile MiuiKeyInterceptExtend INSTANCE = null;
    private static final String TAG = "MiuiKeyIntercept";
    public static final int TYPE_INTERCEPT_AOSP = 3;
    public static final int TYPE_INTERCEPT_MIUI = 2;
    public static final int TYPE_INTERCEPT_MIUI_AND_AOSP = 1;
    public static final int TYPE_INTERCEPT_MIUI_AND_AOSP_NO_PASS_TO_USER = 4;
    public static final int TYPE_INTERCEPT_NULL = 0;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private volatile boolean mIsAospKeyboardShortcutEnable;
    private volatile boolean mIsKidMode;
    private boolean mIsScreenOn;
    private final PowerManager mPowerManager;
    private final SettingsObserver mSettingsObserver;
    private boolean mTriggerLongPress;
    private final Map<String, ArrayList<KeyboardShortcutInfo>> mSkipInterceptWindows = new ConcurrentHashMap();
    private boolean mScreenOnWhenDown = true;
    private volatile boolean mIsKeyboardShortcutEnable = true;
    private Runnable mPartialScreenShot = new Runnable() { // from class: com.android.server.policy.MiuiKeyInterceptExtend.1
        @Override // java.lang.Runnable
        public void run() {
            MiuiKeyInterceptExtend.this.mTriggerLongPress = true;
            ShortCutActionsUtils.getInstance(MiuiKeyInterceptExtend.this.mContext).triggerFunction("partial_screen_shot", ShortCutActionsUtils.REASON_OF_KEYBOARD, null, true);
            InputOneTrackUtil.getInstance(MiuiKeyInterceptExtend.this.mContext).track6FShortcut(MiuiKeyboardUtil.KeyBoardShortcut.getShortcutNameByKeyCodeWithAction(9994, true));
        }
    };
    private Runnable mScreenShot = new Runnable() { // from class: com.android.server.policy.MiuiKeyInterceptExtend.2
        @Override // java.lang.Runnable
        public void run() {
            if (!MiuiKeyInterceptExtend.this.mTriggerLongPress) {
                ShortCutActionsUtils.getInstance(MiuiKeyInterceptExtend.this.mContext).triggerFunction("screen_shot", ShortCutActionsUtils.REASON_OF_KEYBOARD, null, true);
                InputOneTrackUtil.getInstance(MiuiKeyInterceptExtend.this.mContext).track6FShortcut(MiuiKeyboardUtil.KeyBoardShortcut.getShortcutNameByKeyCodeWithAction(9994, false));
            }
        }
    };
    private Handler mHandler = MiuiInputThread.getHandler();

    /* loaded from: classes.dex */
    public enum INTERCEPT_STAGE {
        BEFORE_DISPATCHING,
        BEFORE_QUEUEING
    }

    private MiuiKeyInterceptExtend(Context context) {
        this.mContext = context;
        initSystemSpecialWindow();
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver = settingsObserver;
        settingsObserver.registerObserver();
        this.mPowerManager = (PowerManager) context.getSystemService("power");
    }

    private void initSystemSpecialWindow() {
        updateSkipInterceptWindowList(ALIYUN_PACKAGE_NAME, "system", new ArrayList<>(List.of(new KeyboardShortcutInfo((CharSequence) null, 61, 2), new KeyboardShortcutInfo((CharSequence) null, 117, 65536))));
    }

    public static MiuiKeyInterceptExtend getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MiuiKeyInterceptExtend.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiuiKeyInterceptExtend(context);
                }
            }
        }
        return INSTANCE;
    }

    public boolean interceptMiuiKeyboard(KeyEvent event, boolean isScreenOn) {
        if (PadManager.getInstance().isPad() && InputOneTrackUtil.shouldCountDevice(event)) {
            InputOneTrackUtil.getInstance(this.mContext).trackExternalDevice(event);
        }
        InputDevice device = event.getDevice();
        if (device == null || !MiuiKeyboardHelper.isXiaomiKeyboard(device.getProductId(), device.getVendorId())) {
            return false;
        }
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        int miuiKeyCode = MiuiKeyEventUtil.matchMiuiKey(event);
        if (miuiKeyCode == 9995) {
            if (event.getAction() == 0) {
                InputOneTrackUtil.getInstance(this.mContext).trackVoice2Word(MiuiKeyboardUtil.KeyBoardShortcut.VOICE_TO_WORD);
            }
            return false;
        }
        if (PadManager.getInstance().adjustBrightnessFromKeycode(event)) {
            return true;
        }
        if (!down && this.mScreenOnWhenDown) {
            if (MiuiKeyboardHelper.supportFnKeyboard()) {
                InputOneTrackUtil.getInstance(this.mContext).trackKeyboardShortcut(MiuiKeyboardUtil.KeyBoardShortcut.getShortcutNameByKeyCode(event.getKeyCode()));
            } else if (MiuiKeyboardHelper.support6FKeyboard()) {
                InputOneTrackUtil.getInstance(this.mContext).track6FShortcut(MiuiKeyboardUtil.KeyBoardShortcut.getShortcutNameByKeyCode(miuiKeyCode));
            }
            switch (miuiKeyCode) {
                case 115:
                    PadManager.getInstance().setCapsLockStatus(event.isCapsLockOn());
                    MiuiPadKeyboardManager.getKeyboardManager(this.mContext).setCapsLockLight(event.isCapsLockOn());
                    return false;
                case 9994:
                    this.mHandler.removeCallbacks(this.mPartialScreenShot);
                    this.mHandler.post(this.mScreenShot);
                    return true;
                case 9996:
                    boolean result = !this.mAudioManager.isMicrophoneMute();
                    this.mAudioManager.setMicrophoneMute(result);
                    return true;
                case 9997:
                    ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("launch_voice_assistant", ShortCutActionsUtils.REASON_OF_KEYBOARD, null, true);
                    return true;
                case 9998:
                    boolean currentZenMode = MiuiSettings.SoundMode.isZenModeOn(this.mContext);
                    MiuiSettings.SoundMode.setZenModeOn(this.mContext, !currentZenMode, ShortCutActionsUtils.REASON_OF_KEYBOARD);
                    return true;
                case 9999:
                    ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("go_to_sleep", ShortCutActionsUtils.REASON_OF_KEYBOARD, null, false);
                    return true;
            }
        }
        this.mScreenOnWhenDown = isScreenOn;
        if (miuiKeyCode != keyCode && isScreenOn) {
            if (miuiKeyCode == 9994) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.MiuiKeyInterceptExtend$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiKeyInterceptExtend.this.lambda$interceptMiuiKeyboard$0();
                    }
                });
                this.mHandler.postDelayed(this.mPartialScreenShot, 300L);
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$interceptMiuiKeyboard$0() {
        this.mTriggerLongPress = false;
    }

    public void setKidSpaceMode(boolean isKidMode) {
        this.mIsKidMode = isKidMode;
        Slog.i(TAG, "KidMode status is:" + isKidMode);
    }

    public void setKeyboardShortcutEnable(boolean isEnable) {
        this.mIsKeyboardShortcutEnable = isEnable;
        Slog.i(TAG, "MIUI Keyboard Shortcut status is:" + this.mIsKeyboardShortcutEnable);
    }

    public boolean getKeyboardShortcutEnable() {
        return this.mIsKeyboardShortcutEnable;
    }

    public void setAospKeyboardShortcutEnable(boolean isEnable) {
        this.mIsAospKeyboardShortcutEnable = isEnable;
        Slog.i(TAG, "AOSP Keyboard Shortcut status is:" + this.mIsAospKeyboardShortcutEnable);
    }

    public boolean getAospKeyboardShortcutEnable() {
        return this.mIsAospKeyboardShortcutEnable;
    }

    public int getKeyInterceptTypeBeforeQueueing(KeyEvent event, int policyFlags, WindowManagerPolicy.WindowState focusedWin) {
        return getKeyInterceptType(INTERCEPT_STAGE.BEFORE_QUEUEING, event, policyFlags, focusedWin);
    }

    public int getKeyInterceptTypeBeforeDispatching(KeyEvent event, int policyFlags, WindowManagerPolicy.WindowState focusedWin) {
        return getKeyInterceptType(INTERCEPT_STAGE.BEFORE_DISPATCHING, event, policyFlags, focusedWin);
    }

    public void setScreenState(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
    }

    private int getKeyInterceptType(INTERCEPT_STAGE interceptStage, KeyEvent event, int policyFlags, WindowManagerPolicy.WindowState focusedWin) {
        if (event == null || event.getDeviceId() < 0 || event.getDevice() == null || !event.getDevice().isFullKeyboard() || !Build.IS_TABLET) {
            return 0;
        }
        if (getAospKeyboardShortcutEnable()) {
            return 2;
        }
        if (this.mIsKidMode || !getKeyboardShortcutEnable()) {
            if (!this.mIsScreenOn && interceptStage == INTERCEPT_STAGE.BEFORE_QUEUEING && ((policyFlags & 1) != 0 || event.isWakeKey())) {
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 6, "android.policy:KEY");
            }
            return 1;
        }
        if (focusedWin == null) {
            return 0;
        }
        for (Map.Entry entry : this.mSkipInterceptWindows.entrySet()) {
            String packageName = entry.getKey();
            ArrayList<KeyboardShortcutInfo> currentShortcuts = entry.getValue();
            if (packageName.equals(focusedWin.getOwningPackage())) {
                if (currentShortcuts.size() == 0) {
                    return 1;
                }
                Slog.i(TAG, "Ready intercept meta");
                Iterator<KeyboardShortcutInfo> it = currentShortcuts.iterator();
                while (it.hasNext()) {
                    KeyboardShortcutInfo info = it.next();
                    if (event.getKeyCode() == info.getKeycode() && KeyEvent.metaStateHasModifiers(event.getMetaState(), info.getModifiers())) {
                        return 1;
                    }
                    if (117 == info.getKeycode() && event.getKeyCode() == 117) {
                        return 1;
                    }
                }
            }
        }
        int desktopModeKeyInterceptType = InputMiuiDesktopMode.getKeyInterceptType(interceptStage, this.mContext, event);
        if (desktopModeKeyInterceptType != 0) {
            return desktopModeKeyInterceptType;
        }
        if (!MiuiKeyboardUtil.interceptShortCutKeyIfCustomDefined(this.mContext, event, interceptStage)) {
            return 0;
        }
        return 4;
    }

    private void updateSkipInterceptWindowList(String packageName, String reason, ArrayList<KeyboardShortcutInfo> newShortcuts) {
        if (packageName == null || newShortcuts == null) {
            return;
        }
        MiuiInputLog.defaults("skip policy intercept because: " + reason + " for " + packageName);
        ArrayList<KeyboardShortcutInfo> currentInterceptShortcuts = this.mSkipInterceptWindows.get(packageName);
        if (newShortcuts.size() == 0) {
            this.mSkipInterceptWindows.put(packageName, newShortcuts);
            return;
        }
        if (currentInterceptShortcuts != null) {
            MiuiInputLog.defaults("Recover history special shortcut for " + packageName);
            if (newShortcuts.size() == 0) {
                this.mSkipInterceptWindows.put(packageName, newShortcuts);
                return;
            } else {
                currentInterceptShortcuts.addAll(newShortcuts);
                return;
            }
        }
        this.mSkipInterceptWindows.put(packageName, newShortcuts);
    }

    public void onSystemBooted() {
        this.mSettingsObserver.processCloudData();
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public static final String FILTER_INTERCEPT_CLOUD_NAME = "MIUI_FILTER_INTERCEPT_KEY";
        public static final String FILTER_INTERCEPT_MODE = "inputFilterKeyIntercept";
        private ContentResolver mContentResolver;
        private int mFilterInterceptMode;

        public SettingsObserver(Handler handler) {
            super(handler);
            this.mFilterInterceptMode = 1;
        }

        public void registerObserver() {
            ContentResolver contentResolver = MiuiKeyInterceptExtend.this.mContext.getContentResolver();
            this.mContentResolver = contentResolver;
            contentResolver.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (MiuiSettings.SettingsCloudData.getCloudDataNotifyUri().equals(uri)) {
                processCloudData();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void processCloudData() {
            MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(MiuiKeyInterceptExtend.this.mContext.getContentResolver(), FILTER_INTERCEPT_CLOUD_NAME, (String) null, (String) null, false);
            if (cloudData == null || cloudData.json() == null) {
                return;
            }
            JSONObject jsonObject = cloudData.json();
            int isFilterInterceptMode = this.mFilterInterceptMode;
            try {
                isFilterInterceptMode = jsonObject.getInt(FILTER_INTERCEPT_MODE);
                Slog.i(MiuiKeyInterceptExtend.TAG, "Update CloudData for MiInput Success for:" + isFilterInterceptMode);
            } catch (Exception exception) {
                Slog.e(MiuiKeyInterceptExtend.TAG, "process CloudData Exception:" + exception);
            }
            if (isFilterInterceptMode != this.mFilterInterceptMode) {
                this.mFilterInterceptMode = isFilterInterceptMode;
                InputCommonConfig commonConfig = InputCommonConfig.getInstance();
                commonConfig.setFilterInterceptMode(this.mFilterInterceptMode == 1);
                commonConfig.flushToNative();
            }
        }
    }
}
