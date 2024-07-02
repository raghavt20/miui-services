package com.android.server.input.padkeyboard.bluetooth.upgrade;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;

/* loaded from: classes.dex */
public class OtaHandler extends Handler {
    public static final int MSG_CHECK_SUM = 8;
    public static final int MSG_CLEAR_DEVICE = 15;
    public static final int MSG_CTRL_CREATE_BIN = 10;
    public static final int MSG_CTRL_CREATE_DAT = 6;
    public static final int MSG_CTRL_EXECUTE = 11;
    public static final int MSG_CTRL_SELECT_BIN = 9;
    public static final int MSG_CTRL_SELECT_DAT = 4;
    public static final int MSG_CTRL_SET_PRN = 5;
    public static final int MSG_DATA_PACKET = 7;
    public static final int MSG_GET_KB_STATUS = 2;
    public static final int MSG_OTA_RESULT = 14;
    public static final int MSG_SEND_BIN = 13;
    public static final int MSG_SEND_DAT = 12;
    public static final int MSG_SET_MTU = 1;
    public static final int MSG_START_OTA = 3;
    public static final int OTA_FAIL = 2;
    public static final int OTA_START = 1;
    public static final int OTA_SUCCESS = 0;
    private static final String TAG = "OtaHandler";
    public static final int TYPE_BIN = 1;
    public static final int TYPE_DAT = 0;
    GattDeviceItem mGattDeviceItem;

    /* JADX INFO: Access modifiers changed from: package-private */
    public OtaHandler(Looper looper, GattDeviceItem item) {
        super(looper);
        this.mGattDeviceItem = item;
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (this.mGattDeviceItem == null) {
            Log.d(TAG, "handleMessage: mGattDeviceItem null");
            return;
        }
        switch (msg.what) {
            case 1:
                Slog.i(TAG, "start requestMtu");
                this.mGattDeviceItem.setMtu();
                return;
            case 2:
                Slog.i(TAG, "start get keyboard status");
                this.mGattDeviceItem.getKeyboardStatus();
                return;
            case 3:
                Slog.i(TAG, "start BLE OTA ...");
                this.mGattDeviceItem.startUpgrade(UpgradeZipFile.OTA_FLIE_PATH);
                return;
            case 4:
                Slog.i(TAG, "start select dat");
                this.mGattDeviceItem.selectDat();
                return;
            case 5:
                Slog.i(TAG, "start set prn");
                this.mGattDeviceItem.setPrn();
                return;
            case 6:
                Slog.i(TAG, "start create dat");
                this.mGattDeviceItem.createDat();
                return;
            case 7:
                Slog.i(TAG, "start data packet");
                this.mGattDeviceItem.sendPacket();
                return;
            case 8:
                Slog.i(TAG, "start check sum");
                this.mGattDeviceItem.checkSum();
                return;
            case 9:
                Slog.i(TAG, "start select bin");
                this.mGattDeviceItem.selectBin();
                return;
            case 10:
                Slog.i(TAG, "start create bin");
                this.mGattDeviceItem.createBin();
                return;
            case 11:
                Slog.i(TAG, "start ctrl execute");
                this.mGattDeviceItem.ctrlExcute();
                return;
            case 12:
                int index = msg.arg1;
                this.mGattDeviceItem.sendData(0, index);
                return;
            case 13:
                int result = msg.arg1;
                this.mGattDeviceItem.sendData(1, result);
                return;
            case 14:
                int result2 = msg.arg1;
                showOtaResult(result2);
                return;
            default:
                Slog.i(TAG, "unknown msg " + msg.what);
                return;
        }
    }

    private void showOtaResult(int result) {
        int resId = 0;
        Slog.i(TAG, "showOtaResult: result " + result);
        if (result == 1) {
            resId = 286196302;
        }
        if (result == 0) {
            resId = 286196303;
        }
        if (result == 2) {
            resId = 286196301;
        }
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        Toast.makeText((Context) systemContext, (CharSequence) systemContext.getResources().getString(resId), 0).show();
    }
}
