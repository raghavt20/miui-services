package com.android.server.location;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.am.BroadcastQueueModernStubImpl;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class GnssEventHandler {
    private static String CLOSE_BLUR_LOCATION = "com.miui.gnss.CLOSE_BLUR_LOCATION";
    private static final String TAG = "GnssNps";
    private static GnssEventHandler mInstance;
    private Notification.Action mAction;
    private Context mAppContext;
    private NotifyManager mNotifyManager = new NotifyManager();
    private String pkgName = "";
    private boolean mBlur = false;

    public static synchronized GnssEventHandler getInstance(Context context) {
        GnssEventHandler gnssEventHandler;
        synchronized (GnssEventHandler.class) {
            if (mInstance == null) {
                mInstance = new GnssEventHandler(context);
            }
            gnssEventHandler = mInstance;
        }
        return gnssEventHandler;
    }

    private GnssEventHandler(Context context) {
        this.mAppContext = context;
    }

    public void handleStart() {
        this.mNotifyManager.initNotification();
        this.mNotifyManager.showNotification();
    }

    public void handleStop() {
        if (this.mBlur) {
            return;
        }
        this.mNotifyManager.removeNotification();
    }

    public void handleFix() {
        Log.d(TAG, "fixed");
        this.mNotifyManager.removeNotification();
    }

    public void handleLose() {
        Log.d(TAG, "lose location");
        this.mNotifyManager.showNotification();
    }

    public void handleRecover() {
        Log.d(TAG, "fix again");
        this.mNotifyManager.removeNotification();
    }

    public void handleCallerName(String name) {
        Log.d(TAG, "APP caller name");
        this.mNotifyManager.updateCallerName(name);
    }

    public void handleCallerName(String name, String event) {
        Log.d(TAG, "APP caller name");
        this.pkgName = name;
        this.mNotifyManager.updateCallerName(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isChineseLanguage() {
        String language = Locale.getDefault().toString();
        return language.endsWith("zh_CN");
    }

    /* loaded from: classes.dex */
    public class NotifyManager {
        private Notification.Builder mBuilder;
        private NotificationManager mNotificationManager;
        private final String CHANNEL_ID = "GPS_STATUS_MONITOR_ID";
        private String packageName = null;
        private CharSequence appName = null;
        private boolean mnoise = false;
        private final BroadcastReceiver mCloseBlurLocationListener = new BroadcastReceiver() { // from class: com.android.server.location.GnssEventHandler.NotifyManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Context mContext = context.getApplicationContext();
                PackageManager mPackageManager = mContext.getPackageManager();
                AppOpsManager appOpsManager = (AppOpsManager) mContext.getSystemService("appops");
                try {
                    int mUid = mPackageManager.getApplicationInfo(GnssEventHandler.this.pkgName, 0).uid;
                    appOpsManager.setMode(10036, mUid, GnssEventHandler.this.pkgName, 0);
                    if (NotifyManager.this.mCloseBlurLocationListener != null) {
                        GnssEventHandler.this.mAppContext.unregisterReceiver(NotifyManager.this.mCloseBlurLocationListener);
                    }
                } catch (Exception e) {
                    Log.e(GnssEventHandler.TAG, "exception in close blur location : " + Log.getStackTraceString(e));
                }
                NotifyManager.this.removeNotification();
                GnssEventHandler.this.mBlur = false;
            }
        };

        public NotifyManager() {
        }

        public void initNotification() {
            this.mNotificationManager = (NotificationManager) GnssEventHandler.this.mAppContext.getSystemService("notification");
            this.mBuilder = new Notification.Builder(GnssEventHandler.this.mAppContext, "GPS_STATUS_MONITOR_ID");
            constructNotification();
        }

        private void constructNotification() {
            String description = GnssEventHandler.this.mAppContext.getString(286196413);
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.Settings$LocationSettingsActivity");
            intent.addFlags(268435456);
            PendingIntent pendingIntent = PendingIntent.getActivity(GnssEventHandler.this.mAppContext, 0, intent, 201326592);
            NotificationChannel channel = new NotificationChannel("GPS_STATUS_MONITOR_ID", description, 2);
            channel.setLockscreenVisibility(1);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            this.mBuilder.setSmallIcon(285737401);
            try {
                this.mNotificationManager.createNotificationChannel(channel);
                if (!this.mnoise) {
                    if (GnssEventHandler.this.mBlur) {
                        PackageManager pm = GnssEventHandler.this.mAppContext.getPackageManager();
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(this.packageName, 0);
                            this.appName = ai.loadLabel(pm);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.w(GnssEventHandler.TAG, "No such package for this name!");
                        }
                        if (this.appName == null) {
                            this.appName = "gps server";
                        }
                        PendingIntent blurLocationPendingIntent = PendingIntent.getBroadcast(GnssEventHandler.this.mAppContext, 0, new Intent(GnssEventHandler.CLOSE_BLUR_LOCATION), BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("miui.showAction", true);
                        GnssEventHandler.this.mAction = new Notification.Action(285737368, GnssEventHandler.this.mAppContext.getString(286196411), blurLocationPendingIntent);
                        this.mBuilder.setContentTitle(this.appName).setPriority(2).setContentText(GnssEventHandler.this.mAppContext.getString(286196410)).setActions(GnssEventHandler.this.mAction).setExtras(bundle).setAutoCancel(true);
                        GnssEventHandler.this.mAppContext.registerReceiver(this.mCloseBlurLocationListener, new IntentFilter(GnssEventHandler.CLOSE_BLUR_LOCATION));
                        return;
                    }
                    return;
                }
                this.mBuilder.setContentTitle(GnssEventHandler.this.mAppContext.getString(286196417)).setContentText(GnssEventHandler.this.mAppContext.getString(286196407)).setContentIntent(pendingIntent).setOngoing(true).setAutoCancel(true);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        public void showNotification() {
            if (!GnssEventHandler.this.isChineseLanguage()) {
                Log.d(GnssEventHandler.TAG, "show notification only in CHINESE language");
                return;
            }
            if (this.packageName == null) {
                Log.d(GnssEventHandler.TAG, "no package name presence");
                return;
            }
            try {
                this.mNotificationManager.notifyAsUser(null, 285737401, this.mBuilder.build(), UserHandle.ALL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void updateCallerName(String name) {
            Log.d(GnssEventHandler.TAG, "updateCallerName=" + name);
            this.packageName = name;
            if (name.contains("noise_environment") && !this.mnoise) {
                this.mnoise = true;
                GnssEventHandler.this.mNotifyManager.initNotification();
                GnssEventHandler.this.mNotifyManager.showNotification();
                return;
            }
            if (name.contains("normal_environment") && this.mnoise) {
                this.mnoise = false;
                GnssEventHandler.this.mNotifyManager.removeNotification();
            } else {
                if (name.contains("blurLocation_notify is on")) {
                    GnssEventHandler.this.mBlur = true;
                    this.packageName = GnssEventHandler.this.pkgName;
                    GnssEventHandler.this.mNotifyManager.initNotification();
                    GnssEventHandler.this.mNotifyManager.showNotification();
                    return;
                }
                if (name.contains("blurLocation_notify is off")) {
                    GnssEventHandler.this.mBlur = false;
                    GnssEventHandler.this.mNotifyManager.removeNotification();
                }
            }
        }

        private void unregisterBlurLocationListener() {
            Intent queryIntent = new Intent();
            queryIntent.setAction(GnssEventHandler.CLOSE_BLUR_LOCATION);
            PackageManager pm = GnssEventHandler.this.mAppContext.getPackageManager();
            List<ResolveInfo> resolveInfoList = pm.queryBroadcastReceivers(queryIntent, 0);
            if (resolveInfoList != null && !resolveInfoList.isEmpty()) {
                GnssEventHandler.this.mAppContext.unregisterReceiver(this.mCloseBlurLocationListener);
            }
        }

        public void removeNotification() {
            Log.d(GnssEventHandler.TAG, "removeNotification");
            if (this.mNotificationManager == null) {
                Log.d(GnssEventHandler.TAG, "mNotificationManager is null");
                initNotification();
            }
            try {
                NotificationManager notificationManager = this.mNotificationManager;
                if (notificationManager != null) {
                    notificationManager.cancelAsUser(null, 285737401, UserHandle.ALL);
                    unregisterBlurLocationListener();
                    this.mBuilder = null;
                }
            } catch (Exception e) {
                Log.e(GnssEventHandler.TAG, Log.getStackTraceString(e));
            }
        }
    }
}
