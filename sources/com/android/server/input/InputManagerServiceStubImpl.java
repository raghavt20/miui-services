package com.android.server.input;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayViewport;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.ProcessRecord;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.config.InputDebugConfig;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.MiuiKeyInterceptExtend;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.PadManager;
import com.miui.server.input.magicpointer.MiuiMagicPointerServiceInternal;
import com.miui.server.input.stylus.MiuiStylusUtils;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.util.ArrayList;
import java.util.List;
import miui.hardware.input.MiuiKeyboardHelper;
import miui.util.ITouchFeature;

/* loaded from: classes.dex */
public class InputManagerServiceStubImpl implements InputManagerServiceStub {
    private static final String ANDROID_SYSTEM_UI = "com.android.systemui";
    private static final String INPUT_LOG_LEVEL = "input_log_level";
    private static final int KEY_EVENT_FLAG_NAVIGATION_SYSTEM_UI = Integer.MIN_VALUE;
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final String KEY_STYLUS_QUICK_NOTE_MODE = "stylus_quick_note_screen_off";
    private static final List<String> MIUI_INPUTMETHOD;
    private static final String SHOW_TOUCHES_PREVENTRECORDER = "show_touches_preventrecord";
    private static final String TAG_INPUT = "MIUIInput";
    private Context mContext;
    private Handler mHandler;
    private InputManagerService mInputManagerService;
    private MiuiCustomizeShortCutUtils mMiuiCustomizeShortCutUtils;
    private MiuiInputManagerService mMiuiInputManagerService;
    private MiuiMagicPointerServiceInternal mMiuiMagicPointerService;
    private MiuiPadKeyboardManager mMiuiPadKeyboardManager;
    private NativeInputManagerService mNativeInputManagerService;
    private int mPointerLocationShow;
    private long mPtr;
    private int mShowTouches;
    private int mShowTouchesPreventRecord;
    private static final boolean BERSERK_MODE = SystemProperties.getBoolean("persist.berserk.mode.support", false);
    private static final boolean SUPPORT_QUICK_NOTE_DEFAULT = SystemProperties.getBoolean("persist.sys.quick.note.enable.default", false);
    private final String TAG = "InputManagerServiceStubImpl";
    private int mInputDebugLevel = 0;
    private int mSetDebugLevel = 0;
    private int mFromSetInputLevel = 0;
    private boolean mCustomizeInputMethod = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InputManagerServiceStubImpl> {

        /* compiled from: InputManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InputManagerServiceStubImpl INSTANCE = new InputManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public InputManagerServiceStubImpl m1389provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public InputManagerServiceStubImpl m1388provideNewInstance() {
            return new InputManagerServiceStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        MIUI_INPUTMETHOD = arrayList;
        arrayList.add("com.baidu.input_mi");
        arrayList.add("com.sohu.inputmethod.sogou.xiaomi");
        arrayList.add("com.iflytek.inputmethod.miui");
    }

    public void init(Context context, Handler handler, InputManagerService inputManagerService, NativeInputManagerService nativeInputManagerService) {
        Slog.d("InputManagerServiceStubImpl", "init");
        this.mContext = context;
        this.mHandler = handler;
        this.mNativeInputManagerService = nativeInputManagerService;
        this.mPtr = nativeInputManagerService.getPtr();
        this.mInputManagerService = inputManagerService;
        this.mMiuiInputManagerService = new MiuiInputManagerService(context);
        switchPadMode(isPad());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiuiInputManagerService getMiuiInputManagerService() {
        return this.mMiuiInputManagerService;
    }

    public void registerStub() {
        Slog.d("InputManagerServiceStubImpl", "registerStub");
        this.mMiuiInputManagerService.start();
        registerSynergyModeSettingObserver();
        updateSynergyModeFromSettings();
        registerPointerLocationSettingObserver();
        updatePointerLocationFromSettings();
        registerTouchesPreventRecordSettingObserver();
        updateTouchesPreventRecorderFromSettings();
        registerDefaultInputMethodSettingObserver();
        updateDefaultInputMethodFromSettings();
        registerInputLevelSelect();
        updateFromInputLevelSetting();
        registerMiuiOptimizationObserver();
        if (PadManager.getInstance().isPad()) {
            registerMouseGestureSettingObserver();
            updateMouseGestureSettings();
        }
        if (BERSERK_MODE) {
            registerGameMode();
        }
        if (MiuiStylusUtils.isSupportOffScreenQuickNote()) {
            registerStylusQuickNoteModeSetting();
            updateTouchStylusQuickNoteMode();
        }
    }

    public long getPtr() {
        return this.mPtr;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00a4, code lost:
    
        if (r0.equals("InputDispatcher") != false) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x0109, code lost:
    
        if (r0.equals("InputDispatcher") != false) goto L61;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean dump(java.io.FileDescriptor r12, java.io.PrintWriter r13, java.lang.String[] r14) {
        /*
            Method dump skipped, instructions count: 396
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.input.InputManagerServiceStubImpl.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    public void setShowTouchLevelFromSettings(int showTouchesSwitch) {
        this.mShowTouches = showTouchesSwitch;
        updateDebugLevel();
    }

    public void updateFromUserSwitch() {
        updateSynergyModeFromSettings();
        updatePointerLocationFromSettings();
        updateTouchesPreventRecorderFromSettings();
        updateDefaultInputMethodFromSettings();
        updateFromInputLevelSetting();
        if (BERSERK_MODE) {
            updateOnewayModeFromSettings();
        }
        if (MiuiStylusUtils.isSupportOffScreenQuickNote()) {
            updateTouchStylusQuickNoteMode();
        }
    }

    public void notifySwitch(long whenNanos, int switchValues, int switchMask) {
        Slog.d("InputManagerServiceStubImpl", "notifySwitch: values=" + Integer.toHexString(switchValues) + ", mask=" + Integer.toHexString(switchMask));
    }

    public InputDevice[] filterKeyboardDeviceIfNeeded(InputDevice[] inputDevices) {
        if (getPadManagerInstance() == null) {
            return inputDevices;
        }
        InputDevice[] newInputDevices = getPadManagerInstance().removeKeyboardDevicesIfNeeded(inputDevices);
        return newInputDevices;
    }

    public boolean interceptKeyboardNotification(Context context, int[] keyboardDeviceIds) {
        if (getPadManagerInstance() == null) {
            return false;
        }
        for (int deviceId : keyboardDeviceIds) {
            InputDevice device = this.mInputManagerService.getInputDevice(deviceId);
            if (device != null) {
                int vendorId = device.getVendorId();
                int productId = device.getProductId();
                if (MiuiKeyboardHelper.isXiaomiKeyboard(productId, vendorId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        if (getPadManagerInstance() != null) {
            getPadManagerInstance().notifyTabletSwitchChanged(tabletOpen);
        }
    }

    private void registerSynergyModeSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateSynergyModeFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSynergyModeFromSettings() {
        int synergyMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "synergy_mode", 0);
        setSynergyMode(synergyMode);
    }

    private void setSynergyMode(int synergyMode) {
        Slog.d("InputManagerServiceStubImpl", "SynergyMode changed, mode = " + synergyMode);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setSynergyMode(synergyMode);
        inputCommonConfig.flushToNative();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMouseGestureSettings() {
        int mouseNaturalScroll = Settings.Secure.getInt(this.mContext.getContentResolver(), "mouse_gesture_naturalscroll", 0);
        switchMouseNaturalScrollStatus(mouseNaturalScroll != 0);
    }

    private void registerMouseGestureSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("mouse_gesture_naturalscroll"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateMouseGestureSettings();
            }
        }, -1);
    }

    private void switchMouseNaturalScrollStatus(boolean mouseNaturalScrollStatus) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setMouseNaturalScrollStatus(mouseNaturalScrollStatus);
        inputCommonConfig.flushToNative();
    }

    private void switchPadMode(boolean padMode) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setPadMode(padMode);
        inputCommonConfig.flushToNative();
    }

    public void setInputMethodStatus(boolean shown) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setInputMethodStatus(shown, this.mCustomizeInputMethod);
        inputCommonConfig.flushToNative();
    }

    public void registerMiuiOptimizationObserver() {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.input.InputManagerServiceStubImpl.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean isCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
                Slog.i("InputManagerServiceStubImpl", "ctsMode  is:" + isCtsMode);
                InputManagerServiceStubImpl.this.setCtsMode(isCtsMode);
                MiuiKeyInterceptExtend miuiKeyInterceptExtend = MiuiKeyInterceptExtend.getInstance(InputManagerServiceStubImpl.this.mContext);
                miuiKeyInterceptExtend.setKeyboardShortcutEnable(isCtsMode ? false : true);
                miuiKeyInterceptExtend.setAospKeyboardShortcutEnable(isCtsMode);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCtsMode(boolean ctsMode) {
        Slog.d("InputManagerServiceStubImpl", "CtsMode changed, mode = " + ctsMode);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setCtsMode(ctsMode);
        inputCommonConfig.flushToNative();
    }

    private void updateDebugLevel() {
        int i = this.mShowTouches | this.mPointerLocationShow | this.mShowTouchesPreventRecord;
        this.mSetDebugLevel = i;
        int i2 = this.mFromSetInputLevel;
        if (i2 < 2) {
            this.mSetDebugLevel = i | i2;
        }
        InputDebugConfig inputDebugConfig = InputDebugConfig.getInstance();
        inputDebugConfig.setInputDispatcherMajor(this.mInputDebugLevel, this.mSetDebugLevel);
        inputDebugConfig.flushToNative();
        MiuiInputLog.getInstance().setLogLevel(this.mSetDebugLevel);
    }

    private void registerPointerLocationSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("pointer_location"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updatePointerLocationFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePointerLocationFromSettings() {
        this.mPointerLocationShow = getPointerLocationSettings(0);
        updateDebugLevel();
    }

    private int getPointerLocationSettings(int defaultValue) {
        try {
            int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), "pointer_location", -2);
            return result;
        } catch (Settings.SettingNotFoundException snfe) {
            snfe.printStackTrace();
            return defaultValue;
        }
    }

    private void registerTouchesPreventRecordSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SHOW_TOUCHES_PREVENTRECORDER), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateTouchesPreventRecorderFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTouchesPreventRecorderFromSettings() {
        this.mShowTouchesPreventRecord = getShowTouchesPreventRecordSetting(0);
        updateDebugLevel();
    }

    private int getShowTouchesPreventRecordSetting(int defaultValue) {
        try {
            int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), SHOW_TOUCHES_PREVENTRECORDER, -2);
            return result;
        } catch (Settings.SettingNotFoundException snfe) {
            snfe.printStackTrace();
            return defaultValue;
        }
    }

    public boolean isPad() {
        return PadManager.getInstance().isPad();
    }

    private void registerDefaultInputMethodSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateDefaultInputMethodFromSettings();
            }
        }, -1);
    }

    public void updateDefaultInputMethodFromSettings() {
        String inputMethodId = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        String defaultInputMethod = "";
        if (!TextUtils.isEmpty(inputMethodId) && inputMethodId.contains("/")) {
            defaultInputMethod = inputMethodId.substring(0, inputMethodId.indexOf(47));
        }
        this.mCustomizeInputMethod = MIUI_INPUTMETHOD.contains(defaultInputMethod);
    }

    private void registerInputLevelSelect() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(INPUT_LOG_LEVEL), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateFromInputLevelSetting();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFromInputLevelSetting() {
        this.mFromSetInputLevel = Settings.System.getIntForUser(this.mContext.getContentResolver(), INPUT_LOG_LEVEL, 0, -2);
        setInputLogLevel();
    }

    public boolean jumpPermissionCheck(String permission, int uid) {
        if ("android.permission.INJECT_EVENTS".equals(permission) && uid == 1002) {
            Slog.d("InputManagerServiceStubImpl", "INJECT_EVENTS Permission Denial, bypass BLUETOOTH_UID!");
            return true;
        }
        return false;
    }

    private void registerGameMode() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateOnewayModeFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateOnewayModeFromSettings() {
        boolean z = false;
        boolean isGameMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gb_boosting", 0, -2) == 1;
        boolean isCtsMode = !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        if (isGameMode && !isCtsMode) {
            z = true;
        }
        inputCommonConfig.setOnewayMode(z);
        inputCommonConfig.flushToNative();
    }

    public void registerStylusQuickNoteModeSetting() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_STYLUS_QUICK_NOTE_MODE), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateTouchStylusQuickNoteMode();
            }
        }, -1);
    }

    public void updateTouchStylusQuickNoteMode() {
        boolean booleanForUser = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), KEY_STYLUS_QUICK_NOTE_MODE, SUPPORT_QUICK_NOTE_DEFAULT, -2);
        Slog.d("InputManagerServiceStubImpl", "Update stylus quick note when screen off mode = " + booleanForUser);
        ITouchFeature.getInstance().setTouchMode(0, 24, booleanForUser ? 1 : 0);
    }

    public void beforeInjectEvent(InputEvent event, int pid) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            if ("com.android.systemui".equals(getPackageNameForPid(pid))) {
                keyEvent.setFlags(keyEvent.getFlags() | KEY_EVENT_FLAG_NAVIGATION_SYSTEM_UI);
            }
            Slog.w(TAG_INPUT, "Input event injection from pid " + pid + " action " + KeyEvent.actionToString(keyEvent.getAction()) + " keycode " + keyEvent.getKeyCode());
            return;
        }
        if (event instanceof MotionEvent) {
            MotionEvent motionEvent = (MotionEvent) event;
            int maskedAction = motionEvent.getActionMasked();
            if (maskedAction == 0 || maskedAction == 1 || maskedAction == 3) {
                Slog.w(TAG_INPUT, "Input event injection from pid " + pid + " action " + MotionEvent.actionToString(motionEvent.getAction()));
            }
        }
    }

    private static String getPackageNameForPid(int pid) {
        return ActivityManagerServiceImpl.getInstance().getPackageNameForPid(pid);
    }

    private MiuiPadKeyboardManager getPadManagerInstance() {
        if (this.mMiuiPadKeyboardManager == null) {
            this.mMiuiPadKeyboardManager = (MiuiPadKeyboardManager) LocalServices.getService(MiuiPadKeyboardManager.class);
        }
        return this.mMiuiPadKeyboardManager;
    }

    private void setInputLogLevel() {
        this.mSetDebugLevel = this.mShowTouches | this.mPointerLocationShow | this.mShowTouchesPreventRecord;
        InputDebugConfig inputDebugConfig = InputDebugConfig.getInstance();
        inputDebugConfig.setInputDebugFromDump(this.mInputDebugLevel | this.mFromSetInputLevel | this.mSetDebugLevel);
        inputDebugConfig.flushToNative();
        MiuiInputLog.getInstance().setLogLevel(this.mInputDebugLevel | this.mFromSetInputLevel | this.mSetDebugLevel);
    }

    public void onUpdatePointerDisplayId(int displayId) {
        this.mMiuiInputManagerService.updatePointerDisplayId(displayId);
    }

    public void onDisplayViewportsSet(List<DisplayViewport> viewports) {
        this.mMiuiInputManagerService.updateDisplayViewport(viewports);
    }

    public void setInteractive(boolean interactive) {
        this.mMiuiInputManagerService.setInteractive(interactive);
    }

    public boolean needInterceptSetPointerIconType(int iconType) {
        if (ensureMiuiMagicPointerServiceInit()) {
            return false;
        }
        return this.mMiuiMagicPointerService.needInterceptSetPointerIconType(iconType);
    }

    public boolean needInterceptSetCustomPointerIcon(PointerIcon icon) {
        if (ensureMiuiMagicPointerServiceInit()) {
            return false;
        }
        return this.mMiuiMagicPointerService.needInterceptSetCustomPointerIcon(icon);
    }

    private boolean ensureMiuiMagicPointerServiceInit() {
        if (this.mMiuiMagicPointerService != null) {
            return false;
        }
        MiuiMagicPointerServiceInternal miuiMagicPointerServiceInternal = (MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class);
        this.mMiuiMagicPointerService = miuiMagicPointerServiceInternal;
        return miuiMagicPointerServiceInternal == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPointerIconType(int iconType) {
        this.mInputManagerService.setPointerIconType(iconType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCustomPointerIcon(PointerIcon pointerIcon) {
        this.mInputManagerService.setCustomPointerIcon(pointerIcon);
    }

    public void handleAppDied(int pid, ProcessRecord app) {
        if (ensureMiuiMagicPointerServiceInit()) {
            return;
        }
        this.mMiuiMagicPointerService.handleAppDied(pid, app);
    }
}
