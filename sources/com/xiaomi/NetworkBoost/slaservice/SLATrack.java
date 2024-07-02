package com.xiaomi.NetworkBoost.slaservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import com.miui.server.security.AccessControlImpl;
import java.io.File;

/* loaded from: classes.dex */
public class SLATrack {
    private static final boolean DEBUG = true;
    private static final String METRICS_DIR = "sla";
    private static final String METRICS_PREFS_FILE = "sla_track";
    private static final long MILLIS_ONE_DAY = 86399999;
    private static final int MSG_DOUBLEWIFI_SLA_START = 7;
    private static final int MSG_DOUBLEWIFI_SLA_STOP = 8;
    private static final int MSG_SLA_START = 3;
    private static final int MSG_SLA_STOP = 4;
    private static final int MSG_SLS_START = 5;
    private static final int MSG_SLS_STOP = 6;
    private static final int MSG_START_MONITOR = 2;
    private static final int MSG_START_REPORT = 1;
    private static final int ON_AVAILABLE = 1;
    private static final String PREFERENCE_NAME = "sla_track";
    private static final String STR_DBSLA_DURATION = "dbsla_duration";
    private static final String STR_DBSLA_STATE = "dbsla";
    private static final String STR_SLA_DURATION = "sla_duration";
    private static final String STR_SLA_STATE = "sla";
    private static final String STR_SLS_DURATION = "sls_duration";
    private static final String STR_SLS_STATE = "sls";
    private static final String TAG = "SLM-SRV-SLATrack";
    private static SLATrack mSLATrack = null;
    private static Handler mWorkHandler;
    private Context mContext;
    private File mMetricsPrefsFile;
    private HandlerThread mWorkThread;
    private long SLAStartTime = 0;
    private long SLSStartTime = 0;
    private long DoubleWifiAndSLAStartTime = 0;

    public static SLATrack getSLATrack(Context context) {
        if (mSLATrack == null) {
            mSLATrack = new SLATrack(context);
        }
        return mSLATrack;
    }

    private SLATrack(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mWorkThread = handlerThread;
        handlerThread.start();
        WorkHandler workHandler = new WorkHandler(this.mWorkThread.getLooper());
        mWorkHandler = workHandler;
        workHandler.sendMessageDelayed(workHandler.obtainMessage(2), MILLIS_ONE_DAY);
    }

