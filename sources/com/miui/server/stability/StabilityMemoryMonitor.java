package com.miui.server.stability;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.BroadcastQueueModernStubImpl;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class StabilityMemoryMonitor {
    private static final String ACTION_SMM_ALARM = "miui.intent.action.SET_STABILITY_MEMORY_MONITOR_ALARM";
    private static final int ALARM_HOUR = 3;
    private static final int ALARM_MINUTE = 0;
    private static final int ALARM_SECOND = 0;
    private static final String CLOUD_CONFIG_FILE = "/data/mqsas/cloud/stability_memory_monitor_config.json";
    private static final HashMap<String, Long> DEFAULT_CONFIG_DATA = new HashMap<String, Long>() { // from class: com.miui.server.stability.StabilityMemoryMonitor.1
        {
            put("com.miui.home", 1000000L);
        }
    };
    private static final String SMM_CONTROL_PROPERTY = "persist.sys.stability_memory_monitor.enable";
    private static final String TAG = "StabilityMemoryMonitor";
    private static volatile StabilityMemoryMonitor sInstance;
    private Context mContext;
    private volatile boolean mPendingWork = false;
    private volatile boolean mScreenState = false;
    private boolean mEnabled = SystemProperties.getBoolean(SMM_CONTROL_PROPERTY, false);

    /* loaded from: classes.dex */
    public final class AlarmReceiver extends BroadcastReceiver {
        public AlarmReceiver() {
        }

        /* JADX WARN: Code restructure failed: missing block: B:8:0x0027, code lost:
        
            if (r0.equals(com.miui.server.stability.StabilityMemoryMonitor.ACTION_SMM_ALARM) != false) goto L12;
         */
        @Override // android.content.BroadcastReceiver
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onReceive(android.content.Context r4, android.content.Intent r5) {
            /*
                r3 = this;
                com.miui.server.stability.StabilityMemoryMonitor r0 = com.miui.server.stability.StabilityMemoryMonitor.this
                java.lang.String r1 = "persist.sys.stability_memory_monitor.enable"
                r2 = 0
                boolean r1 = android.os.SystemProperties.getBoolean(r1, r2)
                com.miui.server.stability.StabilityMemoryMonitor.m3477$$Nest$fputmEnabled(r0, r1)
                com.miui.server.stability.StabilityMemoryMonitor r0 = com.miui.server.stability.StabilityMemoryMonitor.this
                boolean r0 = com.miui.server.stability.StabilityMemoryMonitor.m3475$$Nest$fgetmEnabled(r0)
                if (r0 != 0) goto L15
                return
            L15:
                java.lang.String r0 = r5.getAction()
                int r1 = r0.hashCode()
                switch(r1) {
                    case 545200549: goto L21;
                    default: goto L20;
                }
            L20:
                goto L2a
            L21:
                java.lang.String r1 = "miui.intent.action.SET_STABILITY_MEMORY_MONITOR_ALARM"
                boolean r0 = r0.equals(r1)
                if (r0 == 0) goto L20
                goto L2b
            L2a:
                r2 = -1
            L2b:
                switch(r2) {
                    case 0: goto L2f;
                    default: goto L2e;
                }
            L2e:
                goto L44
            L2f:
                com.miui.server.stability.StabilityMemoryMonitor r0 = com.miui.server.stability.StabilityMemoryMonitor.this
                boolean r0 = com.miui.server.stability.StabilityMemoryMonitor.m3476$$Nest$fgetmScreenState(r0)
                if (r0 != 0) goto L3d
                com.miui.server.stability.StabilityMemoryMonitor r0 = com.miui.server.stability.StabilityMemoryMonitor.this
                com.miui.server.stability.StabilityMemoryMonitor.m3479$$Nest$msendTask(r0)
                goto L44
            L3d:
                com.miui.server.stability.StabilityMemoryMonitor r0 = com.miui.server.stability.StabilityMemoryMonitor.this
                r1 = 1
                com.miui.server.stability.StabilityMemoryMonitor.m3478$$Nest$fputmPendingWork(r0, r1)
            L44:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.miui.server.stability.StabilityMemoryMonitor.AlarmReceiver.onReceive(android.content.Context, android.content.Intent):void");
        }
    }

    private StabilityMemoryMonitor() {
    }

    public static StabilityMemoryMonitor getInstance() {
        if (sInstance == null) {
            sInstance = new StabilityMemoryMonitor();
        }
        return sInstance;
    }

    public void initContext(Context context) {
        this.mContext = context;
    }

    public void startMemoryMonitor() {
        registerAlarmReceiver();
        setAlarm();
    }

    public void updateScreenState(boolean screenOn) {
        this.mScreenState = screenOn;
        if (this.mEnabled && !this.mScreenState && this.mPendingWork) {
            sendTask();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendTask() {
        Handler bgHandler = BackgroundThread.getHandler();
        bgHandler.post(new Runnable() { // from class: com.miui.server.stability.StabilityMemoryMonitor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                StabilityMemoryMonitor.this.lambda$sendTask$0();
            }
        });
        this.mPendingWork = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendTask$0() {
        for (Map.Entry<String, Long> entry : getCloudConfig().entrySet()) {
            String procName = entry.getKey();
            long pssLimit = entry.getValue().longValue();
            long pssNow = getPssByPackage(procName);
            if (pssNow > pssLimit) {
                Process.killProcess(getPidByPackage(procName));
                Slog.w(TAG, "kill process: " + procName + " due to abnormal PSS occupied: " + pssNow);
            }
        }
    }

    private void registerAlarmReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SMM_ALARM);
        this.mContext.registerReceiver(new AlarmReceiver(), filter);
    }

    private void setAlarm() {
        AlarmManager alarm = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent intent = new Intent();
        intent.setAction(ACTION_SMM_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 0, intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, 3);
        calendar.set(12, 0);
        calendar.set(13, 0);
        Calendar now = Calendar.getInstance();
        if (now.compareTo(calendar) > 0) {
            calendar.add(5, 1);
        }
        alarm.setRepeating(0, calendar.getTimeInMillis(), 86400000L, pi);
    }

    private long getPssByPackage(String packageName) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.processName.equals(packageName)) {
                long pss = Debug.getPss(info.pid, null, null);
                return pss;
            }
        }
        return 0L;
    }

    private int getPidByPackage(String packageName) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.processName.equals(packageName)) {
                int pid = info.pid;
                return pid;
            }
        }
        return -1;
    }

    private HashMap<String, Long> getCloudConfig() {
        File cloudConfigFile = new File(CLOUD_CONFIG_FILE);
        HashMap<String, Long> configData = new HashMap<>(DEFAULT_CONFIG_DATA);
        if (cloudConfigFile.exists()) {
            try {
                String originData = FileUtils.readTextFile(cloudConfigFile, 0, null);
                if (!originData.isEmpty()) {
                    JSONObject jsonConfig = new JSONObject(originData);
                    JSONArray configItems = jsonConfig.getJSONArray("config");
                    for (int i = 0; i < configItems.length(); i++) {
                        JSONObject item = (JSONObject) configItems.get(i);
                        String appPackage = item.getString("name");
                        Long appPssLimit = Long.valueOf(item.getInt("pss"));
                        if (configData.containsKey(appPackage)) {
                            configData.replace(appPackage, appPssLimit);
                        } else {
                            configData.put(appPackage, appPssLimit);
                        }
                    }
                } else {
                    Slog.e(TAG, "cloud config file contains noting");
                }
            } catch (IOException e) {
                Slog.e(TAG, "read cloud config file failed: IOException");
                e.printStackTrace();
            } catch (JSONException e2) {
                Slog.e(TAG, "retrive config data failed: JSONException");
                configData.clear();
                configData.putAll(DEFAULT_CONFIG_DATA);
                e2.printStackTrace();
            }
        } else {
            Slog.e(TAG, "can't find cloud config file");
        }
        return configData;
    }
}
