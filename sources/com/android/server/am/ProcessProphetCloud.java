package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

/* loaded from: classes.dex */
public class ProcessProphetCloud {
    private static final String CLOUD_PROCPROPHET_EMPTYMEM_THRESH = "cloud_procprophet_emptymem_thresh";
    private static final String CLOUD_PROCPROPHET_ENABLE = "cloud_procprophet_enable";
    private static final String CLOUD_PROCPROPHET_INTERVAL_TIME = "cloud_procprophet_interval_time";
    private static final String CLOUD_PROCPROPHET_LAUNCH_BLACKLIST = "cloud_procprophet_launch_blacklist";
    private static final String CLOUD_PROCPROPHET_MEMPRES_THRESH = "cloud_procprophet_mempres_thresh";
    private static final String CLOUD_PROCPROPHET_STARTFREQ_THRESH = "cloud_procprophet_startfreq_thresh";
    private static final String CLOUD_PROCPROPHET_START_LIMITLIST = "cloud_procprophet_start_limitlist";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.procprophet.debug", false);
    private static final String TAG = "ProcessProphetCloud";
    private ProcessProphetImpl mProcessProphetImpl = null;
    private ProcessProphetModel mProcessProphetModel = null;
    private Context mContext = null;

    public void initCloud(ProcessProphetImpl ppi, ProcessProphetModel ppm, Context context) {
        this.mProcessProphetImpl = ppi;
        this.mProcessProphetModel = ppm;
        this.mContext = context;
        if (ppi == null || ppm == null) {
            Slog.e(TAG, "initCloud failure.");
        } else {
            Slog.i(TAG, "initCloud success");
        }
    }

