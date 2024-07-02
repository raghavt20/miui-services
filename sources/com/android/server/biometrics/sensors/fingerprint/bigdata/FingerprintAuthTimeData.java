package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.util.Slog;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintAuthTimeData extends FingerprintBigData {
    private static final int AUTH_FAIL = 0;
    private static final int AUTH_STEP_FINGERDOWN = 1;
    private static final int AUTH_STEP_NONE = 0;
    private static final int AUTH_STEP_RESULT = 3;
    private static final int AUTH_STEP_UI_READY = 2;
    private static final int AUTH_SUCCESS = 1;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_FINGER_DOWN = 22;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_FINGER_UP = 23;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_HIGHLIGHT_CAPTURE_START = 20;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOWLIGHT_CAPTURE_START = 50;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_UNLOCK_SHOW_WINDOW = 19;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_WAIT_FINGER_INPUT = 21;
    private static final int HBM = 0;
    private static final int LOW_BRIGHT = 1;
    private static final String TAG = "FingerprintAuthTimeData";
    public static authTimeStatisticsUnit sAuthTimeUnit = new authTimeStatisticsUnit();
    private static volatile FingerprintAuthTimeData sInstance;
    private int mFingerUnlockBright = 0;
    private authTimeRecord authFailTime = new authTimeRecord("authFailTime");
    private authTimeRecord authSuccessTime = new authTimeRecord("authSuccessTime");
    private JSONObject authTimeJson = new JSONObject();
    private JSONObject authSuccessTimeJson = new JSONObject();
    private JSONObject authFailTimeJson = new JSONObject();
    private List<authTimeRecord> authResultTimeList = new ArrayList(Arrays.asList(this.authFailTime, this.authSuccessTime));
    private List<JSONObject> authResultTimeJsonList = new ArrayList(Arrays.asList(this.authFailTimeJson, this.authSuccessTimeJson));
    private List<String> authResultTimeJsonKeyList = Arrays.asList("auth_fail_time", "auth_success_time");

    public static FingerprintAuthTimeData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintAuthTimeData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintAuthTimeData();
                }
            }
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class authTimeStatisticsUnit {
        public long authFullTime;
        public long authResultTimeStamp;
        public int authStep;
        public long captureToAuthResultTime;
        public long fingerDownTimeStamp;
        public long fingerDownToCaptureTime;
        public long startCaptureTimeStamp;

        private authTimeStatisticsUnit() {
            this.authStep = 0;
            this.fingerDownTimeStamp = 0L;
            this.startCaptureTimeStamp = 0L;
            this.authResultTimeStamp = 0L;
            this.authFullTime = 0L;
            this.fingerDownToCaptureTime = 0L;
            this.captureToAuthResultTime = 0L;
        }

        public int calculateTime() {
            long j = this.authResultTimeStamp;
            long j2 = this.fingerDownTimeStamp;
            this.authFullTime = j - j2;
            long j3 = this.startCaptureTimeStamp;
            long j4 = j3 - j2;
            this.fingerDownToCaptureTime = j4;
            long j5 = j - j3;
            this.captureToAuthResultTime = j5;
            if (j4 > 10000 || j4 < 0) {
                Slog.e(FingerprintAuthTimeData.TAG, "fingerDownToCaptureTime time abnormal!!!!!");
                return -1;
            }
            if (j5 > 10000 || j5 < 0) {
                Slog.e(FingerprintAuthTimeData.TAG, "captureToAuthResultTime time abnormal!!!!!");
                return -1;
            }
            return 0;
        }

        public void reset() {
            this.authStep = 0;
            this.fingerDownTimeStamp = 0L;
            this.startCaptureTimeStamp = 0L;
            this.authResultTimeStamp = 0L;
            this.authFullTime = 0L;
            this.fingerDownToCaptureTime = 0L;
            this.captureToAuthResultTime = 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class timeCollect {
        public String name;
        public int totalAuthCount = 0;
        public long totalAuthTime = 0;
        public long totalFingerDownToCaptureTime = 0;
        public long totalCaptureToAuthResultTime = 0;
        public long avgAuthTime = 0;
        public long avgFingerDownToCaptureTime = 0;
        public long avgCaptureToAuthResultTime = 0;

        timeCollect(String name) {
            this.name = name;
        }

        public void timeProcess() {
            this.totalAuthTime += FingerprintAuthTimeData.sAuthTimeUnit.authFullTime;
            this.totalFingerDownToCaptureTime += FingerprintAuthTimeData.sAuthTimeUnit.fingerDownToCaptureTime;
            long j = this.totalCaptureToAuthResultTime + FingerprintAuthTimeData.sAuthTimeUnit.captureToAuthResultTime;
            this.totalCaptureToAuthResultTime = j;
            long j2 = this.totalAuthTime;
            int i = this.totalAuthCount;
            this.avgAuthTime = j2 / i;
            this.avgFingerDownToCaptureTime = this.totalFingerDownToCaptureTime / i;
            this.avgCaptureToAuthResultTime = j / i;
            debug();
        }

        public void reset() {
            this.totalAuthCount = 0;
            this.totalAuthTime = 0L;
            this.totalFingerDownToCaptureTime = 0L;
            this.totalCaptureToAuthResultTime = 0L;
            this.avgAuthTime = 0L;
            this.avgFingerDownToCaptureTime = 0L;
            this.avgCaptureToAuthResultTime = 0L;
        }

        public void debug() {
        }
    }

    /* loaded from: classes.dex */
    public static class screenStatusTimeCollect {
        public String name;
        public timeCollect highLightTimeCollect = new timeCollect("highlight");
        public timeCollect lowLightCollect = new timeCollect("lowlight");
        public List<timeCollect> lightStatusList = new ArrayList(Arrays.asList(this.highLightTimeCollect, this.lowLightCollect));
        public JSONObject highLightJSONObject = new JSONObject();
        public JSONObject lowLightJSONObject = new JSONObject();
        public List<JSONObject> lightStatusJsonList = new ArrayList(Arrays.asList(this.highLightJSONObject, this.lowLightJSONObject));
        public List<String> lightStatusJsonKeyList = Arrays.asList("hight_light_auth_time", "low_light_auth_time");

        screenStatusTimeCollect(String name) {
            this.name = name;
        }

        public void reset() {
            for (timeCollect object : this.lightStatusList) {
                object.reset();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class authTimeRecord {
        public String name;
        public screenStatusTimeCollect screenOffTimeCollect = new screenStatusTimeCollect("screenOff");
        public screenStatusTimeCollect screenOnTimeCollect = new screenStatusTimeCollect(EdgeSuppressionManager.REASON_OF_SCREENON);
        public screenStatusTimeCollect screenDozeTimeCollect = new screenStatusTimeCollect("screenDoze");
        public List<screenStatusTimeCollect> screenStatusList = new ArrayList(Arrays.asList(this.screenOffTimeCollect, this.screenOnTimeCollect, this.screenDozeTimeCollect));
        private JSONObject screenOffAuthTimeJson = new JSONObject();
        private JSONObject screenOnAuthTimeJson = new JSONObject();
        private JSONObject screenDozeAuthTimeJson = new JSONObject();
        public List<JSONObject> screenStatusJsonList = new ArrayList(Arrays.asList(this.screenOffAuthTimeJson, this.screenOnAuthTimeJson, this.screenDozeAuthTimeJson));
        private List<String> screenStatusJsonKeyList = Arrays.asList("screen_off_auth_time", "screen_on_auth_time", "screen_doze_auth_time");

        authTimeRecord(String name) {
            this.name = name;
        }

        public void reset() {
            for (screenStatusTimeCollect object : this.screenStatusList) {
                object.reset();
            }
        }
    }

    public void handleAcquiredInfo(int acquiredInfo, int vendorCode) {
        Slog.w(TAG, "handleAcquiredInfo");
        if (acquiredInfo != 0 && acquiredInfo == 6) {
            switch (vendorCode) {
                case 20:
                case 50:
                    if (sAuthTimeUnit.authStep == 1) {
                        sAuthTimeUnit.authStep = 2;
                        sAuthTimeUnit.startCaptureTimeStamp = Calendar.getInstance().getTimeInMillis();
                    } else {
                        sAuthTimeUnit.reset();
                    }
                    this.mFingerUnlockBright = vendorCode == 20 ? 0 : 1;
                    return;
                case 22:
                    sAuthTimeUnit.reset();
                    sAuthTimeUnit.authStep = 1;
                    sAuthTimeUnit.fingerDownTimeStamp = Calendar.getInstance().getTimeInMillis();
                    return;
                default:
                    return;
            }
        }
    }

    public int calculateAuthTime(int authen, int screenState) {
        Slog.w(TAG, "calculateAuthTime, authen: " + authen + ", screenState: " + screenState);
        this.mScreenStatus = screenState;
        if (sAuthTimeUnit.authStep == 2) {
            sAuthTimeUnit.authStep = 3;
            sAuthTimeUnit.authResultTimeStamp = Calendar.getInstance().getTimeInMillis();
            if (sAuthTimeUnit.calculateTime() < 0) {
                Slog.e(TAG, "calculate auth time abnormal, drop it!");
                return -1;
            }
        }
        if (sAuthTimeUnit.authStep == 3) {
            screenStatusTimeCollect tempScreenTimeCollect = this.authResultTimeList.get(authen).screenStatusList.get(this.mScreenStatus);
            tempScreenTimeCollect.lightStatusList.get(this.mFingerUnlockBright).totalAuthCount++;
            tempScreenTimeCollect.lightStatusList.get(this.mFingerUnlockBright).timeProcess();
        }
        sAuthTimeUnit.reset();
        return 0;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateDataToJson(JSONObject jSONObject) {
        String str;
        FingerprintAuthTimeData fingerprintAuthTimeData = this;
        String str2 = TAG;
        Slog.w(TAG, "updateAuthTimeJson");
        int tempIndex = 0;
        while (tempIndex < fingerprintAuthTimeData.authResultTimeList.size()) {
            try {
                authTimeRecord tempAuthResultTimeRecord = fingerprintAuthTimeData.authResultTimeList.get(tempIndex);
                JSONObject jSONObject2 = fingerprintAuthTimeData.authResultTimeJsonList.get(tempIndex);
                String tempAuthResultTimeJsonKey = fingerprintAuthTimeData.authResultTimeJsonKeyList.get(tempIndex);
                int secondTempIndex = 0;
                while (secondTempIndex < tempAuthResultTimeRecord.screenStatusList.size()) {
                    try {
                        screenStatusTimeCollect tempScreenTimeCollect = tempAuthResultTimeRecord.screenStatusList.get(secondTempIndex);
                        JSONObject tempScreenStatusJson = tempAuthResultTimeRecord.screenStatusJsonList.get(secondTempIndex);
                        String tempScreenStatusJsonKey = (String) tempAuthResultTimeRecord.screenStatusJsonKeyList.get(secondTempIndex);
                        int thirdTempIndex = 0;
                        while (thirdTempIndex < tempScreenTimeCollect.lightStatusList.size()) {
                            timeCollect tempTimeCollect = tempScreenTimeCollect.lightStatusList.get(thirdTempIndex);
                            JSONObject tempLightStatusJson = tempScreenTimeCollect.lightStatusJsonList.get(thirdTempIndex);
                            String tempLightStatusJsonKey = tempScreenTimeCollect.lightStatusJsonKeyList.get(thirdTempIndex);
                            tempLightStatusJson.put("total_auth_count", tempTimeCollect.totalAuthCount);
                            str = str2;
                            try {
                                tempLightStatusJson.put("total_auth_time", tempTimeCollect.totalAuthTime);
                                tempLightStatusJson.put("total_down_to_capture_time", tempTimeCollect.totalFingerDownToCaptureTime);
                                tempLightStatusJson.put("total_capture_to_result_time", tempTimeCollect.totalCaptureToAuthResultTime);
                                tempLightStatusJson.put("avg_auth_time", tempTimeCollect.avgAuthTime);
                                tempLightStatusJson.put("avg_down_to_capture_time", tempTimeCollect.avgFingerDownToCaptureTime);
                                tempLightStatusJson.put("avg_capture_to_result_time", tempTimeCollect.avgCaptureToAuthResultTime);
                                tempScreenStatusJson.put(tempLightStatusJsonKey, tempLightStatusJson);
                                thirdTempIndex++;
                                tempAuthResultTimeRecord = tempAuthResultTimeRecord;
                                str2 = str;
                            } catch (JSONException e) {
                                e = e;
                                Slog.e(str, "updateAuthTimeJson exception", e);
                                return false;
                            }
                        }
                        String str3 = str2;
                        authTimeRecord tempAuthResultTimeRecord2 = tempAuthResultTimeRecord;
                        jSONObject2.put(tempScreenStatusJsonKey, tempScreenStatusJson);
                        secondTempIndex++;
                        tempAuthResultTimeRecord = tempAuthResultTimeRecord2;
                        str2 = str3;
                    } catch (JSONException e2) {
                        e = e2;
                        str = str2;
                    }
                }
                str = str2;
                try {
                    jSONObject.put(tempAuthResultTimeJsonKey, jSONObject2);
                    tempIndex++;
                    fingerprintAuthTimeData = this;
                    str2 = str;
                } catch (JSONException e3) {
                    e = e3;
                    Slog.e(str, "updateAuthTimeJson exception", e);
                    return false;
                }
            } catch (JSONException e4) {
                e = e4;
                str = str2;
            }
        }
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateJsonToData(JSONObject dataInfoJson) {
        Slog.i(TAG, "updateJsonToData");
        for (int tempIndex = 0; tempIndex < this.authResultTimeList.size(); tempIndex++) {
            try {
                authTimeRecord tempAuthResultTimeRecord = this.authResultTimeList.get(tempIndex);
                JSONObject tempAuthResultTimeJson = dataInfoJson.getJSONObject(this.authResultTimeJsonKeyList.get(tempIndex));
                for (int secondTempIndex = 0; secondTempIndex < tempAuthResultTimeRecord.screenStatusJsonKeyList.size(); secondTempIndex++) {
                    screenStatusTimeCollect tempScreenTimeCollect = tempAuthResultTimeRecord.screenStatusList.get(secondTempIndex);
                    JSONObject tempScreenStatusTimeJson = tempAuthResultTimeJson.getJSONObject((String) tempAuthResultTimeRecord.screenStatusJsonKeyList.get(secondTempIndex));
                    for (int thirdTempIndex = 0; thirdTempIndex < tempScreenTimeCollect.lightStatusList.size(); thirdTempIndex++) {
                        timeCollect tempTimeCollect = tempScreenTimeCollect.lightStatusList.get(thirdTempIndex);
                        JSONObject tempLightStatusJson = tempScreenStatusTimeJson.getJSONObject(tempScreenTimeCollect.lightStatusJsonKeyList.get(thirdTempIndex));
                        tempTimeCollect.totalAuthCount = tempLightStatusJson.getInt("total_auth_count");
                        tempTimeCollect.totalAuthTime = tempLightStatusJson.getInt("total_auth_time");
                        tempTimeCollect.totalFingerDownToCaptureTime = tempLightStatusJson.getInt("total_down_to_capture_time");
                        tempTimeCollect.totalCaptureToAuthResultTime = tempLightStatusJson.getInt("total_capture_to_result_time");
                        tempTimeCollect.avgAuthTime = tempLightStatusJson.getInt("avg_auth_time");
                        tempTimeCollect.avgFingerDownToCaptureTime = tempLightStatusJson.getInt("avg_down_to_capture_time");
                        tempTimeCollect.avgCaptureToAuthResultTime = tempLightStatusJson.getInt("avg_capture_to_result_time");
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                Slog.e(TAG, "updateJsonToData IndexOutOfBoundsException", e);
                resetLocalInfo();
                return false;
            } catch (JSONException e2) {
                Slog.e(TAG, "updateJsonToData exception", e2);
                resetLocalInfo();
                return false;
            }
        }
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public void resetLocalInfo() {
        Slog.w(TAG, "resetLocalInfo");
        for (authTimeRecord object : this.authResultTimeList) {
            object.reset();
        }
    }
}
