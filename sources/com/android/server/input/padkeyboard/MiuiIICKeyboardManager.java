package com.android.server.input.padkeyboard;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.UiThread;
import com.android.server.input.InputManagerService;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.android.server.input.padkeyboard.iic.IICNodeHelper;
import com.android.server.input.padkeyboard.iic.NanoSocketCallback;
import com.android.server.policy.MiuiKeyInterceptExtend;
import com.miui.server.input.PadManager;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.NetworkBoost.StatusManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import miui.hardware.input.InputFeature;
import miui.hardware.input.MiuiKeyboardStatus;
import miui.util.FeatureParser;
import miuix.appcompat.app.AlertDialog;

/* loaded from: classes.dex */
public class MiuiIICKeyboardManager implements NanoSocketCallback, MiuiPadKeyboardManager, KeyboardInteraction.KeyboardStatusChangedListener {
    public static final String AUTO_BACK_BRIGHTNESS = "keyboard_back_light_automatic_adjustment";
    public static final String CURRENT_BACK_BRIGHTNESS = "keyboard_back_light_brightness";
    private static final int DEFAULT_DEVICE_ID = -99;
    public static final String ENABLE_TAP_TOUCH_PAD = "enable_tap_touch_pad";
    private static final String ENABLE_UPGRADE_NO_CHECK = "enable_upgrade_no_check";
    public static final String LAST_CONNECT_BLE_ADDRESS = "miui_keyboard_address";
    private static final int MAX_CHECK_IDENTITY_TIMES = 5;
    private static volatile MiuiIICKeyboardManager sInstance;
    private final Sensor mAccSensor;
    private final AngleStateController mAngleStateController;
    private final BluetoothKeyboardManager mBluetoothKeyboardManager;
    private CommunicationUtil mCommunicationUtil;
    private final Context mContext;
    private AlertDialog mDialog;
    private final Handler mHandler;
    private boolean mHasTouchPad;
    private final IICNodeHelper mIICNodeHelper;
    private final InputManagerService mInputManager;
    private boolean mIsKeyboardReady;
    private boolean mIsKeyboardSleep;
    private InputDevice mKeyboardDevice;
    private final KeyboardSettingsObserver mKeyboardObserver;
    private String mKeyboardVersion;
    private LanguageChangeReceiver mLanguageChangeReceiver;
    private final Sensor mLightSensor;
    private String mMCUVersion;
    private final PowerManager mPowerManager;
    private final SensorManager mSensorManager;
    private boolean mShouldStayWakeKeyboard;
    private String mTouchPadVersion;
    private final PowerManager.WakeLock mWakeLock;
    private final float[] mLocalGData = new float[3];
    private final float[] mKeyboardGData = new float[3];
    private final float G = 9.8f;
    private volatile boolean mCheckIdentityPass = false;
    private int mKeyboardType = 321;
    private Object mlock = new Object();
    private int mCheckIdentityTimes = -1;
    private int mKeyboardDeviceId = DEFAULT_DEVICE_ID;
    private int mConsumerDeviceId = DEFAULT_DEVICE_ID;
    private int mTouchDeviceId = DEFAULT_DEVICE_ID;
    private final Set<Integer> mBleDeviceList = Collections.synchronizedSet(new HashSet());
    private final Object mBleDeviceListLock = new Object();
    private boolean mScreenState = true;
    private boolean mPadAccReady = false;
    private boolean mKeyboardAccReady = false;
    private boolean mIsAutoBackLightEffected = true;
    private boolean mIsAutoBackLight = false;
    private int mBackLightBrightness = 0;
    private volatile boolean mAuthStarted = false;
    private boolean mUserSetBackLight = false;
    private boolean mShouldUpgradeMCU = true;
    private boolean mShouldUpgradeKeyboard = true;
    private boolean mShouldUpgradeTouchPad = true;
    private final BroadcastReceiver mMicMuteReceiver = new BroadcastReceiver() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.action.MICROPHONE_MUTE_CHANGED")) {
                MiuiIICKeyboardManager.this.setMuteLight(PadManager.getInstance().getMuteStatus());
            }
        }
    };
    private Runnable mStopAwakeRunnable = new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager.2
        @Override // java.lang.Runnable
        public void run() {
            Slog.i(MiuiPadKeyboardManager.TAG, "release awake");
            MiuiIICKeyboardManager.this.releaseWakeLock();
            MiuiIICKeyboardManager.this.stopWakeForKeyboard176();
        }
    };

    private MiuiIICKeyboardManager(Context context) {
        Slog.i(MiuiPadKeyboardManager.TAG, "MiuiIICKeyboardManager");
        HandlerThread handlerThread = new HandlerThread("IIC_Pad_Manager");
        handlerThread.start();
        this.mContext = context;
        this.mBluetoothKeyboardManager = BluetoothKeyboardManager.getInstance(context);
        this.mHandler = new H(handlerThread.getLooper());
        this.mAngleStateController = new AngleStateController(context);
        this.mInputManager = ServiceManager.getService(DumpSysInfoUtil.INPUT);
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mAccSensor = sensorManager.getDefaultSensor(1);
        this.mLightSensor = sensorManager.getDefaultSensor(5);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mWakeLock = powerManager.newWakeLock(6, "iic_upgrade");
        this.mLanguageChangeReceiver = new LanguageChangeReceiver();
        IntentFilter languageFilter = new IntentFilter("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mLanguageChangeReceiver, languageFilter);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setMiuiKeyboardInfo(5593, 163);
        inputCommonConfig.flushToNative();
        KeyboardSettingsObserver keyboardSettingsObserver = new KeyboardSettingsObserver(MiuiInputThread.getHandler());
        this.mKeyboardObserver = keyboardSettingsObserver;
        IICNodeHelper iICNodeHelper = IICNodeHelper.getInstance(context);
        this.mIICNodeHelper = iICNodeHelper;
        iICNodeHelper.setOtaCallBack(this);
        keyboardSettingsObserver.observerObject();
        KeyboardInteraction.INTERACTION.setMiuiIICKeyboardManagerLocked(this);
        KeyboardInteraction.INTERACTION.addListener(this);
        this.mCommunicationUtil = CommunicationUtil.getInstance();
        readKeyboardStatus();
        readHallStatus();
        sendCommand(10);
        sendCommand(9);
    }

    public static MiuiIICKeyboardManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MiuiIICKeyboardManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiIICKeyboardManager(context);
                }
            }
        }
        return sInstance;
    }

    public static boolean supportPadKeyboard() {
        return FeatureParser.getBoolean("support_iic_keyboard", false);
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (!isKeyboardReady()) {
            return;
        }
        if (event.sensor.getType() == 1) {
            if (motionJudge(event.values[0], event.values[1], event.values[2])) {
                this.mPadAccReady = false;
                return;
            }
            if (this.mIsKeyboardReady) {
                if (!this.mPadAccReady || Math.abs(event.values[0] - this.mLocalGData[0]) > 0.294f || Math.abs(event.values[1] - this.mLocalGData[1]) > 0.196f || Math.abs(event.values[2] - this.mLocalGData[2]) > 0.588f) {
                    float[] fArr = event.values;
                    float[] fArr2 = this.mLocalGData;
                    System.arraycopy(fArr, 0, fArr2, 0, fArr2.length);
                    this.mPadAccReady = true;
                    if (!isIICKeyboardActive()) {
                        wakeKeyboard176();
                    }
                    calculateAngle();
                    return;
                }
                return;
            }
            return;
        }
        if (event.sensor.getType() == 5 && !this.mUserSetBackLight) {
            int brightness = MiuiKeyboardUtil.getBackLightBrightnessWithSensor(event.values[0]);
            if (this.mIsKeyboardReady && brightness != this.mBackLightBrightness) {
                if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
                    if (isIICKeyboardActive()) {
                        setBacklight(brightness);
                        return;
                    } else {
                        this.mBackLightBrightness = brightness;
                        return;
                    }
                }
                if (KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
                    setBacklight(brightness);
                }
            }
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public InputDevice[] removeKeyboardDevicesIfNeeded(InputDevice[] allInputDevices) {
        int i;
        ArrayList<InputDevice> tempDevices = new ArrayList<>();
        Handler handler = this.mHandler;
        final Set<Integer> set = this.mBleDeviceList;
        Objects.requireNonNull(set);
        handler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                set.clear();
            }
        });
        for (InputDevice device : allInputDevices) {
            final int deviceId = device.getId();
            if (device.getVendorId() == 5593) {
                if (device.getProductId() == 163) {
                    if (this.mKeyboardDeviceId != deviceId) {
                        this.mKeyboardDeviceId = deviceId;
                    }
                    this.mKeyboardDevice = device;
                    i = KeyboardInteraction.INTERACTION.isConnectIICLocked() ? 0 : i + 1;
                    tempDevices.add(device);
                } else if (device.getProductId() == 164) {
                    if (this.mConsumerDeviceId != deviceId) {
                        this.mConsumerDeviceId = deviceId;
                    }
                    if (!KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
                    }
                    tempDevices.add(device);
                } else if (device.getProductId() == 161) {
                    if (this.mTouchDeviceId != deviceId) {
                        this.mTouchDeviceId = deviceId;
                    }
                    if (!KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
                    }
                    tempDevices.add(device);
                } else {
                    if (device.getProductId() == 162) {
                    }
                    tempDevices.add(device);
                }
            } else {
                if (device.getVendorId() == 48897 && device.getProductId() == 64) {
                    this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda7
                        @Override // java.lang.Runnable
                        public final void run() {
                            MiuiIICKeyboardManager.this.lambda$removeKeyboardDevicesIfNeeded$0(deviceId);
                        }
                    });
                    if (InputFeature.isSingleBleKeyboard()) {
                        if ((device.getSources() & BluetoothKeyboardManager.XIAOMI_KEYBOARD_ATTACH_FINISH_SOURCE_N83) != 769) {
                        }
                    } else if ((device.getSources() & BluetoothKeyboardManager.XIAOMI_KEYBOARD_ATTACH_FINISH_SOURCE) != 8963) {
                    }
                }
                tempDevices.add(device);
            }
        }
        return (InputDevice[]) tempDevices.toArray(new InputDevice[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeKeyboardDevicesIfNeeded$0(int deviceId) {
        this.mBleDeviceList.add(Integer.valueOf(deviceId));
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyLidSwitchChanged(boolean lidOpen) {
        Slog.i(MiuiPadKeyboardManager.TAG, "Lid Switch Changed to " + lidOpen);
        this.mAngleStateController.notifyLidSwitchChanged(lidOpen);
        enableOrDisableInputDevice();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        Slog.i(MiuiPadKeyboardManager.TAG, "TabletSwitch changed to " + tabletOpen);
        this.mAngleStateController.notifyTabletSwitchChanged(tabletOpen);
        enableOrDisableInputDevice();
        PadManager.getInstance().setIsTableOpen(tabletOpen);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onHallStatusChanged(byte status) {
        boolean lidStatus = (MiuiKeyboardUtil.byte2int(status) & 16) != 0;
        boolean tabletStatus = (MiuiKeyboardUtil.byte2int(status) & 1) != 0;
        notifyLidSwitchChanged(lidStatus);
        notifyTabletSwitchChanged(tabletStatus);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyScreenState(boolean screenState) {
        if (this.mScreenState == screenState) {
            return;
        }
        this.mScreenState = screenState;
        Slog.i(MiuiPadKeyboardManager.TAG, "Notify Screen State changed to " + screenState);
        if (!screenState) {
            MiuiKeyInterceptExtend.getInstance(this.mContext).setKeyboardShortcutEnable(false);
        } else {
            MiuiInputThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiIICKeyboardManager.this.lambda$notifyScreenState$1();
                }
            }, 300L);
            if (InputFeature.isSingleBleKeyboard()) {
                this.mBluetoothKeyboardManager.notifyUpgradeIfNeed();
            }
        }
        stopAllCommandWorker();
        if (this.mScreenState) {
            if (this.mIsKeyboardReady) {
                updateLightSensor();
            }
            if (MiuiKeyboardUtil.supportCalculateAngle()) {
                this.mSensorManager.registerListener(this, this.mAccSensor, 3);
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiIICKeyboardManager.this.onKeyboardAttach();
                    }
                }, 200L);
                return;
            } else {
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiIICKeyboardManager.this.lambda$notifyScreenState$2();
                    }
                }, 200L);
                return;
            }
        }
        this.mUserSetBackLight = false;
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null && alertDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        this.mSensorManager.unregisterListener(this);
        this.mHandler.removeCallbacksAndMessages(null);
        resetCalculateStatus();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyScreenState$1() {
        MiuiKeyInterceptExtend.getInstance(this.mContext).setKeyboardShortcutEnable(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyScreenState$2() {
        sendCommand(9);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void getKeyboardReportData() {
        sendCommand(3);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void enableOrDisableInputDevice() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.changeDeviceEnableState();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void changeDeviceEnableState() {
        if (!isDeviceEnableStatusChanged()) {
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "enableOrEnableDevice:" + this.mKeyboardDeviceId + "," + this.mConsumerDeviceId + "," + this.mTouchDeviceId);
        if (this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            Slog.i(MiuiPadKeyboardManager.TAG, "Disable Xiaomi Keyboard!");
            setKeyboardStatus();
            int i = this.mKeyboardDeviceId;
            if (i != DEFAULT_DEVICE_ID) {
                this.mInputManager.disableInputDevice(i);
            }
            int i2 = this.mConsumerDeviceId;
            if (i2 != DEFAULT_DEVICE_ID) {
                this.mInputManager.disableInputDevice(i2);
            }
            int i3 = this.mTouchDeviceId;
            if (i3 != DEFAULT_DEVICE_ID) {
                this.mInputManager.disableInputDevice(i3);
            }
            Iterator<Integer> it = this.mBleDeviceList.iterator();
            while (it.hasNext()) {
                int bleDevice = it.next().intValue();
                if (bleDevice != DEFAULT_DEVICE_ID) {
                    Slog.i(MiuiPadKeyboardManager.TAG, "Disable Ble Keyboard:" + bleDevice);
                    this.mInputManager.disableInputDevice(bleDevice);
                }
            }
        } else {
            Slog.i(MiuiPadKeyboardManager.TAG, "Enable Xiaomi Keyboard!");
            setKeyboardStatus();
            int i4 = this.mKeyboardDeviceId;
            if (i4 != DEFAULT_DEVICE_ID) {
                this.mInputManager.enableInputDevice(i4);
            }
            int i5 = this.mConsumerDeviceId;
            if (i5 != DEFAULT_DEVICE_ID) {
                this.mInputManager.enableInputDevice(i5);
            }
            int i6 = this.mTouchDeviceId;
            if (i6 != DEFAULT_DEVICE_ID) {
                this.mInputManager.enableInputDevice(i6);
            }
            Iterator<Integer> it2 = this.mBleDeviceList.iterator();
            while (it2.hasNext()) {
                int bleDevice2 = it2.next().intValue();
                if (bleDevice2 != DEFAULT_DEVICE_ID) {
                    Slog.i(MiuiPadKeyboardManager.TAG, "Enable Ble Keyboard:" + bleDevice2);
                    this.mInputManager.enableInputDevice(bleDevice2);
                }
            }
        }
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    private boolean isDeviceEnableStatusChanged() {
        InputDevice iicKeyboardDevice = this.mInputManager.getInputDevice(this.mKeyboardDeviceId);
        if (iicKeyboardDevice != null && iicKeyboardDevice.isEnabled() == this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            return true;
        }
        InputDevice consumerKeyboardDevice = this.mInputManager.getInputDevice(this.mConsumerDeviceId);
        if (consumerKeyboardDevice != null && consumerKeyboardDevice.isEnabled() == this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            return true;
        }
        InputDevice touchPadDevice = this.mInputManager.getInputDevice(this.mTouchDeviceId);
        if (touchPadDevice != null && touchPadDevice.isEnabled() == this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            return true;
        }
        Iterator<Integer> it = this.mBleDeviceList.iterator();
        while (it.hasNext()) {
            int belDeviceId = it.next().intValue();
            InputDevice bleDevice = this.mInputManager.getInputDevice(belDeviceId);
            if (bleDevice != null && bleDevice.isEnabled() == this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public boolean isKeyboardReady() {
        return this.mIsKeyboardReady && this.mScreenState;
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onUpdateVersion(int type, String version) {
        String device = null;
        if (type == 20) {
            device = "Keyboard";
            this.mKeyboardVersion = version;
            getKeyboardReportData();
        } else if (type == 3) {
            device = "Flash Keyboard";
        } else if (type == 10) {
            device = "Pad MCU";
            this.mMCUVersion = version;
            MiuiKeyboardUtil.setMcuVersion(version);
        } else if (type == 40) {
            device = "TouchPad";
            this.mTouchPadVersion = version;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "getVersion for " + device + " is :" + version);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onUpdateKeyboardType(byte type, boolean hasTouchPad) {
        this.mHasTouchPad = hasTouchPad;
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), MiuiKeyboardUtil.KEYBOARD_TYPE_LEVEL, hasTouchPad ? 1 : 0, -2);
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onOtaStateChange(byte targetAddress, int status) {
        String target = parseAddress2String(targetAddress);
        if (status == 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, target + " OTA Start!");
            if (targetAddress == 56) {
                Context context = this.mContext;
                Toast.makeText(context, context.getResources().getString(286196302), 0).show();
            }
        }
        if (status == 2) {
            if (targetAddress == 56) {
                Context context2 = this.mContext;
                Toast.makeText(context2, context2.getResources().getString(286196303), 0).show();
                this.mShouldUpgradeKeyboard = false;
            } else if (targetAddress == 64) {
                this.mShouldUpgradeTouchPad = false;
            } else if (targetAddress == 24) {
                this.mShouldUpgradeMCU = false;
            }
            Slog.i(MiuiPadKeyboardManager.TAG, target + " OTA Successfully!");
        }
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onWriteSocketErrorInfo(String reason) {
        Slog.e(MiuiPadKeyboardManager.TAG, "Write Socket Fail because:" + reason);
        if (reason.contains(NanoSocketCallback.OTA_ERROR_REASON_WRITE_SOCKET_EXCEPTION)) {
            stopAllCommandWorker();
        }
    }

    public void stopStayAwake() {
        this.mHandler.postDelayed(this.mStopAwakeRunnable, 300L);
    }

    public void stayAwake() {
        Slog.i(MiuiPadKeyboardManager.TAG, "stay awake");
        this.mHandler.removeCallbacks(this.mStopAwakeRunnable);
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.acquireWakeLock();
            }
        });
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.stayWakeForKeyboard176();
            }
        });
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onOtaErrorInfo(byte targetAddress, String reason) {
        onOtaErrorInfo(targetAddress, reason, false, -1);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onOtaErrorInfo(byte targetAddress, String reason, boolean isNeedToast, int resId) {
        if (isNeedToast && resId != -1) {
            Context context = this.mContext;
            Toast.makeText(context, context.getResources().getString(resId), 0).show();
        }
        Slog.e(MiuiPadKeyboardManager.TAG, parseAddress2String(targetAddress) + " OTA Fail because: " + reason);
        if (reason != null) {
            if (reason.contains(NanoSocketCallback.OTA_ERROR_REASON_VERSION) || reason.contains(NanoSocketCallback.OTA_ERROR_REASON_NO_VALID)) {
                if (targetAddress == 56) {
                    this.mShouldUpgradeKeyboard = false;
                } else if (targetAddress == 64) {
                    this.mShouldUpgradeTouchPad = false;
                } else if (targetAddress == 24) {
                    this.mShouldUpgradeMCU = false;
                }
            }
        }
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onReadSocketNumError(String reason) {
        Slog.e(MiuiPadKeyboardManager.TAG, "read socket find num error:" + reason);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onOtaProgress(byte targetAddress, float pro) {
        if (pro == 50.0f || pro == 100.0f) {
            Slog.i(MiuiPadKeyboardManager.TAG, "Current upgrade progress： type = " + parseAddress2String(targetAddress) + ", process: " + pro);
        }
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardGSensorChanged(float x, float y, float z) {
        float[] fArr = this.mKeyboardGData;
        fArr[0] = x;
        fArr[1] = y;
        fArr[2] = z;
        this.mKeyboardAccReady = true;
        calculateAngle();
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardSleepStatusChanged(boolean isSleep) {
        if (this.mIsKeyboardSleep != isSleep) {
            this.mIsKeyboardSleep = isSleep;
            Slog.i(MiuiPadKeyboardManager.TAG, "Keyboard will sleep:" + this.mIsKeyboardSleep);
            this.mBluetoothKeyboardManager.setIICKeyboardSleepStatus(isSleep);
            if (!this.mIsKeyboardSleep) {
                setBacklight(this.mBackLightBrightness);
            }
        }
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onNFCTouched() {
        if (!this.mScreenState) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "NFC Device Touched");
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void readKeyboardStatus() {
        sendCommand(6);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void readHallStatus() {
        sendCommand(8);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] sendCommandForRespond(byte[] command, MiuiPadKeyboardManager.CommandCallback callback) {
        byte[] result = this.mIICNodeHelper.checkAuth(command);
        if (result != null && (callback == null || callback.isCorrectPackage(result))) {
            return result;
        }
        return new byte[0];
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiDevAuthInit() {
        return MiuiKeyboardUtil.commandMiDevAuthInitForIIC();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiAuthStep3Type1(byte[] keyMeta, byte[] challenge) {
        return MiuiKeyboardUtil.commandMiAuthStep3Type1ForIIC(keyMeta, challenge);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiAuthStep5Type1(byte[] token) {
        return MiuiKeyboardUtil.commandMiAuthStep5Type1ForIIC(token);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public MiuiKeyboardStatus getKeyboardStatus() {
        return new MiuiKeyboardStatus(this.mIsKeyboardReady, this.mAngleStateController.getIdentityStatus(), this.mAngleStateController.isWorkState(), this.mAngleStateController.getLidStatus(), this.mAngleStateController.getTabletStatus(), this.mAngleStateController.shouldIgnoreKeyboardForIIC(), this.mMCUVersion, this.mKeyboardVersion);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void setCapsLockLight(boolean z) {
        int index;
        Slog.i(MiuiPadKeyboardManager.TAG, "setCapsLight :" + z);
        Bundle bundle = new Bundle();
        bundle.putInt(H.DATA_FEATURE_VALUE, z ? 1 : 0);
        if (MiuiKeyboardUtil.isXM2022MCU()) {
            index = CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getIndex();
        } else {
            index = CommunicationUtil.KB_FEATURE.KB_CAPS_KEY_NEW.getIndex();
        }
        bundle.putInt(H.DATA_FEATURE_TYPE, index);
        sendCommand(14, bundle);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void setMuteLight(boolean z) {
        int index;
        Slog.i(MiuiPadKeyboardManager.TAG, "setMuteLight :" + z);
        Bundle bundle = new Bundle();
        bundle.putInt(H.DATA_FEATURE_VALUE, z ? 1 : 0);
        if (MiuiKeyboardUtil.isXM2022MCU()) {
            index = CommunicationUtil.KB_FEATURE.KB_MIC_MUTE.getIndex();
        } else {
            index = CommunicationUtil.KB_FEATURE.KB_MIC_MUTE_NEW.getIndex();
        }
        bundle.putInt(H.DATA_FEATURE_TYPE, index);
        sendCommand(15, bundle);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void setKeyboardBackLightBrightness(int brightness) {
        this.mUserSetBackLight = true;
        updateBackLightBrightness(brightness);
    }

    private void updateBackLightBrightness(int brightness) {
        if (this.mBackLightBrightness != brightness) {
            this.mBluetoothKeyboardManager.wakeUpKeyboardIfNeed();
            setBacklight(brightness);
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public int getKeyboardBackLightBrightness() {
        return this.mBackLightBrightness;
    }

    @Override // com.android.server.input.padkeyboard.KeyboardInteraction.KeyboardStatusChangedListener
    public void onKeyboardStatusChanged(int connectionType, boolean connectionStatus) {
        onKeyboardStateChanged(connectionType, connectionStatus);
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardEnableStateChanged(boolean isEnable) {
        Slog.i(MiuiPadKeyboardManager.TAG, "receiver keyboard enable status change to " + isEnable);
        this.mAngleStateController.setShouldIgnoreKeyboardFromKeyboard(!isEnable);
        enableOrDisableInputDevice();
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void requestStayAwake() {
        stayAwake();
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void requestReleaseAwake() {
        stopStayAwake();
    }

    @Override // com.android.server.input.padkeyboard.iic.NanoSocketCallback
    public void requestReAuth() {
        startCheckIdentity(0, true);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println("MIuiI2CKeyboardManager");
        pw.print(prefix);
        pw.print("mMcuVersion=");
        pw.println(this.mMCUVersion);
        pw.print(prefix);
        pw.print("mKeyboardVersion=");
        pw.println(this.mKeyboardVersion);
        pw.print(prefix);
        pw.print("mTouchPadVersion=");
        pw.println(this.mTouchPadVersion);
        pw.print(prefix);
        pw.print("AuthState=");
        pw.println(this.mCheckIdentityPass);
        pw.print(prefix);
        pw.print("mIsKeyboardReady=");
        pw.println(this.mIsKeyboardReady);
        pw.print(prefix);
        pw.print("isKeyboardDisable=");
        pw.println(this.mAngleStateController.shouldIgnoreKeyboardForIIC());
        pw.print("mAuthStarted=");
        pw.println(this.mAuthStarted);
        if (this.mHasTouchPad) {
            pw.print(prefix);
            pw.print("hasTouchPad=");
            pw.println(this.mHasTouchPad);
            pw.print(prefix);
            pw.print("backLightBrightness=");
            pw.println(this.mBackLightBrightness);
        }
        this.mAngleStateController.dump(prefix, pw);
        pw.print(prefix);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LanguageChangeReceiver extends BroadcastReceiver {
        private LanguageChangeReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:45:0x00b4, code lost:
        
            if (r1.equals(com.android.server.input.padkeyboard.MiuiKeyboardUtil.UK) != false) goto L41;
         */
        @Override // android.content.BroadcastReceiver
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onReceive(android.content.Context r12, android.content.Intent r13) {
            /*
                Method dump skipped, instructions count: 492
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.input.padkeyboard.MiuiIICKeyboardManager.LanguageChangeReceiver.onReceive(android.content.Context, android.content.Intent):void");
        }
    }

    private String parseAddress2String(byte address) {
        if (address == 24) {
            return "MCU";
        }
        if (address == 64) {
            return "TouchPad";
        }
        if (address != 56) {
            return "";
        }
        return "Keyboard";
    }

    private void onKeyboardStateChanged(int connectionType, boolean connectionStatus) {
        boolean result = KeyboardInteraction.INTERACTION.haveConnection();
        if (connectionType == 0) {
            stopAllCommandWorker();
            if (!connectionStatus && KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
                updateBackLightBrightness(Math.min(this.mBackLightBrightness, 50));
            }
        }
        if (this.mIsKeyboardReady == result) {
            if (connectionType == 0 && connectionStatus) {
                this.mShouldUpgradeKeyboard = true;
                this.mShouldUpgradeTouchPad = true;
                sendCommand(12);
                sendCommand(1);
                startCheckIdentity(0, true);
                this.mSensorManager.registerListener(this, this.mAccSensor, 3);
            }
            ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "Keyboard link state changed:" + result + ", old status:" + this.mIsKeyboardReady);
        this.mIsKeyboardReady = result;
        if (result) {
            if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
                this.mShouldUpgradeKeyboard = true;
                onKeyboardAttach();
            }
            updateLightSensor();
            this.mSensorManager.registerListener(this, this.mAccSensor, 3);
        } else {
            this.mSensorManager.unregisterListener(this);
            resetCalculateStatus();
            onKeyboardDetach();
        }
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    public static int shouldClearActivityInfoFlags() {
        return StatusManager.CALLBACK_VPN_STATUS;
    }

    public void writeCommandToIIC(byte[] command) {
        this.mCommunicationUtil.writeSocketCmd(command);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLightSensor() {
        this.mSensorManager.unregisterListener(this, this.mLightSensor);
        if (this.mIsAutoBackLight) {
            this.mSensorManager.registerListener(this, this.mLightSensor, 3);
        }
    }

    void sendCommand(int command) {
        sendCommand(command, new Bundle());
    }

    void sendCommand(int command, Bundle data) {
        sendCommand(command, data, 0);
    }

    void sendCommand(int command, Bundle data, int delay) {
        Message message = this.mHandler.obtainMessage();
        message.what = command;
        message.setData(data);
        this.mHandler.removeMessages(command);
        this.mHandler.sendMessageDelayed(message, delay);
    }

    boolean isIICKeyboardActive() {
        return KeyboardInteraction.INTERACTION.isConnectIICLocked() && !this.mIsKeyboardSleep;
    }

    public void writePadKeyBoardStatus(int target, int value) {
        Bundle data = new Bundle();
        data.putInt(H.DATA_FEATURE_VALUE, value);
        data.putInt(H.DATA_FEATURE_TYPE, target);
        sendCommand(4, data);
    }

    private void startCheckIdentity(int delay, boolean isFirst) {
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked() && this.mScreenState) {
            stayAwake();
            if (isFirst) {
                if (this.mAuthStarted) {
                    return;
                }
                this.mAuthStarted = true;
                this.mCheckIdentityTimes = 0;
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean(H.DATA_KEY_FIRST_CHECK, isFirst);
            sendCommand(5, bundle, delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onKeyboardAttach() {
        this.mAngleStateController.wakeUpIfNeed(!this.mScreenState);
        setKeyboardStatus();
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
            sendCommand(10);
            sendCommand(9);
            sendCommand(12);
            sendCommand(1);
            startCheckIdentity(0, true);
        }
    }

    private void onKeyboardDetach() {
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null && alertDialog.isShowing()) {
            this.mDialog.dismiss();
        }
    }

    private void stopAllCommandWorker() {
        this.mHandler.removeCallbacksAndMessages(null);
        stopStayAwake();
        Handler handler = this.mHandler;
        final IICNodeHelper iICNodeHelper = this.mIICNodeHelper;
        Objects.requireNonNull(iICNodeHelper);
        handler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.shutdownAllCommandQueue();
            }
        });
        this.mAuthStarted = false;
    }

    private void setAuthState(boolean state) {
        this.mCheckIdentityPass = state;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doCheckKeyboardIdentity(boolean isFirst) {
        int result;
        if (MiuiKeyboardUtil.isXM2022MCU()) {
            result = KeyboardAuthHelper.getInstance(this.mContext).doCheckKeyboardIdentityLaunchBeforeU(this, isFirst);
        } else {
            result = KeyboardAuthHelper.getInstance(this.mContext).doCheckKeyboardIdentityLaunchAfterU(this, isFirst);
        }
        processIdentity(result);
        if (isFailedIdentity(result)) {
            stopAllCommandWorker();
            return;
        }
        if (isSuccessfulIdentity(result)) {
            if (this.mShouldUpgradeKeyboard) {
                sendCommand(0);
            }
            if (this.mShouldUpgradeTouchPad) {
                sendCommand(13);
            }
            if (!this.mShouldUpgradeKeyboard && !this.mShouldUpgradeTouchPad) {
                stopStayAwake();
            }
        }
    }

    private boolean isSuccessfulIdentity(int result) {
        return result == 0 || result == 3 || (result == 2 && this.mCheckIdentityTimes == 5);
    }

    private boolean isFailedIdentity(int result) {
        return result == 1 || result == 4;
    }

    private void processIdentity(int identity) {
        switch (identity) {
            case 0:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth ok");
                this.mAngleStateController.setIdentityState(true);
                setAuthState(true);
                enableOrDisableInputDevice();
                break;
            case 1:
                postShowRejectConfirmDialog(1);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth reject");
                this.mAngleStateController.setIdentityState(false);
                setAuthState(false);
                enableOrDisableInputDevice();
                break;
            case 2:
                int i = this.mCheckIdentityTimes + 1;
                this.mCheckIdentityTimes = i;
                if (i < 5) {
                    Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity need check again");
                    startCheckIdentity(5000, false);
                    break;
                } else {
                    this.mAngleStateController.setIdentityState(true);
                    setAuthState(true);
                    break;
                }
            case 3:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity internal error");
                this.mAngleStateController.setIdentityState(false);
                setAuthState(false);
                break;
            case 4:
                postShowRejectConfirmDialog(4);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity transfer error");
                this.mAngleStateController.setIdentityState(false);
                setAuthState(false);
                enableOrDisableInputDevice();
                break;
        }
        if (identity != 2 || (identity == 2 && this.mCheckIdentityTimes == 5)) {
            this.mAuthStarted = false;
        }
    }

    private void postShowRejectConfirmDialog(final int type) {
        UiThread.getHandler().post(new Runnable() { // from class: com.android.server.input.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.lambda$postShowRejectConfirmDialog$3(type);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: showRejectConfirmDialog, reason: merged with bridge method [inline-methods] */
    public void lambda$postShowRejectConfirmDialog$3(int type) {
        String message;
        ContextImpl systemUiContext = ActivityThread.currentActivityThread().getSystemUiContext();
        switch (type) {
            case 1:
                message = systemUiContext.getResources().getString(286196299);
                break;
            case 4:
                message = systemUiContext.getResources().getString(286196300);
                break;
            default:
                return;
        }
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null) {
            alertDialog.setMessage(message);
        } else {
            AlertDialog create = new AlertDialog.Builder(systemUiContext, 1712390150).setCancelable(true).setMessage(message).setPositiveButton(systemUiContext.getResources().getString(286196298), (DialogInterface.OnClickListener) null).create();
            this.mDialog = create;
            WindowManager.LayoutParams attrs = create.getWindow().getAttributes();
            attrs.type = 2003;
            attrs.flags |= 131072;
            attrs.gravity = 17;
            attrs.privateFlags |= 272;
            this.mDialog.getWindow().setAttributes(attrs);
        }
        this.mDialog.show();
    }

    private void calculateAngle() {
        if (this.mKeyboardAccReady && this.mPadAccReady && MiuiKeyboardUtil.supportCalculateAngle()) {
            float[] fArr = this.mKeyboardGData;
            float f = fArr[0];
            float f2 = fArr[1];
            float f3 = fArr[2];
            float[] fArr2 = this.mLocalGData;
            int resultAngle = MiuiKeyboardUtil.calculatePKAngleV2(f, f2, f3, fArr2[0], fArr2[1], fArr2[2]);
            if (resultAngle == -1) {
                Slog.i(MiuiPadKeyboardManager.TAG, "accel angle error > 0.98, dont update angle");
                return;
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "result Angle = " + resultAngle);
            this.mAngleStateController.updateAngleState(resultAngle);
            enableOrDisableInputDevice();
            return;
        }
        Slog.e(MiuiPadKeyboardManager.TAG, "The Pad or Keyboard gsensor not ready, mKeyboardAccReady: " + this.mKeyboardAccReady + ", mPadAccReady: " + this.mPadAccReady + ", IIC maybe disconnect");
    }

    private boolean motionJudge(float accX, float accY, float accZ) {
        float mCombAcc = (float) Math.sqrt((accX * accX) + (accY * accY) + (accZ * accZ));
        return Math.abs(mCombAcc - 9.8f) > 0.294f;
    }

    private void resetCalculateStatus() {
        this.mKeyboardAccReady = false;
        this.mPadAccReady = false;
        float[] fArr = this.mLocalGData;
        fArr[0] = 0.0f;
        fArr[1] = 0.0f;
        fArr[2] = 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseWakeLock() {
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void acquireWakeLock() {
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void wakeKeyboard176() {
        writePadKeyBoardStatus(CommunicationUtil.KB_FEATURE.KB_POWER.getIndex(), 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stayWakeForKeyboard176() {
        if (!this.mShouldStayWakeKeyboard) {
            Message msg = this.mHandler.obtainMessage(7);
            this.mHandler.sendMessageDelayed(msg, 10000L);
            this.mShouldStayWakeKeyboard = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopWakeForKeyboard176() {
        if (this.mShouldStayWakeKeyboard) {
            this.mShouldStayWakeKeyboard = false;
            this.mHandler.removeMessages(7);
        }
    }

    private void setBacklight(int brightness) {
        Bundle data = new Bundle();
        data.putByte(H.DATA_FEATURE_VALUE, (byte) brightness);
        sendCommand(11, data);
        Slog.i(MiuiPadKeyboardManager.TAG, "set keyboard backLight brightness " + brightness);
    }

    private void setKeyboardStatus() {
        if (!this.mScreenState) {
            Slog.i(MiuiPadKeyboardManager.TAG, "Abandon Communicate with keyboard because screen");
            return;
        }
        if (!this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            setCapsLockLight(PadManager.getInstance().getCapsLockStatus());
            setBacklight(this.mBackLightBrightness);
            setMuteLight(PadManager.getInstance().getMuteStatus());
        } else {
            setCapsLockLight(false);
            setBacklight(0);
            setMuteLight(false);
        }
    }

    public void setKeyboardType(int type) {
        synchronized (this.mlock) {
            this.mKeyboardType = type;
        }
    }

    public void registerMuteLightReceiver() {
        IntentFilter muteFilter = new IntentFilter("android.media.action.MICROPHONE_MUTE_CHANGED");
        this.mContext.registerReceiver(this.mMicMuteReceiver, muteFilter);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public int getKeyboardType() {
        int i;
        synchronized (this.mlock) {
            i = this.mKeyboardType;
        }
        return i;
    }

    /* loaded from: classes.dex */
    class H extends Handler {
        public static final String DATA_FEATURE_TYPE = "feature";
        public static final String DATA_FEATURE_VALUE = "value";
        public static final String DATA_KEY_FIRST_CHECK = "is_first_check";
        public static final int MSG_CHECK_MCU_STATUS = 6;
        public static final int MSG_CHECK_STAY_WAKE_KEYBOARD = 7;
        public static final int MSG_GET_KEYBOARD_VERSION = 1;
        public static final int MSG_GET_MCU_VERSION = 10;
        public static final int MSG_READ_HALL_DATA = 8;
        public static final int MSG_READ_KB_STATUS = 3;
        public static final int MSG_RESTORE_MCU = 2;
        public static final int MSG_SEND_NFC_DATA = 12;
        public static final int MSG_SET_BACK_LIGHT = 11;
        public static final int MSG_SET_CAPS_LOCK_LIGHT = 14;
        public static final int MSG_SET_KB_STATUS = 4;
        public static final int MSG_SET_MUTE_LIGHT = 15;
        public static final int MSG_START_CHECK_AUTH = 5;
        public static final int MSG_UPGRADE_KEYBOARD = 0;
        public static final int MSG_UPGRADE_MCU = 9;
        public static final int MSG_UPGRADE_TOUCHPAD = 13;

        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (MiuiIICKeyboardManager.this.mShouldUpgradeKeyboard) {
                        MiuiIICKeyboardManager.this.mIICNodeHelper.upgradeKeyboardIfNeed();
                        return;
                    }
                    return;
                case 1:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendGetKeyboardVersionCommand();
                    return;
                case 2:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendRestoreMcuCommand();
                    return;
                case 3:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        MiuiIICKeyboardManager.this.mIICNodeHelper.readKeyboardStatus();
                        return;
                    }
                    return;
                case 4:
                case 14:
                case 15:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        boolean enable = msg.getData().getInt(DATA_FEATURE_VALUE, 0) == 1;
                        int feature = msg.getData().getInt(DATA_FEATURE_TYPE, -1);
                        MiuiIICKeyboardManager.this.mIICNodeHelper.setKeyboardFeature(enable, feature);
                        return;
                    }
                    return;
                case 5:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "start authentication");
                        Bundle bundle = msg.getData();
                        if (bundle != null) {
                            boolean isFirst = bundle.getBoolean(DATA_KEY_FIRST_CHECK, true);
                            MiuiIICKeyboardManager.this.doCheckKeyboardIdentity(isFirst);
                            return;
                        }
                        return;
                    }
                    return;
                case 6:
                    byte[] command = MiuiIICKeyboardManager.this.mCommunicationUtil.getLongRawCommand();
                    MiuiIICKeyboardManager.this.mCommunicationUtil.setCheckMCUStatusCommand(command, CommunicationUtil.COMMAND_CHECK_MCU_STATUS);
                    MiuiIICKeyboardManager.this.writeCommandToIIC(command);
                    return;
                case 7:
                    if (MiuiIICKeyboardManager.this.mShouldStayWakeKeyboard) {
                        MiuiIICKeyboardManager.this.wakeKeyboard176();
                        Message stayWakeMsg = MiuiIICKeyboardManager.this.mHandler.obtainMessage(7);
                        MiuiIICKeyboardManager.this.mHandler.sendMessageDelayed(stayWakeMsg, 10000L);
                        return;
                    }
                    return;
                case 8:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.checkHallStatus();
                    return;
                case 9:
                    if (MiuiIICKeyboardManager.this.mShouldUpgradeMCU && MiuiKeyboardUtil.isXM2022MCU()) {
                        MiuiIICKeyboardManager.this.mIICNodeHelper.startUpgradeMCUIfNeed();
                        return;
                    }
                    return;
                case 10:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendGetMCUVersionCommand();
                    return;
                case 11:
                    byte value = msg.getData().getByte(DATA_FEATURE_VALUE);
                    MiuiIICKeyboardManager.this.mIICNodeHelper.setKeyboardBacklight(value);
                    return;
                case 12:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendNFCData();
                    return;
                case 13:
                    if (MiuiIICKeyboardManager.this.mShouldUpgradeTouchPad) {
                        MiuiIICKeyboardManager.this.mIICNodeHelper.upgradeTouchPadIfNeed();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class KeyboardSettingsObserver extends ContentObserver {
        public boolean mShouldUnregisterLanguageReceiver;

        public KeyboardSettingsObserver(Handler handler) {
            super(handler);
            this.mShouldUnregisterLanguageReceiver = true;
        }

        public void observerObject() {
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MiuiIICKeyboardManager.AUTO_BACK_BRIGHTNESS), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.System.getUriFor(MiuiIICKeyboardManager.AUTO_BACK_BRIGHTNESS));
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.System.getUriFor(MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS));
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_TAP_TOUCH_PAD), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_TAP_TOUCH_PAD));
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_UPGRADE_NO_CHECK), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_UPGRADE_NO_CHECK));
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MiuiIICKeyboardManager.LAST_CONNECT_BLE_ADDRESS), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.System.getUriFor(MiuiIICKeyboardManager.LAST_CONNECT_BLE_ADDRESS));
            MiuiIICKeyboardManager.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, MiuiIICKeyboardManager.this.mKeyboardObserver);
            onChange(false, Settings.Secure.getUriFor("user_setup_complete"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(MiuiIICKeyboardManager.AUTO_BACK_BRIGHTNESS).equals(uri)) {
                MiuiIICKeyboardManager miuiIICKeyboardManager = MiuiIICKeyboardManager.this;
                miuiIICKeyboardManager.mIsAutoBackLight = Settings.System.getIntForUser(miuiIICKeyboardManager.mContext.getContentResolver(), MiuiIICKeyboardManager.AUTO_BACK_BRIGHTNESS, 0, -2) != 0;
                Slog.i(MiuiPadKeyboardManager.TAG, "update auto back light:" + MiuiIICKeyboardManager.this.mIsAutoBackLight);
                if (MiuiIICKeyboardManager.this.mIsAutoBackLight) {
                    MiuiIICKeyboardManager.this.mUserSetBackLight = false;
                }
                MiuiIICKeyboardManager.this.updateLightSensor();
                return;
            }
            if (Settings.System.getUriFor(MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS).equals(uri)) {
                MiuiIICKeyboardManager miuiIICKeyboardManager2 = MiuiIICKeyboardManager.this;
                miuiIICKeyboardManager2.mBackLightBrightness = Settings.System.getIntForUser(miuiIICKeyboardManager2.mContext.getContentResolver(), MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS, 0, -2);
                return;
            }
            if (Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_TAP_TOUCH_PAD).equals(uri)) {
                boolean enable = Settings.System.getIntForUser(MiuiIICKeyboardManager.this.mContext.getContentResolver(), MiuiIICKeyboardManager.ENABLE_TAP_TOUCH_PAD, InputFeature.getTouchpadTapDefaultValue(), -2) == 1;
                InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
                inputCommonConfig.setTapTouchPad(enable);
                inputCommonConfig.flushToNative();
                Slog.i(MiuiPadKeyboardManager.TAG, "update tap touchpad:" + enable);
                return;
            }
            if (Settings.System.getUriFor(MiuiIICKeyboardManager.ENABLE_UPGRADE_NO_CHECK).equals(uri)) {
                MiuiIICKeyboardManager.this.mIICNodeHelper.setNoCheckUpgrade(Settings.System.getIntForUser(MiuiIICKeyboardManager.this.mContext.getContentResolver(), MiuiIICKeyboardManager.ENABLE_UPGRADE_NO_CHECK, 0, -2) != 0);
                return;
            }
            if (Settings.System.getUriFor(MiuiIICKeyboardManager.LAST_CONNECT_BLE_ADDRESS).equals(uri)) {
                String address = Settings.System.getStringForUser(MiuiIICKeyboardManager.this.mContext.getContentResolver(), MiuiIICKeyboardManager.LAST_CONNECT_BLE_ADDRESS, -2);
                MiuiIICKeyboardManager.this.mBluetoothKeyboardManager.setCurrentBleKeyboardAddress(address);
                return;
            }
            if (Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                boolean isUserSetupComplete = Settings.Secure.getIntForUser(MiuiIICKeyboardManager.this.mContext.getContentResolver(), "user_setup_complete", 0, -2) == 1;
                if (isUserSetupComplete && this.mShouldUnregisterLanguageReceiver) {
                    this.mShouldUnregisterLanguageReceiver = false;
                    try {
                        MiuiIICKeyboardManager.this.mContext.unregisterReceiver(MiuiIICKeyboardManager.this.mLanguageChangeReceiver);
                        Slog.i(MiuiPadKeyboardManager.TAG, "User setup completed, unregister language listener");
                    } catch (IllegalArgumentException e) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "Unregister Exception!");
                    }
                }
            }
        }
    }
}
