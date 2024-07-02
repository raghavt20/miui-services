package com.android.server.input.padkeyboard.bluetooth.upgrade;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Slog;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import miui.hardware.input.InputFeature;

/* loaded from: classes.dex */
public class GattDeviceItem implements Serializable {
    private static final String TAG = "GattDeviceItem";
    public static final int TYPE_CONTROL = 1;
    public static final int TYPE_HID = 0;
    public static final int TYPE_PACKET = 2;
    private String mAddress;
    public BluetoothGattCharacteristic mBleKeyboardBatteryRead;
    public BluetoothGattCharacteristic mBleKeyboardHidRead;
    public BluetoothGattCharacteristic mBleKeyboardHidWrite;
    private String mCurVersion;
    private BluetoothDevice mDevice;
    public BluetoothGattCharacteristic mDfuControl;
    public BluetoothGattCharacteristic mDfuPacket;
    private BluetoothGatt mGatt;
    private Handler mHandler;
    HandlerThread mHandlerThread;
    private String mName;
    private UpgradeZipFile mOtaFile;
    private boolean mOtaRunning;
    private long mOtaStartTime;
    public List<Integer> mSupportProfile;
    private volatile boolean mConnected = false;
    private boolean mDatDone = false;
    private double mOtaProgerss = 0.0d;
    private int mPackageSize = 20;
    final Object mDeviceBusyLock = new Object();
    private volatile boolean mRequestCompleted = true;

    public GattDeviceItem(BluetoothGatt gatt) {
        this.mSupportProfile = new ArrayList();
        this.mSupportProfile = getSupportProfile(gatt);
        this.mGatt = gatt;
        this.mDevice = gatt.getDevice();
        this.mAddress = gatt.getDevice().getAddress();
        this.mName = gatt.getDevice().getName();
        if (InputFeature.isSingleBleKeyboard()) {
            HandlerThread handlerThread = new HandlerThread("KEYBOARD_BLE_OTA");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new OtaHandler(this.mHandlerThread.getLooper(), this);
        }
    }

    private List<Integer> getSupportProfile(BluetoothGatt gatt) {
        List<Integer> profiles = new ArrayList<>();
        if (gatt != null) {
            List<BluetoothGattService> list = gatt.getServices();
            List<String> serviceUUIDList = new ArrayList<>();
            for (BluetoothGattService bluetoothGattService : list) {
                serviceUUIDList.add(bluetoothGattService.getUuid().toString().toUpperCase());
            }
            if (serviceUUIDList.contains(Constant.UUID_HID_SERVICE.toString().toUpperCase())) {
                Slog.i(TAG, "Support HID Profile");
                profiles.add(Integer.valueOf(Constant.GATT_HID_PROFILE));
                BluetoothGattService service = gatt.getService(Constant.UUID_HID_SERVICE);
                if (service != null) {
                    int size = service.getCharacteristics().size();
                    this.mBleKeyboardHidRead = service.getCharacteristics().get(size - 1);
                    this.mBleKeyboardHidWrite = service.getCharacteristics().get(size - 2);
                    KeyboardInteraction.INTERACTION.setConnectBleLocked(true);
                    BluetoothGattService batteryService = gatt.getService(Constant.UUID_BATTERY_SERVICE);
                    if (batteryService != null) {
                        this.mBleKeyboardBatteryRead = batteryService.getCharacteristic(Constant.UUID_BATTERY_LEVEL);
                    }
                }
            }
            if (InputFeature.isSingleBleKeyboard() && serviceUUIDList.contains(Constant.DFU_SERVICE.toString().toUpperCase())) {
                Slog.i(TAG, "Support DFU Profile");
                profiles.add(Integer.valueOf(Constant.GATT_DFU_PROFILE));
                BluetoothGattService service2 = gatt.getService(Constant.UUID_DFU_SERVICE);
                if (service2 != null) {
                    this.mDfuControl = service2.getCharacteristic(Constant.UUID_DFU_CONTROL_CHARACTER);
                    this.mDfuPacket = service2.getCharacteristic(Constant.UUID_DFU_PACKET_CHARACTER);
                }
            }
        }
        return profiles;
    }

    public void setConnected(boolean connected) {
        this.mConnected = connected;
    }

    public String getVersion() {
        return this.mCurVersion;
    }

    public void setVersionAndStartOta(String version) {
        this.mCurVersion = version;
        sendStartOta();
    }

