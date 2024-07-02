package com.miui.app.smartpower;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowManager;
import com.android.server.am.ContentProviderConnection;
import com.android.server.am.PendingIntentRecord;
import com.android.server.am.ProcessRecord;
import com.android.server.display.mode.DisplayModeDirector;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.miui.server.smartpower.IAppState;
import java.util.ArrayList;
import miui.security.CallerInfo;
import miui.smartpower.MultiTaskActionManager;

/* loaded from: classes.dex */
public interface SmartPowerServiceInternal {
    public static final int MULTI_TASK_INVALID_LISTENER = 3;
    public static final int MULTI_TASK_LISTENER = 0;
    public static final int MULTI_TASK_LISTENER_MULTI_SCENE = 2;
    public static final int MULTI_TASK_LISTENER_SCHED_BOOST = 1;

    /* loaded from: classes.dex */
    public interface MultiTaskActionListener {
        void onMultiTaskActionEnd(MultiTaskActionManager.ActionInfo actionInfo);

        void onMultiTaskActionStart(MultiTaskActionManager.ActionInfo actionInfo);
    }

    void addFrozenPid(int i, int i2);

    void applyOomAdjLocked(ProcessRecord processRecord);

    boolean checkSystemSignature(int i);

    ArrayList<IAppState> getAllAppState();

    IAppState getAppState(int i);

    int getBackgroundCpuCoreNum();

    long getLastMusicPlayTimeStamp(int i);

    ArrayList<IAppState.IRunningProcess> getLruProcesses();

    ArrayList<IAppState.IRunningProcess> getLruProcesses(int i, String str);

    IAppState.IRunningProcess getRunningProcess(int i, String str);

    void hibernateAllIfNeeded(String str);

    boolean isAppAudioActive(int i);

    boolean isAppAudioActive(int i, int i2);

    boolean isAppGpsActive(int i);

    boolean isAppGpsActive(int i, int i2);

    boolean isProcessInTaskStack(int i, int i2);

    boolean isProcessInTaskStack(String str);

    boolean isProcessPerceptible(int i, String str);

    boolean isProcessWhiteList(int i, String str, String str2);

    boolean isUidIdle(int i);

    boolean isUidVisible(int i);

    void notifyCameraForegroundState(String str, boolean z, String str2, int i, int i2);

    void notifyMultiSenceEnable(boolean z);

    void onAcquireWakelock(IBinder iBinder, int i, String str, int i2, int i3);

    void onActivityReusmeUnchecked(String str, int i, int i2, String str2, int i3, int i4, String str3, boolean z);

    void onActivityStartUnchecked(String str, int i, int i2, String str2, int i3, int i4, String str3, boolean z);

    void onActivityVisibilityChanged(int i, String str, ActivityRecord activityRecord, boolean z);

    void onAlarmStatusChanged(int i, boolean z);

    void onAppTransitionStartLocked(long j);

    void onBackupChanged(boolean z, ProcessRecord processRecord);

    void onBackupServiceAppChanged(boolean z, int i, int i2);

    void onBluetoothEvent(boolean z, int i, int i2, int i3, String str, int i4);

    void onBroadcastStatusChanged(ProcessRecord processRecord, boolean z, Intent intent);

    void onCpuExceptionEvents(int i);

    void onCpuPressureEvents(int i);

    void onDisplayDeviceStateChangeLocked(int i);

    void onDisplaySwitchResolutionLocked(int i, int i2, int i3);

    void onExternalAudioRegister(int i, int i2);

    void onFocusedWindowChangeLocked(String str, String str2);

    void onForegroundActivityChangedLocked(String str, int i, int i2, String str2, int i3, int i4, String str3, boolean z, int i5, int i6, String str4);

    void onInputMethodShow(int i);

    void onInsetAnimationHide(int i);

    void onInsetAnimationShow(int i);

    void onMediaKey(int i);

    void onMediaKey(int i, int i2);

    void onPendingPackagesExempt(String str);

    void onPlayerEvent(int i, int i2, int i3, int i4);

    void onPlayerRlease(int i, int i2, int i3);

    void onPlayerTrack(int i, int i2, int i3, int i4);

    void onProcessKill(ProcessRecord processRecord);

    void onProcessStart(ProcessRecord processRecord);

    void onProviderConnectionChanged(ContentProviderConnection contentProviderConnection, boolean z);

    void onRecentsAnimationEnd();

    void onRecentsAnimationStart(ActivityRecord activityRecord);

    void onRecorderEvent(int i, int i2, int i3);

    void onRecorderRlease(int i, int i2);

    void onRecorderTrack(int i, int i2, int i3);

    void onReleaseWakelock(IBinder iBinder, int i);

    void onRemoteAnimationEnd();

    void onRemoteAnimationStart(RemoteAnimationTarget[] remoteAnimationTargetArr);

    void onSendPendingIntent(PendingIntentRecord.Key key, String str, String str2, Intent intent);

    void onUsbStateChanged(boolean z, long j, Intent intent);

    void onVpnChanged(boolean z, int i, String str);

    void onWallpaperComponentChanged(boolean z, int i, String str);

    void onWindowAnimationStartLocked(long j, SurfaceControl surfaceControl);

    void onWindowVisibilityChanged(int i, int i2, WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, boolean z);

    void playbackStateChanged(int i, int i2, int i3, int i4);

    void powerFrozenServiceReady(boolean z);

    void recordAudioFocus(int i, int i2, String str, boolean z);

    void recordAudioFocusLoss(int i, String str, int i2);

    boolean registerMultiTaskActionListener(int i, Handler handler, MultiTaskActionListener multiTaskActionListener);

    void removeFrozenPid(int i, int i2);

    void reportBinderState(int i, int i2, int i3, int i4, long j);

    void reportBinderTrans(int i, int i2, int i3, int i4, int i5, boolean z, long j, long j2);

    void reportNet(int i, long j);

    void reportSignal(int i, int i2, long j);

    void reportTrackStatus(int i, int i2, int i3, boolean z);

    boolean shouldInterceptAlarm(int i, int i2);

    boolean shouldInterceptProvider(int i, ProcessRecord processRecord, ProcessRecord processRecord2, boolean z);

    boolean shouldInterceptService(Intent intent, CallerInfo callerInfo, ServiceInfo serviceInfo);

    boolean shouldInterceptUpdateDisplayModeSpecs(int i, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs);

    boolean skipFrozenAppAnr(ApplicationInfo applicationInfo, int i, String str);

    void thawedByOther(int i, int i2);

    void uidAudioStatusChanged(int i, boolean z);

    void uidVideoStatusChanged(int i, boolean z);

    boolean unregisterMultiTaskActionListener(int i, MultiTaskActionListener multiTaskActionListener);

    void updateActivitiesVisibility();

    ArrayList<IAppState> updateAllAppUsageStats();

    long updateCpuStatsNow(boolean z);

    void updateProcess(ProcessRecord processRecord);
}
