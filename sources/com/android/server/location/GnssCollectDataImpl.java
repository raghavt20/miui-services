package com.android.server.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.location.GnssCollectData;
import com.android.server.location.gnss.GnssCollectDataStub;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub;
import com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecoveryStub;
import com.android.server.location.gnss.hal.GnssPowerOptimizeStub;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.base.MiuiStubRegistry;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class GnssCollectDataImpl implements GnssCollectDataStub {
    private static final String ACTION_COLLECT_DATA = "action collect data";
    private static final String BO_DEF_VAL = "-1";
    private static final String CLOUDGPOKEY = "GpoVersion";
    private static final String CLOUDGSRKEY = "enableGSR";
    private static final String CLOUDKEY = "enabled";
    private static final String CLOUDKEYSUPL = "enableCaict";
    private static final String CLOUDMODULE = "bigdata";
    private static final String CLOUDSDKKEY = "SDK_source";
    private static final String CLOUD_MODULE_NLP = "nlp";
    private static final String CLOUD_MODULE_RTK = "gnssRtk";
    private static final String CN_MCC = "460";
    private static final String COLLECT_DATA_PATH = "/data/mqsas/gps/gps-strength";
    private static final boolean DEBUG = false;
    private static final String DEF_VAL_GSCO = "-1";
    private static final String GMO_DEF_VAL = "-1";
    private static final String GMO_DISABLE = "0";
    private static final String GMO_ENABLE = "1";
    private static final String GNSS_GSR_SWITCH = "persist.sys.gps.selfRecovery";
    private static final String GNSS_MQS_SWITCH = "persist.sys.mqs.gps";
    public static final int INFO_B1CN0_TOP4 = 9;
    public static final int INFO_E1CN0_TOP4 = 11;
    public static final int INFO_G1CN0_TOP4 = 10;
    public static final int INFO_L1CN0_TOP4 = 7;
    public static final int INFO_L5CN0_TOP4 = 8;
    public static final int INFO_NMEA_PQWP6 = 6;
    private static final String IS_COLLECT = "1";
    private static final String KEY_CLOUD_BO = "bo_status";
    private static final String KEY_CLOUD_GMO = "gmo_status";
    private static final String KEY_CLOUD_GSCO_PKG = "gsco_status_pkg";
    private static final String KEY_CLOUD_GSCO_STATUS = "gsco_status";
    private static final String KEY_DISABLE = "0";
    private static final String KEY_ENABLE = "1";
    private static final String RTK_SWITCH = "persist.sys.mqs.gps.rtk";
    public static final int SIGNAL_ERROR_CODE = 919021001;
    public static final int STATE_FIX = 2;
    public static final int STATE_INIT = 0;
    public static final int STATE_LOSE = 4;
    public static final int STATE_SAVE = 5;
    public static final int STATE_START = 1;
    public static final int STATE_STOP = 3;
    public static final int STATE_UNKNOWN = 100;
    private static final String SUPL_SWITCH = "persist.sys.mqs.gps.supl";
    public static final int SV_ERROR_CODE = 919012101;
    private static final String TAG = "GnssCD";
    private boolean hasStartUploadData;
    private Context mContext;
    private GnssCollectDataDbDao mGnssCollectDataDbDao;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    public static int mCurrentState = 100;
    private static String mMqsGpsModuleId = "mqs_gps_data_63921000";
    private static final boolean IS_STABLE_VERSION = Build.IS_STABLE_VERSION;
    private MQSEventManagerDelegate mMqsEventManagerDelegate = MQSEventManagerDelegate.getInstance();
    private GnssSessionInfo mSessionInfo = new GnssSessionInfo();
    private int SV_ERROR_SEND_INTERVAL = 600000;
    private int UPLOAD_REPEAT_TIME = 86400000;
    private JSONArray mJsonArray = new JSONArray();
    private boolean mIsCnSim = false;
    private long mLastSvTime = -600000;
    private int mSvUsedInFix = 0;
    private int mNumGPSUsedInFix = 0;
    private int mNumBEIDOUUsedInFix = 0;
    private int mNumGLONASSUsedInFix = 0;
    private int mNumGALILEOUsedInFix = 0;
    private int AmapBackCount = 0;
    private int BaiduBackCount = 0;
    private int TencentBackCount = 0;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.location.GnssCollectDataImpl.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (GnssCollectDataImpl.ACTION_COLLECT_DATA.equals(intent.getAction())) {
                GnssCollectDataImpl.this.startUploadFixData(context);
                GnssCollectDataImpl.this.startUploadBackData(context);
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssCollectDataImpl> {

        /* compiled from: GnssCollectDataImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssCollectDataImpl INSTANCE = new GnssCollectDataImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssCollectDataImpl m1747provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssCollectDataImpl m1746provideNewInstance() {
            return new GnssCollectDataImpl();
        }
    }

    private boolean allowCollect() {
        return "1".equals(SystemProperties.get(GNSS_MQS_SWITCH, "1"));
    }

    public boolean getSuplState() {
        String suplstate = SystemProperties.get(SUPL_SWITCH, "0");
        return "1".equals(suplstate);
    }

    public void setCnSimInserted(String mccMnc) {
        this.mIsCnSim = mccMnc.startsWith(CN_MCC);
        SystemProperties.set("persist.sys.mcc.mnc", mccMnc);
    }

    public boolean isCnSimInserted() {
        return this.mIsCnSim;
    }

    private String packToJsonArray() {
        JSONObject jsonObj = new JSONObject();
        if (this.mSessionInfo.getTtff() < 0) {
            Log.d(TAG, "abnormal data");
            return null;
        }
        if (this.mSessionInfo.getL1Top4Cn0Mean() < 28.0d) {
            MiEvent event = MiSight.constructEvent(SIGNAL_ERROR_CODE, new Object[]{"SignalWeak", "Signal weak"});
            MiSight.sendEvent(event);
        }
        try {
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, this.mSessionInfo.getStartTimeInHour());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF, this.mSessionInfo.getTtff());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME, this.mSessionInfo.getRunTime());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES, this.mSessionInfo.getLoseTimes());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0, this.mSessionInfo.getL1Top4Cn0Mean());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0, this.mSessionInfo.getL5Top4Cn0Mean());
            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0, this.mSessionInfo.getB1Top4Cn0Mean());
            jsonObj.put("G1Top4MeanCn0", this.mSessionInfo.getG1Top4Cn0Mean());
            jsonObj.put("E1Top4MeanCn0", this.mSessionInfo.getE1Top4Cn0Mean());
            this.mJsonArray.put(jsonObj);
            String jsonString = this.mJsonArray.toString();
            return jsonString;
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception " + e);
            return null;
        }
    }

    private void saveToFile(String messageToFile) {
        FileOutputStream out = null;
        try {
            try {
                try {
                    File bigdataFile = new File(COLLECT_DATA_PATH);
                    if (bigdataFile.exists()) {
                        long fileSize = bigdataFile.length() / FormatBytesUtil.KB;
                        if (fileSize > 5) {
                            bigdataFile.delete();
                            bigdataFile.getParentFile().mkdirs();
                            bigdataFile.createNewFile();
                        }
                    } else {
                        bigdataFile.getParentFile().mkdirs();
                        bigdataFile.createNewFile();
                    }
                    out = new FileOutputStream(bigdataFile, true);
                    out.write(messageToFile.getBytes());
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveLog() {
        String output = packToJsonArray();
        if (!this.mSessionInfo.checkValidity()) {
            return;
        }
        if (this.mContext == null || output == null) {
            Log.d(TAG, "mContext == null || output == null");
            return;
        }
        if (this.mJsonArray.length() > 20) {
            for (int i = 0; i < this.mJsonArray.length(); i++) {
                try {
                    JSONObject jsons = this.mJsonArray.getJSONObject(i);
                    Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
                    intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
                    if (Build.IS_INTERNATIONAL_BUILD) {
                        intent.setFlags(1);
                    } else {
                        intent.setFlags(3);
                    }
                    intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "2882303761518758754");
                    intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "GNSS");
                    intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
                    Bundle params = new Bundle();
                    params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME));
                    params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF));
                    params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME));
                    params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES));
                    intent.putExtras(params);
                    this.mContext.startService(intent);
                    Log.d(TAG, "GNSS data uploaded");
                    uploadCn0(jsons);
                } catch (Exception e) {
                    Log.e(TAG, "unexpected error when send GNSS event to onetrack");
                    e.printStackTrace();
                }
            }
            saveToFile(output);
            Log.d(TAG, "send to file");
            this.mJsonArray = new JSONArray();
        }
        if (!this.hasStartUploadData) {
            this.hasStartUploadData = true;
            setAlarm(this.mContext, ACTION_COLLECT_DATA);
        }
    }

    private void uploadCn0(JSONObject jsons) {
        try {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            if (Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(1);
            } else {
                intent.setFlags(3);
            }
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "2882303761518758754");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "GNSS_CN0");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            Bundle params = new Bundle();
            params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0));
            params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0));
            params.putString(GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0, jsons.getString(GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0));
            params.putString("G1Top4MeanCn0", jsons.getString("G1Top4MeanCn0"));
            params.putString("E1Top4MeanCn0", jsons.getString("E1Top4MeanCn0"));
            intent.putExtras(params);
            this.mContext.startService(intent);
            Log.d(TAG, "GNSS_CN0 uploaded");
        } catch (Exception e) {
            Log.e(TAG, "unexpected error when send GNSS event to onetrack");
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startUploadFixData(Context context) {
        if (this.mSvUsedInFix == 0) {
            return;
        }
        try {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            if (Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(1);
            } else {
                intent.setFlags(3);
            }
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "2882303761518758754");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "GNSS_CONSTELLATION_FIX");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            Bundle params = new Bundle();
            params.putInt("SV_FIX", this.mSvUsedInFix);
            params.putInt("GPS_FIX", this.mNumGPSUsedInFix);
            params.putInt("BEIDOU_FIX", this.mNumBEIDOUUsedInFix);
            params.putInt("GLONASS_FIX", this.mNumGLONASSUsedInFix);
            params.putInt("GALILEO_FIX", this.mNumGALILEOUsedInFix);
            intent.putExtras(params);
            this.mContext.startService(intent);
            Log.d(TAG, "GNSS_CONSTELLATION_FIX uploaded");
        } catch (Exception e) {
            Log.e(TAG, "unexpected error when send GNSS event to onetrack");
            e.printStackTrace();
        }
        this.mSvUsedInFix = 0;
        this.mNumGPSUsedInFix = 0;
        this.mNumBEIDOUUsedInFix = 0;
        this.mNumGLONASSUsedInFix = 0;
        this.mNumGALILEOUsedInFix = 0;
    }

    public void startUploadBackData(Context context) {
        if (this.AmapBackCount == 0 && this.BaiduBackCount == 0 && this.TencentBackCount == 0) {
            return;
        }
        try {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            if (Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(1);
            } else {
                intent.setFlags(3);
            }
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "2882303761518758754");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "GNSS_BACKGROUND_PERMISSION");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            Bundle params = new Bundle();
            params.putInt("AmapBackCount", this.AmapBackCount / 3);
            params.putInt("BaiduBackCount", this.BaiduBackCount / 3);
            params.putInt("TencentBackCount", this.TencentBackCount / 3);
            intent.putExtras(params);
            this.mContext.startService(intent);
            Log.d(TAG, "GNSS_BACKGROUND_PERMISSION uploaded");
        } catch (Exception e) {
            Log.e(TAG, "unexpected error when send GNSS event to onetrack");
            e.printStackTrace();
        }
        this.AmapBackCount = 0;
        this.BaiduBackCount = 0;
        this.TencentBackCount = 0;
    }

    public void saveUngrantedBackPermission(int mUid, int mPid, String packageName) {
        Context context = this.mContext;
        if (context == null || context.checkPermission("android.permission.ACCESS_BACKGROUND_LOCATION", mPid, mUid) == 0) {
            return;
        }
        if (packageName.equals("com.autonavi.minimap")) {
            this.AmapBackCount++;
        } else if (packageName.equals("com.baidu.BaiduMap")) {
            this.BaiduBackCount++;
        } else if (packageName.equals("com.tencent.map")) {
            this.TencentBackCount++;
        }
    }

    public String getCurrentTime() {
        long mNow = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mNow);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
        return sb.toString();
    }

    private void sendMessage(int message, Object obj) {
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.e(TAG, "mhandler is null  ");
        } else {
            Message lMessage = Message.obtain(handler, message, obj);
            this.mHandler.sendMessage(lMessage);
        }
    }

    private boolean isL1Sv(float carrierFreq) {
        return ((double) carrierFreq) >= 1.559E9d && ((double) carrierFreq) <= 1.61E9d;
    }

    private boolean isL5Sv(float carrierFreq) {
        return ((double) carrierFreq) >= 1.164E9d && ((double) carrierFreq) <= 1.189E9d;
    }

    public void savePoint(int type, float[] cn0s, int numSv, float[] svCarrierFreqs, float[] svConstellation) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
            return;
        }
        if (10 != type || numSv == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < numSv) {
            return;
        }
        if (numSv < 4) {
            if (SystemClock.elapsedRealtime() - this.mLastSvTime > this.SV_ERROR_SEND_INTERVAL) {
                MiEvent event = MiSight.constructEvent(SV_ERROR_CODE, new Object[]{"SvCount", Integer.valueOf(numSv)});
                MiSight.sendEvent(event);
                this.mLastSvTime = SystemClock.elapsedRealtime();
                return;
            }
            return;
        }
        saveL1Cn0(numSv, cn0s, svCarrierFreqs, svConstellation);
        saveL5Cn0(numSv, cn0s, svCarrierFreqs, svConstellation);
        saveB1Cn0(numSv, cn0s, svCarrierFreqs, svConstellation);
        saveG1Cn0(numSv, cn0s, svCarrierFreqs, svConstellation);
        saveE1Cn0(numSv, cn0s, svCarrierFreqs, svConstellation);
        for (int i = 0; i < svConstellation.length; i++) {
            if (svConstellation[i] != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                if (svConstellation[i] == 1.0f) {
                    this.mNumGPSUsedInFix++;
                } else if (svConstellation[i] == 5.0f) {
                    this.mNumBEIDOUUsedInFix++;
                } else if (svConstellation[i] == 3.0f) {
                    this.mNumGLONASSUsedInFix++;
                } else if (svConstellation[i] == 6.0f) {
                    this.mNumGALILEOUsedInFix++;
                }
                this.mSvUsedInFix++;
            }
        }
    }

    private void saveL1Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs, float[] svConstellation) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoL1Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL1Sv(svCarrierFreqs[i]) && svConstellation[i] == 1.0f) {
                CnoL1Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoL1Array.size();
        if (i2 == 0 || CnoL1Array.size() < 4) {
            return;
        }
        int numSvL1 = CnoL1Array.size();
        Collections.sort(CnoL1Array);
        if (CnoL1Array.get(numSvL1 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvL1 - 4; i3 < numSvL1; i3++) {
                top4AvgCn0 += CnoL1Array.get(i3).floatValue();
            }
            sendMessage(7, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    private void saveL5Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs, float[] svConstellation) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoL5Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL5Sv(svCarrierFreqs[i]) && svConstellation[i] == 1.0f) {
                CnoL5Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoL5Array.size();
        if (i2 == 0 || CnoL5Array.size() < 4) {
            return;
        }
        int numSvL5 = CnoL5Array.size();
        Collections.sort(CnoL5Array);
        if (CnoL5Array.get(numSvL5 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvL5 - 4; i3 < numSvL5; i3++) {
                top4AvgCn0 += CnoL5Array.get(i3).floatValue();
            }
            sendMessage(8, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    private void saveB1Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs, float[] svConstellation) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoB1Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL1Sv(svCarrierFreqs[i]) && svConstellation[i] == 5.0f) {
                CnoB1Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoB1Array.size();
        if (i2 == 0 || CnoB1Array.size() < 4) {
            return;
        }
        int numSvB1 = CnoB1Array.size();
        Collections.sort(CnoB1Array);
        if (CnoB1Array.get(numSvB1 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvB1 - 4; i3 < numSvB1; i3++) {
                top4AvgCn0 += CnoB1Array.get(i3).floatValue();
            }
            sendMessage(9, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    private void saveG1Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs, float[] svConstellation) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoG1Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL1Sv(svCarrierFreqs[i]) && svConstellation[i] == 3.0f) {
                CnoG1Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoG1Array.size();
        if (i2 == 0 || CnoG1Array.size() < 4) {
            return;
        }
        int numSvG1 = CnoG1Array.size();
        Collections.sort(CnoG1Array);
        if (CnoG1Array.get(numSvG1 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvG1 - 4; i3 < numSvG1; i3++) {
                top4AvgCn0 += CnoG1Array.get(i3).floatValue();
            }
            sendMessage(10, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    private void saveE1Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs, float[] svConstellation) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoE1Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL1Sv(svCarrierFreqs[i]) && svConstellation[i] == 6.0f) {
                CnoE1Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoE1Array.size();
        if (i2 == 0 || CnoE1Array.size() < 4) {
            return;
        }
        int numSvE1 = CnoE1Array.size();
        Collections.sort(CnoE1Array);
        if (CnoE1Array.get(numSvE1 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvE1 - 4; i3 < numSvE1; i3++) {
                top4AvgCn0 += CnoE1Array.get(i3).floatValue();
            }
            sendMessage(11, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    public void savePoint(int type, String extraInfo, Context context) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
        } else if (type == 0) {
            this.mContext = context;
            savePoint(0, extraInfo);
            Log.d(TAG, "register listener");
            registerControlListener();
        }
    }

    public void savePoint(int type, String extraInfo) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
            return;
        }
        if (type == 0) {
            startHandlerThread();
            setCurrentState(type);
            return;
        }
        if (1 == type) {
            int i = mCurrentState;
            if (i == 0 || i == 3 || i == 5) {
                this.mSessionInfo.newSessionReset();
                sendMessage(type, extraInfo);
                return;
            }
            return;
        }
        if (2 != type) {
            if (3 == type) {
                int i2 = mCurrentState;
                if (i2 == 1 || i2 == 2 || i2 == 4) {
                    sendMessage(type, extraInfo);
                    return;
                }
                return;
            }
            if (4 == type) {
                int i3 = mCurrentState;
                if (i3 == 2 || i3 == 4) {
                    sendMessage(type, extraInfo);
                    return;
                }
                return;
            }
            if (6 == type && extraInfo != null && extraInfo.startsWith("$PQWP6")) {
                sendMessage(type, extraInfo);
                return;
            }
            return;
        }
        if (mCurrentState == 1) {
            sendMessage(type, extraInfo);
        }
    }

    private void registerControlListener() {
        Context context = this.mContext;
        if (context == null) {
            Log.e(TAG, "no context");
        } else {
            context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(null) { // from class: com.android.server.location.GnssCollectDataImpl.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    GnssCollectDataImpl.this.updateControlState();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateControlState() {
        Log.d(TAG, "got rule changed");
        String newState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDKEY, "1");
        String newsuplState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDKEYSUPL, "0");
        SystemProperties.set(GNSS_MQS_SWITCH, newState);
        SystemProperties.set(SUPL_SWITCH, newsuplState);
        String rtkNewState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUD_MODULE_RTK, CLOUDKEY, "ON");
        SystemProperties.set(RTK_SWITCH, rtkNewState);
        updateGsrCloudConfig();
        updateGpoCloudConfig();
        updateBackgroundOptCloudConfig();
        updateSatelliteCallOptCloudConfig();
        updateMockLocationOptCloudConfig();
        int sdk_source = MiuiSettings.SettingsCloudData.getCloudDataInt(this.mContext.getContentResolver(), CLOUD_MODULE_NLP, CLOUDSDKKEY, 5);
        Intent intent = new Intent();
        Log.i(TAG, "send CLOUD_UPDATE Broadcast");
        intent.setAction("com.xiaomi.location.metoknlp.CLOUD_UPDATE");
        intent.setComponent(new ComponentName("com.xiaomi.metoknlp", "com.xiaomi.location.metoknlp.CloudReceiver"));
        intent.putExtra(CLOUDSDKKEY, sdk_source);
        this.mContext.sendBroadcast(intent);
    }

    private void updateSatelliteCallOptCloudConfig() {
        String newScoStatus = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, KEY_CLOUD_GSCO_STATUS, "-1");
        if ("0".equals(newScoStatus)) {
            LocationExtCooperateStub.getInstance().setSatelliteCallOptStatus(false);
            Log.d(TAG, "Satellite Call Opt disable by cloud...");
        } else if ("1".equals(newScoStatus)) {
            LocationExtCooperateStub.getInstance().setSatelliteCallOptStatus(true);
            Log.d(TAG, "Satellite Call Opt enable by cloud...");
        }
        String newScoPkg = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, KEY_CLOUD_GSCO_PKG, "-1");
        if (newScoPkg != null && !newScoPkg.isEmpty()) {
            LocationExtCooperateStub.getInstance().setSatelliteCallOptPkg(newScoPkg);
        }
    }

    private void updateGsrCloudConfig() {
        int gnssSelfRecovery = MiuiSettings.SettingsCloudData.getCloudDataInt(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDGSRKEY, -1);
        if (gnssSelfRecovery >= 0) {
            SystemProperties.set(GNSS_GSR_SWITCH, String.valueOf(gnssSelfRecovery));
            GnssSelfRecoveryStub.getInstance().setGsrConfig();
        }
    }

    private void updateBackgroundOptCloudConfig() {
        String newBoStatus = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, KEY_CLOUD_BO, "-1");
        if ("0".equals(newBoStatus)) {
            GnssBackgroundUsageOptStub.getInstance().setBackgroundOptStatus(false);
            Log.d(TAG, "background Opt disable by cloud...");
        } else if ("1".equals(newBoStatus)) {
            GnssBackgroundUsageOptStub.getInstance().setBackgroundOptStatus(true);
            Log.d(TAG, "background Opt enable by cloud...");
        }
    }

    private void updateMockLocationOptCloudConfig() {
        String newGmoStatus = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, KEY_CLOUD_GMO, "-1");
        if ("0".equals(newGmoStatus)) {
            GnssMockLocationOptStub.getInstance().setMockLocationOptStatus(false);
            Log.d(TAG, "mock location Opt disable by cloud");
        } else if ("1".equals(newGmoStatus)) {
            GnssMockLocationOptStub.getInstance().setMockLocationOptStatus(true);
            Log.d(TAG, "mock location Opt enable by cloud");
        }
    }

    private void updateGpoCloudConfig() {
        int gpoNewVersion = MiuiSettings.SettingsCloudData.getCloudDataInt(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDGPOKEY, -1);
        Log.i(TAG, "Got GpoVersion Cloud Config ? " + (gpoNewVersion >= 0));
        if (gpoNewVersion >= 0) {
            GnssPowerOptimizeStub.getInstance().setGpoVersionValue(gpoNewVersion);
        }
    }

    private void startHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("GnssCD thread");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.location.GnssCollectDataImpl.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int message = msg.what;
                switch (message) {
                    case 1:
                        GnssCollectDataImpl.this.saveStartStatus();
                        GnssCollectDataImpl.this.setCurrentState(1);
                        return;
                    case 2:
                        GnssCollectDataImpl.this.saveFixStatus();
                        GnssCollectDataImpl.this.setCurrentState(2);
                        return;
                    case 3:
                        GnssCollectDataImpl.this.saveStopStatus();
                        GnssCollectDataImpl.this.setCurrentState(3);
                        GnssCollectDataImpl.this.saveState();
                        return;
                    case 4:
                        GnssCollectDataImpl.this.saveLoseStatus();
                        GnssCollectDataImpl.this.setCurrentState(4);
                        return;
                    case 5:
                        GnssCollectDataImpl.this.saveLog();
                        GnssCollectDataImpl.this.setCurrentState(5);
                        return;
                    case 6:
                        GnssCollectDataImpl.this.parseNmea((String) msg.obj);
                        return;
                    case 7:
                        GnssCollectDataImpl.this.setL1Cn0(((Double) msg.obj).doubleValue());
                        return;
                    case 8:
                        GnssCollectDataImpl.this.setL5Cn0(((Double) msg.obj).doubleValue());
                        return;
                    case 9:
                        GnssCollectDataImpl.this.setB1Cn0(((Double) msg.obj).doubleValue());
                        return;
                    default:
                        return;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCurrentState(int s) {
        mCurrentState = s;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveStartStatus() {
        this.mSessionInfo.setStart();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveFixStatus() {
        this.mSessionInfo.setTtffAuto();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveLoseStatus() {
        this.mSessionInfo.setLostTimes();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveStopStatus() {
        this.mSessionInfo.setEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveState() {
        sendMessage(5, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseNmea(String nmea) {
        this.mSessionInfo.parseNmea(nmea);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setL1Cn0(double cn0) {
        this.mSessionInfo.setL1Cn0(cn0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setL5Cn0(double cn0) {
        this.mSessionInfo.setL5Cn0(cn0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setB1Cn0(double cn0) {
        this.mSessionInfo.setB1Cn0(cn0);
    }

    private void setAlarm(Context context, String action) {
        if (context == null || action == null) {
            Log.d(TAG, "context || action == null");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(action);
        context.registerReceiver(this.mReceiver, filter);
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            Intent i = new Intent(action);
            PendingIntent p = PendingIntent.getBroadcast(context, 0, i, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int i2 = this.UPLOAD_REPEAT_TIME;
            alarmManager.setRepeating(2, elapsedRealtime + i2, i2, p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