    public void registerProcProphetCloudObserver() {
        ContentObserver observer = new ContentObserver(this.mProcessProphetImpl.mHandler) { // from class: com.android.server.am.ProcessProphetCloud.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_ENABLE))) {
                    ProcessProphetCloud.this.updateEnableCloudControlParas();
                    return;
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_INTERVAL_TIME))) {
                    ProcessProphetCloud.this.updateTrackTimeCloudControlParas();
                    return;
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_STARTFREQ_THRESH))) {
                    ProcessProphetCloud.this.updateStartProcFreqThresCloudControlParas();
                    return;
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_MEMPRES_THRESH))) {
                    ProcessProphetCloud.this.updateMemThresCloudControlParas();
                    return;
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_EMPTYMEM_THRESH))) {
                    ProcessProphetCloud.this.updateEmptyMemThresCloudControlParas();
                    return;
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_START_LIMITLIST))) {
                    ProcessProphetCloud.this.updateStartProcLimitCloudControlParas();
                } else if (uri != null && uri.equals(Settings.System.getUriFor(ProcessProphetCloud.CLOUD_PROCPROPHET_LAUNCH_BLACKLIST))) {
                    ProcessProphetCloud.this.updateLaunchProcCloudControlParas();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_ENABLE), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_INTERVAL_TIME), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_STARTFREQ_THRESH), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_MEMPRES_THRESH), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_EMPTYMEM_THRESH), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_START_LIMITLIST), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PROCPROPHET_LAUNCH_BLACKLIST), false, observer, -2);
    }

    public void updateProcProphetCloudControlParas() {
        updateEnableCloudControlParas();
        updateTrackTimeCloudControlParas();
        updateStartProcFreqThresCloudControlParas();
        updateMemThresCloudControlParas();
        updateEmptyMemThresCloudControlParas();
        updateStartProcLimitCloudControlParas();
        updateLaunchProcCloudControlParas();
    }

    public void updateEnableCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_ENABLE, -2) != null) {
            String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_ENABLE, -2);
            if (!TextUtils.isEmpty(enableStr)) {
                Boolean enableB = Boolean.valueOf(new Boolean(enableStr).booleanValue());
                this.mProcessProphetImpl.updateEnable(enableB.booleanValue());
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: ProcessProphetEnable - " + enableStr);
                }
            }
        }
    }

    public void updateTrackTimeCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_INTERVAL_TIME, -2) != null) {
            String intervalStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_INTERVAL_TIME, -2);
            if (!TextUtils.isEmpty(intervalStr)) {
                int valTime = Integer.parseInt(intervalStr);
                this.mProcessProphetImpl.updateTrackInterval(valTime);
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: ProcessProphet-Onetrack interval - " + intervalStr);
                }
            }
        }
    }

    public void updateStartProcFreqThresCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_STARTFREQ_THRESH, -2) != null) {
            String probThresStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_STARTFREQ_THRESH, -2);
            if (!TextUtils.isEmpty(probThresStr)) {
                String[] probThress = probThresStr.split(",");
                this.mProcessProphetModel.updateModelThreshold(probThress);
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: Process Frequent Threshold - " + probThress[0] + " " + probThress[1] + " " + probThress[2]);
                }
            }
        }
    }

    public void updateMemThresCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_MEMPRES_THRESH, -2) != null) {
            String memThresStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_MEMPRES_THRESH, -2);
            if (!TextUtils.isEmpty(memThresStr)) {
                Long tmpMemlong = getCloudAttribute(memThresStr);
                if (tmpMemlong.longValue() == -1) {
                    Slog.e(TAG, "getCloudAttribute operations is error , return -1");
                    return;
                }
                this.mProcessProphetImpl.updateImplThreshold(tmpMemlong.longValue(), "memPress");
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: Memory Pressure Threshold - " + tmpMemlong);
                }
            }
        }
    }

    public void updateEmptyMemThresCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_EMPTYMEM_THRESH, -2) != null) {
            String emptyMemThresStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_EMPTYMEM_THRESH, -2);
            if (!TextUtils.isEmpty(emptyMemThresStr)) {
                Long tmpEmptylong = getCloudAttribute(emptyMemThresStr);
                if (tmpEmptylong.longValue() == -1) {
                    Slog.e(TAG, "getCloudAttribute operations is error , return -1");
                    return;
                }
                this.mProcessProphetImpl.updateImplThreshold(tmpEmptylong.longValue(), "emptyMem");
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: Empty Process Memory Threshold - " + tmpEmptylong);
                }
            }
        }
    }

    public void updateStartProcLimitCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_START_LIMITLIST, -2) != null) {
            String procLimitStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_START_LIMITLIST, -2);
            if (!TextUtils.isEmpty(procLimitStr)) {
                String[] arrStr = procLimitStr.split(",");
                this.mProcessProphetImpl.updateList(arrStr, "whitelist");
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: StartProcLimitList");
                }
            }
        }
    }

    public void updateLaunchProcCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_LAUNCH_BLACKLIST, -2) != null) {
            String luchBLStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PROCPROPHET_LAUNCH_BLACKLIST, -2);
            if (!TextUtils.isEmpty(luchBLStr)) {
                String[] arrStr = luchBLStr.split(",");
                this.mProcessProphetImpl.updateList(arrStr, "blakclist");
                if (DEBUG) {
                    Slog.i(TAG, "receive info from cloud: LaunchProcBlackList");
                }
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:48:0x00b9, code lost:
    
        if (0 == 0) goto L39;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Long getCloudAttribute(java.lang.String r11) {
        /*
            r10 = this;
            java.lang.String r0 = "close io failed: "
            java.lang.String r1 = "ProcessProphetCloud"
            r2 = 0
            r3 = 0
            long r4 = android.os.Process.getTotalMemory()
            r6 = 1073741824(0x40000000, double:5.304989477E-315)
            long r4 = r4 / r6
            r6 = 1
            long r4 = r4 + r6
            int r4 = (int) r4
            r5 = 4
            if (r4 > r5) goto L16
            r4 = 4
        L16:
            r5 = 12
            if (r4 < r5) goto L1c
            r4 = 12
        L1c:
            java.io.StringReader r5 = new java.io.StringReader     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r5.<init>(r11)     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r2 = r5
            com.google.gson.stream.JsonReader r5 = new com.google.gson.stream.JsonReader     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r5.<init>(r2)     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r3 = r5
            r3.beginObject()     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
        L2b:
            boolean r5 = r3.hasNext()     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            if (r5 == 0) goto L6b
            java.lang.String r5 = r3.nextName()     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            int r5 = java.lang.Integer.parseInt(r5)     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            java.lang.String r6 = r3.nextString()     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            if (r4 != r5) goto L6a
            long r7 = java.lang.Long.parseLong(r6)     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            java.lang.Long r7 = java.lang.Long.valueOf(r7)     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r3.close()     // Catch: java.io.IOException -> L4c
            goto L65
        L4c:
            r8 = move-exception
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.StringBuilder r0 = r9.append(r0)
            java.lang.String r9 = r8.getMessage()
            java.lang.StringBuilder r0 = r0.append(r9)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
        L65:
            r2.close()
            return r7
        L6a:
            goto L2b
        L6b:
            r3.endObject()     // Catch: java.lang.Throwable -> L91 java.io.IOException -> L93
            r3.close()     // Catch: java.io.IOException -> L73
            goto L8c
        L73:
            r5 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.StringBuilder r0 = r6.append(r0)
            java.lang.String r6 = r5.getMessage()
            java.lang.StringBuilder r0 = r0.append(r6)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
        L8c:
        L8d:
            r2.close()
            goto Lbc
        L91:
            r5 = move-exception
            goto Lc3
        L93:
            r5 = move-exception
            java.lang.String r6 = "json reader error"
            android.util.Slog.e(r1, r6)     // Catch: java.lang.Throwable -> L91
            if (r3 == 0) goto Lb9
            r3.close()     // Catch: java.io.IOException -> La0
            goto Lb9
        La0:
            r5 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.StringBuilder r0 = r6.append(r0)
            java.lang.String r6 = r5.getMessage()
            java.lang.StringBuilder r0 = r0.append(r6)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
        Lb9:
            if (r2 == 0) goto Lbc
            goto L8d
        Lbc:
            r0 = -1
            java.lang.Long r0 = java.lang.Long.valueOf(r0)
            return r0
        Lc3:
            if (r3 == 0) goto Le2
            r3.close()     // Catch: java.io.IOException -> Lc9
            goto Le2
        Lc9:
            r6 = move-exception
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.StringBuilder r0 = r7.append(r0)
            java.lang.String r7 = r6.getMessage()
            java.lang.StringBuilder r0 = r0.append(r7)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
        Le2:
            if (r2 == 0) goto Le7
            r2.close()
        Le7:
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ProcessProphetCloud.getCloudAttribute(java.lang.String):java.lang.Long");
    }
}
