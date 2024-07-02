package com.android.server.input.padkeyboard;

import android.os.Handler;
import android.util.Slog;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public enum KeyboardInteraction {
    INTERACTION;

    public static final int CONNECTION_TYPE_BLE = 1;
    public static final int CONNECTION_TYPE_IIC = 0;
    private static final String TAG = "KeyboardInteraction";
    private BluetoothKeyboardManager mBlueToothKeyboardManager;
    private boolean mIsConnectBle;
    private boolean mIsConnectIIC;
    private MiuiIICKeyboardManager mMiuiIICKeyboardManager;
    private final Object mLock = new Object();
    private List<KeyboardStatusChangedListener> mListenerList = new CopyOnWriteArrayList();
    private final Handler mHandler = MiuiInputThread.getHandler();

    /* loaded from: classes.dex */
    public interface KeyboardStatusChangedListener {
        void onKeyboardStatusChanged(int i, boolean z);
    }

    KeyboardInteraction() {
    }

    public void addListener(KeyboardStatusChangedListener listener) {
        synchronized (this.mLock) {
            if (!this.mListenerList.contains(listener)) {
                this.mListenerList.add(listener);
            }
        }
    }

    public void removeListener(KeyboardStatusChangedListener listener) {
        synchronized (this.mLock) {
            this.mListenerList.remove(listener);
        }
    }

    public void setConnectBleLocked(boolean connectBle) {
        boolean needNotify = false;
        synchronized (this.mLock) {
            if (this.mIsConnectBle != connectBle) {
                needNotify = true;
                this.mIsConnectBle = connectBle;
            }
        }
        if (needNotify) {
            Slog.i(TAG, "notify Keyboard Ble Connection is Changed to " + this.mIsConnectBle);
            for (final KeyboardStatusChangedListener listener : this.mListenerList) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.KeyboardInteraction$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        KeyboardInteraction.this.lambda$setConnectBleLocked$0(listener);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setConnectBleLocked$0(KeyboardStatusChangedListener listener) {
        listener.onKeyboardStatusChanged(1, this.mIsConnectBle);
    }

    public boolean isConnectBleLocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsConnectBle;
        }
        return z;
    }

    public void setConnectIICLocked(boolean connectIIC) {
        boolean needNotify = false;
        synchronized (this.mLock) {
            if (this.mIsConnectIIC != connectIIC) {
                needNotify = true;
                this.mIsConnectIIC = connectIIC;
            }
        }
        if (needNotify) {
            Slog.i(TAG, "notify Keyboard IIC Connection is Changed to " + this.mIsConnectIIC);
            for (final KeyboardStatusChangedListener listener : this.mListenerList) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.KeyboardInteraction$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        KeyboardInteraction.this.lambda$setConnectIICLocked$1(listener);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setConnectIICLocked$1(KeyboardStatusChangedListener listener) {
        listener.onKeyboardStatusChanged(0, this.mIsConnectIIC);
    }

    public boolean isConnectIICLocked() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsConnectIIC;
        }
        return z;
    }

    public void setBlueToothKeyboardManagerLocked(BluetoothKeyboardManager blueToothKeyboardManager) {
        synchronized (this.mLock) {
            this.mBlueToothKeyboardManager = blueToothKeyboardManager;
        }
    }

    public void setMiuiIICKeyboardManagerLocked(MiuiIICKeyboardManager iicKeyboardManager) {
        synchronized (this.mLock) {
            this.mMiuiIICKeyboardManager = iicKeyboardManager;
        }
    }

    public void communicateWithKeyboardLocked(byte[] command) {
        boolean iicConnected;
        boolean bleConnected;
        BluetoothKeyboardManager bluetoothKeyboardManager;
        MiuiIICKeyboardManager miuiIICKeyboardManager;
        synchronized (this.mLock) {
            iicConnected = this.mIsConnectIIC;
            bleConnected = this.mIsConnectBle;
        }
        if (iicConnected && (miuiIICKeyboardManager = this.mMiuiIICKeyboardManager) != null) {
            miuiIICKeyboardManager.writeCommandToIIC(command);
        } else if (bleConnected && (bluetoothKeyboardManager = this.mBlueToothKeyboardManager) != null) {
            bluetoothKeyboardManager.writeDataToBleDevice(command);
        } else {
            Slog.e(TAG, "Can't communicate with keyboard because no connection");
        }
    }

    public void communicateWithIIC(byte[] command) {
        if (command == null) {
            return;
        }
        this.mMiuiIICKeyboardManager.writeCommandToIIC(command);
    }

    public boolean haveConnection() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsConnectBle || this.mIsConnectIIC;
        }
        return z;
    }
}
