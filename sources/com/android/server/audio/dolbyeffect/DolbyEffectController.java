package com.android.server.audio.dolbyeffect;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.IAudioService;
import android.media.audiofx.AudioEffectCenter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.audio.dolbyeffect.deviceinfo.BtDeviceInfo;
import com.android.server.audio.dolbyeffect.deviceinfo.DeviceInfoBase;
import com.android.server.audio.dolbyeffect.deviceinfo.UsbDeviceInfo;
import com.android.server.audio.dolbyeffect.deviceinfo.WiredDeviceInfo;
import com.android.server.pm.CloudControlPreinstallService;
import com.dolby.dax.DolbyAudioEffect;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class DolbyEffectController {
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String HEADSET_STATE = "state";
    static final int MANU_ID_XIAO_MI = 911;
    private static final int MSG_BT_STATE_CHANGED = 1;
    private static final int MSG_BT_STATE_CHANGED_DEVICEBROKER = 3;
    private static final int MSG_HEADSET_PLUG = 2;
    private static final int MSG_ROTATION_CHANGED = 4;
    private static final int MSG_SPATIALIZER_AVALIABLE_STATE = 6;
    private static final int MSG_SPATIALIZER_ENABLED_STATE = 5;
    private static final int MSG_VOLUME_CHANGED = 0;
    private static final int PORT_BLUETOOTH = 4;
    private static final int PORT_HDMI = 1;
    private static final int PORT_HEADPHONE = 3;
    private static final int PORT_INTERNAL_SPEAKER = 0;
    private static final int PORT_MIRACAST = 2;
    private static final int PORT_USB = 5;
    private static final String TAG = "DolbyEffectController";
    private static LinkedList<DeviceInfoBase> mCurrentDevices = new LinkedList<>();
    private static volatile DolbyEffectController sInstance;
    private static IAudioService sService;
    private Context mContext;
    private DolbyAudioEffect mDolbyAudioEffect;
    private HandlerThread mHandlerThread;
    private WorkHandler mWorkHandler;
    private final boolean mIsSupportFWAudioEffectCenter = SystemProperties.getBoolean("ro.vendor.audio.fweffect", false);
    private int mVolumeThreshold = 100;
    private String mCurrentVolumeLevel = "disabled";
    private String mCurrentRotation = "disabled";
    private int mCurrentPort = 0;
    private boolean[] mSpatializerEnabledAvaliable = {false, false};
    private final boolean mAdspSpatializerAvailable = SystemProperties.getBoolean("ro.vendor.audio.adsp.spatial", false);
    private boolean mAdspSpatializerEnable = false;
    private DolbyTunning mCurrentDolbyTunning = new DolbyTunning();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DolbyTunning {
        String mDeviceId;
        int mPort;

        private DolbyTunning() {
            this.mPort = 0;
            this.mDeviceId = DeviceId.SPK_DEFAULT;
        }
    }

    private DolbyEffectController(Context context) {
        this.mContext = context;
    }

    public static DolbyEffectController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DolbyEffectController(context);
        }
        return sInstance;
    }

    public void init() {
        try {
            Log.d(TAG, "DolbyEffectController init...");
            if ("true".equals(SystemProperties.get("ro.audio.spatializer_enabled"))) {
                this.mSpatializerEnabledAvaliable[0] = getService().isSpatializerEnabled();
                this.mSpatializerEnabledAvaliable[1] = getService().isSpatializerAvailable();
            }
            if ("true".equals(SystemProperties.get("vendor.audio.dolby.control.tunning.by.volume.support"))) {
                this.mVolumeThreshold = (getService().getStreamMaxVolume(3) * 10) / 15;
                int index = getService().getDeviceStreamVolume(3, 2);
                if (index > this.mVolumeThreshold) {
                    this.mCurrentVolumeLevel = "HIGH";
                } else {
                    this.mCurrentVolumeLevel = "LOW";
                }
            }
            HandlerThread handlerThread = new HandlerThread("DolbyEffect");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mWorkHandler = new WorkHandler(this.mHandlerThread.getLooper());
            Log.d(TAG, "updateDolbyTunning from init()");
            updateDolbyTunning();
        } catch (Exception e) {
            Log.e(TAG, "init: Exception " + e);
        }
    }

    public void receiveVolumeChanged(int newVolume) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = Integer.valueOf(newVolume);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    public void receiveRotationChanged(int rotationChanged) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 4;
            msg.obj = Integer.valueOf(rotationChanged);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    public void receiveSpatializerEnabledChanged(boolean enabled) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 5;
            msg.obj = Boolean.valueOf(enabled);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    public void receiveSpatializerAvailableChanged(boolean available) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 6;
            msg.obj = Boolean.valueOf(available);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void receiveDeviceConnectStateChanged(Context context, Intent intent) {
        char c;
        String action = intent.getAction();
        if (action != null) {
            Log.d(TAG, "onReceive: " + action);
            switch (action.hashCode()) {
                case -1676458352:
                    if (action.equals("android.intent.action.HEADSET_PLUG")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1772843706:
                    if (action.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 2116862345:
                    if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                        c = 2;
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
                    Message msg = Message.obtain();
                    msg.what = 2;
                    msg.obj = intent;
                    this.mWorkHandler.sendMessage(msg);
                    return;
                case 1:
                case 2:
                    Message msg2 = Message.obtain();
                    msg2.what = 1;
                    msg2.obj = intent;
                    this.mWorkHandler.sendMessage(msg2);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.d(DolbyEffectController.TAG, "receive MSG_VOLUME_CHANGED");
                    DolbyEffectController.this.onVolumeChanged(((Integer) msg.obj).intValue());
                    return;
                case 1:
                    Log.d(DolbyEffectController.TAG, "receive MSG_BT_STATE_CHANGED");
                    DolbyEffectController.this.onBTStateChanged((Intent) msg.obj);
                    return;
                case 2:
                    Log.d(DolbyEffectController.TAG, "receive MSG_HEADSET_PLUG");
                    DolbyEffectController.this.onHeadsetPlug((Intent) msg.obj);
                    return;
                case 3:
                    Log.d(DolbyEffectController.TAG, "receive MSG_BT_STATE_CHANGED_DEVICEBROKER");
                    DolbyEffectController.this.onBTStateChangedFromDeviceBroker(msg.getData().getString(CloudControlPreinstallService.ConnectEntity.DEVICE), msg.getData().getString("profile"), msg.getData().getString("state"));
                    return;
                case 4:
                    Log.d(DolbyEffectController.TAG, "receive MSG_ROTATION_CHANGED");
                    DolbyEffectController.this.onRotationChanged(((Integer) msg.obj).intValue());
                    return;
                case 5:
                    Log.d(DolbyEffectController.TAG, "receive MSG_SPATIALIZER_ENABLED_STATE");
                    DolbyEffectController.this.onSpatializerStateChanged(5, ((Boolean) msg.obj).booleanValue());
                    return;
                case 6:
                    Log.d(DolbyEffectController.TAG, "receive MSG_SPATIALIZER_AVALIABLE_STATE");
                    DolbyEffectController.this.onSpatializerStateChanged(6, ((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    public void btStateChangedFromDeviceBroker(Bundle btinfo) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 3;
            msg.setData(btinfo);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onHeadsetPlug(Intent intent) {
        boolean changed = false;
        try {
            int newDevices = getService().getDeviceMaskForStream(3);
            int state = intent.getIntExtra("state", 0);
            if (state == 0) {
                Log.d(TAG, "detected device disconnected, new devices: " + newDevices);
                if ((newDevices & BroadcastQueueModernStubImpl.FLAG_IMMUTABLE) == 0 && containsDevice("USB headset") >= 0) {
                    changed = removeDeviceForName("USB headset");
                }
                if ((newDevices & 12) == 0 && containsDevice("wired headset") >= 0) {
                    changed = removeDeviceForName("wired headset");
                }
            } else if (state == 1) {
                Log.d(TAG, "detected device connected, new devices: " + newDevices);
                if ((67108864 & newDevices) != 0) {
                    UsbDeviceInfo newDevice = new UsbDeviceInfo(DeviceInfoBase.TYPE_USB, 0, 0);
                    if (containsDevice("USB headset") < 0) {
                        mCurrentDevices.add(newDevice);
                        changed = true;
                        Log.d(TAG, "onReceive: USB headset connected");
                    }
                } else if ((newDevices & 12) != 0) {
                    WiredDeviceInfo newDevice2 = new WiredDeviceInfo(DeviceInfoBase.TYPE_WIRED, 0, 0);
                    if (containsDevice("wired headset") < 0) {
                        mCurrentDevices.add(newDevice2);
                        changed = true;
                        Log.d(TAG, "onReceive: wired headset connected");
                    }
                }
            }
            if (changed) {
                onDeviceChanged(mCurrentDevices);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onHeadsetPlug: RemoteException " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSpatializerStateChanged(int type, boolean value) {
        boolean[] zArr = this.mSpatializerEnabledAvaliable;
        boolean currentState = zArr[0] & zArr[1];
        if (type == 5) {
            zArr[0] = value;
        } else if (type == 6) {
            zArr[1] = value;
        }
        boolean newState = zArr[1] & zArr[0];
        if (currentState != newState) {
            Log.d(TAG, "updateDolbyTunning() from onSpatializerStateChanged(), currentState is" + currentState + " newState is" + newState);
            updateDolbyTunning();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void onBTStateChanged(Intent intent) {
        boolean z;
        String action = intent.getAction();
        if (action != null) {
            boolean changed = false;
            switch (action.hashCode()) {
                case 1772843706:
                    if (action.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")) {
                        z = false;
                        break;
                    }
                    z = -1;
                    break;
                case 2116862345:
                    if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                        z = true;
                        break;
                    }
                    z = -1;
                    break;
                default:
                    z = -1;
                    break;
            }
            switch (z) {
                case false:
                    int[] ids = new int[2];
                    String[] deviceName = {""};
                    if (BtDeviceInfo.tryGetIdsFromIntent(intent, ids, deviceName) && ids[0] != 0 && ids[1] != 0) {
                        int idx = containsDevice(deviceName[0]);
                        if (idx >= 0) {
                            BtDeviceInfo device2 = (BtDeviceInfo) mCurrentDevices.get(idx);
                            if (device2.getMajorID() != ids[0] && device2.getMinorID() != ids[1]) {
                                device2.setMajorID(ids[0]);
                                device2.setMinorID(ids[1]);
                                Log.d(TAG, "onBTStateChanged: device updated " + deviceName[0] + " majorid: " + ids[0] + " minorid: " + ids[1]);
                                changed = true;
                                break;
                            }
                        } else {
                            BtDeviceInfo newDevice = new BtDeviceInfo(DeviceInfoBase.TYPE_BT, ids[0], ids[1], deviceName[0], false);
                            mCurrentDevices.add(newDevice);
                            changed = true;
                            Log.d(TAG, "onBTStateChanged: device connected " + deviceName[0] + " majorid: " + ids[0] + " minorid: " + ids[1]);
                            break;
                        }
                    }
                    break;
                case true:
                    int state = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", -1);
                    BluetoothDevice extraDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    String deviceName2 = extraDevice != null ? extraDevice.getAddress() : "NULL";
                    if (state == 10) {
                        Log.d(TAG, "ACTION_BOND_STATE_CHANGED: device " + deviceName2 + " bond disconnect");
                        changed = removeDeviceForName(deviceName2);
                        break;
                    }
                    break;
            }
            if (changed) {
                onDeviceChanged(mCurrentDevices);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBTStateChangedFromDeviceBroker(String device, String profile, String state) {
        int idx;
        Log.d(TAG, "onBTStateChangedFromDeviceBroker btDevice=" + device + " profile=" + profile + " state=" + state);
        if ("A2DP".equals(profile)) {
            if ("STATE_CONNECTED".equals(state)) {
                int idx2 = containsDevice(device);
                if (idx2 >= 0) {
                    BtDeviceInfo product = (BtDeviceInfo) mCurrentDevices.get(idx2);
                    mCurrentDevices.remove(idx2);
                    product.setState(true);
                    mCurrentDevices.add(product);
                    Log.d(TAG, "onBTStateChangedFromDeviceBroker: device active: " + device + " majorid: " + product.getMajorID() + " minorid: " + product.getMinorID());
                } else {
                    mCurrentDevices.add(new BtDeviceInfo(DeviceInfoBase.TYPE_BT, 0, 0, device, true));
                    Log.d(TAG, "onBTStateChangedFromDeviceBroker: Bt headset connected");
                }
                onDeviceChanged(mCurrentDevices);
                return;
            }
            if ("STATE_DISCONNECTED".equals(state) && (idx = containsDevice(device)) >= 0) {
                ((BtDeviceInfo) mCurrentDevices.get(idx)).setState(false);
                mCurrentDevices.remove(idx);
                Log.d(TAG, "onBTStateChangedFromDeviceBroker: Bt headset disconnected");
                onDeviceChanged(mCurrentDevices);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVolumeChanged(int newVolume) {
        String newVolumeLevel;
        try {
            String cachCurrentVolumeLevel = this.mCurrentVolumeLevel;
            if (newVolume > this.mVolumeThreshold) {
                newVolumeLevel = "HIGH";
            } else {
                newVolumeLevel = "LOW";
            }
            if (newVolumeLevel.equals(cachCurrentVolumeLevel)) {
                Log.d(TAG, "newVolumeLevel = " + newVolumeLevel + " mCurrentVolumeLevel = " + this.mCurrentVolumeLevel + " bypass onVolumeChanged");
                return;
            }
            this.mCurrentVolumeLevel = newVolumeLevel;
            Log.d(TAG, "updateDolbyTunning from onVolumeChanged(), mCurrentVolumeLevel is " + cachCurrentVolumeLevel + " newVolumeLevel is " + newVolumeLevel);
            updateDolbyTunning();
        } catch (RuntimeException e) {
            Log.e(TAG, "onVolumeChanged: RuntimeException " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRotationChanged(int rotation) {
        try {
            String cachCurrentRotation = this.mCurrentRotation;
            String newRotation = String.valueOf(rotation);
            Log.d(TAG, "newRotation is " + newRotation);
            if ("0".equals(newRotation)) {
                newRotation = "90";
            }
            this.mCurrentRotation = newRotation;
            boolean[] zArr = this.mSpatializerEnabledAvaliable;
            if (zArr[0] && zArr[1]) {
                if (this.mCurrentPort != 0) {
                    Log.d(TAG, "Current Spatilizer routing device is not speaker, bypass onRotationChanged");
                    return;
                } else if (cachCurrentRotation.equals(newRotation)) {
                    Log.d(TAG, "mCurrentRotation is " + cachCurrentRotation + "newRotation is" + newRotation + ", bypass onRotationChanged");
                    return;
                } else {
                    Log.d(TAG, "updateDolbyTunning from onRotationChanged(), mCurrentRotation is " + cachCurrentRotation + "newRotation is" + newRotation);
                    updateDolbyTunning();
                    return;
                }
            }
            Log.d(TAG, "SpatializerEnabledAvaliable is false, bypass onRotationChanged");
        } catch (RuntimeException e) {
            Log.e(TAG, "onRotationChanged: RuntimeException " + e);
        }
    }

    private void onDeviceChanged(LinkedList<DeviceInfoBase> currentDevices) {
        Log.d(TAG, "onDeviceChanged");
        try {
            int cacheCurrentPort = this.mCurrentPort;
            if (currentDevices.isEmpty()) {
                Log.d(TAG, "no devices connected, CurrentPort is set to INTERNAL_SPEAKER");
                this.mCurrentPort = 0;
                if ("true".equals(SystemProperties.get("vendor.audio.dolby.control.tunning.by.volume.support"))) {
                    int index = getService().getDeviceStreamVolume(3, 2);
                    if (index > this.mVolumeThreshold) {
                        this.mCurrentVolumeLevel = "HIGH";
                        Log.d(TAG, "no devices connected and volume >= " + this.mVolumeThreshold);
                    } else {
                        this.mCurrentVolumeLevel = "LOW";
                        Log.d(TAG, "no devices connected and volume < " + this.mVolumeThreshold);
                    }
                }
                Log.d(TAG, "updateDolbyTunning from onDeviceChanged(),mCurrentPort is " + cacheCurrentPort + " newPort is" + this.mCurrentPort);
                updateDolbyTunning();
            } else {
                DeviceInfoBase device = currentDevices.getLast();
                if (device.getDeviceType() == DeviceInfoBase.TYPE_USB) {
                    this.mCurrentPort = 5;
                    Log.d(TAG, "setSelectedTuningDevice for USB devices");
                } else if (device.getDeviceType() == DeviceInfoBase.TYPE_WIRED) {
                    this.mCurrentPort = 3;
                    Log.d(TAG, "setSelectedTuningDevice for wired devices");
                } else if (device.getDeviceType() == DeviceInfoBase.TYPE_BT) {
                    BtDeviceInfo device2 = (BtDeviceInfo) device;
                    device2.getMajorID();
                    device2.getMinorID();
                    boolean state = device2.getState();
                    if (state) {
                        this.mCurrentPort = 4;
                    } else {
                        this.mCurrentPort = 0;
                    }
                }
                Log.d(TAG, "updateDolbyTunning from onDeviceChanged(), mCurrentPort is " + cacheCurrentPort + " newPort is " + this.mCurrentPort);
                updateDolbyTunning();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDeviceChanged: Exception " + e);
        }
        if (this.mIsSupportFWAudioEffectCenter) {
            AudioEffectCenter audioEffectCenter = AudioEffectCenter.getInstance(this.mContext);
            if (!audioEffectCenter.isEffectActive("dolby") && !audioEffectCenter.isEffectActive("misound")) {
                audioEffectCenter.setEffectActive("misound", true);
            } else {
                notifyEffectChanged();
            }
        }
    }

    private void notifyEffectChanged() {
        Log.d(TAG, "notifyEffectChanged");
        Intent intent = new Intent("miui.intent.action.ACTION_AUDIO_EFFECT_CHANGED");
        this.mContext.sendBroadcast(intent);
    }

    private static IAudioService getService() {
        IAudioService iAudioService = sService;
        if (iAudioService != null) {
            return iAudioService;
        }
        IBinder b = ServiceManager.getService("audio");
        IAudioService asInterface = IAudioService.Stub.asInterface(b);
        sService = asInterface;
        return asInterface;
    }

    private boolean removeDeviceForType(int type) {
        boolean removed = false;
        for (int i = mCurrentDevices.size() - 1; i >= 0; i--) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDeviceType() == type) {
                Log.d(TAG, "remove device type: " + type);
                mCurrentDevices.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private int containsDevice(String deviceName) {
        for (int i = 0; i < mCurrentDevices.size(); i++) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDevice().equals(deviceName)) {
                return i;
            }
        }
        return -1;
    }

    private boolean removeDeviceForName(String deviceName) {
        for (int i = mCurrentDevices.size() - 1; i >= 0; i--) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDevice().equals(deviceName)) {
                Log.d(TAG, "remove device name: " + deviceName);
                mCurrentDevices.remove(i);
                return true;
            }
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public String acquireDolbyDeviceIdForNonSpatializer(int Port, String VolumeLevel, String Rotation) {
        char c;
        switch (Port) {
            case 0:
                switch (VolumeLevel.hashCode()) {
                    case 75572:
                        if (VolumeLevel.equals("LOW")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2217378:
                        if (VolumeLevel.equals("HIGH")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 270940796:
                        if (VolumeLevel.equals("disabled")) {
                            c = 0;
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
                        return DeviceId.SPK_DEFAULT;
                    case 1:
                        return DeviceId.SPK_VOLUME_HIGH;
                    case 2:
                        return DeviceId.SPK_VOLUME_LOW;
                    default:
                        return DeviceId.SPK_DEFAULT;
                }
            case 1:
            case 2:
            default:
                return DeviceId.SPK_DEFAULT;
            case 3:
                return DeviceId.WIRED_DEFAULT;
            case 4:
                return DeviceId.BLUETOOTH_DEFAULT;
            case 5:
                return DeviceId.USB_DEFAULT;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0053, code lost:
    
        if (r12.equals("disabled") != false) goto L37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0087, code lost:
    
        if (r12.equals("disabled") != false) goto L55;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00b8, code lost:
    
        if (r12.equals("disabled") != false) goto L74;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.String acquireDolbyDeviceIdForSpatializer(int r10, java.lang.String r11, java.lang.String r12) {
        /*
            Method dump skipped, instructions count: 336
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.dolbyeffect.DolbyEffectController.acquireDolbyDeviceIdForSpatializer(int, java.lang.String, java.lang.String):java.lang.String");
    }

    public String acquireDolbyDeviceIdForAdspSpatializer(int port) {
        switch (port) {
            case 0:
                return DeviceId.SPK_SPATIALIZER;
            case 1:
            case 2:
            default:
                return DeviceId.SPK_DEFAULT;
            case 3:
                return "headphone_spatializer";
            case 4:
                return DeviceId.BLUETOOTH_SPATIALIZER;
            case 5:
                return "headphone_spatializer";
        }
    }

    public void updateDolbyTunning() {
        DolbyAudioEffect dolbyAudioEffect;
        String newDeviceId;
        try {
            try {
                try {
                    this.mDolbyAudioEffect = new DolbyAudioEffect(0, 0);
                    this.mAdspSpatializerEnable = SystemProperties.getBoolean("persist.vendor.audio.dolbysurround.enable", false);
                    if (this.mDolbyAudioEffect.hasControl()) {
                        boolean[] zArr = this.mSpatializerEnabledAvaliable;
                        if (zArr[0] && zArr[1]) {
                            Log.d(TAG, "The dolby tuning selected for Spatializer, begin...");
                            newDeviceId = acquireDolbyDeviceIdForSpatializer(this.mCurrentPort, this.mCurrentVolumeLevel, this.mCurrentRotation);
                        } else if (this.mAdspSpatializerAvailable && this.mAdspSpatializerEnable) {
                            Log.d(TAG, "[adsp spatial] The dolby tuning selected for adsp Spatializer, begin...");
                            newDeviceId = acquireDolbyDeviceIdForAdspSpatializer(this.mCurrentPort);
                        } else {
                            Log.d(TAG, "The dolby tuning selected for nonSpatializer, begin...");
                            newDeviceId = acquireDolbyDeviceIdForNonSpatializer(this.mCurrentPort, this.mCurrentVolumeLevel, this.mCurrentRotation);
                        }
                        if (this.mCurrentPort == this.mCurrentDolbyTunning.mPort && newDeviceId.equals(this.mCurrentDolbyTunning.mDeviceId)) {
                            Log.d(TAG, "The selected dolby tuning is same as current [mCurrentPort =" + this.mCurrentPort + "newDeviceId = " + newDeviceId + "],bypass updateDolbyTunning");
                            DolbyAudioEffect dolbyAudioEffect2 = this.mDolbyAudioEffect;
                            if (dolbyAudioEffect2 != null) {
                                dolbyAudioEffect2.release();
                                return;
                            }
                            return;
                        }
                        this.mCurrentDolbyTunning.mPort = this.mCurrentPort;
                        this.mCurrentDolbyTunning.mDeviceId = newDeviceId;
                        Log.d(TAG, "updateDolbyTunning, Port = " + this.mCurrentDolbyTunning.mPort + ", DeviceId = " + this.mCurrentDolbyTunning.mDeviceId);
                        this.mDolbyAudioEffect.setSelectedTuningDevice(this.mCurrentDolbyTunning.mPort, this.mCurrentDolbyTunning.mDeviceId);
                    } else {
                        Log.d(TAG, "setSelectedTuningDevice do not hasControl");
                    }
                    dolbyAudioEffect = this.mDolbyAudioEffect;
                    if (dolbyAudioEffect == null) {
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "updateDolbyTunning: IllegalArgumentException" + e);
                    dolbyAudioEffect = this.mDolbyAudioEffect;
                    if (dolbyAudioEffect == null) {
                        return;
                    }
                }
            } catch (UnsupportedOperationException e2) {
                Log.e(TAG, "updateDolbyTunning: UnsupportedOperationException" + e2);
                dolbyAudioEffect = this.mDolbyAudioEffect;
                if (dolbyAudioEffect == null) {
                    return;
                }
            } catch (RuntimeException e3) {
                Log.e(TAG, "updateDolbyTunning: RuntimeException" + e3);
                dolbyAudioEffect = this.mDolbyAudioEffect;
                if (dolbyAudioEffect == null) {
                    return;
                }
            }
            dolbyAudioEffect.release();
        } catch (Throwable th) {
            DolbyAudioEffect dolbyAudioEffect3 = this.mDolbyAudioEffect;
            if (dolbyAudioEffect3 != null) {
                dolbyAudioEffect3.release();
            }
            throw th;
        }
    }

    private String getDeviceidForDevice(int type, int id1, int id2) {
        if (type == DeviceInfoBase.TYPE_USB) {
            String device_id = DeviceId.getUsbDeviceId(id1, id2);
            return device_id;
        }
        if (type == DeviceInfoBase.TYPE_BT) {
            String device_id2 = DeviceId.getBtDeviceId(id1, id2);
            return device_id2;
        }
        if (type != DeviceInfoBase.TYPE_WIRED) {
            return "";
        }
        String device_id3 = DeviceId.getWiredDeviceId(id1, id2);
        return device_id3;
    }
}
