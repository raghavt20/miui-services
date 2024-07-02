package com.android.server.input.padkeyboard.bluetooth;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.padkeyboard.MiuiIICKeyboardManager;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.bluetooth.upgrade.Constant;
import com.android.server.input.padkeyboard.bluetooth.upgrade.GattDeviceItem;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import miui.hardware.input.InputFeature;

/* loaded from: classes.dex */
public class KeyboardGattCallback extends BluetoothGattCallback {
    private static final String TAG = "KeyboardGattCallback";
    private boolean mAlreadyNotifyBattery;
    private CommunicationUtil mCommunicationUtil;
    private GattDeviceItem mGattDeviceItem;
    public final int BLE_DEVICE_SLEEP_INTERVAL = 40;
    private volatile boolean mIsActiveMode = true;
    private final Handler mBleHandler = MiuiInputThread.getHandler();

    public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
        if (status == 0) {
            this.mIsActiveMode = interval < 40;
        }
    }

    public void setActiveMode(boolean isActiveMode) {
        this.mIsActiveMode = isActiveMode;
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (gatt == null) {
            Slog.e(TAG, "ConnectionStateChanged, but gatt is null!");
            return;
        }
        switch (newState) {
            case 0:
                Slog.i(TAG, "ConnectionStateChanged disconnected!");
                clearDevice();
                return;
            case 1:
            default:
                Slog.i(TAG, "New state not processed: " + newState);
                clearDevice();
                return;
            case 2:
                Slog.e(TAG, "ConnectionStateChanged connected!");
                this.mIsActiveMode = true;
                gatt.discoverServices();
                return;
        }
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (gatt == null || status != 0) {
            Slog.i(TAG, "onServicesDiscovered received: " + status + " or null pointer");
            return;
        }
        Slog.i(TAG, "onServicesDiscovered: deviceName:" + gatt.getDevice().getName() + " status:" + status);
        if (this.mGattDeviceItem == null) {
            GattDeviceItem gattDeviceItem = new GattDeviceItem(gatt);
            this.mGattDeviceItem = gattDeviceItem;
            gattDeviceItem.setNotificationCharacters();
            this.mGattDeviceItem.setConnected(true);
        }
        if (InputFeature.isSingleBleKeyboard()) {
            this.mGattDeviceItem.sendSetMtu();
        }
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        GattDeviceItem gattDeviceItem;
        super.onCharacteristicWrite(gatt, characteristic, status);
        Slog.i(TAG, " onCharacteristicWrite: write data:" + MiuiKeyboardUtil.Bytes2Hex(characteristic.getValue(), 18) + ", and status is : " + status);
        if ((Constant.UUID_DFU_CONTROL_CHARACTER.equals(characteristic.getUuid()) || Constant.UUID_DFU_PACKET_CHARACTER.equals(characteristic.getUuid())) && (gattDeviceItem = this.mGattDeviceItem) != null) {
            gattDeviceItem.notifyDeviceFree();
        }
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
            Slog.w(TAG, "The iic devices is connected, ignore ble info!");
            return;
        }
        if (Constant.UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            Slog.i(TAG, "get keyboard battery :" + ((int) value[0]));
            if (KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
                int nowElectricity = value[0];
                if (nowElectricity <= 10 && !this.mAlreadyNotifyBattery) {
                    this.mAlreadyNotifyBattery = true;
                    Bundle castData = new Bundle();
                    castData.putInt("electricity", nowElectricity);
                    MiuiKeyboardUtil.notifySettingsKeyboardStatusChanged(ActivityThread.currentActivityThread().getSystemContext(), castData);
                }
                if (nowElectricity > 10 && this.mAlreadyNotifyBattery) {
                    this.mAlreadyNotifyBattery = false;
                    return;
                }
                return;
            }
            return;
        }
        if (Constant.UUID_DFU_CONTROL_CHARACTER.equals(characteristic.getUuid())) {
            Slog.i(TAG, "deviceName:" + gatt.getDevice().getName() + " received:" + MiuiKeyboardUtil.Bytes2Hex(characteristic.getValue(), characteristic.getValue().length));
            GattDeviceItem gattDeviceItem = this.mGattDeviceItem;
            if (gattDeviceItem != null) {
                gattDeviceItem.parseInfo(characteristic.getValue());
                return;
            }
            return;
        }
        if (value.length > 1) {
            Slog.i(TAG, "get raw ble data:" + MiuiKeyboardUtil.Bytes2Hex(value, value.length));
            byte[] temp = new byte[64];
            temp[0] = 35;
            temp[1] = value[0];
            temp[2] = CommunicationUtil.KEYBOARD_ADDRESS;
            temp[3] = CommunicationUtil.PAD_ADDRESS;
            temp[4] = value[1];
            if (value.length >= 3) {
                System.arraycopy(value, 2, temp, 5, value.length - 2);
            }
            if (value[0] == 48 && value[1] == 1) {
                String version = String.format("%02x", Byte.valueOf(value[4])) + String.format("%02x", Byte.valueOf(value[3]));
                Slog.i(TAG, "ble device version:" + version);
                GattDeviceItem gattDeviceItem2 = this.mGattDeviceItem;
                if (gattDeviceItem2 != null) {
                    gattDeviceItem2.setVersionAndStartOta(version);
                }
            }
            this.mCommunicationUtil.receiverNanoAppData(temp);
        }
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Slog.i(TAG, "onMtuChanged: deviceName:" + gatt.getDevice().getName() + " mtu:" + mtu);
        GattDeviceItem gattDeviceItem = this.mGattDeviceItem;
        if (gattDeviceItem != null) {
            gattDeviceItem.setPackageSize(mtu - 3);
            this.mGattDeviceItem.sendGetKeyboardStatus();
        }
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        byte[] value = descriptor.getValue();
        Slog.i(TAG, "onDescriptorRead: deviceName:" + gatt.getDevice().getName() + "read data:" + MiuiKeyboardUtil.Bytes2Hex(value, value.length));
    }

    @Override // android.bluetooth.BluetoothGattCallback
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        byte[] value = descriptor.getValue();
        Slog.i(TAG, "onDescriptorWrite: deviceName:" + gatt.getDevice().getName() + " write data:" + MiuiKeyboardUtil.Bytes2Hex(value, value.length));
    }

    private void WriteBleCharacter(byte[] buf, int type) {
        if (this.mGattDeviceItem == null) {
            Slog.i(TAG, "WriteBleCharacter mGattDeviceItem is null");
        } else {
            Slog.i(TAG, "WriteBleCharacter type " + type + ", write data " + MiuiKeyboardUtil.Bytes2Hex(buf, 18));
            this.mGattDeviceItem.syncWriteCommand(buf, type);
        }
    }

    public void sendCommandToKeyboard(byte[] command) {
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked() || !this.mIsActiveMode) {
            Slog.w(TAG, "Abandon command because ble isn't connected or sleep");
            return;
        }
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        if (command != null && command.length > 4 && command[1] == 35 && systemContext != null) {
            Settings.System.putInt(systemContext.getContentResolver(), MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS, command[3]);
        }
        WriteBleCharacter(command, 0);
    }

    public BluetoothGatt getBluetoothGatt() {
        GattDeviceItem gattDeviceItem = this.mGattDeviceItem;
        if (gattDeviceItem == null) {
            return null;
        }
        return gattDeviceItem.getGatt();
    }

    public void setCommunicationUtil(CommunicationUtil communicationUtil) {
        this.mCommunicationUtil = communicationUtil;
    }

    private synchronized void clearDevice() {
        Slog.i(TAG, "clearDevice");
        this.mIsActiveMode = false;
        KeyboardInteraction.INTERACTION.setConnectBleLocked(false);
        GattDeviceItem gattDeviceItem = this.mGattDeviceItem;
        if (gattDeviceItem != null) {
            gattDeviceItem.clear();
            this.mGattDeviceItem = null;
        }
    }

    public void notifyUpgradeIfNeed() {
        GattDeviceItem gattDeviceItem = this.mGattDeviceItem;
        if (gattDeviceItem != null) {
            gattDeviceItem.sendStartOta();
        }
    }
}
