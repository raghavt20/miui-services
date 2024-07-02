package com.android.server;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.pm.PackageManagerServiceUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.GeneralExceptionEvent;

/* loaded from: classes.dex */
public class PackageWatchdogImpl extends PackageWatchdogStub {
    private static final Set<String> SYSTEMUI_DEPEND_APPS = new HashSet<String>() { // from class: com.android.server.PackageWatchdogImpl.1
        {
            add("miui.systemui.plugin");
        }
    };
    private HandlerThread ResultJudgeThread;
    private ResultJudgeHandler mResultJudgeHandler;
    private final long DELAY_TIME = SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION;
    HashMap<String, HashMap<Integer, GeneralExceptionEvent>> rescuemap = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PackageWatchdogImpl> {

        /* compiled from: PackageWatchdogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PackageWatchdogImpl INSTANCE = new PackageWatchdogImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PackageWatchdogImpl m258provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PackageWatchdogImpl m257provideNewInstance() {
            return new PackageWatchdogImpl();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ResultJudgeHandler extends Handler {
        public ResultJudgeHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            GeneralExceptionEvent event = (GeneralExceptionEvent) msg.obj;
            MQSEventManagerDelegate.getInstance().reportGeneralException(event);
            if (PackageWatchdogImpl.this.rescuemap.getOrDefault(event.getPackageName(), new HashMap<>()).get(Integer.valueOf(msg.what)) != null) {
                PackageWatchdogImpl.this.rescuemap.getOrDefault(event.getPackageName(), new HashMap<>()).remove(Integer.valueOf(msg.what));
            }
        }
    }

    public void sendMessage(String packageName, int rescueLevel, GeneralExceptionEvent event) {
        HashMap status = this.rescuemap.getOrDefault(packageName, new HashMap<>());
        status.put(Integer.valueOf(rescueLevel), event);
        this.rescuemap.put(packageName, status);
        Message msg = this.mResultJudgeHandler.obtainMessage(rescueLevel, event);
        this.mResultJudgeHandler.sendMessageDelayed(msg, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION);
    }

    public void removeMessage(int rescueLevel, String packageName) {
        if (this.rescuemap.getOrDefault(packageName, new HashMap<>()).get(Integer.valueOf(rescueLevel)) != null) {
            GeneralExceptionEvent removeEvent = this.rescuemap.getOrDefault(packageName, new HashMap<>()).remove(Integer.valueOf(rescueLevel));
            this.mResultJudgeHandler.removeMessages(rescueLevel, removeEvent);
        }
    }

    public PackageWatchdogImpl() {
        HandlerThread handlerThread = new HandlerThread("resultJudgeWork");
        this.ResultJudgeThread = handlerThread;
        handlerThread.start();
        this.mResultJudgeHandler = new ResultJudgeHandler(this.ResultJudgeThread.getLooper());
    }

    private void clearAppCacheAndData(PackageManager pm, String currentCrashAppName) {
        Slog.w("RescuePartyPlus", "Clear app cache and data: " + currentCrashAppName);
        PackageManagerServiceUtils.logCriticalInfo(3, "Clear app cache and data: " + currentCrashAppName);
        pm.deleteApplicationCacheFiles(currentCrashAppName, null);
        pm.clearApplicationUserData(currentCrashAppName, null);
    }

    public boolean doRescuePartyPlusStep(int mitigationCount, VersionedPackage versionedPackage, Context context) {
        String details;
        if (RescuePartyPlusHelper.checkDisableRescuePartyPlus()) {
            return false;
        }
        RescuePartyPlusHelper.setMitigationTempCount(mitigationCount);
        if (versionedPackage == null || versionedPackage.getPackageName() == null) {
            Slog.e("RescuePartyPlus", "Package Watchdog check versioned package failed: " + versionedPackage);
            return false;
        }
        Slog.w("RescuePartyPlus", "doRescuePartyPlusStep " + versionedPackage.getPackageName() + ": [" + mitigationCount + "]");
        String currentCrashAppName = versionedPackage.getPackageName();
        if (RescuePartyPlusHelper.checkPackageIsCore(currentCrashAppName)) {
            Slog.e("RescuePartyPlus", "Skip because system crash: " + versionedPackage);
            return false;
        }
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        event.setType(430);
        event.setPackageName(currentCrashAppName);
        event.setSummary("rescueparty level: " + mitigationCount);
        event.setTimeStamp(System.currentTimeMillis());
        PackageManager pm = context.getPackageManager();
        switch (mitigationCount) {
            case 0:
                return false;
            case 1:
                event.setDetails(currentCrashAppName + " LEVEL_RESET_SETTINGS_UNTRUSTED_DEFAULTS;");
                sendMessage(currentCrashAppName, 1, event);
                return false;
            case 2:
                removeMessage(1, currentCrashAppName);
                event.setDetails(currentCrashAppName + " LEVEL_RESET_SETTINGS_UNTRUSTED_CHANGES;");
                sendMessage(currentCrashAppName, 2, event);
                return false;
            case 3:
                removeMessage(2, currentCrashAppName);
                String details2 = currentCrashAppName + " LEVEL_RESET_SETTINGS_TRUSTED_DEFAULTS;";
                if (RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    clearAppCacheAndData(pm, currentCrashAppName);
                    event.setDetails(details2 + " CLEAR_APP_CACHE_AND_DATA;");
                    sendMessage(currentCrashAppName, 3, event);
                    return false;
                }
                event.setDetails(details2);
                sendMessage(currentCrashAppName, 3, event);
                return false;
            case 4:
                removeMessage(3, currentCrashAppName);
                if (currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                    clearAppCacheAndData(pm, currentCrashAppName);
                    event.setDetails(currentCrashAppName + " CLEAR_APP_CACHE_AND_DATA;");
                } else if (!RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    Slog.w("RescuePartyPlus", "Clear app cache: " + currentCrashAppName);
                    pm.deleteApplicationCacheFiles(currentCrashAppName, null);
                    event.setDetails(currentCrashAppName + " DELETE_APPLICATION_CACHE_FILES;");
                } else {
                    clearAppCacheAndData(pm, currentCrashAppName);
                    if (!RescuePartyPlusHelper.resetTheme(currentCrashAppName)) {
                        Slog.e("RescuePartyPlus", "Reset theme failed: " + currentCrashAppName);
                        event.setDetails(currentCrashAppName + " CLEAR_APP_CACHE_AND_DATA;");
                    } else {
                        event.setDetails(currentCrashAppName + " CLEAR_APP_CACHE_AND_DATA;RESET_THEME;");
                    }
                    RescuePartyPlusHelper.setLastResetConfigStatus(true);
                    RescuePartyPlusHelper.setShowResetConfigUIStatus(false);
                    maybeShowRecoveryTip(context);
                }
                sendMessage(currentCrashAppName, 4, event);
                return true;
            case 5:
                removeMessage(4, currentCrashAppName);
                String details3 = currentCrashAppName + " LEVEL_RESET_SETTINGS_UNTRUSTED_CHANGES;";
                if (!RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    Slog.w("RescuePartyPlus", "Try to rollback app and clear cache : " + currentCrashAppName);
                    if (!currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                        Slog.w("RescuePartyPlus", "Clear app cache: " + currentCrashAppName);
                        pm.deleteApplicationCacheFiles(currentCrashAppName, null);
                        details = details3 + "DELETE_APPLICATION_CACHE_FILES;";
                    } else {
                        clearAppCacheAndData(pm, currentCrashAppName);
                        details = details3 + "CLEAR_APP_CACHE_AND_DATA;";
                    }
                    try {
                        ApplicationInfo applicationInfo = pm.getApplicationInfo(currentCrashAppName, 0);
                        if (applicationInfo != null) {
                            if (applicationInfo.isSystemApp() && applicationInfo.isUpdatedSystemApp()) {
                                Slog.w("RescuePartyPlus", "App install path: " + applicationInfo.sourceDir);
                                PackageManagerServiceUtils.logCriticalInfo(3, "Finished rescue level ROLLBACK_APP for package " + currentCrashAppName);
                                pm.deletePackage(currentCrashAppName, null, 2);
                                Slog.w("RescuePartyPlus", "Uninstall app: " + currentCrashAppName);
                            } else if (applicationInfo.isSystemApp() || !currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                                Slog.w("RescuePartyPlus", "no action for app: " + currentCrashAppName);
                            } else {
                                Slog.w("RescuePartyPlus", "Third party Launcher, no action for app: " + currentCrashAppName);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.e("RescuePartyPlus", "get application info failed!", e);
                    }
                    event.setDetails(details + "ROLLBACK_APP;");
                    sendMessage(currentCrashAppName, 5, event);
                    return true;
                }
                clearAppCacheAndData(pm, currentCrashAppName);
                for (String systemuiPlugin : SYSTEMUI_DEPEND_APPS) {
                    pm.deletePackage(systemuiPlugin, null, 2);
                    Slog.w("RescuePartyPlus", "Try to rollback Top UI App plugin: " + systemuiPlugin + " (" + currentCrashAppName + ")");
                }
                Slog.w("RescuePartyPlus", "Delete Top UI App cache and data: " + currentCrashAppName);
                return false;
            default:
                removeMessage(5, currentCrashAppName);
                clearAppCacheAndData(pm, currentCrashAppName);
                String details4 = currentCrashAppName + " CLEAR_APP_CACHE_AND_DATA;";
                if (!currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                    if (RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                        Slog.w("RescuePartyPlus", "Delete Top UI App cache and data: " + currentCrashAppName);
                        return false;
                    }
                    Slog.w("RescuePartyPlus", "Disable App restart, than clear app cache and data: " + currentCrashAppName);
                    RescuePartyPlusHelper.disableAppRestart(currentCrashAppName);
                    details4 = details4 + "DISABLE_APP_RESTART;";
                }
                event.setDetails(details4);
                sendMessage(currentCrashAppName, 0, event);
                return true;
        }
    }

    public void maybeShowRecoveryTip(final Context context) {
        if (!RescuePartyPlusHelper.checkDisableRescuePartyPlus() && !RescuePartyPlusHelper.getConfigResetProcessStatus() && RescuePartyPlusHelper.getLastResetConfigStatus() && !RescuePartyPlusHelper.getShowResetConfigUIStatus()) {
            RescuePartyPlusHelper.setLastResetConfigStatus(false);
            new Thread(new Runnable() { // from class: com.android.server.PackageWatchdogImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PackageWatchdogImpl.lambda$maybeShowRecoveryTip$0(context);
                }
            }).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$maybeShowRecoveryTip$0(Context context) {
        Slog.w("RescuePartyPlus", "ShowTipsUI Start");
        while (true) {
            if (!RescuePartyPlusHelper.checkBootCompletedStatus() || RescuePartyPlusHelper.getShowResetConfigUIStatus()) {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException e) {
                }
            } else {
                Slog.w("RescuePartyPlus", "Show RescueParty Plus Tips UI Ready!!!");
                Uri uri = Uri.parse(context.getString(286196504));
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
                Notification notification = new Notification.Builder(context, SystemNotificationChannels.DEVELOPER_IMPORTANT).setSmallIcon(R.drawable.stat_notify_error).setCategory("sys").setShowWhen(true).setContentTitle(context.getString(286196503)).setContentText(context.getString(286196502)).setContentIntent(pendingIntent).setOngoing(true).setPriority(5).setDefaults(-1).setVisibility(1).setAutoCancel(true).build();
                notificationManager.notify("RescueParty", "RescueParty".hashCode(), notification);
                RescuePartyPlusHelper.setShowResetConfigUIStatus(true);
                return;
            }
        }
    }

    public void recordMitigationCount(int mitigationCount) {
        if (RescuePartyPlusHelper.checkDisableRescuePartyPlus()) {
            return;
        }
        Slog.w("RescuePartyPlus", "note SystemServer crash: " + mitigationCount);
        RescuePartyPlusHelper.setMitigationTempCount(mitigationCount);
    }
}
