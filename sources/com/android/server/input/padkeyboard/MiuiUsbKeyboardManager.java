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
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.input.InputManagerService;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.android.server.input.padkeyboard.usb.KeyboardUpgradeHelper;
import com.android.server.input.padkeyboard.usb.McuUpgradeHelper;
import com.android.server.input.padkeyboard.usb.UsbKeyboardDevicesObserver;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.pm.CloudControlPreinstallService;
import com.miui.server.greeze.AurogonImmobulusMode;
import com.miui.server.input.util.ShortCutActionsUtils;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import miui.hardware.input.MiuiKeyboardStatus;
import miui.util.FeatureParser;
import miuix.appcompat.app.AlertDialog;

/* loaded from: classes.dex */
public class MiuiUsbKeyboardManager implements UsbKeyboardDevicesObserver.KeyboardActionListener, MiuiPadKeyboardManager {
    private static final String KEYBOARD_AUTO_UPGRADE = "keyboard_auto_upgrade";
    private static final String KEYBOARD_BACK_LIGHT = "keyboard_back_light";
    private static final String KEY_COMMAND_TARGET = "target";
    private static final String KEY_COMMAND_VALUE = "value";
    private static final String KEY_FIRST_CHECK = "first_check";
    private static final int MAX_CHECK_IDENTITY_TIMES = 5;
    private static final int MAX_GET_USB_DEVICE_TIME_OUT = 20000;
    private static final int MAX_RETRY_TIMES = 2;
    private static final int MAX_UPGRADE_FAILED_TIMES = 5;
    private static volatile MiuiUsbKeyboardManager sInstance;
    private final Sensor mAccSensor;
    private final AngleStateController mAngleStateController;
    private String mBinKbHighVersion;
    private String mBinKbLowVersion;
    private String mBinKbVersion;
    private String mBinMcuVersion;
    private String mBinWirelessVersion;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private AlertDialog mDialog;
    private boolean mEnableAutoUpgrade;
    private final H mHandler;
    private final HandlerThread mHandlerThread;
    private UsbEndpoint mInUsbEndpoint;
    private int mInputDeviceId;
    private final InputManagerService mInputManager;
    private UsbKeyboardDevicesObserver mKeyboardDevicesObserver;
    private String mKeyboardVersion;
    private String mMcuVersion;
    private UsbEndpoint mOutUsbEndpoint;
    private Map<String, String> mRecentConnTime;
    private UsbEndpoint mReportInUsbEndpoint;
    private UsbInterface mReportInterface;
    private boolean mScreenState;
    private final SensorManager mSensorManager;
    private boolean mShouldEnableBackLight;
    private boolean mShouldWakeUp;
    private UsbActionReceiver mUsbActionReceiver;
    private UsbDeviceConnection mUsbConnection;
    private UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private final UsbManager mUsbManager;
    private String mWirelessVersion;
    private boolean mIsKeyboardReady = false;
    private final Object mUsbDeviceLock = new Object();
    private final byte[] mSendBuf = new byte[64];
    private final byte[] mRecBuf = new byte[64];
    private final float[] mLocalGData = new float[3];
    private int mMcuUpgradeFailedTimes = 0;
    private int mKeyboardUpgradeFailedTimes = 0;
    private int mPenState = -1;
    private int mKbTypeLevel = 2;
    private boolean mIsSetupComplete = false;
    private int mCheckIdentityTimes = 0;
    private boolean mIsFirstAction = true;

