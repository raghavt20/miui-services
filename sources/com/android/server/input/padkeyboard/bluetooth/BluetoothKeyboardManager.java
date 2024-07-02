package com.android.server.input.padkeyboard.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Slog;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.padkeyboard.bluetooth.upgrade.Constant;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import miui.hardware.input.InputFeature;

/* loaded from: classes.dex */
public class BluetoothKeyboardManager {
    public static String ACTION_CONNECTION_STATE_CHANGED = "com.xiaomi.bluetooth.action.keyboard.CONNECTION_STATE_CHANGED";
    private static final String ACTION_USER_CONFIRMATION_RESULT = "com.xiaomi.bluetooth.action.keyboard.USER_CONFIRMATION_RESULT";
    private static final String EXTRA_KEYBOARD_CONFIRMATION_RESULT = "com.xiaomi.bluetooth.keyboard.extra.CONFIRMATION_RESULT";
    private static final String TAG = "BluetoothKeyboardManager";
    public static final int XIAOMI_KEYBOARD_ATTACH_FINISH_SOURCE = 8963;
    public static final int XIAOMI_KEYBOARD_ATTACH_FINISH_SOURCE_N83 = 769;
    private static volatile BluetoothKeyboardManager mInstance;
    private Context mContext;
    private volatile boolean mIICKeyboardSleepStatus;
    private final ConcurrentHashMap<String, KeyboardGattCallback> mBleGattMapForAdd = new ConcurrentHashMap<>();
    private final BroadcastReceiver mInternalBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager.1
        static final int CONNECT_STATUS_DEFAULT = 0;
        static final int CONNECT_STATUS_FAIL = 2;
        static final int CONNECT_STATUS_LOSE = 3;
        static final int CONNECT_STATUS_SUCCESS = 1;

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothKeyboardManager.ACTION_USER_CONFIRMATION_RESULT.equals(action)) {
                if (BluetoothKeyboardManager.this.mIICKeyboardSleepStatus) {
                    MiuiPadKeyboardManager.getKeyboardManager(BluetoothKeyboardManager.this.mContext).wakeKeyboard176();
                    return;
                }
                return;
            }
            int newState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            int oldState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (device == null) {
                Slog.i(BluetoothKeyboardManager.TAG, "BLE BroadcastReceiver changed, but the device is null");
                return;
            }
            String bleAddress = device.getAddress();
            if (BluetoothKeyboardManager.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothKeyboardManager.this.setCurrentBleKeyboardAddress(bleAddress);
            }
            int state = 0;
            if (!BluetoothKeyboardManager.this.mBleGattMapForAdd.containsKey(bleAddress)) {
                Slog.i(BluetoothKeyboardManager.TAG, "The Ble devices is not keyboard! " + device.getAddress());
                return;
            }
            KeyboardGattCallback callback = (KeyboardGattCallback) BluetoothKeyboardManager.this.mBleGattMapForAdd.get(bleAddress);
            BluetoothGatt gatt = callback.getBluetoothGatt();
            if (newState == 2) {
                if (gatt == null) {
                    device.connectGatt(BluetoothKeyboardManager.this.mContext, false, callback, 2);
                }
                callback.setCommunicationUtil(CommunicationUtil.getInstance());
                Slog.i(BluetoothKeyboardManager.TAG, "Connect the device successfully, keyboard:" + bleAddress);
                state = 1;
            } else if (newState == 0 && oldState == 1) {
                state = 2;
            } else if ((newState == 0 && oldState == 2) || (newState == 0 && oldState == 3)) {
                state = 3;
            }
            Slog.i(BluetoothKeyboardManager.TAG, "status:" + state + ", keyboard:" + bleAddress);
            if (!KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
                Bundle castData = new Bundle();
                castData.putInt("keyboardStatus", state);
                MiuiKeyboardUtil.notifySettingsKeyboardStatusChanged(BluetoothKeyboardManager.this.mContext, castData);
            }
            if (state != 1 && gatt != null) {
                gatt.disconnect();
            }
        }
    };

    public static BluetoothKeyboardManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (BluetoothKeyboardManager.class) {
                if (mInstance == null) {
                    mInstance = new BluetoothKeyboardManager(context);
                }
            }
        }
        return mInstance;
    }

    private BluetoothKeyboardManager(Context context) {
        this.mContext = context;
        registerBroadcastReceiver();
        KeyboardInteraction.INTERACTION.setBlueToothKeyboardManagerLocked(this);
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        if (InputFeature.isSingleBleKeyboard()) {
            filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        } else {
            filter.addAction(Constant.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(ACTION_USER_CONFIRMATION_RESULT);
        }
        this.mContext.registerReceiver(this.mInternalBroadcastReceiver, filter);
    }

    public void setCurrentBleKeyboardAddress(String address) {
        if (address == null) {
            return;
        }
        String convertedStr = address.toUpperCase();
        if (!this.mBleGattMapForAdd.containsKey(convertedStr)) {
            Slog.i(TAG, "update ble address:" + convertedStr);
            this.mBleGattMapForAdd.put(convertedStr, new KeyboardGattCallback());
        }
    }

    public void setIICKeyboardSleepStatus(boolean isKeyboardSleep) {
        this.mIICKeyboardSleepStatus = isKeyboardSleep;
    }

    public void wakeUpKeyboardIfNeed() {
        this.mBleGattMapForAdd.forEach(new BiConsumer() { // from class: com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((KeyboardGattCallback) obj2).setActiveMode(true);
            }
        });
    }

    public void writeDataToBleDevice(final byte[] command) {
        this.mBleGattMapForAdd.forEach(new BiConsumer() { // from class: com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((KeyboardGattCallback) obj2).sendCommandToKeyboard(command);
            }
        });
    }

    public void notifyUpgradeIfNeed() {
        this.mBleGattMapForAdd.forEach(new BiConsumer() { // from class: com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((KeyboardGattCallback) obj2).notifyUpgradeIfNeed();
            }
        });
    }
}
