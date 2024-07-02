package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.hardware.fingerprint.MiFxTunnelAidl;
import android.util.Slog;
import java.util.Map;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintHalEnrollData extends FingerprintBigData {
    private static final String TAG = "FingerprintHalEnrollData";
    private static Map<String, Integer> goodix_optical_map;
    private static volatile FingerprintHalEnrollData sInstance;

    public static FingerprintHalEnrollData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintHalEnrollData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintHalEnrollData();
                }
            }
        }
        return sInstance;
    }

    public void getEnrollDataFromHal(String vendorName, Boolean is_fod) {
        try {
            MiFxTunnelAidl.getInstance().getHalData(FingerprintBigData.HAL_DATA_CMD_GET_INFO_ENROLL, null);
        } catch (Exception e) {
            Slog.e(TAG, "Get halEnrollData error.", e);
        }
    }

    public void parseGoodixFodInfo() {
        Slog.i(TAG, "parseGoodixFodInfo.");
    }

    public void parseGoodixInfo() {
    }

    public void parseFpcFodInfo() {
    }

    public void parseFpcInfo() {
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateJsonToData(JSONObject dataInfoJson) {
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateDataToJson(JSONObject dataInfoJson) {
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public void resetLocalInfo() {
    }
}
