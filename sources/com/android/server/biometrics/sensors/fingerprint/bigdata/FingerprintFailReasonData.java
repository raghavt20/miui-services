package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintFailReasonData extends FingerprintBigData {
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_ANTI_SPOOF = 31;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_DRY_FINGER = 32;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_DUPLICATE_AREA = 25;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_DUPLICATE_FINGER = 26;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_ENROLL_ERROR = 47;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_FIXED_PATTERN = 57;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_HBM_TOO_SLOW = 40;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_INPUT_TOO_LONG = 24;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_INVALID_DATA = 48;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LATENT_CLASSIFIER = 30;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOGO = 34;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOW_BRIGHTNESS = 33;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOW_CONTRAST = 56;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOW_MOBILITY = 49;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOW_OVERLAP = 54;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_LOW_SIMILARITY = 55;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_NOT_MATCH = 44;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_OVER_EXPOSURE = 53;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_QUICK_CLICK = 29;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_SHOW_CIRCLE = 46;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_SIMULATED_FINGER = 27;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_SPI_COMMUNICATION = 41;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_SUSPICIOUS_BAD_QUALITY = 52;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_SUSPICIOUS_LOW_QUALITY = 51;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_TOO_DIM = 43;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_TOUCH_BY_MISTAKE = 28;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_UI_DISAPPEAR = 45;
    private static final int CUSTOMIZED_FINGERPRINT_ACQUIRED_WET_FINGER = 42;
    private static final String TAG = "FingerprintFailReasonData";
    private static volatile FingerprintFailReasonData sInstance;
    private List<String> mFailReasonCodeList = Arrays.asList("1-0", "2-0", "3-0", "4-0", "5-0", "6-24", "6-25", "6-26", "6-27", "6-28", "6-29", "6-30", "6-31", "6-32", "6-33", "6-34", "6-40", "6-41", "6-42", "6-43", "6-44", "6-45", "6-46", "6-47", "6-48", "6-49", "6-51", "6-52", "6-53", "6-54", "6-55", "6-56", "6-57");
    private List<String> mFailReasonInfoList = Arrays.asList("fail_reason_partial_cnt", "fail_reason_insufficient_cnt", "fail_reason_img_dirty_cnt", "fail_reason_too_slow_cnt", "fail_reason_finger_leave_too_fast_cnt", "fail_reason_input_too_long_cnt", "fail_reason_duplicate_area_cnt", "fail_reason_duplicate_finger_cnt", "fail_reason_simulated_finger_cnt", "fail_reason_touch_by_mistake_cnt", "fail_reason_suspicious_cmn_cnt", "fail_reason_latent_classifier_cnt", "fail_reason_anti_spoof_cnt", "fail_reason_dry_finger_cnt", "fail_reason_low_brightness_cnt", "fail_reason_logo_cnt", "fail_reason_HBM_too_slow_cnt", "fail_reason_SPI_communication_cnt", "fail_reason_wet_finger_cnt", "fail_reason_too_dim_cnt", "fail_reason_not_match_cnt", "fail_reason_UI_disappear_cnt", "fail_reason_show_circle_cnt", "fail_reason_enroll_error_cnt", "fail_reason_invalid_data_cnt", "fail_reason_low_mobility_cnt", "fail_reason_suspicious_low_quality_cnt", "fail_reason_suspicious_bad_quality_cnt", "fail_reason_over_exposure_cnt", "fail_reason_low_overlap_cnt", "fail_reason_low_similarity_cnt", "fail_reason_low_contrast_cnt", "fail_reason_fixed_pattern_cnt");
    private List<Integer> failReasonStatisticList = new ArrayList(Collections.nCopies(this.mFailReasonInfoList.size(), 0));
    private List<Integer> screenOnFailReasonStatisticList = new ArrayList(Collections.nCopies(this.mFailReasonInfoList.size(), 0));
    private List<Integer> screenOffFailReasonStatisticList = new ArrayList(Collections.nCopies(this.mFailReasonInfoList.size(), 0));
    private List<Integer> hbmFailReasonStatisticList = new ArrayList(Collections.nCopies(this.mFailReasonInfoList.size(), 0));
    private List<Integer> lowlightFailReasonStatisticList = new ArrayList(Collections.nCopies(this.mFailReasonInfoList.size(), 0));
    private JSONObject failReasonJson = new JSONObject();
    private JSONArray failReasonArray = new JSONArray();
    private JSONArray screenOnFailReasonArray = new JSONArray();
    private JSONArray screenOffFailReasonArray = new JSONArray();
    private JSONArray hbmFailReasonArray = new JSONArray();
    private JSONArray lowlightFailReasonArray = new JSONArray();
    private List<List<Integer>> failReasonRecordList = new ArrayList(Arrays.asList(this.failReasonStatisticList, this.screenOnFailReasonStatisticList, this.screenOffFailReasonStatisticList, this.hbmFailReasonStatisticList, this.lowlightFailReasonStatisticList));
    private List<JSONArray> failReasonJsonArrayList = new ArrayList(Arrays.asList(this.failReasonArray, this.screenOnFailReasonArray, this.screenOffFailReasonArray, this.hbmFailReasonArray, this.lowlightFailReasonArray));
    private List<String> failReasonJsonKeyList = Arrays.asList("fail_reason", "screen_on_fail_reason", "screen_off_fail_reason", "hbm_fail_reason", "lowlight_fail_reason");

    public static FingerprintFailReasonData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintFailReasonData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintFailReasonData();
                }
            }
        }
        return sInstance;
    }

    public boolean calculateFailReasonCnt(int acquiredInfo, int vendorCode, int state) {
        Slog.w(TAG, "calculateFailReasonCnt, acquiredInfo: " + acquiredInfo + " ,vendorCode : " + vendorCode + ",screenStatus: " + state);
        String failReasonCode = String.valueOf(acquiredInfo) + '-' + String.valueOf(vendorCode);
        if (this.mFailReasonCodeList.contains(failReasonCode)) {
            Slog.w(TAG, "start calculateFailReasonCnt");
            this.failReasonStatisticList.set(this.mFailReasonCodeList.indexOf(failReasonCode), Integer.valueOf(this.failReasonStatisticList.get(this.mFailReasonCodeList.indexOf(failReasonCode)).intValue() + 1));
            this.mScreenStatus = state;
            if (this.mScreenStatus == 2) {
                this.screenOnFailReasonStatisticList.set(this.mFailReasonCodeList.indexOf(failReasonCode), Integer.valueOf(this.screenOnFailReasonStatisticList.get(this.mFailReasonCodeList.indexOf(failReasonCode)).intValue() + 1));
            } else {
                this.screenOffFailReasonStatisticList.set(this.mFailReasonCodeList.indexOf(failReasonCode), Integer.valueOf(this.screenOffFailReasonStatisticList.get(this.mFailReasonCodeList.indexOf(failReasonCode)).intValue() + 1));
            }
            if (this.mFingerUnlockBright == 1) {
                this.lowlightFailReasonStatisticList.set(this.mFailReasonCodeList.indexOf(failReasonCode), Integer.valueOf(this.lowlightFailReasonStatisticList.get(this.mFailReasonCodeList.indexOf(failReasonCode)).intValue() + 1));
            } else {
                this.hbmFailReasonStatisticList.set(this.mFailReasonCodeList.indexOf(failReasonCode), Integer.valueOf(this.hbmFailReasonStatisticList.get(this.mFailReasonCodeList.indexOf(failReasonCode)).intValue() + 1));
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateJsonToData(JSONObject dataInfoJson) {
        Slog.i(TAG, "updateJsonToData");
        for (int tempIndex = 0; tempIndex < this.failReasonJsonArrayList.size(); tempIndex++) {
            try {
                this.failReasonJsonArrayList.get(tempIndex);
                JSONArray tempJSONArray = dataInfoJson.getJSONArray(this.failReasonJsonKeyList.get(tempIndex));
                String tempString = tempJSONArray.toString();
                if (!tempString.isEmpty()) {
                    int secondTempIndex = 0;
                    for (String splitstring : tempString.replace("[", "").replace("]", "").split(",")) {
                        if (!splitstring.isEmpty() && secondTempIndex < this.failReasonRecordList.size()) {
                            int secondTempIndex2 = secondTempIndex + 1;
                            this.failReasonRecordList.get(tempIndex).set(secondTempIndex, Integer.valueOf(Integer.parseInt(splitstring)));
                            secondTempIndex = secondTempIndex2;
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                Slog.e(TAG, "updateJsonToData IndexOutOfBoundsException", e);
                resetLocalInfo();
                return false;
            } catch (JSONException e2) {
                Slog.e(TAG, "updateJsonToData JSONException", e2);
                resetLocalInfo();
                return false;
            }
        }
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateDataToJson(JSONObject dataInfoJson) {
        Slog.i(TAG, "updateDataToJson");
        if (dataInfoJson == null) {
            try {
                dataInfoJson = new JSONObject();
            } catch (JSONException e) {
                Slog.e(TAG, "updateDataToJson JSONException", e);
                return false;
            }
        }
        List<String> failReasonStringList = new ArrayList<>();
        for (List<Integer> object : this.failReasonRecordList) {
            failReasonStringList.add("[" + ((String) object.stream().map(new Function() { // from class: com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintFailReasonData$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((Integer) obj).toString();
                }
            }).collect(Collectors.joining(","))) + "]");
        }
        int tempIndex = 0 + 1;
        JSONArray jSONArray = new JSONArray(failReasonStringList.get(0));
        this.failReasonArray = jSONArray;
        dataInfoJson.put("fail_reason", jSONArray);
        int tempIndex2 = tempIndex + 1;
        JSONArray jSONArray2 = new JSONArray(failReasonStringList.get(tempIndex));
        this.screenOnFailReasonArray = jSONArray2;
        dataInfoJson.put("screen_on_fail_reason", jSONArray2);
        int tempIndex3 = tempIndex2 + 1;
        JSONArray jSONArray3 = new JSONArray(failReasonStringList.get(tempIndex2));
        this.screenOffFailReasonArray = jSONArray3;
        dataInfoJson.put("screen_off_fail_reason", jSONArray3);
        int tempIndex4 = tempIndex3 + 1;
        JSONArray jSONArray4 = new JSONArray(failReasonStringList.get(tempIndex3));
        this.hbmFailReasonArray = jSONArray4;
        dataInfoJson.put("hbm_fail_reason", jSONArray4);
        int i = tempIndex4 + 1;
        JSONArray jSONArray5 = new JSONArray(failReasonStringList.get(tempIndex4));
        this.lowlightFailReasonArray = jSONArray5;
        dataInfoJson.put("lowlight_fail_reason", jSONArray5);
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public void resetLocalInfo() {
        Slog.i(TAG, "resetLocalInfo");
        for (List<Integer> object : this.failReasonRecordList) {
            object.replaceAll(new UnaryOperator() { // from class: com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintFailReasonData$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return FingerprintFailReasonData.lambda$resetLocalInfo$0((Integer) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$resetLocalInfo$0(Integer i) {
        return 0;
    }
}
