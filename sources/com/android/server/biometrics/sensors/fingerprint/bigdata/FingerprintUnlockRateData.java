package com.android.server.biometrics.sensors.fingerprint.bigdata;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Slog;
import com.android.server.DarkModeOneTrackHelper;
import com.android.server.biometrics.log.ALSProbe;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FingerprintUnlockRateData extends FingerprintBigData {
    protected static final int HBM = 0;
    private static final String SYSTEMUI_NAME = "com.android.systemui";
    private static final String TAG = "FingerprintUnlockRateData";
    private static volatile FingerprintUnlockRateData sInstance;
    private ALSProbe mALSProbe;
    private int mAmbientLux;
    private Context mContext;
    private SensorManager sensorManager;
    private int mScreenStatus = -1;
    private int mFingerUnlockBright = 0;
    private final int LUX_LEVEL_UNDEFINED = -1;
    private final int LUX_LIGHT_LEVEL0 = 0;
    private final int LUX_LIGHT_LEVEL1 = 1;
    private final int LUX_LIGHT_LEVEL2 = 2;
    private final int LUX_LIGHT_LEVEL3 = 3;
    private final int LUX_LIGHT_LEVEL4 = 4;
    private final int LUX_LIGHT_LEVEL5 = 5;
    private final int LUX_LIGHT_LEVEL6 = 6;
    private final int LUX_LIGHT_LEVEL7 = 7;
    private final int LUX_LIGHT_LEVEL8 = 8;
    private int mProbeIsEnable = 0;
    private AmbientLuxUnlockRateStatistics systemuiLuxUnlockRate = new AmbientLuxUnlockRateStatistics("com.android.systemui");
    private AmbientLuxUnlockRateStatistics otherAppLuxUnlockRate = new AmbientLuxUnlockRateStatistics("other App");
    private UnlockRateUnit totalAuth = new UnlockRateUnit();
    private List<String> sPackageList = Arrays.asList("com.android.systemui", ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.tencent.mm", ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
    private AppUnlockRateStatistic systemuiUnlockRate = new SystemUIUnlockRateStatistics("com.android.systemui");
    private List<AppUnlockRateStatistic> sAppStatisticList = new ArrayList(Arrays.asList(this.systemuiUnlockRate, new AppUnlockRateStatistic(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME), new AppUnlockRateStatistic("com.tencent.mm"), new AppUnlockRateStatistic(ActivityStarterImpl.PACKAGE_NAME_ALIPAY)));
    private Map<String, AppUnlockRateStatistic> appAuthMap = new HashMap();
    private JSONArray appUnlockRateArray = new JSONArray();
    JSONArray luxLightSystemuiSyAuthCountArray = new JSONArray();
    JSONArray luxLightSystemuiAuthSuccCountArray = new JSONArray();
    JSONArray luxLightOtherSyAuthCountArray = new JSONArray();
    JSONArray luxLightOtherAuthSuccCountArray = new JSONArray();

    public void setContext(Context context) {
        Slog.i(TAG, "setContext");
        this.mContext = context;
        SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
        this.sensorManager = sensorManager;
        if (sensorManager != null) {
            this.mALSProbe = new ALSProbe(this.sensorManager);
        }
    }

    public void handleAcquiredInfo(int acquiredInfo, int vendorCode) {
        if (!IS_FOD) {
            Slog.d(TAG, "is not fod！");
            return;
        }
        if (vendorCode == 22 && this.mProbeIsEnable == 0) {
            Slog.d(TAG, "getMostRecentLux: finger down");
            ALSProbe aLSProbe = this.mALSProbe;
            if (aLSProbe != null) {
                aLSProbe.enable();
                this.mProbeIsEnable = 1;
            }
        }
        if (vendorCode == 23 && this.mProbeIsEnable == 1) {
            Slog.d(TAG, "getMostRecentLux:finger up ");
            ALSProbe aLSProbe2 = this.mALSProbe;
            if (aLSProbe2 != null) {
                float ambientLux = aLSProbe2.getMostRecentLux();
                Slog.d(TAG, "getMostRecentLux: " + ambientLux);
                this.mAmbientLux = (int) ambientLux;
                this.mALSProbe.disable();
                this.mProbeIsEnable = 0;
            }
        }
    }

    private int getAmbientLuxLevel() {
        int i = this.mAmbientLux;
        if (i >= 0 && i < 500) {
            return 0;
        }
        if (i < 500 || i >= 1000) {
            if (i < 1000 || i >= 2000) {
                if (i < 2000 || i >= 5000) {
                    if (i < 5000 || i >= 7000) {
                        if (i < 7000 || i >= 10000) {
                            if (i < 10000 || i >= 15000) {
                                if (i >= 15000 && i < 20000) {
                                    return 7;
                                }
                                if (i >= 20000) {
                                    return 8;
                                }
                                return -1;
                            }
                            return 6;
                        }
                        return 5;
                    }
                    return 4;
                }
                return 3;
            }
            return 2;
        }
        return 1;
    }

    public static FingerprintUnlockRateData getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            synchronized (FingerprintUnlockRateData.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance new");
                    sInstance = new FingerprintUnlockRateData();
                }
            }
        }
        return sInstance;
    }

    private FingerprintUnlockRateData() {
        for (int tempIndex = 0; tempIndex < this.sPackageList.size(); tempIndex++) {
            this.appAuthMap.put(this.sPackageList.get(tempIndex), this.sAppStatisticList.get(tempIndex));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UnlockRateUnit {
        public int authCount;
        public int authSuccessCount;

        private UnlockRateUnit() {
            this.authCount = 0;
            this.authSuccessCount = 0;
        }

        public void reset() {
            this.authCount = 0;
            this.authSuccessCount = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AppUnlockRateStatistic {
        public String name;
        UnlockRateUnit totalAuth = new UnlockRateUnit();

        AppUnlockRateStatistic() {
        }

        AppUnlockRateStatistic(String name) {
            this.name = name;
        }

        public void reset() {
            this.totalAuth.reset();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AmbientLuxUnlockRateStatistics extends AppUnlockRateStatistic {
        public UnlockRateUnit luxLightUnlock0;
        public UnlockRateUnit luxLightUnlock1;
        public UnlockRateUnit luxLightUnlock2;
        public UnlockRateUnit luxLightUnlock3;
        public UnlockRateUnit luxLightUnlock4;
        public UnlockRateUnit luxLightUnlock5;
        public UnlockRateUnit luxLightUnlock6;
        public UnlockRateUnit luxLightUnlock7;
        public UnlockRateUnit luxLightUnlock8;
        public JSONArray luxLightUnlockJsonArray = new JSONArray();
        public List<UnlockRateUnit> luxLightUnlockList;

        public void addLuxAuthCount(int luxLevel, int authen) {
            if (luxLevel >= this.luxLightUnlockList.size() || luxLevel < 0) {
                return;
            }
            this.luxLightUnlockList.get(luxLevel).authCount++;
            if (authen == 1) {
                this.luxLightUnlockList.get(luxLevel).authSuccessCount++;
            }
        }

        AmbientLuxUnlockRateStatistics(String name) {
            this.luxLightUnlock0 = new UnlockRateUnit();
            this.luxLightUnlock1 = new UnlockRateUnit();
            this.luxLightUnlock2 = new UnlockRateUnit();
            this.luxLightUnlock3 = new UnlockRateUnit();
            this.luxLightUnlock4 = new UnlockRateUnit();
            this.luxLightUnlock5 = new UnlockRateUnit();
            this.luxLightUnlock6 = new UnlockRateUnit();
            this.luxLightUnlock7 = new UnlockRateUnit();
            this.luxLightUnlock8 = new UnlockRateUnit();
            this.luxLightUnlockList = new ArrayList(Arrays.asList(this.luxLightUnlock0, this.luxLightUnlock1, this.luxLightUnlock2, this.luxLightUnlock3, this.luxLightUnlock4, this.luxLightUnlock5, this.luxLightUnlock6, this.luxLightUnlock7, this.luxLightUnlock8));
            this.name = name;
        }

        public void printAmbientLuxUnlock() {
            for (int index = 0; index < this.luxLightUnlockList.size(); index++) {
                Slog.d(FingerprintUnlockRateData.TAG, "printAmbientLuxUnlock,authCount index: " + index + ", " + this.luxLightUnlockList.get(index).authCount);
                Slog.d(FingerprintUnlockRateData.TAG, "printAmbientLuxUnlock,authSuccessCount: " + index + ", " + this.luxLightUnlockList.get(index).authSuccessCount);
            }
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintUnlockRateData.AppUnlockRateStatistic
        public void reset() {
            super.reset();
            for (UnlockRateUnit luxLightUnlock : this.luxLightUnlockList) {
                luxLightUnlock.reset();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SystemUIUnlockRateStatistics extends AppUnlockRateStatistic {
        public UnlockRateUnit highLightAuth;
        public UnlockRateUnit lowLightAuth;
        public UnlockRateUnit screenOffAuth;
        public UnlockRateUnit screenOff_highLight_Auth;
        public UnlockRateUnit screenOff_lowLight_Auth;
        public UnlockRateUnit screenOnAuth;
        public UnlockRateUnit screenOn_highLight_Auth;
        public UnlockRateUnit screenOn_lowLight_Auth;

        SystemUIUnlockRateStatistics(String name) {
            this.screenOnAuth = new UnlockRateUnit();
            this.screenOffAuth = new UnlockRateUnit();
            this.lowLightAuth = new UnlockRateUnit();
            this.highLightAuth = new UnlockRateUnit();
            this.screenOn_lowLight_Auth = new UnlockRateUnit();
            this.screenOn_highLight_Auth = new UnlockRateUnit();
            this.screenOff_lowLight_Auth = new UnlockRateUnit();
            this.screenOff_highLight_Auth = new UnlockRateUnit();
            this.name = name;
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintUnlockRateData.AppUnlockRateStatistic
        public void reset() {
            super.reset();
            this.screenOnAuth.reset();
            this.screenOffAuth.reset();
            this.lowLightAuth.reset();
            this.highLightAuth.reset();
            this.screenOn_lowLight_Auth.reset();
            this.screenOn_highLight_Auth.reset();
            this.screenOff_lowLight_Auth.reset();
            this.screenOff_highLight_Auth.reset();
        }
    }

    private void ambientLuxUnlockRate(String packName, int authen) {
        int luxLevel = getAmbientLuxLevel();
        if (luxLevel == -1) {
            return;
        }
        if (packName.equals("com.android.systemui")) {
            Slog.d(TAG, "ambientLuxUnlockRate: SystemUi ");
            this.systemuiLuxUnlockRate.addLuxAuthCount(luxLevel, authen);
        } else {
            Slog.d(TAG, "ambientLuxUnlockRate: OtherApp ");
            this.otherAppLuxUnlockRate.addLuxAuthCount(luxLevel, authen);
        }
    }

    public void appUnlockRate(String packName, int authen) {
        if (this.appAuthMap.containsKey(packName)) {
            this.appAuthMap.get(packName).totalAuth.authCount++;
            if (authen == 1) {
                this.appAuthMap.get(packName).totalAuth.authSuccessCount++;
            }
        } else {
            Slog.e(TAG, "calculateUnlockCnt: appAuthMap doesn't contains:" + packName);
        }
        if (packName.equals("com.android.systemui")) {
            SystemUIUnlockRateStatistics systemuiStatistics = (SystemUIUnlockRateStatistics) this.systemuiUnlockRate;
            if (this.mScreenStatus == 2) {
                systemuiStatistics.screenOnAuth.authCount++;
                if (this.mFingerUnlockBright == 1) {
                    systemuiStatistics.screenOn_lowLight_Auth.authCount++;
                } else {
                    systemuiStatistics.screenOn_highLight_Auth.authCount++;
                }
                if (authen == 1) {
                    systemuiStatistics.screenOnAuth.authSuccessCount++;
                    if (this.mFingerUnlockBright == 1) {
                        systemuiStatistics.screenOn_lowLight_Auth.authSuccessCount++;
                    } else {
                        systemuiStatistics.screenOn_highLight_Auth.authSuccessCount++;
                    }
                }
            } else {
                systemuiStatistics.screenOffAuth.authCount++;
                if (this.mFingerUnlockBright == 1) {
                    systemuiStatistics.screenOff_lowLight_Auth.authCount++;
                } else {
                    systemuiStatistics.screenOff_highLight_Auth.authCount++;
                }
                if (authen == 1) {
                    systemuiStatistics.screenOffAuth.authSuccessCount++;
                    if (this.mFingerUnlockBright == 1) {
                        systemuiStatistics.screenOff_lowLight_Auth.authSuccessCount++;
                    } else {
                        systemuiStatistics.screenOff_highLight_Auth.authSuccessCount++;
                    }
                }
            }
            if (this.mFingerUnlockBright == 1) {
                systemuiStatistics.lowLightAuth.authCount++;
                if (authen == 1) {
                    systemuiStatistics.lowLightAuth.authSuccessCount++;
                    return;
                }
                return;
            }
            systemuiStatistics.highLightAuth.authCount++;
            if (authen == 1) {
                systemuiStatistics.highLightAuth.authSuccessCount++;
            }
        }
    }

    public void calculateUnlockCnt(String packName, int authen, int screenState) {
        Slog.d(TAG, "calculateUnlockCnt, packName:  " + packName + ",  authen: " + authen + ",screenState: " + screenState);
        this.mScreenStatus = screenState;
        this.totalAuth.authCount++;
        if (authen == 1) {
            this.totalAuth.authSuccessCount++;
        }
        appUnlockRate(packName, authen);
        ambientLuxUnlockRate(packName, authen);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateDataToJson(JSONObject dataInfoJson) {
        JSONObject dataInfoJson2;
        String str;
        String str2 = "high_light_status";
        Slog.i(TAG, "updateDataToJson");
        if (dataInfoJson != null) {
            dataInfoJson2 = dataInfoJson;
        } else {
            try {
                dataInfoJson2 = new JSONObject();
            } catch (JSONException e) {
                e = e;
                Slog.e(TAG, "updateDataToJson exception", e);
                return false;
            }
        }
        try {
            if (this.appUnlockRateArray == null) {
                this.appUnlockRateArray = new JSONArray();
            }
            dataInfoJson2.put("total_auth_cnt", this.totalAuth.authCount);
            dataInfoJson2.put("total_auth_succ_cnt", this.totalAuth.authSuccessCount);
            int index = 0;
            while (index < this.sPackageList.size()) {
                JSONObject jSONObject = new JSONObject();
                AppUnlockRateStatistic tempUnit = this.appAuthMap.get(this.sPackageList.get(index));
                jSONObject.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_NAME, this.sPackageList.get(index));
                jSONObject.put("auth_count", tempUnit.totalAuth.authCount);
                jSONObject.put("auth_success_count", tempUnit.totalAuth.authSuccessCount);
                if (!this.sPackageList.get(index).equals("com.android.systemui")) {
                    str = str2;
                } else {
                    SystemUIUnlockRateStatistics tempSystemUIUnlockRate = (SystemUIUnlockRateStatistics) this.systemuiUnlockRate;
                    JSONObject screenOn = new JSONObject();
                    screenOn.put("auth_count", tempSystemUIUnlockRate.screenOnAuth.authCount);
                    screenOn.put("auth_success_count", tempSystemUIUnlockRate.screenOnAuth.authSuccessCount);
                    JSONObject screenOnHighLight = new JSONObject();
                    screenOnHighLight.put("auth_count", tempSystemUIUnlockRate.screenOn_highLight_Auth.authCount);
                    screenOnHighLight.put("auth_success_count", tempSystemUIUnlockRate.screenOn_highLight_Auth.authSuccessCount);
                    screenOn.put(str2, screenOnHighLight);
                    JSONObject screenOnLowLight = new JSONObject();
                    screenOnLowLight.put("auth_count", tempSystemUIUnlockRate.screenOn_lowLight_Auth.authCount);
                    screenOnLowLight.put("auth_success_count", tempSystemUIUnlockRate.screenOn_lowLight_Auth.authSuccessCount);
                    screenOn.put("low_light_status", screenOnLowLight);
                    jSONObject.put("screen_on_status", screenOn);
                    JSONObject screenOff = new JSONObject();
                    screenOff.put("auth_count", tempSystemUIUnlockRate.screenOffAuth.authCount);
                    screenOff.put("auth_success_count", tempSystemUIUnlockRate.screenOffAuth.authSuccessCount);
                    JSONObject screenOffHighLight = new JSONObject();
                    screenOffHighLight.put("auth_count", tempSystemUIUnlockRate.screenOff_highLight_Auth.authCount);
                    screenOffHighLight.put("auth_success_count", tempSystemUIUnlockRate.screenOff_highLight_Auth.authSuccessCount);
                    screenOff.put(str2, screenOffHighLight);
                    JSONObject screenOffLowLight = new JSONObject();
                    screenOffLowLight.put("auth_count", tempSystemUIUnlockRate.screenOff_lowLight_Auth.authCount);
                    screenOffLowLight.put("auth_success_count", tempSystemUIUnlockRate.screenOff_lowLight_Auth.authSuccessCount);
                    screenOff.put("low_light_status", screenOffLowLight);
                    jSONObject.put("screen_off_status", screenOff);
                    JSONObject highLight = new JSONObject();
                    highLight.put("auth_count", tempSystemUIUnlockRate.highLightAuth.authCount);
                    highLight.put("auth_success_count", tempSystemUIUnlockRate.highLightAuth.authSuccessCount);
                    jSONObject.put(str2, highLight);
                    JSONObject lowLight = new JSONObject();
                    str = str2;
                    lowLight.put("auth_count", tempSystemUIUnlockRate.lowLightAuth.authCount);
                    lowLight.put("auth_success_count", tempSystemUIUnlockRate.lowLightAuth.authSuccessCount);
                    jSONObject.put("low_light_status", lowLight);
                }
                this.appUnlockRateArray.put(index, jSONObject);
                index++;
                str2 = str;
            }
            dataInfoJson2.put("app_unlock_rate", this.appUnlockRateArray);
            for (int index2 = 0; index2 < this.otherAppLuxUnlockRate.luxLightUnlockList.size(); index2++) {
                this.luxLightSystemuiSyAuthCountArray.put(index2, this.systemuiLuxUnlockRate.luxLightUnlockList.get(index2).authCount);
                this.luxLightSystemuiAuthSuccCountArray.put(index2, this.systemuiLuxUnlockRate.luxLightUnlockList.get(index2).authSuccessCount);
                this.luxLightOtherSyAuthCountArray.put(index2, this.otherAppLuxUnlockRate.luxLightUnlockList.get(index2).authCount);
                this.luxLightOtherAuthSuccCountArray.put(index2, this.otherAppLuxUnlockRate.luxLightUnlockList.get(index2).authSuccessCount);
            }
            dataInfoJson2.put("lux_systemui_count_array", this.luxLightSystemuiSyAuthCountArray);
            dataInfoJson2.put("lux_systemui_succ_count_array", this.luxLightSystemuiAuthSuccCountArray);
            dataInfoJson2.put("lux_otherapp_count_array", this.luxLightOtherSyAuthCountArray);
            dataInfoJson2.put("lux_otherapp_succ_count_array", this.luxLightOtherAuthSuccCountArray);
            return true;
        } catch (JSONException e2) {
            e = e2;
            Slog.e(TAG, "updateDataToJson exception", e);
            return false;
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public boolean updateJsonToData(JSONObject dataInfoJson) {
        String str;
        String str2;
        String str3 = "low_light_status";
        String str4 = "high_light_status";
        try {
            this.totalAuth.authCount = dataInfoJson.getInt("total_auth_cnt");
            this.totalAuth.authSuccessCount = dataInfoJson.getInt("total_auth_succ_cnt");
            this.appUnlockRateArray = dataInfoJson.getJSONArray("app_unlock_rate");
            int tempIndex = 0;
            while (tempIndex < this.appUnlockRateArray.length()) {
                JSONObject appUnlockRateUnit = this.appUnlockRateArray.getJSONObject(tempIndex);
                AppUnlockRateStatistic tempUnit = this.sAppStatisticList.get(tempIndex);
                tempUnit.totalAuth.authCount = appUnlockRateUnit.getInt("auth_count");
                tempUnit.totalAuth.authSuccessCount = appUnlockRateUnit.getInt("auth_success_count");
                if (!appUnlockRateUnit.get(DarkModeOneTrackHelper.PARAM_VALUE_APP_NAME).equals("com.android.systemui")) {
                    str = str3;
                    str2 = str4;
                } else {
                    SystemUIUnlockRateStatistics tempSystemUIUnlockRate = (SystemUIUnlockRateStatistics) this.systemuiUnlockRate;
                    JSONObject screenOn = appUnlockRateUnit.getJSONObject("screen_on_status");
                    tempSystemUIUnlockRate.screenOnAuth.authCount = screenOn.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOnAuth.authSuccessCount = screenOn.getInt("auth_success_count");
                    JSONObject screenOnHighLight = screenOn.getJSONObject(str4);
                    tempSystemUIUnlockRate.screenOn_highLight_Auth.authCount = screenOnHighLight.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOn_highLight_Auth.authSuccessCount = screenOnHighLight.getInt("auth_success_count");
                    JSONObject screenOnLowLight = screenOn.getJSONObject(str3);
                    tempSystemUIUnlockRate.screenOn_lowLight_Auth.authCount = screenOnLowLight.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOn_lowLight_Auth.authSuccessCount = screenOnLowLight.getInt("auth_success_count");
                    JSONObject screenOff = appUnlockRateUnit.getJSONObject("screen_off_status");
                    tempSystemUIUnlockRate.screenOffAuth.authCount = screenOff.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOffAuth.authSuccessCount = screenOff.getInt("auth_success_count");
                    JSONObject screenOffHighLight = screenOff.getJSONObject(str4);
                    tempSystemUIUnlockRate.screenOff_highLight_Auth.authCount = screenOffHighLight.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOff_highLight_Auth.authSuccessCount = screenOffHighLight.getInt("auth_success_count");
                    JSONObject screenOffLowLight = screenOff.getJSONObject(str3);
                    tempSystemUIUnlockRate.screenOff_lowLight_Auth.authCount = screenOffLowLight.getInt("auth_count");
                    tempSystemUIUnlockRate.screenOff_lowLight_Auth.authSuccessCount = screenOffLowLight.getInt("auth_success_count");
                    JSONObject highLight = appUnlockRateUnit.getJSONObject(str4);
                    str2 = str4;
                    tempSystemUIUnlockRate.highLightAuth.authCount = highLight.getInt("auth_count");
                    tempSystemUIUnlockRate.highLightAuth.authSuccessCount = highLight.getInt("auth_success_count");
                    JSONObject lowLight = appUnlockRateUnit.getJSONObject(str3);
                    str = str3;
                    tempSystemUIUnlockRate.lowLightAuth.authCount = lowLight.getInt("auth_count");
                    tempSystemUIUnlockRate.lowLightAuth.authSuccessCount = lowLight.getInt("auth_success_count");
                }
                tempIndex++;
                str4 = str2;
                str3 = str;
            }
            this.luxLightSystemuiSyAuthCountArray = dataInfoJson.getJSONArray("lux_systemui_count_array");
            this.luxLightSystemuiAuthSuccCountArray = dataInfoJson.getJSONArray("lux_systemui_succ_count_array");
            this.luxLightOtherSyAuthCountArray = dataInfoJson.getJSONArray("lux_otherapp_count_array");
            this.luxLightOtherAuthSuccCountArray = dataInfoJson.getJSONArray("lux_otherapp_succ_count_array");
            int arraylength = this.luxLightSystemuiSyAuthCountArray.length();
            if (this.luxLightSystemuiSyAuthCountArray.length() == arraylength && this.luxLightSystemuiAuthSuccCountArray.length() == arraylength && this.luxLightOtherSyAuthCountArray.length() == arraylength && this.luxLightOtherAuthSuccCountArray.length() == arraylength) {
                for (int tempIndex2 = 0; tempIndex2 < this.appUnlockRateArray.length(); tempIndex2++) {
                    Integer luxSystemuicount = Integer.valueOf(Integer.parseInt(this.luxLightSystemuiSyAuthCountArray.get(tempIndex2).toString()));
                    Integer luxSystemuiSuccCount = Integer.valueOf(Integer.parseInt(this.luxLightSystemuiAuthSuccCountArray.get(tempIndex2).toString()));
                    Integer luxOtherAppcount = Integer.valueOf(Integer.parseInt(this.luxLightOtherSyAuthCountArray.get(tempIndex2).toString()));
                    Integer luxOtherAppSuccCount = Integer.valueOf(Integer.parseInt(this.luxLightOtherAuthSuccCountArray.get(tempIndex2).toString()));
                    this.systemuiLuxUnlockRate.luxLightUnlockList.get(tempIndex2).authCount = luxSystemuicount.intValue();
                    this.systemuiLuxUnlockRate.luxLightUnlockList.get(tempIndex2).authSuccessCount = luxSystemuiSuccCount.intValue();
                    this.otherAppLuxUnlockRate.luxLightUnlockList.get(tempIndex2).authCount = luxOtherAppcount.intValue();
                    this.otherAppLuxUnlockRate.luxLightUnlockList.get(tempIndex2).authSuccessCount = luxOtherAppSuccCount.intValue();
                }
                return true;
            }
            Slog.e(TAG, "bad luxLight JSONArray");
            return false;
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

    @Override // com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintBigData
    public void resetLocalInfo() {
        Slog.d(TAG, "resetLocalInfo ");
        this.totalAuth.reset();
        for (AppUnlockRateStatistic appunlock : this.sAppStatisticList) {
            appunlock.reset();
        }
        this.systemuiLuxUnlockRate.reset();
        this.otherAppLuxUnlockRate.reset();
    }
}