    public MiuiUsbKeyboardManager(Context context) {
        boolean z;
        ContentObserver contentObserver = new ContentObserver(new Handler()) { // from class: com.android.server.input.padkeyboard.MiuiUsbKeyboardManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.Secure.getUriFor(MiuiUsbKeyboardManager.KEYBOARD_BACK_LIGHT))) {
                    MiuiUsbKeyboardManager miuiUsbKeyboardManager = MiuiUsbKeyboardManager.this;
                    miuiUsbKeyboardManager.mShouldEnableBackLight = Settings.Secure.getInt(miuiUsbKeyboardManager.mContext.getContentResolver(), MiuiUsbKeyboardManager.KEYBOARD_BACK_LIGHT, 1) != 0;
                    Slog.i(MiuiPadKeyboardManager.TAG, "mBackLightObserver onChange:" + MiuiUsbKeyboardManager.this.mShouldEnableBackLight);
                    if (!MiuiUsbKeyboardManager.this.mIsKeyboardReady || MiuiUsbKeyboardManager.this.mAngleStateController.shouldIgnoreKeyboard()) {
                        return;
                    }
                    if (MiuiUsbKeyboardManager.this.mShouldEnableBackLight) {
                        MiuiUsbKeyboardManager.this.writePadKeyBoardStatus(35, 1);
                        return;
                    } else {
                        MiuiUsbKeyboardManager.this.writePadKeyBoardStatus(35, 0);
                        return;
                    }
                }
                if (uri.equals(Settings.Secure.getUriFor(MiuiUsbKeyboardManager.KEYBOARD_AUTO_UPGRADE))) {
                    MiuiUsbKeyboardManager miuiUsbKeyboardManager2 = MiuiUsbKeyboardManager.this;
                    miuiUsbKeyboardManager2.mEnableAutoUpgrade = Settings.Secure.getInt(miuiUsbKeyboardManager2.mContext.getContentResolver(), MiuiUsbKeyboardManager.KEYBOARD_AUTO_UPGRADE, 1) != 0;
                    Slog.i(MiuiPadKeyboardManager.TAG, "mEnableAutoUpgrade onChange:" + MiuiUsbKeyboardManager.this.mEnableAutoUpgrade);
                }
            }
        };
        this.mContentObserver = contentObserver;
        this.mRecentConnTime = new LinkedHashMap<String, String>() { // from class: com.android.server.input.padkeyboard.MiuiUsbKeyboardManager.2
            @Override // java.util.LinkedHashMap
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 3;
            }
        };
        Slog.i(MiuiPadKeyboardManager.TAG, "MiuiUsbKeyboardManager");
        this.mContext = context;
        registerBroadcastReceiver(context);
        this.mInputManager = ServiceManager.getService(DumpSysInfoUtil.INPUT);
        this.mUsbManager = (UsbManager) context.getSystemService("usb");
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mAccSensor = sensorManager.getDefaultSensor(1);
        HandlerThread handlerThread = new HandlerThread("pad_keyboard_transfer_thread");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(handlerThread.getLooper());
        UsbKeyboardDevicesObserver usbKeyboardDevicesObserver = new UsbKeyboardDevicesObserver(this);
        this.mKeyboardDevicesObserver = usbKeyboardDevicesObserver;
        usbKeyboardDevicesObserver.startWatching();
        this.mAngleStateController = new AngleStateController(context);
        if (Settings.Secure.getInt(context.getContentResolver(), KEYBOARD_BACK_LIGHT, 1) == 0) {
            z = false;
        } else {
            z = true;
        }
        this.mShouldEnableBackLight = z;
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEYBOARD_BACK_LIGHT), false, contentObserver);
        this.mEnableAutoUpgrade = Settings.Secure.getInt(context.getContentResolver(), KEYBOARD_AUTO_UPGRADE, 1) != 0;
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEYBOARD_AUTO_UPGRADE), false, contentObserver);
        Settings.Secure.putIntForUser(context.getContentResolver(), MiuiKeyboardUtil.KEYBOARD_TYPE_LEVEL, getKeyboardTypeValue(), -2);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setMiuiKeyboardInfo(12806, 16380);
        inputCommonConfig.flushToNative();
    }

    public static MiuiUsbKeyboardManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MiuiUsbKeyboardManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiUsbKeyboardManager(context);
                }
            }
        }
        return sInstance;
    }

    public static boolean supportPadKeyboard() {
        return FeatureParser.getBoolean("support_usb_keyboard", false);
    }

    private void registerBroadcastReceiver(Context context) {
        Slog.i(MiuiPadKeyboardManager.TAG, "registerBroadcastReceiver");
        this.mUsbActionReceiver = new UsbActionReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        context.registerReceiver(this.mUsbActionReceiver, intentFilter);
    }

    private void unRegisterBroadcastReceiver(Context context) {
        Slog.i(MiuiPadKeyboardManager.TAG, "unRegisterBroadcastReceiver");
        UsbActionReceiver usbActionReceiver = this.mUsbActionReceiver;
        if (usbActionReceiver != null) {
            context.unregisterReceiver(usbActionReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UsbActionReceiver extends BroadcastReceiver {
        UsbActionReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(CloudControlPreinstallService.ConnectEntity.DEVICE);
            switch (action.hashCode()) {
                case -2114103349:
                    if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1608292967:
                    if (action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    Slog.i(MiuiPadKeyboardManager.TAG, "USB device attached: " + device.getDeviceName());
                    if (device.getVendorId() == 12806 && device.getProductId() == 16380) {
                        MiuiUsbKeyboardManager.this.mHandler.removeCallbacksAndMessages(null);
                        synchronized (MiuiUsbKeyboardManager.this.mUsbDeviceLock) {
                            MiuiUsbKeyboardManager.this.mUsbDevice = device;
                        }
                        MiuiUsbKeyboardManager.this.onUsbDeviceAttach();
                        return;
                    }
                    return;
                case 1:
                    Slog.i(MiuiPadKeyboardManager.TAG, "USB device detached: " + device.getDeviceName());
                    if (device.getVendorId() == 12806 && device.getProductId() == 16380) {
                        MiuiUsbKeyboardManager.this.mHandler.removeCallbacksAndMessages(null);
                        MiuiUsbKeyboardManager.this.closeDevice();
                        synchronized (MiuiUsbKeyboardManager.this.mUsbDeviceLock) {
                            MiuiUsbKeyboardManager.this.mUsbDevice = null;
                        }
                        MiuiUsbKeyboardManager.this.onUsbDeviceDetach();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    @Override // com.android.server.input.padkeyboard.usb.UsbKeyboardDevicesObserver.KeyboardActionListener
    public void onKeyboardAction() {
        Slog.i(MiuiPadKeyboardManager.TAG, "onKeyboardAction");
        if (this.mIsFirstAction) {
            this.mShouldWakeUp = true;
            this.mIsFirstAction = false;
        }
        getKeyboardReportData();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void testFunction() {
        Slog.i(MiuiPadKeyboardManager.TAG, "---testFunction start-----");
        for (int i = 0; i < 1; i++) {
            readConnectState();
            readMcuVersion();
            writePadKeyBoardStatus(35, 1);
            readKeyboardStatus();
        }
    }

    public void readConnectState() {
        H h = this.mHandler;
        h.sendMessage(h.obtainMessage(1));
    }

    public void readMcuVersion() {
        H h = this.mHandler;
        h.sendMessage(h.obtainMessage(2));
    }

    public void getMcuResetMode() {
        H h = this.mHandler;
        h.sendMessage(h.obtainMessage(3));
    }

    public void readKeyboardVersion() {
        H h = this.mHandler;
        h.sendMessageAtFrontOfQueue(h.obtainMessage(4));
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void readKeyboardStatus() {
        H h = this.mHandler;
        h.sendMessage(h.obtainMessage(5));
    }

    public void writePadKeyBoardStatus(int target, int value) {
        Message msg = this.mHandler.obtainMessage(6);
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_COMMAND_TARGET, target);
        bundle.putInt("value", value);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    public void sendKeyboardCapsLock() {
        writePadKeyBoardStatus(38, 1);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void getKeyboardReportData() {
        if (!this.mHandler.hasMessages(7)) {
            H h = this.mHandler;
            h.sendMessage(h.obtainMessage(7));
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public InputDevice[] removeKeyboardDevicesIfNeeded(InputDevice[] allInputDevices) {
        ArrayList<InputDevice> newInputDevices = new ArrayList<>();
        for (InputDevice inputDevice : allInputDevices) {
            if (inputDevice.getProductId() == 16380 && inputDevice.getVendorId() == 12806) {
                if (this.mInputDeviceId != inputDevice.getId()) {
                    this.mInputDeviceId = inputDevice.getId();
                    Slog.i(MiuiPadKeyboardManager.TAG, "update keyboard device id: " + this.mInputDeviceId);
                }
                if (this.mIsKeyboardReady) {
                    return allInputDevices;
                }
                Slog.i(MiuiPadKeyboardManager.TAG, "filter keyboard device :" + inputDevice.getName());
            } else {
                newInputDevices.add(inputDevice);
            }
        }
        return (InputDevice[]) newInputDevices.toArray(new InputDevice[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUsbDeviceAttach() {
        Slog.i(MiuiPadKeyboardManager.TAG, "mcu usb device attach");
        this.mHandler.removeMessages(12);
        enableOrDisableInputDevice();
        getKeyboardReportData();
        readMcuVersion();
        if (this.mEnableAutoUpgrade && isUserSetUp()) {
            startMcuUpgrade();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUsbDeviceDetach() {
        Slog.i(MiuiPadKeyboardManager.TAG, "mcu usb device detach");
        if (this.mIsKeyboardReady) {
            notifyKeyboardStateChanged(false);
        }
        this.mIsFirstAction = true;
    }

    public void startMcuUpgrade() {
        if (!this.mHandler.hasMessages(8)) {
            H h = this.mHandler;
            h.sendMessage(h.obtainMessage(8));
        }
    }

    public void startKeyboardUpgrade() {
        if (!this.mHandler.hasMessages(9)) {
            H h = this.mHandler;
            h.sendMessage(h.obtainMessage(9));
        }
    }

    public void startWirelessUpgrade() {
        if (!this.mHandler.hasMessages(10)) {
            H h = this.mHandler;
            h.sendMessage(h.obtainMessage(10));
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyLidSwitchChanged(boolean lidOpen) {
        this.mAngleStateController.notifyLidSwitchChanged(lidOpen);
        enableOrDisableInputDevice();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        this.mAngleStateController.notifyTabletSwitchChanged(tabletOpen);
        enableOrDisableInputDevice();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public boolean isKeyboardReady() {
        return this.mIsKeyboardReady && !this.mAngleStateController.shouldIgnoreKeyboard();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public MiuiKeyboardStatus getKeyboardStatus() {
        return new MiuiKeyboardStatus(this.mIsKeyboardReady, this.mAngleStateController.getIdentityStatus(), !this.mAngleStateController.isWorkState(), this.mAngleStateController.getLidStatus(), this.mAngleStateController.getTabletStatus(), this.mAngleStateController.shouldIgnoreKeyboardForIIC(), this.mMcuVersion, this.mKeyboardVersion);
    }

    private void checkKeyboardIdentity(int delay, boolean isFirst) {
        if (isFirst) {
            this.mCheckIdentityTimes = 0;
        }
        this.mHandler.removeMessages(11);
        Message msg = this.mHandler.obtainMessage(11);
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_FIRST_CHECK, isFirst);
        msg.setData(bundle);
        this.mHandler.sendMessageDelayed(msg, delay);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void notifyScreenState(boolean screenState) {
        this.mScreenState = screenState;
        if (!screenState) {
            this.mShouldWakeUp = false;
            if (this.mIsFirstAction) {
                this.mIsFirstAction = false;
            }
            AlertDialog alertDialog = this.mDialog;
            if (alertDialog != null && alertDialog.isShowing()) {
                this.mDialog.dismiss();
            }
        }
    }

    public static int shouldClearActivityInfoFlags() {
        return 48;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final int MSG_CHECK_KEYBOARD_IDENTITY = 11;
        private static final int MSG_GET_DEVICE_TIME_OUT = 12;
        private static final int MSG_GET_KEYBOARD_REPORT_DATA = 7;
        private static final int MSG_GET_MCU_RESET_MODE = 3;
        private static final int MSG_READ_CONNECT_STATE = 1;
        private static final int MSG_READ_KEYBOARD_STATUS = 5;
        private static final int MSG_READ_KEYBOARD_VERSION = 4;
        private static final int MSG_READ_MCU_VERSION = 2;
        private static final int MSG_START_KEYBOARD_UPGRADE = 9;
        private static final int MSG_START_MCU_UPGRADE = 8;
        private static final int MSG_START_WIRELESS_UPGRADE = 10;
        private static final int MSG_TEST_FUNCTION = 0;
        private static final int MSG_WRITE_KEYBOARD_STATUS = 6;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    MiuiUsbKeyboardManager.this.testFunction();
                    break;
                case 1:
                    MiuiUsbKeyboardManager.this.doReadConnectState();
                    break;
                case 2:
                    MiuiUsbKeyboardManager.this.doReadMcuVersion();
                    break;
                case 3:
                    MiuiUsbKeyboardManager.this.doGetMcuReset();
                    break;
                case 4:
                    MiuiUsbKeyboardManager.this.doReadKeyboardVersion();
                    break;
                case 5:
                    MiuiUsbKeyboardManager.this.doReadKeyboardStatus();
                    break;
                case 6:
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        int target = bundle.getInt(MiuiUsbKeyboardManager.KEY_COMMAND_TARGET, 0);
                        int value = bundle.getInt("value", 0);
                        MiuiUsbKeyboardManager.this.doWritePadKeyBoardStatus(target, value);
                        break;
                    }
                    break;
                case 7:
                    MiuiUsbKeyboardManager.this.doGetReportData();
                    break;
                case 8:
                    MiuiUsbKeyboardManager.this.doStartMcuUpgrade();
                    break;
                case 9:
                    MiuiUsbKeyboardManager.this.doStartKeyboardUpgrade();
                    break;
                case 10:
                    MiuiUsbKeyboardManager.this.doStartWirelessUpgrade();
                    break;
                case 11:
                    Bundle bundle2 = msg.getData();
                    if (bundle2 != null) {
                        boolean isFirst = bundle2.getBoolean(MiuiUsbKeyboardManager.KEY_FIRST_CHECK, true);
                        MiuiUsbKeyboardManager.this.doCheckKeyboardIdentity(isFirst);
                        break;
                    }
                    break;
                case 12:
                    Slog.i(MiuiPadKeyboardManager.TAG, "reset keyboard usb host cause get usb device time out");
                    MiuiUsbKeyboardManager.this.mKeyboardDevicesObserver.resetKeyboardHost();
                    break;
            }
            if (!hasMessagesOrCallbacks()) {
                MiuiUsbKeyboardManager.this.closeDevice();
            }
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] sendCommandForRespond(byte[] command, MiuiPadKeyboardManager.CommandCallback callback) {
        if (Thread.currentThread() != this.mHandlerThread) {
            Slog.i(MiuiPadKeyboardManager.TAG, "sendCommandForRespond should be called in mHandlerThread");
            return new byte[0];
        }
        if (!getDeviceReadyForTransfer()) {
            Slog.i(MiuiPadKeyboardManager.TAG, "getDeviceReadyForTransfer fail");
            return new byte[0];
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        int failCount = 0;
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (callback == null || callback.isCorrectPackage(this.mRecBuf)) {
                        return this.mRecBuf;
                    }
                }
            } else {
                StringBuilder append = new StringBuilder().append("Try Send command failed:");
                byte[] bArr = this.mSendBuf;
                Slog.v(MiuiPadKeyboardManager.TAG, append.append(MiuiKeyboardUtil.Bytes2Hex(bArr, bArr.length)).toString());
            }
            failCount++;
        }
        if (failCount > 0) {
            cleanUsbCash(failCount);
        }
        if (failCount == 2) {
            return new byte[0];
        }
        return (byte[]) this.mRecBuf.clone();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiDevAuthInit() {
        return MiuiKeyboardUtil.commandMiDevAuthInitForUSB();
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiAuthStep3Type1(byte[] keyMeta, byte[] challenge) {
        return MiuiKeyboardUtil.commandMiAuthStep3Type1ForUSB(keyMeta, challenge);
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiAuthStep5Type1(byte[] token) {
        return new byte[0];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doReadConnectState() {
        if (!getDeviceReadyForTransfer()) {
            return;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetConnectState();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseConnectState(this.mRecBuf)) {
                        return;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send connect failed");
            }
        }
    }

    private boolean parseConnectState(byte[] recBuf) {
        if (recBuf[4] != -94) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive connect state error:" + String.format("%02x", Byte.valueOf(recBuf[4])));
            return false;
        }
        byte[] voltage = new byte[2];
        System.arraycopy(recBuf, 10, voltage, 0, voltage.length);
        byte[] clockOff = new byte[2];
        System.arraycopy(recBuf, 12, clockOff, 0, clockOff.length);
        byte[] trueOff = new byte[4];
        System.arraycopy(recBuf, 14, trueOff, 0, trueOff.length);
        Slog.i(MiuiPadKeyboardManager.TAG, "receive connect state:" + String.format("%02x", Byte.valueOf(recBuf[9])) + "  Voltage:" + MiuiKeyboardUtil.Bytes2RevertHexString(voltage) + "  E_UART:" + MiuiKeyboardUtil.Bytes2RevertHexString(clockOff) + "  E_PPM:" + MiuiKeyboardUtil.Bytes2RevertHexString(trueOff));
        if (recBuf[18] == 1) {
            Slog.i(MiuiPadKeyboardManager.TAG, "keyboard is over charged");
            return false;
        }
        if ((recBuf[9] & 3) == 1) {
            Slog.i(MiuiPadKeyboardManager.TAG, "TRX check failed");
            return false;
        }
        if ((recBuf[9] & 99) == 67) {
            Slog.i(MiuiPadKeyboardManager.TAG, "pin connect failed");
            Context context = this.mContext;
            Toast.makeText(context, context.getResources().getString(286196297), 0).show();
            return false;
        }
        if ((recBuf[9] & 3) == 0) {
            notifyKeyboardStateChanged(false);
            return true;
        }
        if ((recBuf[9] & 99) == 35) {
            notifyKeyboardStateChanged(true);
            return true;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "unhandled connect state:" + String.format("%02x", Byte.valueOf(recBuf[9])));
        return false;
    }

    private void notifyKeyboardStateChanged(boolean newState) {
        boolean oldState = this.mIsKeyboardReady;
        if (oldState == newState) {
            return;
        }
        this.mIsKeyboardReady = newState;
        Slog.i(MiuiPadKeyboardManager.TAG, "keyboardStatus changed :" + this.mIsKeyboardReady);
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
        if (!oldState && newState) {
            onKeyboardAttach();
        }
        if (oldState && !newState) {
            onKeyboardDetach();
        }
        if (this.mIsKeyboardReady) {
            this.mSensorManager.registerListener(this, this.mAccSensor, 3);
        } else {
            this.mSensorManager.unregisterListener(this);
        }
    }

    private void onKeyboardAttach() {
        Slog.i(MiuiPadKeyboardManager.TAG, "onKeyboardAttach");
        this.mRecentConnTime.put(DateFormat.getDateTimeInstance().format(new Date()), "");
        readKeyboardVersion();
        if (!this.mAngleStateController.shouldIgnoreKeyboard()) {
            int value = this.mShouldEnableBackLight ? 1 : 0;
            writePadKeyBoardStatus(35, value);
        }
        if (this.mEnableAutoUpgrade && isUserSetUp()) {
            startKeyboardUpgrade();
            startWirelessUpgrade();
            checkKeyboardIdentity(0, true);
        }
    }

    private void onKeyboardDetach() {
        Slog.i(MiuiPadKeyboardManager.TAG, "onKeyboardDetach");
        Map.Entry<String, String>[] entries = new Map.Entry[this.mRecentConnTime.size()];
        this.mRecentConnTime.entrySet().toArray(entries);
        if (entries.length > 0) {
            entries[entries.length - 1].setValue(DateFormat.getDateTimeInstance().format(new Date()));
        }
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), MiuiKeyboardUtil.KEYBOARD_TYPE_LEVEL, 0, -2);
        this.mHandler.removeCallbacksAndMessages(null);
        this.mAngleStateController.setIdentityState(true);
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null && alertDialog.isShowing()) {
            this.mDialog.dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doReadMcuVersion() {
        if (!getDeviceReadyForTransfer()) {
            return;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetVersionInfo();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 4; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUsbDeviceVersion(this.mRecBuf)) {
                        return;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send version failed");
                MiuiKeyboardUtil.operationWait(200);
            }
        }
    }

    private boolean parseUsbDeviceVersion(byte[] recBuf) {
        if (recBuf[6] != 2) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive version state error:" + String.format("%02x", Byte.valueOf(recBuf[6])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 30, recBuf[30])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive version checksum error:" + String.format("%02x", Byte.valueOf(recBuf[30])));
            return false;
        }
        byte[] deviceVersion = new byte[16];
        System.arraycopy(recBuf, 7, deviceVersion, 0, 16);
        this.mMcuVersion = MiuiKeyboardUtil.Bytes2String(deviceVersion);
        Slog.i(MiuiPadKeyboardManager.TAG, "receive mcu version:" + this.mMcuVersion);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doGetMcuReset() {
        if (!getDeviceReadyForTransfer()) {
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "get mcu reset");
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetResetInfo();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUsbReset(this.mRecBuf)) {
                        return;
                    }
                }
                if (this.mUsbDevice == null) {
                    return;
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send reset failed");
                MiuiKeyboardUtil.operationWait(200);
            }
        }
    }

    private boolean parseUsbReset(byte[] recBuf) {
        if (recBuf[0] == 36 && recBuf[1] == 49 && recBuf[6] == 0) {
            return false;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset success");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doReadKeyboardVersion() {
        if (!getDeviceReadyForTransfer()) {
            return;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetKeyboardVersion();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseKeyboardVersion(this.mRecBuf)) {
                        return;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send version failed");
            }
        }
    }

    private boolean parseKeyboardVersion(byte[] recBuf) {
        if (recBuf[4] != 1) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard version error:" + String.format("%02x", Byte.valueOf(recBuf[4])));
            return false;
        }
        byte[] kbVersion = new byte[2];
        System.arraycopy(recBuf, 6, kbVersion, 0, kbVersion.length);
        this.mKeyboardVersion = MiuiKeyboardUtil.Bytes2RevertHexString(kbVersion);
        Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard version:" + this.mKeyboardVersion);
        int parseKeyboardType = parseKeyboardType();
        this.mKbTypeLevel = parseKeyboardType;
        this.mAngleStateController.setKbLevel(parseKeyboardType, this.mShouldWakeUp && !this.mScreenState);
        enableOrDisableInputDevice();
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), MiuiKeyboardUtil.KEYBOARD_TYPE_LEVEL, this.mKbTypeLevel == 2 ? 0 : 1, -2);
        byte[] wlVersion = new byte[2];
        System.arraycopy(recBuf, 8, wlVersion, 0, wlVersion.length);
        this.mWirelessVersion = MiuiKeyboardUtil.Bytes2RevertHexString(wlVersion);
        Slog.i(MiuiPadKeyboardManager.TAG, "receive wireless version:" + this.mWirelessVersion);
        return true;
    }

    private int parseKeyboardType() {
        String str = this.mKeyboardVersion;
        if (str == null) {
            return 2;
        }
        String type = str.substring(0, 1);
        if ("1".equals(type)) {
            return 1;
        }
        return ("2".equals(type) || "4".equals(type) || !"3".equals(type)) ? 2 : 3;
    }

    private int getKeyboardTypeValue() {
        return this.mKbTypeLevel == 2 ? 0 : 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doReadKeyboardStatus() {
        if (!getDeviceReadyForTransfer() || !this.mIsKeyboardReady) {
            return;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetKeyboardStatus();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseReadKeyboardStatus(this.mRecBuf)) {
                        return;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send read keyboard failed");
                MiuiKeyboardUtil.operationWait(200);
            }
        }
    }

    private boolean parseReadKeyboardStatus(byte[] recBuf) {
        if (recBuf[7] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive mcu state error:" + String.format("%02x", Byte.valueOf(recBuf[7])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 8, recBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive mcu checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        return receiveKeyboardStatus();
    }

    private boolean receiveKeyboardStatus() {
        if (!getDeviceReadyForTransfer() || !this.mIsKeyboardReady) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            Arrays.fill(this.mRecBuf, (byte) 0);
            if (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                if (parseReceiveKeyboardStatus(this.mRecBuf)) {
                    return true;
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard status failed");
            }
            MiuiKeyboardUtil.operationWait(100);
        }
        return false;
    }

    private boolean parseReceiveKeyboardStatus(byte[] recBuf) {
        if (recBuf[4] != 82) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard state error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 19, recBuf[19])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        byte[] gsensorData = new byte[6];
        System.arraycopy(recBuf, 10, gsensorData, 0, 6);
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive TouchPadEnable:" + ((int) recBuf[6]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive KeyBoardEnable:" + ((int) recBuf[7]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive BackLightEnable:" + ((int) recBuf[8]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive TouchPadSensitivity:" + ((int) recBuf[9]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive GsensorData:" + MiuiKeyboardUtil.Bytes2Hex(gsensorData, gsensorData.length));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive PenState:" + ((int) recBuf[16]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive PenBatteryState:" + ((int) recBuf[17]));
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive PowerState:" + ((int) recBuf[18]));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doWritePadKeyBoardStatus(int target, int value) {
        if (!getDeviceReadyForTransfer() || !this.mIsKeyboardReady) {
            return;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandWriteKeyboardStatus(target, value);
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseWriteKeyBoardStatus(this.mRecBuf, target, value)) {
                        return;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send write mcu failed");
                MiuiKeyboardUtil.operationWait(200);
            }
        }
    }

    private boolean parseWriteKeyBoardStatus(byte[] recBuf, int target, int value) {
        if (recBuf[7] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive mcu state error:" + String.format("%02x", Byte.valueOf(recBuf[7])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 8, recBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive mcu checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        return readWriteCmdAck(target, value);
    }

    private boolean readWriteCmdAck(int target, int value) {
        if (!getDeviceReadyForTransfer() || !this.mIsKeyboardReady) {
            return false;
        }
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = UsbKeyboardUtil.commandGetKeyboardStatus(target);
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 2; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseWriteCmdAck(this.mRecBuf, target, value)) {
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send write cmd ack failed");
            }
        }
        return false;
    }

    private boolean parseWriteCmdAck(byte[] recBuf, int target, int value) {
        if (recBuf[7] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive write cmd ack error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 8, recBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive write cmd ack checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        return receiveWriteCmdAck(target, value);
    }

    private boolean receiveWriteCmdAck(int target, int value) {
        for (int i = 0; i < 2; i++) {
            Arrays.fill(this.mRecBuf, (byte) 0);
            if (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                if (parseReceiveWriteCmdAck(this.mRecBuf, target, value)) {
                    return true;
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard write result failed");
                MiuiKeyboardUtil.operationWait(100);
            }
        }
        return false;
    }

    private boolean parseReceiveWriteCmdAck(byte[] recBuf, int target, int value) {
        if (recBuf[4] != 48) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard write result status error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 8, recBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive keyboard write result checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return false;
        }
        if (target == recBuf[6] && value == recBuf[7]) {
            Slog.i(MiuiPadKeyboardManager.TAG, "write cmd success, command:" + target + " value:" + value);
            return true;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "write cmd failed");
        return true;
    }

    private boolean sendUsbData(UsbDeviceConnection connection, UsbEndpoint endpoint, byte[] data) {
        return (connection == null || endpoint == null || data == null || connection.bulkTransfer(endpoint, data, data.length, 500) == -1) ? false : true;
    }

    private boolean getDeviceReadyForTransfer() {
        UsbDeviceConnection usbDeviceConnection;
        UsbInterface usbInterface;
        synchronized (this.mUsbDeviceLock) {
            UsbDevice usbDevice = this.mUsbDevice;
            if (usbDevice != null && (usbDeviceConnection = this.mUsbConnection) != null && (usbInterface = this.mUsbInterface) != null && this.mOutUsbEndpoint != null && this.mInUsbEndpoint != null) {
                usbDeviceConnection.claimInterface(usbInterface, true);
                return true;
            }
            if (usbDevice == null && !getUsbDevice()) {
                return false;
            }
            if (!getTransferEndpoint(this.mUsbDevice)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "get transfer endpoint failed");
                return false;
            }
            if (this.mUsbConnection == null) {
                this.mUsbConnection = this.mUsbManager.openDevice(this.mUsbDevice);
            }
            UsbDeviceConnection usbDeviceConnection2 = this.mUsbConnection;
            if (usbDeviceConnection2 != null && this.mOutUsbEndpoint != null && this.mInUsbEndpoint != null) {
                usbDeviceConnection2.claimInterface(this.mUsbInterface, true);
                return true;
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "get usb transfer connection failed");
            return false;
        }
    }

    private boolean getUsbDevice() {
        synchronized (this.mUsbDeviceLock) {
            HashMap<String, UsbDevice> deviceList = this.mUsbManager.getDeviceList();
            for (UsbDevice device : deviceList.values()) {
                if (this.mUsbManager.hasPermission(device) && device.getVendorId() == 12806 && device.getProductId() == 16380) {
                    Slog.i(MiuiPadKeyboardManager.TAG, "getUsbDevice: " + device.getDeviceName());
                    this.mUsbDevice = device;
                }
            }
        }
        if (this.mUsbDevice == null) {
            Slog.i(MiuiPadKeyboardManager.TAG, "get usb device failed");
            if (!this.mHandler.hasMessages(12)) {
                this.mHandler.sendEmptyMessageDelayed(12, ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION);
            }
        } else {
            this.mHandler.removeMessages(12);
        }
        return this.mUsbDevice != null;
    }

    private boolean getTransferEndpoint(UsbDevice device) {
        if (device == null) {
            return false;
        }
        UsbConfiguration configuration = device.getConfiguration(0);
        for (int interfaceNum = 0; interfaceNum < configuration.getInterfaceCount(); interfaceNum++) {
            UsbInterface anInterface = configuration.getInterface(interfaceNum);
            if (anInterface != null && anInterface.getEndpointCount() >= 2) {
                for (int endPointNum = 0; endPointNum < anInterface.getEndpointCount(); endPointNum++) {
                    UsbEndpoint endpoint = anInterface.getEndpoint(endPointNum);
                    if (endpoint != null) {
                        int direction = endpoint.getDirection();
                        if (direction == 128) {
                            this.mInUsbEndpoint = endpoint;
                        } else if (direction == 0) {
                            this.mOutUsbEndpoint = endpoint;
                        }
                    }
                }
                if (this.mInUsbEndpoint != null && this.mOutUsbEndpoint != null) {
                    this.mUsbInterface = anInterface;
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doGetReportData() {
        if (!getDeviceReadyForReport()) {
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "get report data");
        Arrays.fill(this.mRecBuf, (byte) 0);
        long startTime = System.currentTimeMillis();
        while (true) {
            boolean hasReport = sendUsbData(this.mUsbConnection, this.mReportInUsbEndpoint, this.mRecBuf);
            if (hasReport || System.currentTimeMillis() - startTime < 20) {
                if (hasReport) {
                    parseReportData(this.mRecBuf);
                }
            } else {
                return;
            }
        }
    }

    private void parseReportData(byte[] recBuf) {
        if (recBuf[0] == 38 && recBuf[2] == 56) {
            switch (recBuf[4]) {
                case -94:
                    parseConnectState(recBuf);
                    return;
                case AurogonImmobulusMode.MSG_LAUNCH_MODE_TRIGGER_ACTION /* 105 */:
                    switch (recBuf[6]) {
                        case -96:
                            parseGsensorData(recBuf);
                            return;
                        case -95:
                            parseHallState(recBuf);
                            return;
                        case -94:
                            parsePenState(recBuf);
                            return;
                        default:
                            return;
                    }
                default:
                    return;
            }
        }
    }

    private void parseGsensorData(byte[] recBuf) {
        float x;
        float y;
        float z;
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 14, recBuf[14])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive gsensor data checksum error:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
            return;
        }
        byte[] temp = new byte[2];
        System.arraycopy(recBuf, 8, temp, 0, temp.length);
        if ((temp[1] & CommunicationUtil.PAD_ADDRESS) != 0) {
            x = new BigInteger("FFFF" + MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16).intValue();
        } else {
            x = Integer.parseInt(MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16);
        }
        System.arraycopy(recBuf, 10, temp, 0, temp.length);
        if ((temp[1] & CommunicationUtil.PAD_ADDRESS) != 0) {
            y = new BigInteger("FFFF" + MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16).intValue();
        } else {
            y = Integer.parseInt(MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16);
        }
        System.arraycopy(recBuf, 12, temp, 0, temp.length);
        if ((temp[1] & CommunicationUtil.PAD_ADDRESS) != 0) {
            z = new BigInteger("FFFF" + MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16).intValue();
        } else {
            z = Integer.parseInt(MiuiKeyboardUtil.Bytes2RevertHexString(temp), 16);
        }
        float x2 = (x * 9.8f) / 2048.0f;
        float x3 = y * 9.8f;
        float f = x3 / 2048.0f;
        float z2 = (9.8f * z) / 2048.0f;
        float[] fArr = this.mLocalGData;
        float xLocal = fArr[0];
        float f2 = fArr[1];
        float zLocal = fArr[2];
        float resultAngle = MiuiKeyboardUtil.calculatePKAngle(x2, xLocal, z2, zLocal);
        Slog.i(MiuiPadKeyboardManager.TAG, "result angle = " + resultAngle);
        this.mAngleStateController.updateAngleState(resultAngle);
        enableOrDisableInputDevice();
    }

    private void parseHallState(byte[] recBuf) {
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive Hall State:" + String.format("%02x", Byte.valueOf(recBuf[8])));
    }

    private void parsePenState(byte[] recBuf) {
        Slog.i(MiuiPadKeyboardManager.TAG, "Receive Pen State:" + String.format("%02x", Byte.valueOf(recBuf[8])) + " PenBatteryState:" + String.format("%02x", Byte.valueOf(recBuf[9])));
        if (this.mPenState == recBuf[8]) {
            return;
        }
        this.mPenState = recBuf[8];
        if (recBuf[8] == 2) {
            sendPenBatteryState(4, recBuf[9]);
        } else if (recBuf[8] == 0) {
            sendPenBatteryState(2, recBuf[9]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doStartMcuUpgrade() {
        String str;
        String str2 = this.mMcuVersion;
        if (str2 != null && (str = this.mBinMcuVersion) != null && str2.compareTo(str) >= 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "no need to start mcu upgrade");
            return;
        }
        if (this.mMcuUpgradeFailedTimes > 5) {
            Slog.i(MiuiPadKeyboardManager.TAG, "upgrade mcu failed too many times");
            return;
        }
        String str3 = this.mMcuVersion;
        if (str3 == null || "0000000000000000".equals(str3)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "unknown mcu version");
            return;
        }
        McuUpgradeHelper mcuUpgradeHelper = new McuUpgradeHelper(this.mContext);
        String version = mcuUpgradeHelper.getVersion();
        this.mBinMcuVersion = version;
        if (!version.startsWith(McuUpgradeHelper.VERSION_HEAD)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "give up upgrade : invalid version head");
            return;
        }
        getDeviceReadyForTransfer();
        String str4 = this.mMcuVersion;
        if (str4 != null && mcuUpgradeHelper.isLowerVersionThan(str4)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "give up upgrade : upper version");
            return;
        }
        if (mcuUpgradeHelper.startUpgrade(this.mUsbDevice, this.mUsbConnection, this.mOutUsbEndpoint, this.mInUsbEndpoint)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "upgrade mcu success");
            this.mMcuUpgradeFailedTimes = 0;
        } else {
            Slog.i(MiuiPadKeyboardManager.TAG, "upgrade mcu failed");
            this.mMcuUpgradeFailedTimes++;
            this.mBinMcuVersion = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x004e, code lost:
    
        if (r8.mKeyboardVersion.compareTo(r8.mBinKbLowVersion) >= 0) goto L27;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void doStartKeyboardUpgrade() {
        /*
            Method dump skipped, instructions count: 284
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.input.padkeyboard.MiuiUsbKeyboardManager.doStartKeyboardUpgrade():void");
    }

    private KeyboardUpgradeHelper getKeyboardUpgradeHelper() {
        switch (this.mKbTypeLevel) {
            case 1:
                KeyboardUpgradeHelper helper = new KeyboardUpgradeHelper(this.mContext, KeyboardUpgradeHelper.KB_BIN_PATH);
                this.mBinKbVersion = helper.getVersion();
                return helper;
            case 2:
                StringBuilder append = new StringBuilder().append(this.mKeyboardVersion.substring(0, 1));
                String str = this.mKeyboardVersion;
                String lowKbType = append.append(str.substring(str.length() - 1)).toString();
                if (lowKbType.substring(1, 2).compareTo(KeyboardUpgradeHelper.FIRST_LOW_KB_TYPE) < 0) {
                    lowKbType = this.mKeyboardVersion.substring(0, 1) + "0";
                }
                String lowBinPath = KeyboardUpgradeHelper.KB_L_BIN_PATH_MAP.get(lowKbType);
                if (lowBinPath == null) {
                    Slog.i(MiuiPadKeyboardManager.TAG, "unhandled low keyboard type:" + lowKbType + ", stop upgrade");
                    return null;
                }
                KeyboardUpgradeHelper helper2 = new KeyboardUpgradeHelper(this.mContext, lowBinPath);
                this.mBinKbLowVersion = helper2.getVersion();
                return helper2;
            case 3:
                KeyboardUpgradeHelper helper3 = new KeyboardUpgradeHelper(this.mContext, KeyboardUpgradeHelper.KB_H_BIN_PATH);
                this.mBinKbHighVersion = helper3.getVersion();
                return helper3;
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doStartWirelessUpgrade() {
        String str = this.mWirelessVersion;
        if (str == null || "0000".equals(str)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "unknown wireless version");
            return;
        }
        String str2 = this.mBinWirelessVersion;
        if (str2 != null && this.mWirelessVersion.compareTo(str2) >= 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "no need to start wireless upgrade");
            return;
        }
        if (this.mKeyboardUpgradeFailedTimes > 5) {
            Slog.i(MiuiPadKeyboardManager.TAG, "upgrade keyboard failed too many times");
            return;
        }
        KeyboardUpgradeHelper wirelessUpgradeHelper = new KeyboardUpgradeHelper(this.mContext, KeyboardUpgradeHelper.WL_BIN_PATH);
        this.mBinWirelessVersion = wirelessUpgradeHelper.getVersion();
        getDeviceReadyForTransfer();
        String str3 = this.mWirelessVersion;
        if (str3 != null && wirelessUpgradeHelper.isLowerVersionThan(str3)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "give up wireless upgrade : upper version");
            return;
        }
        Context context = this.mContext;
        Toast.makeText(context, context.getResources().getString(286196302), 0).show();
        if (wirelessUpgradeHelper.startUpgrade(this.mUsbDevice, this.mUsbConnection, this.mOutUsbEndpoint, this.mInUsbEndpoint)) {
            Slog.i(MiuiPadKeyboardManager.TAG, "wireless upgrade success");
            Context context2 = this.mContext;
            Toast.makeText(context2, context2.getResources().getString(286196303), 0).show();
            this.mKeyboardUpgradeFailedTimes = 0;
            doGetMcuReset();
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "wireless upgrade failed");
        Context context3 = this.mContext;
        Toast.makeText(context3, context3.getResources().getString(286196301), 0).show();
        this.mKeyboardUpgradeFailedTimes++;
        this.mBinWirelessVersion = null;
    }

    private boolean getDeviceReadyForReport() {
        UsbInterface usbInterface;
        UsbDeviceConnection usbDeviceConnection;
        UsbInterface usbInterface2;
        synchronized (this.mUsbDeviceLock) {
            UsbDevice usbDevice = this.mUsbDevice;
            if (usbDevice != null && (usbDeviceConnection = this.mUsbConnection) != null && (usbInterface2 = this.mReportInterface) != null && this.mReportInUsbEndpoint != null) {
                usbDeviceConnection.claimInterface(usbInterface2, true);
                return true;
            }
            if (usbDevice == null && !getUsbDevice()) {
                return false;
            }
            if (!getReportEndpoint(this.mUsbDevice)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "get usb report endpoint fail");
                return false;
            }
            if (this.mUsbConnection == null) {
                this.mUsbConnection = this.mUsbManager.openDevice(this.mUsbDevice);
            }
            UsbDeviceConnection usbDeviceConnection2 = this.mUsbConnection;
            if (usbDeviceConnection2 != null && (usbInterface = this.mReportInterface) != null && this.mReportInUsbEndpoint != null) {
                usbDeviceConnection2.claimInterface(usbInterface, true);
                return true;
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "get usb report connection fail");
            return false;
        }
    }

    private boolean getReportEndpoint(UsbDevice device) {
        if (device == null) {
            return false;
        }
        UsbConfiguration configuration = device.getConfiguration(0);
        for (int i = 0; i < configuration.getInterfaceCount(); i++) {
            UsbInterface anInterface = configuration.getInterface(i);
            if (anInterface.getId() == 3 && anInterface.getEndpointCount() == 1) {
                UsbEndpoint endpoint = anInterface.getEndpoint(0);
                this.mReportInUsbEndpoint = endpoint;
                if (endpoint != null) {
                    this.mReportInterface = anInterface;
                    return true;
                }
            }
        }
        return false;
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == 1) {
            float[] fArr = event.values;
            float[] fArr2 = this.mLocalGData;
            System.arraycopy(fArr, 0, fArr2, 0, fArr2.length);
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void cleanUsbCash(int times) {
        if (!getDeviceReadyForTransfer()) {
            return;
        }
        for (int i = 0; i < times; i++) {
            sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeDevice() {
        synchronized (this.mUsbDeviceLock) {
            UsbDeviceConnection usbDeviceConnection = this.mUsbConnection;
            if (usbDeviceConnection == null) {
                return;
            }
            UsbInterface usbInterface = this.mUsbInterface;
            if (usbInterface != null) {
                usbDeviceConnection.releaseInterface(usbInterface);
            } else {
                UsbInterface usbInterface2 = this.mReportInterface;
                if (usbInterface2 != null) {
                    usbDeviceConnection.releaseInterface(usbInterface2);
                }
            }
            this.mUsbConnection.close();
            this.mUsbConnection = null;
            this.mUsbInterface = null;
            this.mReportInterface = null;
        }
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void enableOrDisableInputDevice() {
        InputDevice inputDevice = this.mInputManager.getInputDevice(this.mInputDeviceId);
        if (inputDevice != null && inputDevice.isEnabled() != this.mAngleStateController.shouldIgnoreKeyboard()) {
            return;
        }
        if (this.mAngleStateController.shouldIgnoreKeyboard()) {
            this.mInputManager.disableInputDevice(this.mInputDeviceId);
            if (this.mIsKeyboardReady) {
                writePadKeyBoardStatus(35, 0);
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "disable keyboard device id: " + this.mInputDeviceId);
        } else {
            this.mInputManager.enableInputDevice(this.mInputDeviceId);
            if (this.mIsKeyboardReady && this.mShouldEnableBackLight) {
                writePadKeyBoardStatus(35, 1);
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "enable keyboard device id: " + this.mInputDeviceId);
        }
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    private void sendPenBatteryState(int penState, int penBatteryState) {
        Intent intent = new Intent();
        intent.setAction("miui.intent.action.ACTION_PEN_REVERSE_CHARGE_STATE");
        intent.setClassName("com.android.settings", "com.android.settings.stylus.MiuiStylusReceiver");
        intent.putExtra("miui.intent.extra.ACTION_PEN_REVERSE_CHARGE_STATE", penState);
        intent.putExtra("miui.intent.extra.REVERSE_PEN_SOC", penBatteryState);
        intent.putExtra("source", ShortCutActionsUtils.REASON_OF_KEYBOARD);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        Slog.i(MiuiPadKeyboardManager.TAG, "pen battery state send");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doCheckKeyboardIdentity(boolean isFirst) {
        int identity;
        if (MiuiKeyboardUtil.isXM2022MCU()) {
            identity = KeyboardAuthHelper.getInstance(this.mContext).doCheckKeyboardIdentityLaunchBeforeU(this, isFirst);
        } else {
            identity = KeyboardAuthHelper.getInstance(this.mContext).doCheckKeyboardIdentityLaunchAfterU(this, isFirst);
        }
        processIdentity(identity);
    }

    private void processIdentity(int identity) {
        switch (identity) {
            case 0:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth ok");
                this.mAngleStateController.setIdentityState(true);
                return;
            case 1:
                showRejectConfirmDialog(1);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth reject");
                this.mAngleStateController.setIdentityState(false);
                enableOrDisableInputDevice();
                return;
            case 2:
                if (this.mCheckIdentityTimes < 5) {
                    checkKeyboardIdentity(5000, false);
                    this.mCheckIdentityTimes++;
                }
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity need check again");
                this.mAngleStateController.setIdentityState(true);
                return;
            case 3:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity internal error");
                this.mAngleStateController.setIdentityState(false);
                return;
            case 4:
                showRejectConfirmDialog(4);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity transfer error");
                this.mAngleStateController.setIdentityState(false);
                enableOrDisableInputDevice();
                return;
            default:
                return;
        }
    }

    private void showRejectConfirmDialog(int type) {
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

    private boolean isUserSetUp() {
        if (!this.mIsSetupComplete) {
            this.mIsSetupComplete = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        }
        return this.mIsSetupComplete;
    }

    @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager
    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(MiuiPadKeyboardManager.TAG);
        pw.print(prefix);
        pw.print("mUsbDevice=");
        UsbDevice usbDevice = this.mUsbDevice;
        if (usbDevice != null) {
            pw.println("[DeviceName=" + this.mUsbDevice.getDeviceName() + ",VendorId=" + this.mUsbDevice.getVendorId() + ",ProductId=" + this.mUsbDevice.getProductId() + "]");
        } else {
            pw.println(usbDevice);
        }
        pw.print(prefix);
        pw.print("mMcuVersion=");
        pw.println(this.mMcuVersion);
        pw.print(prefix);
        pw.print("mKeyboardVersion=");
        pw.println(this.mKeyboardVersion);
        pw.print(prefix);
        pw.print("mWirelessVersion=");
        pw.println(this.mWirelessVersion);
        this.mAngleStateController.dump(prefix, pw);
        pw.print(prefix);
        pw.print("mRecentConnTime=");
        pw.println(this.mRecentConnTime.toString());
    }
}
