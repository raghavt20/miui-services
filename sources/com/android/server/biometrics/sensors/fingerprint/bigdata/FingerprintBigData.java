package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.os.SystemProperties;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/* loaded from: classes.dex */
public abstract class FingerprintBigData {
    protected static final int ACQUIRED_FINGER_DOWN = 22;
    protected static final int ACQUIRED_FINGER_UP = 23;
    protected static final int ACQUIRED_WAIT_FINGER_INPUT = 22;
    protected static final int AUTH_FAIL = 0;
    protected static final int AUTH_SUCCESS = 1;
    protected static final boolean FP_LOCAL_STATISTICS_DEBUG = false;
    public static final int HAL_DATA_CMD_GET_INFO_AUTH = 500001;
    public static final int HAL_DATA_CMD_GET_INFO_ENROLL = 500002;
    public static final int HAL_DATA_CMD_GET_INFO_INIT = 500000;
    public static final int HAL_DATA_PARSE_START_INDEX_FPC = 0;
    public static final int HAL_DATA_PARSE_START_INDEX_FPC_FOD = 0;
    public static final int HAL_DATA_PARSE_START_INDEX_GOODIX = 0;
    public static final int HAL_DATA_PARSE_START_INDEX_GOODIX_FOD = 0;
    public static final int HAL_DATA_PARSE_START_INDEX_JIIOV = 0;
    protected static final int HBM = 0;
    protected static boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    protected static final int LOW_BRIGHT = 1;
    protected static final int SCREEN_STATUS_DOZE = 3;
    protected static final int SCREEN_STATUS_OFF = 1;
    protected static final int SCREEN_STATUS_ON = 2;
    private static final String TAG = "FingerprintHalData";
    private int currentIndex;
    private byte[] data;
    protected int mScreenStatus = -1;
    protected int mFingerUnlockBright = 0;
    private Map<String, Object> bigData = new LinkedHashMap();

    public abstract void resetLocalInfo();

    public abstract boolean updateDataToJson(JSONObject jSONObject);

    public abstract boolean updateJsonToData(JSONObject jSONObject);

    public void addLocalInfo(String key, Object value) {
        this.bigData.put(key, value);
    }

    public void clearLocalInfo() {
        this.bigData.clear();
    }

    public Object getLocalInfo(String key) {
        return this.bigData.get(key);
    }

    public void setHalData(byte[] halData) {
        this.data = halData;
        this.currentIndex = 0;
    }

    public void setParseIndex(int start) {
        this.currentIndex = start;
    }

    public int getHalDataInt() {
        byte[] bArr = this.data;
        int length = bArr.length;
        int i = this.currentIndex;
        if (length < i + 4) {
            return 0;
        }
        int i2 = i + 1;
        this.currentIndex = i2;
        int i3 = bArr[i] & 255;
        int i4 = i2 + 1;
        this.currentIndex = i4;
        int i5 = ((bArr[i2] << 8) & 65280) | i3;
        int i6 = i4 + 1;
        this.currentIndex = i6;
        int i7 = i5 | ((bArr[i4] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680);
        this.currentIndex = i6 + 1;
        return ((bArr[i6] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) | i7;
    }

    public String getHalDataString(int len) {
        int length = this.data.length;
        int i = this.currentIndex;
        if (length < i + len) {
            return "";
        }
        int newcurrent = i + len;
        char[] charArray = new char[len];
        StringBuilder sb = new StringBuilder();
        while (0 < len) {
            byte[] bArr = this.data;
            int i2 = this.currentIndex;
            byte b = bArr[i2];
            if (b == 0) {
                break;
            }
            this.currentIndex = i2 + 1;
            charArray[0] = (char) b;
            sb.append(charArray[0]);
        }
        this.currentIndex = newcurrent;
        return sb.toString();
    }

    public Map<String, Object> getBigData() {
        return this.bigData;
    }

    public String currentTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(1);
        int month = calendar.get(2) + 1;
        int day = calendar.get(5);
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int second = calendar.get(13);
        int millisecond = calendar.get(14);
        String dayTime = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + ":" + millisecond;
        return dayTime;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : this.bigData.entrySet()) {
            if (entry.getValue().getClass().isArray()) {
                int len = Array.getLength(entry.getValue());
                for (int i = 0; i < len; i++) {
                    sb.append(entry.getKey()).append("[").append(i).append("]").append("=").append(Array.get(entry.getValue(), i)).append("; ");
                }
            } else {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
            }
        }
        return sb.toString();
    }
}