    public BluetoothGatt getGatt() {
        return this.mGatt;
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public String getName() {
        return this.mName;
    }

    public boolean isDfuDatDone() {
        return this.mDatDone;
    }

    public void setDfuDatDone(boolean done) {
        this.mDatDone = done;
    }

    public boolean isOtaRun() {
        return this.mOtaRunning;
    }

    public void setOtaRun(boolean run) {
        this.mOtaRunning = run;
    }

    public double getOtaProgerss() {
        return this.mOtaProgerss;
    }

    public void setOtaProgerss(double otaProgerss) {
        this.mOtaProgerss = otaProgerss;
    }

    public int getPackageSize() {
        return this.mPackageSize;
    }

    public void setPackageSize(int size) {
        this.mPackageSize = size;
    }

    public UpgradeZipFile getOtaFile() {
        return this.mOtaFile;
    }

    public void setOtaFile(UpgradeZipFile otaFile) {
        this.mOtaFile = otaFile;
    }

    public void clear() {
        BluetoothGatt bluetoothGatt = this.mGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            this.mGatt = null;
        }
        this.mOtaRunning = false;
        this.mDatDone = false;
        this.mConnected = false;
        if (this.mDevice != null) {
            this.mDevice = null;
        }
        if (this.mAddress != null) {
            this.mAddress = null;
        }
        if (this.mName != null) {
            this.mName = null;
        }
        if (this.mSupportProfile != null) {
            this.mSupportProfile = null;
        }
        if (this.mOtaFile != null) {
            this.mOtaFile = null;
        }
        if (this.mCurVersion != null) {
            this.mCurVersion = null;
        }
        if (this.mDfuControl != null) {
            this.mDfuControl = null;
        }
        if (this.mDfuPacket != null) {
            this.mDfuPacket = null;
        }
        if (this.mBleKeyboardHidWrite != null) {
            this.mBleKeyboardHidWrite = null;
        }
        if (this.mBleKeyboardHidRead != null) {
            this.mBleKeyboardHidRead = null;
        }
        if (this.mBleKeyboardBatteryRead != null) {
            this.mBleKeyboardBatteryRead = null;
        }
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            this.mHandler = null;
        }
        HandlerThread handlerThread = this.mHandlerThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            this.mHandlerThread = null;
        }
    }

    public void setNotificationCharacters() {
        if (this.mGatt != null) {
            if (this.mSupportProfile.size() == 0) {
                Slog.i(TAG, "Device SupportProfile is null");
                return;
            }
            if (this.mSupportProfile.contains(Integer.valueOf(Constant.GATT_DFU_PROFILE))) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = this.mDfuControl;
                if (bluetoothGattCharacteristic != null) {
                    if (!this.mGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)) {
                        Slog.e(TAG, "set Notification DfuControl Characteristic failed");
                    }
                    BluetoothGattDescriptor descriptor = this.mDfuControl.getDescriptor(Constant.CLIENT_CHARACTERISTIC_CONFIG);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    this.mGatt.writeDescriptor(descriptor);
                }
                BluetoothGattCharacteristic bluetoothGattCharacteristic2 = this.mDfuPacket;
                if (bluetoothGattCharacteristic2 != null && !this.mGatt.setCharacteristicNotification(bluetoothGattCharacteristic2, true)) {
                    Slog.e(TAG, "set Notification DfuPacket Characteristic failed");
                }
            }
            if (this.mSupportProfile.contains(Integer.valueOf(Constant.GATT_HID_PROFILE))) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic3 = this.mBleKeyboardHidRead;
                if (bluetoothGattCharacteristic3 != null && !this.mGatt.setCharacteristicNotification(bluetoothGattCharacteristic3, true)) {
                    Slog.e(TAG, "set Notification Hid Characteristic failed");
                }
                BluetoothGattCharacteristic bluetoothGattCharacteristic4 = this.mBleKeyboardBatteryRead;
                if (bluetoothGattCharacteristic4 != null && !this.mGatt.setCharacteristicNotification(bluetoothGattCharacteristic4, true)) {
                    Slog.e(TAG, "set Notification Battery Characteristic failed");
                }
            }
        }
    }

    public void syncWriteCommand(byte[] command, int type) {
        if (type == 1 || type == 2) {
            try {
                Slog.i(TAG, "syncWriteCommand " + MiuiKeyboardUtil.Bytes2Hex(command, 18) + " type = " + type);
                synchronized (this.mDeviceBusyLock) {
                    if (!this.mRequestCompleted) {
                        this.mDeviceBusyLock.wait(500L);
                    }
                    if (!this.mRequestCompleted) {
                        Slog.i(TAG, "syncWriteCommand ota failed");
                        sendOtaFailed();
                        return;
                    } else {
                        if (!this.mConnected) {
                            Slog.i(TAG, "syncWriteCommand failed because device has disconnected");
                            return;
                        }
                        this.mRequestCompleted = false;
                        writeCommand(command, type);
                        Slog.i(TAG, "syncWriteCommand " + MiuiKeyboardUtil.Bytes2Hex(command, 18) + ", type = " + type + ", out mRequestCompleted " + this.mRequestCompleted);
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Slog.i(TAG, "Sleeping interrupted");
                return;
            }
        }
        writeCommand(command, type);
    }

    private void writeCommand(byte[] command, int type) {
        if (this.mGatt == null) {
            Slog.i(TAG, "writeCommand failed gatt null");
            return;
        }
        if (type != 0) {
            if (type == 1) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = this.mDfuControl;
                if (bluetoothGattCharacteristic != null) {
                    bluetoothGattCharacteristic.setWriteType(2);
                    this.mDfuControl.setValue(command);
                    boolean ret = this.mGatt.writeCharacteristic(this.mDfuControl);
                    if (!ret) {
                        Slog.e(TAG, "DfuControl Characteristic is false");
                        return;
                    }
                    return;
                }
                Slog.i(TAG, "DfuControl Characteristic is null:");
                return;
            }
            if (type == 2) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic2 = this.mDfuPacket;
                if (bluetoothGattCharacteristic2 != null) {
                    bluetoothGattCharacteristic2.setWriteType(1);
                    this.mDfuPacket.setValue(command);
                    boolean ret2 = this.mGatt.writeCharacteristic(this.mDfuPacket);
                    if (!ret2) {
                        Slog.i(TAG, "DfuPacket Characteristic is false");
                        return;
                    }
                    return;
                }
                Slog.i(TAG, "DfuPacket Characteristic is null:");
                return;
            }
            return;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic3 = this.mBleKeyboardHidWrite;
        if (bluetoothGattCharacteristic3 != null) {
            bluetoothGattCharacteristic3.setValue(command);
            this.mBleKeyboardHidWrite.setWriteType(1);
            boolean ret3 = this.mGatt.writeCharacteristic(this.mBleKeyboardHidWrite);
            if (!ret3) {
                Slog.e(TAG, "write to BLE Hid fail! error code:" + ret3);
            } else {
                Slog.i(TAG, "write to BLE Hid:" + MiuiKeyboardUtil.Bytes2Hex(command, command.length));
            }
        }
    }

    private void sendDatInfo(int index) {
        try {
            byte[] fileBufDat = this.mOtaFile.getFileBufDat();
            int data_len = fileBufDat.length;
            int dfu_len = this.mPackageSize;
            int all = ((data_len + dfu_len) - 1) / dfu_len;
            byte[] temp = new byte[index == all + (-1) ? data_len - (index * dfu_len) : dfu_len];
            System.arraycopy(fileBufDat, index * dfu_len, temp, 0, temp.length);
            syncWriteCommand(temp, 2);
            if (this.mConnected && isOtaRun()) {
                Message message = this.mHandler.obtainMessage();
                if (index + 1 < all) {
                    message.what = 12;
                    message.arg1 = index + 1;
                } else {
                    message.what = 8;
                }
                this.mHandler.sendMessage(message);
                return;
            }
            Slog.i(TAG, "sendDatInfo: device has disconnected or ota failed");
        } catch (Exception e) {
            Slog.e(TAG, "sendDatInfo exception " + e.getMessage());
        }
    }

    private void sendBinInfo(int index) {
        try {
            byte[] fileBufBin = this.mOtaFile.getFileBufBin();
            int data_len = fileBufBin.length;
            int dfu_len = this.mPackageSize;
            int all = ((data_len + dfu_len) - 1) / dfu_len;
            if (index == 0) {
                Slog.i(TAG, "data_len:" + data_len + ", dfu_len:" + dfu_len + ", all:" + all);
            }
            byte[] temp = new byte[index == all + (-1) ? data_len - (index * dfu_len) : dfu_len];
            System.arraycopy(fileBufBin, index * dfu_len, temp, 0, temp.length);
            syncWriteCommand(temp, 2);
            if (this.mConnected && isOtaRun()) {
                double percent = this.mOtaFile.getPercent(index, dfu_len);
                if (percent > this.mOtaProgerss) {
                    Slog.i(TAG, "-- DFU progerss:" + percent);
                    this.mOtaProgerss = percent;
                }
                Message message = this.mHandler.obtainMessage();
                if (index + 1 < all) {
                    message.what = 13;
                    message.arg1 = index + 1;
                } else {
                    message.what = 8;
                }
                this.mHandler.sendMessage(message);
                return;
            }
            Slog.i(TAG, "sendBinInfo: device has disconnected or ota failed");
        } catch (Exception e) {
            Slog.e(TAG, "sendBinInfo exception " + e.getMessage());
        }
    }

    public void startUpgrade(String binPath) {
        try {
            if (isOtaRun()) {
                Slog.i(TAG, "StartUpgrade ota is running");
                return;
            }
            setOtaRun(true);
            boolean zipValid = false;
            if (binPath != null) {
                UpgradeZipFile upgradeFile = new UpgradeZipFile(binPath);
                if (upgradeFile.isValidFile()) {
                    zipValid = true;
                    setOtaFile(upgradeFile);
                }
            }
            if (!zipValid) {
                Slog.i(TAG, "The OTA file unvalid!");
                setOtaRun(false);
            } else if (!checkVersion()) {
                Slog.i(TAG, "version not need to upgrade");
                setOtaRun(false);
            } else {
                this.mOtaStartTime = System.currentTimeMillis();
                sendOtaResult(1);
                sendCtrlSelectDat();
            }
        } catch (Exception e) {
            Slog.e(TAG, "startUpgrade exception " + e.getMessage());
        }
    }

    private void sendCtrlSelectDat() {
        Message message = this.mHandler.obtainMessage();
        message.what = 4;
        this.mHandler.sendMessage(message);
    }

    private void sendCtrlSelectBin() {
        Message message = this.mHandler.obtainMessage();
        message.what = 9;
        this.mHandler.sendMessage(message);
    }

    private void sendCtrlSetPrn() {
        Message message = this.mHandler.obtainMessage();
        message.what = 5;
        this.mHandler.sendMessage(message);
    }

    private void sendCtrlCreateBin() {
        Message message = this.mHandler.obtainMessage();
        message.what = 10;
        this.mHandler.sendMessage(message);
    }

    private void sendDataPacket() {
        Message message = this.mHandler.obtainMessage();
        message.what = 7;
        this.mHandler.sendMessage(message);
    }

    private void sendCtrlCreateDat() {
        Message message = this.mHandler.obtainMessage();
        message.what = 6;
        this.mHandler.sendMessage(message);
    }

    private void sendCtrlExecute() {
        Message message = this.mHandler.obtainMessage();
        message.what = 11;
        this.mHandler.sendMessage(message);
    }

    public void sendSetMtu() {
        Message message = this.mHandler.obtainMessage();
        message.what = 1;
        this.mHandler.sendMessage(message);
    }

    public void sendGetKeyboardStatus() {
        Message message = this.mHandler.obtainMessage();
        message.what = 2;
        this.mHandler.sendMessage(message);
    }

    public void sendStartOta() {
        if (this.mHandler.hasMessages(3)) {
            this.mHandler.removeMessages(3);
        }
        Message message = this.mHandler.obtainMessage();
        message.what = 3;
        this.mHandler.sendMessageDelayed(message, 5000L);
    }

    private boolean checkVersion() {
        String binVersion = this.mOtaFile.getVersion();
        String str = this.mCurVersion;
        return (str == null || binVersion == null || binVersion.compareTo(str) <= 0) ? false : true;
    }

    private void sendOtaResult(int result) {
        Message message = this.mHandler.obtainMessage();
        message.what = 14;
        message.arg1 = result;
        this.mHandler.sendMessage(message);
    }

    public void sendOtaFailed() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        this.mRequestCompleted = true;
        setOtaRun(false);
        sendOtaResult(2);
    }

    public void parseInfo(byte[] info) {
        int file_crc;
        if (this.mGatt == null) {
            Slog.i(TAG, "parseInfo: mGatt is null");
            return;
        }
        if (info[0] == 96) {
            if (info[2] != 1) {
                Slog.e(TAG, "-- Recive: DFU_CTRL_RESPONSE Failed: cmd: " + ((int) info[1]) + "result:" + ((int) info[2]));
                sendOtaFailed();
                return;
            }
            switch (info[1]) {
                case 1:
                    sendDataPacket();
                    return;
                case 2:
                    Slog.i(TAG, "-- Recive: DAT DFU_CTRL_SET_PRN SUCCESS");
                    sendCtrlCreateDat();
                    return;
                case 3:
                    if (this.mDatDone) {
                        Slog.i(TAG, "-- Recive: BIN DFU_CTRL_CAL_CHECKSUM SUCCESS");
                        file_crc = this.mOtaFile.getFileBufBinCrc();
                    } else {
                        Slog.i(TAG, "-- Recive: DAT DFU_CTRL_CAL_CHECKSUM SUCCESS");
                        file_crc = this.mOtaFile.getFileBufDatCrc();
                    }
                    int crc = (info[7] & 255) + ((info[8] & 255) << 8) + ((info[9] & 255) << 16) + ((info[10] & 255) << 24);
                    if (file_crc == crc) {
                        sendCtrlExecute();
                        return;
                    } else {
                        Slog.i(TAG, "-- Recive: DFU_CTRL_CAL_CHECKSUM crc Failed" + crc + "/" + file_crc);
                        sendOtaFailed();
                        return;
                    }
                case 4:
                    if (!this.mDatDone) {
                        this.mDatDone = true;
                        Slog.i(TAG, "-- Recive: DAT DFU_CTRL_EXECTUE SUCCESS");
                        sendCtrlSelectBin();
                        return;
                    } else {
                        Slog.i(TAG, "-- Recive: BIN DFU_CTRL_EXECTUE SUCCESS ota time: " + (System.currentTimeMillis() - this.mOtaStartTime));
                        sendOtaResult(0);
                        return;
                    }
                case 5:
                default:
                    return;
                case 6:
                    if (this.mDatDone) {
                        sendCtrlCreateBin();
                        return;
                    } else {
                        Slog.i(TAG, "-- Recive: DAT DFU_CTRL_SELECT SUCCESS");
                        sendCtrlSetPrn();
                        return;
                    }
            }
        }
    }

    public void setMtu() {
        this.mGatt.requestMtu(363);
    }

    public void getKeyboardStatus() {
        syncWriteCommand(CommunicationUtil.BleDeviceUtil.CMD_KB_STATUS, 0);
    }

    public void selectDat() {
        syncWriteCommand(Constant.CMD_CTRL_SELECT_DAT, 1);
    }

    public void setPrn() {
        syncWriteCommand(Constant.CMD_CTRL_SET_PRN, 1);
    }

    public void createDat() {
        byte[] data = Constant.CMD_CTRL_CREATE_DAT;
        UpgradeZipFile otaBinFile = getOtaFile();
        byte[] dat_len = MiuiKeyboardUtil.int2Bytes(otaBinFile.getFileBufDat().length);
        data[2] = dat_len[3];
        data[3] = dat_len[2];
        data[4] = dat_len[1];
        data[5] = dat_len[0];
        syncWriteCommand(data, 1);
    }

    public void checkSum() {
        syncWriteCommand(Constant.CMD_DFU_CTRL_CAL_CHECKSUM, 1);
    }

    public void ctrlExcute() {
        syncWriteCommand(Constant.CMD_DFU_CTRL_EXECUTE, 1);
    }

    public void selectBin() {
        syncWriteCommand(Constant.CMD_CTRL_SELECT_BIN, 1);
    }

    public void createBin() {
        byte[] data = Constant.CMD_CTRL_CREATE_BIN;
        byte[] dat_len = MiuiKeyboardUtil.int2Bytes(this.mOtaFile.getFileBufBin().length);
        data[2] = dat_len[3];
        data[3] = dat_len[2];
        data[4] = dat_len[1];
        data[5] = dat_len[0];
        syncWriteCommand(data, 1);
    }

    public void sendPacket() {
        if (this.mDatDone) {
            sendBinInfo(0);
        } else {
            sendDatInfo(0);
        }
    }

    public void sendData(int type, int index) {
        if (type == 0) {
            sendDatInfo(index);
        }
        if (type == 1) {
            sendBinInfo(index);
        }
    }

    public void notifyDeviceFree() {
        synchronized (this.mDeviceBusyLock) {
            this.mRequestCompleted = true;
            this.mDeviceBusyLock.notifyAll();
        }
    }
}
