package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.hardware.fingerprint.HalDataCmdResult;
import android.hardware.fingerprint.MiFxTunnelAidl;
import android.os.SystemProperties;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintHalAuthData extends FingerprintBigData {
    private static final String TAG = "FingerprintHalAuthData";
    private static volatile FingerprintHalAuthData sInstance;
    private HalDataCmdResult halAuthData;
    private JSONObject unlockHalInfoJson = new JSONObject();
    private int[] retryCountArray = new int[4];
    private int[] qualityScoreSuccCountArray = new int[11];
    private int[] weightScoreSuccCountArray = new int[11];
    private int[] qualityScoreFailCountArray = new int[11];
    private int[] weightScoreFailCountArray = new int[11];
    private List<String> UnlockHalInfoKeyList = Arrays.asList("quality_score_succ_count", "quality_score_fail_count", "weight_score_succ_count", "weight_score_fail_count", "retry_count_arr");
    private List<int[]> UnlockHalInfoValueList = new ArrayList(Arrays.asList(this.qualityScoreSuccCountArray, this.qualityScoreFailCountArray, this.weightScoreSuccCountArray, this.weightScoreFailCountArray, this.retryCountArray));
    private ConsecutiveUnlockFailUnit consecutiveUnlockFail = new ConsecutiveUnlockFailUnit();
    private JSONArray consecutiveFailJsonArray = new JSONArray();

    private FingerprintHalAuthData() {
        Arrays.fill(this.qualityScoreSuccCountArray, 0);
        Arrays.fill(this.weightScoreSuccCountArray, 0);
        Arrays.fill(this.qualityScoreFailCountArray, 0);
        Arrays.fill(this.weightScoreFailCountArray, 0);
        Arrays.fill(this.retryCountArray, 0);
    }

    public void resetLocalFpInfo() {
        Arrays.fill(this.qualityScoreSuccCountArray, 0);
        Arrays.fill(this.weightScoreSuccCountArray, 0);
        Arrays.fill(this.qualityScoreFailCountArray, 0);
        Arrays.fill(this.weightScoreFailCountArray, 0);
        Arrays.fill(this.retryCountArray, 0);
    }

    public static FingerprintHalAuthData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintHalAuthData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintHalAuthData();
                }
            }
        }
        return sInstance;
    }

    private boolean getAuthDataFromHal() {
        try {
            Slog.d(TAG, "getAuthDataFromHal");
            HalDataCmdResult halData = MiFxTunnelAidl.getInstance().getHalData(FingerprintBigData.HAL_DATA_CMD_GET_INFO_AUTH, null);
            this.halAuthData = halData;
            if (halData.mResultCode != 0) {
                Slog.e(TAG, "Get halAuthData null");
                return false;
            }
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "Get halAuthData error.", e);
            return false;
        }
    }

    private boolean parseAndInsertHalInfo(HalDataCmdResult halData) {
        if (halData == null) {
            return false;
        }
        String fpType = SystemProperties.get("persist.vendor.sys.fp.vendor");
        Slog.d(TAG, "parseAndInsertHalInfo, fpType:" + fpType);
        if (fpType == null || fpType == "") {
            return false;
        }
        setHalData(halData.mResultData);
        if (fpType.equals("goodix_fod") || fpType.equals("goodix_fod6") || fpType.equals("goodix_fod7") || fpType.equals("goodix_fod7s")) {
            String bigdataType = SystemProperties.get("persist.vendor.sys.fp.bigdata.type");
            Slog.d(TAG, "parseAndInsertHalInfo, bigdataType:" + bigdataType);
            if (bigdataType == null || bigdataType == "") {
                return false;
            }
            setParseIndex(0);
            parseGoodixFodInfo(bigdataType);
            return true;
        }
        if (fpType.equals("goodix")) {
            setParseIndex(0);
            parseGoodixInfo();
            return true;
        }
        if (fpType.equals("fpc_fod")) {
            setParseIndex(0);
            parseFpcFodInfo();
            return true;
        }
        if (fpType.equals("fpc")) {
            setParseIndex(0);
            parseFpcInfo();
            return true;
        }
        if (fpType.equals("jiiov")) {
            setParseIndex(0);
            parseJiiovInfo();
            return true;
        }
        Slog.d(TAG, "unknown sensor type: " + fpType);
        return false;
    }

    private void parseGoodixFodInfo(String bigdataType) {
        if (bigdataType.equals("goodix_fod")) {
            Slog.d(TAG, "G3S");
            addLocalInfo("auth_result", Integer.valueOf(getHalDataInt()));
            Integer[] fail_reason_retry = {Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt())};
            addLocalInfo("fail_reason_retry0", fail_reason_retry[0]);
            addLocalInfo("fail_reason_retry1", fail_reason_retry[1]);
            addLocalInfo("fail_reason_retry2", fail_reason_retry[2]);
            addLocalInfo("quality_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("match_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("img_area", Integer.valueOf(getHalDataInt()));
            addLocalInfo("touch_diff", Integer.valueOf(getHalDataInt()));
            addLocalInfo("retry_count", Integer.valueOf(getHalDataInt()));
            addLocalInfo("screen_protector_type", Integer.valueOf(getHalDataInt()));
            Integer[] kpi_time_all = {Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt())};
            addLocalInfo("kpi_time_all0", kpi_time_all[0]);
            addLocalInfo("kpi_time_all1", kpi_time_all[1]);
            addLocalInfo("kpi_time_all2", kpi_time_all[2]);
            addLocalInfo("highlight_flag", Integer.valueOf(getHalDataInt()));
            return;
        }
        if (bigdataType.equals("goodix_fod3u")) {
            Slog.d(TAG, "G3U");
            addLocalInfo("auth_result", Integer.valueOf(getHalDataInt()));
            addLocalInfo("quality_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("match_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("img_area", Integer.valueOf(getHalDataInt()));
            addLocalInfo("retry_count", Integer.valueOf(getHalDataInt()));
            return;
        }
        if (bigdataType.equals("goodix_fod7") || bigdataType.equals("goodix_fod7s")) {
            Slog.d(TAG, "goodix_fod7");
            addLocalInfo("auth_result", Integer.valueOf(getHalDataInt()));
            addLocalInfo("weight_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("quality_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("match_score", Integer.valueOf(getHalDataInt()));
            addLocalInfo("img_area", Integer.valueOf(getHalDataInt()));
            addLocalInfo("retry_count", Integer.valueOf(getHalDataInt()));
            Integer[] fail_reason_retry2 = {Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt()), Integer.valueOf(getHalDataInt())};
            addLocalInfo("fail_reason_retry0", fail_reason_retry2[0]);
            addLocalInfo("fail_reason_retry1", fail_reason_retry2[1]);
            addLocalInfo("fail_reason_retry2", fail_reason_retry2[2]);
        }
    }

    private void parseGoodixInfo() {
    }

    private void parseFpcFodInfo() {
    }

    private void parseFpcInfo() {
    }

    private void parseJiiovInfo() {
        Slog.i(TAG, "parseJiiovInfo");
        addLocalInfo("image_name", getHalDataString(255));
        int auth_result = 0;
        if (getHalDataInt() == 0) {
            auth_result = 1;
        }
        addLocalInfo("auth_result", auth_result);
        addLocalInfo("algo_version", getHalDataString(32));
        addLocalInfo("project_version", getHalDataString(32));
        addLocalInfo("chip_id", getHalDataString(32));
        addLocalInfo("finger_quality_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compare_cache_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("seg_feat_mean", Integer.valueOf(getHalDataInt()));
        addLocalInfo("quality_score", Integer.valueOf(getHalDataInt() / 10));
        addLocalInfo("fp_direction", Integer.valueOf(getHalDataInt()));
        addLocalInfo("retry_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("finger_light_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("is_studied", Integer.valueOf(getHalDataInt()));
        addLocalInfo("enrolled_template_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("fp_protector_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("fp_temperature_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("finger_live_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("matched_template_idx", Integer.valueOf(getHalDataInt()));
        addLocalInfo("match_score", Integer.valueOf(getHalDataInt() / 10));
        addLocalInfo("img_variance", Integer.valueOf(getHalDataInt()));
        addLocalInfo("img_contrast", Integer.valueOf(getHalDataInt()));
        addLocalInfo("finger_strange_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("debase_quality_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("debase_has_pattern", Integer.valueOf(getHalDataInt()));
        addLocalInfo("matched_image_sum", Integer.valueOf(getHalDataInt()));
        addLocalInfo("matched_user_id", Integer.valueOf(getHalDataInt()));
        addLocalInfo("matched_finger_id", Integer.valueOf(getHalDataInt()));
        addLocalInfo("mat_s", Integer.valueOf(getHalDataInt()));
        addLocalInfo("mat_d", Integer.valueOf(getHalDataInt()));
        addLocalInfo("mat_c", Integer.valueOf(getHalDataInt()));
        addLocalInfo("mat_cs", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_cls_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_tnum_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_area_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_island_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_image_pixel_1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_image_pixel_2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("verify_index", Integer.valueOf(getHalDataInt()));
        addLocalInfo("env_light", Integer.valueOf(getHalDataInt()));
        addLocalInfo("is_abnormal_expo", Integer.valueOf(getHalDataInt()));
        addLocalInfo("fusion_cnt", Integer.valueOf(getHalDataInt()));
        addLocalInfo("expo_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("ghost_cache_behavior", Integer.valueOf(getHalDataInt()));
        addLocalInfo("dynamic_liveness_th", Integer.valueOf(getHalDataInt()));
        addLocalInfo("hbm_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("keyghost_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("ctnghost_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("small_cls_fast_reject_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("small_cls_fast_accept_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("tnum_fast_accept_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("glb_fast_reject_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("lf_fast_reject_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("total_cmp_cls_times", Integer.valueOf(getHalDataInt()));
        addLocalInfo("total_cache_count", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_seg_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("algo_internal_failed_reason", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_double_ghost_score", Integer.valueOf(getHalDataInt()));
        addLocalInfo("ta_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("study_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("signal", Integer.valueOf(getHalDataInt()));
        addLocalInfo("noise", Integer.valueOf(getHalDataInt()));
        addLocalInfo("hi_freq", Integer.valueOf(getHalDataInt()));
        addLocalInfo("screen_leak_ratio", Integer.valueOf(getHalDataInt()));
        addLocalInfo("fov", Integer.valueOf(getHalDataInt()));
        addLocalInfo("shading", Integer.valueOf(getHalDataInt()));
        addLocalInfo("cal_expo_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("magnification", Integer.valueOf(getHalDataInt()));
        addLocalInfo("algo_status0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("raw_img_sum0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_image_pixel0_3", Integer.valueOf(getHalDataInt()));
        addLocalInfo("capture_time0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("extract_time0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("verify_time0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("kpi_time0", Integer.valueOf(getHalDataInt()));
        addLocalInfo("algo_status1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("raw_img_sum1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_image_pixel1_3", Integer.valueOf(getHalDataInt()));
        addLocalInfo("capture_time1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("extract_time1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("verify_time1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("kpi_time1", Integer.valueOf(getHalDataInt()));
        addLocalInfo("algo_status2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("raw_img_sum2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("compress_image_pixel2_3", Integer.valueOf(getHalDataInt()));
        addLocalInfo("capture_time2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("extract_time2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("verify_time2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("kpi_time2", Integer.valueOf(getHalDataInt()));
        addLocalInfo("finger_up_down_time", Integer.valueOf(getHalDataInt()));
        addLocalInfo("screen_off", Integer.valueOf(getHalDataInt()));
        addLocalInfo("finger_status", Integer.valueOf(getHalDataInt()));
        addLocalInfo("image_special_data", Integer.valueOf(getHalDataInt()));
        addLocalInfo("area_ratio", Integer.valueOf(getHalDataInt()));
    }

    private void imageQualityDistribution() {
        Slog.d(TAG, "auth_result:" + getLocalInfo("auth_result") + " ,quality_score: " + getLocalInfo("quality_score"));
        if (getLocalInfo("auth_result") == null || getLocalInfo("quality_score") == null) {
            Slog.i(TAG, "auth_result or quality_score is null");
            return;
        }
        Integer auth_result = Integer.valueOf(getLocalInfo("auth_result").toString());
        Integer quality_score = Integer.valueOf(getLocalInfo("quality_score").toString());
        if (auth_result.intValue() == 1 && quality_score.intValue() >= 0 && quality_score.intValue() < 110) {
            int[] iArr = this.qualityScoreSuccCountArray;
            int intValue = quality_score.intValue() / 10;
            iArr[intValue] = iArr[intValue] + 1;
        } else {
            if (auth_result.intValue() == 0 && quality_score.intValue() >= 0 && quality_score.intValue() < 110) {
                int[] iArr2 = this.qualityScoreFailCountArray;
                int intValue2 = quality_score.intValue() / 10;
                iArr2[intValue2] = iArr2[intValue2] + 1;
                return;
            }
            Slog.d(TAG, "quality_score is illegal");
        }
    }

    private void weightScoreDistribution() {
        Slog.d(TAG, "auth_result:" + getLocalInfo("auth_result") + " ,weight_score: " + getLocalInfo("weight_score"));
        if (getLocalInfo("auth_result") == null || getLocalInfo("weight_score") == null) {
            Slog.d(TAG, "auth_result or weight_score is null");
            return;
        }
        Integer auth_result = Integer.valueOf(getLocalInfo("auth_result").toString());
        Integer weight_score = Integer.valueOf(getLocalInfo("weight_score").toString());
        if (auth_result.intValue() == 1 && weight_score.intValue() >= 0 && weight_score.intValue() < 110) {
            int[] iArr = this.weightScoreSuccCountArray;
            int intValue = weight_score.intValue() / 10;
            iArr[intValue] = iArr[intValue] + 1;
        } else {
            if (auth_result.intValue() == 0 && weight_score.intValue() >= 0 && weight_score.intValue() < 110) {
                int[] iArr2 = this.weightScoreFailCountArray;
                int intValue2 = weight_score.intValue() / 10;
                iArr2[intValue2] = iArr2[intValue2] + 1;
                return;
            }
            Slog.d(TAG, "weight_score is illegal");
        }
    }

    private void ratioOfRetry() {
        Slog.d(TAG, "ratioOfRetry: " + getLocalInfo("retry_count"));
        if (getLocalInfo("auth_result") == null || getLocalInfo("retry_count") == null) {
            Slog.i(TAG, "auth_result or retry_count is null");
            return;
        }
        Integer auth_result = Integer.valueOf(getLocalInfo("auth_result").toString());
        Integer retry_count = Integer.valueOf(getLocalInfo("retry_count").toString());
        if (retry_count.intValue() == 0) {
            if (auth_result.intValue() == 1) {
                int[] iArr = this.retryCountArray;
                iArr[1] = iArr[1] + 1;
            }
            int[] iArr2 = this.retryCountArray;
            iArr2[0] = iArr2[0] + 1;
            return;
        }
        if (retry_count.intValue() == 1) {
            if (auth_result.intValue() == 1) {
                int[] iArr3 = this.retryCountArray;
                iArr3[3] = iArr3[3] + 1;
            }
            int[] iArr4 = this.retryCountArray;
            iArr4[0] = iArr4[0] + 1;
            iArr4[2] = iArr4[2] + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ConsecutiveUnlockFailUnit {
        public JSONObject consecutiveFailUnitJson;
        public int consecutiveFaileCount;
        public int dayCount;
        public String endFailTime;
        public int[] failedMatchScore;
        public int[] failedQualityScore;
        public int[] retry0Reason;
        public int[] retry1Reason;
        public String startFailTime;

        private ConsecutiveUnlockFailUnit() {
            this.dayCount = 0;
            this.consecutiveFaileCount = 0;
            this.startFailTime = "";
            this.endFailTime = "";
            this.retry0Reason = new int[6];
            this.retry1Reason = new int[6];
            this.failedQualityScore = new int[6];
            this.failedMatchScore = new int[6];
            this.consecutiveFailUnitJson = new JSONObject();
        }

        public JSONObject consecutiveFailToJson() {
            Slog.d(FingerprintHalAuthData.TAG, "dayCount: " + this.dayCount + " ,consecutiveFaileCount: " + this.consecutiveFaileCount);
            try {
                this.consecutiveFailUnitJson.put("day_count", String.valueOf(this.dayCount));
                this.consecutiveFailUnitJson.put("consecutive_faile_count", String.valueOf(this.consecutiveFaileCount));
                this.consecutiveFailUnitJson.put("start_fail_time", this.startFailTime);
                this.consecutiveFailUnitJson.put("end_fail_time", this.endFailTime);
                this.consecutiveFailUnitJson.put("retry0_reason_array", Arrays.toString(this.retry0Reason));
                this.consecutiveFailUnitJson.put("retry1_reason_array", Arrays.toString(this.retry1Reason));
                this.consecutiveFailUnitJson.put("faile_quality_array", Arrays.toString(this.failedQualityScore));
                this.consecutiveFailUnitJson.put("faile_match_array", Arrays.toString(this.failedMatchScore));
                return this.consecutiveFailUnitJson;
            } catch (JSONException e) {
                Slog.d(FingerprintHalAuthData.TAG, "consecutiveFailToJson :JSONException " + e);
                reset();
                return null;
            }
        }

        public void printConsecutiveUnlockFailUnit() {
            Slog.d(FingerprintHalAuthData.TAG, "printConsecutive, dayCount: " + this.dayCount + " ,consecutiveFaileCount: " + this.consecutiveFaileCount);
            for (int i = 0; i < this.retry0Reason.length; i++) {
                Slog.d(FingerprintHalAuthData.TAG, "printConsecutive, retry0Reason: " + i + " : " + this.retry0Reason[i]);
            }
            for (int i2 = 0; i2 < this.retry1Reason.length; i2++) {
                Slog.d(FingerprintHalAuthData.TAG, "printConsecutive, retry1Reason: " + i2 + " : " + this.retry1Reason[i2]);
            }
            for (int i3 = 0; i3 < this.failedQualityScore.length; i3++) {
                Slog.d(FingerprintHalAuthData.TAG, "printConsecutive, failedQualityScore: " + i3 + " : " + this.failedQualityScore[i3]);
            }
            for (int i4 = 0; i4 < this.failedMatchScore.length; i4++) {
                Slog.d(FingerprintHalAuthData.TAG, "printConsecutive, failedMatchScore: " + i4 + " : " + this.failedMatchScore[i4]);
            }
        }

        public void reset() {
            Slog.d(FingerprintHalAuthData.TAG, "reset");
            this.consecutiveFaileCount = 0;
            this.startFailTime = "";
            this.endFailTime = "";
            Arrays.fill(this.retry0Reason, 0);
            Arrays.fill(this.retry1Reason, 0);
            Arrays.fill(this.failedQualityScore, 0);
            Arrays.fill(this.failedMatchScore, 0);
            this.consecutiveFailUnitJson = new JSONObject();
        }
    }

    private void consecutiveUnlockFailDistribution() {
        if (getLocalInfo("auth_result") == null || getLocalInfo("quality_score") == null || getLocalInfo("match_score") == null || getLocalInfo("fail_reason_retry0") == null || getLocalInfo("fail_reason_retry1") == null) {
            Slog.d(TAG, "auth_result or quality_score or match_score or fail_reason_retry0 is null");
            return;
        }
        Integer auth_result = Integer.valueOf(getLocalInfo("auth_result").toString());
        Integer fail_reason_retry0 = Integer.valueOf(getLocalInfo("fail_reason_retry0").toString());
        Integer fail_reason_retry1 = Integer.valueOf(getLocalInfo("fail_reason_retry1").toString());
        Integer quality_score = Integer.valueOf(getLocalInfo("quality_score").toString());
        Integer match_score = Integer.valueOf(getLocalInfo("match_score").toString());
        int consecutiveFaileCount = this.consecutiveUnlockFail.consecutiveFaileCount;
        if (auth_result.intValue() != 1 && consecutiveFaileCount < 5) {
            if (this.consecutiveUnlockFail.consecutiveFaileCount == 0) {
                this.consecutiveUnlockFail.startFailTime = currentTime();
            }
            this.consecutiveUnlockFail.retry0Reason[consecutiveFaileCount] = fail_reason_retry0.intValue();
            this.consecutiveUnlockFail.retry1Reason[consecutiveFaileCount] = fail_reason_retry1.intValue();
            this.consecutiveUnlockFail.failedQualityScore[consecutiveFaileCount] = quality_score.intValue();
            this.consecutiveUnlockFail.failedMatchScore[consecutiveFaileCount] = match_score.intValue();
            this.consecutiveUnlockFail.consecutiveFaileCount++;
            Slog.d(TAG, "consecutiveUnlockFailDistribution, consecutiveFaileCount++: " + this.consecutiveUnlockFail.consecutiveFaileCount);
            return;
        }
        if ((auth_result.intValue() != 1 && consecutiveFaileCount >= 5) || (auth_result.intValue() == 1 && consecutiveFaileCount >= 3)) {
            this.consecutiveUnlockFail.endFailTime = currentTime();
            this.consecutiveUnlockFail.dayCount++;
            try {
                String str = "times_unlock_fail" + String.valueOf(this.consecutiveUnlockFail.dayCount);
                JSONObject temp = this.consecutiveUnlockFail.consecutiveFailToJson();
                if (this.consecutiveFailJsonArray == null) {
                    Slog.i(TAG, "consecutiveUnlockFailDistribution: consecutiveFailJsonArray == null ");
                    this.consecutiveFailJsonArray = new JSONArray();
                }
                if (temp != null && this.consecutiveUnlockFail.dayCount > 0) {
                    this.consecutiveFailJsonArray.put(this.consecutiveUnlockFail.dayCount - 1, temp);
                }
            } catch (JSONException e) {
                Slog.d(TAG, "consecutiveUnlockFailDistribution :JSONException " + e);
            }
            this.consecutiveUnlockFail.reset();
            return;
        }
        this.consecutiveUnlockFail.reset();
    }

    public boolean calculateHalAuthInfo() {
        if (!getAuthDataFromHal()) {
            return false;
        }
        if (!parseAndInsertHalInfo(this.halAuthData)) {
            clearLocalInfo();
            return false;
        }
        imageQualityDistribution();
        weightScoreDistribution();
        ratioOfRetry();
        consecutiveUnlockFailDistribution();
        clearLocalInfo();
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateJsonToData(JSONObject dataInfoJson) {
        for (int index = 0; index < this.UnlockHalInfoKeyList.size(); index++) {
            try {
                String tempString = dataInfoJson.getString(this.UnlockHalInfoKeyList.get(index));
                Slog.w(TAG, "updateJsonToData: tempString: " + tempString);
                if (!tempString.isEmpty()) {
                    String tempString2 = tempString.replace("[", "").replace("]", "").replace(" ", "");
                    Slog.i(TAG, "updateJsonToData: new tempString: " + tempString2);
                    int secondTempIndex = 0;
                    for (String splitstring : tempString2.split(",")) {
                        if (!splitstring.isEmpty() && secondTempIndex < this.UnlockHalInfoValueList.get(index).length) {
                            this.UnlockHalInfoValueList.get(index)[secondTempIndex] = Integer.parseInt(splitstring);
                            secondTempIndex++;
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
            } catch (Exception e3) {
                Slog.e(TAG, "updateJsonToData Exception", e3);
                return false;
            }
        }
        this.consecutiveFailJsonArray = dataInfoJson.getJSONArray("consecutive_fail_arr");
        return true;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateDataToJson(JSONObject dataInfoJson) {
        Slog.d(TAG, "updateDataToJson");
        if (sInstance == null) {
            sInstance = getInstance();
        }
        if (dataInfoJson == null) {
            dataInfoJson = new JSONObject();
        }
        try {
            dataInfoJson.put("quality_score_succ_count", Arrays.toString(this.qualityScoreSuccCountArray));
            dataInfoJson.put("quality_score_fail_count", Arrays.toString(this.qualityScoreFailCountArray));
            dataInfoJson.put("weight_score_succ_count", Arrays.toString(this.weightScoreSuccCountArray));
            dataInfoJson.put("weight_score_fail_count", Arrays.toString(this.weightScoreFailCountArray));
            dataInfoJson.put("retry_count_arr", Arrays.toString(this.retryCountArray));
            JSONArray jSONArray = this.consecutiveFailJsonArray;
            if (jSONArray != null) {
                dataInfoJson.put("consecutive_fail_arr", jSONArray);
                return true;
            }
            return true;
        } catch (JSONException e) {
            Slog.e(TAG, "updateDataToJson JSONException", e);
            return false;
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public void resetLocalInfo() {
        Slog.d(TAG, "resetLocalInfo");
        Arrays.fill(this.qualityScoreSuccCountArray, 0);
        Arrays.fill(this.weightScoreSuccCountArray, 0);
        Arrays.fill(this.qualityScoreFailCountArray, 0);
        Arrays.fill(this.weightScoreFailCountArray, 0);
        Arrays.fill(this.retryCountArray, 0);
        this.consecutiveUnlockFail.dayCount = 0;
        this.consecutiveUnlockFail.reset();
        this.consecutiveFailJsonArray = new JSONArray();
    }
}
