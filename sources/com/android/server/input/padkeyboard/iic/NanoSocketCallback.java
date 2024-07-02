package com.android.server.input.padkeyboard.iic;

/* loaded from: classes.dex */
public interface NanoSocketCallback {
    public static final int CALLBACK_TYPE_KB = 20;
    public static final int CALLBACK_TYPE_KB_DEV = 3;
    public static final int CALLBACK_TYPE_PAD_MCU = 10;
    public static final int CALLBACK_TYPE_TOUCHPAD = 40;
    public static final String OTA_ERROR_REASON_NO_SOCKET = "Socket linked exception";
    public static final String OTA_ERROR_REASON_NO_VALID = "Invalid upgrade file";
    public static final String OTA_ERROR_REASON_PACKAGE_NUM = "Accumulation and verification failed";
    public static final String OTA_ERROR_REASON_VERSION = "The device version doesn't need upgrading";
    public static final String OTA_ERROR_REASON_WRITE_SOCKET_EXCEPTION = "Write data Socket exception";
    public static final int OTA_STATE_TYPE_BEGIN = 0;
    public static final int OTA_STATE_TYPE_FAIL = 1;
    public static final int OTA_STATE_TYPE_SUCCESS = 2;

    void onHallStatusChanged(byte b);

    void onKeyboardEnableStateChanged(boolean z);

    void onKeyboardGSensorChanged(float f, float f2, float f3);

    void onKeyboardSleepStatusChanged(boolean z);

    void onNFCTouched();

    void onOtaErrorInfo(byte b, String str);

    void onOtaErrorInfo(byte b, String str, boolean z, int i);

    void onOtaProgress(byte b, float f);

    void onOtaStateChange(byte b, int i);

    void onReadSocketNumError(String str);

    void onUpdateKeyboardType(byte b, boolean z);

    void onUpdateVersion(int i, String str);

    void onWriteSocketErrorInfo(String str);

    void requestReAuth();

    void requestReleaseAwake();

    void requestStayAwake();
}