    /* loaded from: classes.dex */
    private class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            File prefsDir = new File(Environment.getDataSystemCeDirectory(0), "sla");
            SLATrack.this.mMetricsPrefsFile = new File(prefsDir, "sla_track");
            try {
                SharedPreferences prefs = SLATrack.this.mContext.getSharedPreferences(SLATrack.this.mMetricsPrefsFile, 0);
                SharedPreferences.Editor editor = prefs.edit();
                switch (msg.what) {
                    case 1:
                        Log.i(SLATrack.TAG, "handleMessage: MSG_START_REPORT");
                        SLATrack.this.reportMQSEvent(prefs);
                        if (SLATrack.this.SLAStartTime == 0) {
                            editor.putInt("sla", 0);
                        }
                        if (SLATrack.this.SLSStartTime == 0) {
                            editor.putInt(SLATrack.STR_SLS_STATE, 0);
                        }
                        if (SLATrack.this.DoubleWifiAndSLAStartTime == 0) {
                            editor.putInt(SLATrack.STR_DBSLA_STATE, 0);
                        }
                        editor.putInt(SLATrack.STR_SLA_DURATION, 0);
                        editor.putInt(SLATrack.STR_SLS_DURATION, 0);
                        editor.putInt(SLATrack.STR_DBSLA_DURATION, 0);
                        SLATrack.mWorkHandler.sendMessageDelayed(SLATrack.mWorkHandler.obtainMessage(2), SLATrack.MILLIS_ONE_DAY);
                        break;
                    case 2:
                        SLATrack.mWorkHandler.sendMessageDelayed(SLATrack.mWorkHandler.obtainMessage(1), 1L);
                        break;
                    case 3:
                        SLATrack sLATrack = SLATrack.this;
                        sLATrack.SLAStartTime = sLATrack.getTime();
                        editor.putInt("sla", 1);
                        break;
                    case 4:
                        if (SLATrack.this.SLAStartTime != 0) {
                            Log.i(SLATrack.TAG, "MSG_SLA_STOP time = " + ((SLATrack.this.getTime() - SLATrack.this.SLAStartTime) + prefs.getInt(SLATrack.STR_SLA_DURATION, 0)));
                            editor.putInt(SLATrack.STR_SLA_DURATION, ((int) (SLATrack.this.getTime() - SLATrack.this.SLAStartTime)) + prefs.getInt(SLATrack.STR_SLA_DURATION, 0));
                            SLATrack.this.SLAStartTime = 0L;
                            break;
                        }
                        break;
                    case 5:
                        SLATrack sLATrack2 = SLATrack.this;
                        sLATrack2.SLSStartTime = sLATrack2.getTime();
                        editor.putInt(SLATrack.STR_SLS_STATE, 1);
                        break;
                    case 6:
                        if (SLATrack.this.SLSStartTime != 0) {
                            Log.i(SLATrack.TAG, "MSG_SLS_STOP time = " + ((SLATrack.this.getTime() - SLATrack.this.SLSStartTime) + prefs.getInt(SLATrack.STR_SLS_DURATION, 0)));
                            editor.putInt(SLATrack.STR_SLS_DURATION, ((int) (SLATrack.this.getTime() - SLATrack.this.SLSStartTime)) + prefs.getInt(SLATrack.STR_SLS_DURATION, 0));
                            SLATrack.this.SLSStartTime = 0L;
                            break;
                        }
                        break;
                    case 7:
                        if (SLATrack.this.SLAStartTime != 0) {
                            SLATrack.sendMsgSlaStop();
                        }
                        SLATrack sLATrack3 = SLATrack.this;
                        sLATrack3.DoubleWifiAndSLAStartTime = sLATrack3.getTime();
                        editor.putInt(SLATrack.STR_DBSLA_STATE, 1);
                        break;
                    case 8:
                        if (SLATrack.this.DoubleWifiAndSLAStartTime != 0) {
                            Log.i(SLATrack.TAG, "MSG_DOUBLEWIFI_SLA_STOP time = " + ((SLATrack.this.getTime() - SLATrack.this.DoubleWifiAndSLAStartTime) + prefs.getInt(SLATrack.STR_DBSLA_DURATION, 0)));
                            editor.putInt(SLATrack.STR_DBSLA_DURATION, ((int) (SLATrack.this.getTime() - SLATrack.this.DoubleWifiAndSLAStartTime)) + prefs.getInt(SLATrack.STR_DBSLA_DURATION, 0));
                            SLATrack.this.DoubleWifiAndSLAStartTime = 0L;
                            break;
                        }
                        break;
                }
                editor.commit();
            } catch (IllegalStateException e) {
                Log.e(SLATrack.TAG, "IllegalStateException", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getTime() {
        long totalMilliSeconds = SystemClock.elapsedRealtime();
        return totalMilliSeconds / AccessControlImpl.LOCK_TIME_OUT;
    }

    public static void sendMsgSlaStart() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(3), 1L);
    }

    public static void sendMsgSlsStart() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(5), 1L);
    }

    public static void sendMsgDoubleWifiAndSLAStart() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(7), 1L);
    }

    public static void sendMsgSlaStop() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(4), 1L);
    }

    public static void sendMsgSlsStop() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(6), 1L);
    }

    public static void sendMsgDoubleWifiAndSLAStop() {
        Handler handler = mWorkHandler;
        handler.sendMessageDelayed(handler.obtainMessage(8), 1L);
    }

    public void reportMQSEvent(SharedPreferences prefs) {
        Log.i(TAG, "reportSLATrackEvent");
        Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
        intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000060");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "SLM");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "com.qti.slaservice");
        Bundle params = new Bundle();
        intent.putExtra("sla", prefs.getInt("sla", 0));
        intent.putExtra(STR_SLS_STATE, prefs.getInt(STR_SLS_STATE, 0));
        intent.putExtra(STR_DBSLA_STATE, prefs.getInt(STR_DBSLA_STATE, 0));
        intent.putExtra(STR_SLA_DURATION, prefs.getInt(STR_SLA_DURATION, 0));
        intent.putExtra(STR_SLS_DURATION, prefs.getInt(STR_SLS_DURATION, 0));
        intent.putExtra(STR_DBSLA_DURATION, prefs.getInt(STR_DBSLA_DURATION, 0));
        intent.putExtras(params);
        this.mContext.startService(intent);
    }
}
