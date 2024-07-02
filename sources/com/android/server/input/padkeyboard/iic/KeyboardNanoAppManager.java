package com.android.server.input.padkeyboard.iic;

import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import vendor.xiaomi.hardware.keyboardnanoapp.V1_0.IKeyboardNanoapp;
import vendor.xiaomi.hardware.keyboardnanoapp.V1_0.INanoappCallback;

/* loaded from: classes.dex */
public class KeyboardNanoAppManager {
    private static final String TAG = "KeyboardNanoApp";
    private static volatile KeyboardNanoAppManager mIKeyboardNanoapp;
    private CommunicationUtil mCommunicationUtil;
    private IKeyboardNanoapp mKeyboardNanoapp;
    private INanoappCallback mNanoappCallback;

    private KeyboardNanoAppManager() {
        try {
            this.mKeyboardNanoapp = IKeyboardNanoapp.getService();
        } catch (Exception e) {
            Slog.i(TAG, "exception when get service");
        }
    }

    public static KeyboardNanoAppManager getInstance() {
        if (mIKeyboardNanoapp == null) {
            synchronized (KeyboardNanoAppManager.class) {
                if (mIKeyboardNanoapp == null) {
                    mIKeyboardNanoapp = new KeyboardNanoAppManager();
                }
            }
        }
        return mIKeyboardNanoapp;
    }

    public boolean sendCommandToNano(ArrayList<Byte> value) {
        try {
            this.mKeyboardNanoapp.sendCmd(value);
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "set callback error:" + e);
            return false;
        }
    }

    public void initCallback() {
        try {
            INanoappCallback.Stub stub = new INanoappCallback.Stub() { // from class: com.android.server.input.padkeyboard.iic.KeyboardNanoAppManager.1
                @Override // vendor.xiaomi.hardware.keyboardnanoapp.V1_0.INanoappCallback
                public void dataReceive(ArrayList<Byte> buf) throws RemoteException {
                    if (KeyboardNanoAppManager.this.mCommunicationUtil != null) {
                        byte[] responseData = new byte[buf.size()];
                        for (int i = 0; i < buf.size(); i++) {
                            responseData[i] = buf.get(i).byteValue();
                        }
                        KeyboardNanoAppManager.this.mCommunicationUtil.receiverNanoAppData(responseData);
                    }
                }

                @Override // vendor.xiaomi.hardware.keyboardnanoapp.V1_0.INanoappCallback
                public void errorReceive(int errorCode) throws RemoteException {
                    Slog.i(KeyboardNanoAppManager.TAG, "errorCode: " + errorCode);
                }
            };
            this.mNanoappCallback = stub;
            this.mKeyboardNanoapp.setCallback(stub);
        } catch (Exception e) {
            Slog.e(TAG, "set callback error:" + e);
        }
    }

    public void setCommunicationUtil(CommunicationUtil communicationUtil) {
        this.mCommunicationUtil = communicationUtil;
    }
}
