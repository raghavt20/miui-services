package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IActivityTaskManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.policy.IKeyguardLockedStateListener;
import com.android.server.LocalServices;
import com.android.server.display.RhythmicEyeCareManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.WindowManagerService;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/* loaded from: classes.dex */
public class RhythmicEyeCareManager {
    private static final String ALARM_TAG = "rhythmic_eyecare_alarm";
    private static final int RHYTHMIC_APP_CATEGORY_TYPE_DEFAULT = 0;
    private static final int RHYTHMIC_APP_CATEGORY_TYPE_IMAGE = 1;
    private static final int RHYTHMIC_APP_CATEGORY_TYPE_READ = 2;
    private static final String TAG = "RhythmicEyeCareManager";
    private IActivityTaskManager mActivityTaskManager;
    private int mAlarmIndex;
    private AlarmManager mAlarmManager;
    private ComponentName mComponentName;
    private Handler mHandler;
    private boolean mIsDeskTopMode;
    private boolean mIsEnabled;
    private RhythmicEyeCareListener mRhythmicEyeCareListener;
    private int mTime;
    private SparseArray<List<String>> mAppCategoryMapper = new SparseArray<>();
    private int mAppType = 0;
    private int mDisplayState = 0;
    private boolean mAppChanged = true;
    private final AlarmManager.OnAlarmListener mOnAlarmListener = new AnonymousClass1();
    private final BroadcastReceiver mRhythmicEyeCareAlarmReceiver = new AnonymousClass2();
    private final TaskStackListener mTaskStackListener = new AnonymousClass3();
    private final IKeyguardLockedStateListener mKeyguardLockedStateListener = new AnonymousClass4();
    private final IProcessObserver.Stub mProcessObserver = new AnonymousClass5();
    private int[] mAlarmTimePoints = Resources.getSystem().getIntArray(285409357);
    private WindowManagerPolicy mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    private WindowManagerService mWindowManagerService = ServiceManager.getService(DumpSysInfoUtil.WINDOW);

    /* loaded from: classes.dex */
    public interface RhythmicEyeCareListener {
        void onRhythmicEyeCareChange(int i, int i2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.RhythmicEyeCareManager$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements AlarmManager.OnAlarmListener {
        AnonymousClass1() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onAlarm$0() {
            RhythmicEyeCareManager.this.updateState(true);
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RhythmicEyeCareManager.AnonymousClass1.this.lambda$onAlarm$0();
                }
            });
            RhythmicEyeCareManager rhythmicEyeCareManager = RhythmicEyeCareManager.this;
            rhythmicEyeCareManager.mAlarmIndex = (rhythmicEyeCareManager.mAlarmIndex + 1) % RhythmicEyeCareManager.this.mAlarmTimePoints.length;
            RhythmicEyeCareManager.this.setAlarm();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.RhythmicEyeCareManager$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (RhythmicEyeCareManager.this.mIsEnabled) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.DATE_CHANGED".equals(action)) {
                    if (RhythmicEyeCareManager.this.mOnAlarmListener != null) {
                        RhythmicEyeCareManager.this.mAlarmManager.cancel(RhythmicEyeCareManager.this.mOnAlarmListener);
                    }
                    RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$2$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            RhythmicEyeCareManager.AnonymousClass2.this.lambda$onReceive$0();
                        }
                    });
                    RhythmicEyeCareManager.this.scheduleRhythmicAlarm();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            RhythmicEyeCareManager.this.updateState(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.RhythmicEyeCareManager$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends TaskStackListener {
        AnonymousClass3() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onTaskStackChanged$0() {
            RhythmicEyeCareManager.this.updateState(false);
        }

        public void onTaskStackChanged() {
            RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RhythmicEyeCareManager.AnonymousClass3.this.lambda$onTaskStackChanged$0();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.RhythmicEyeCareManager$4, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass4 extends IKeyguardLockedStateListener.Stub {
        AnonymousClass4() {
        }

        public void onKeyguardLockedStateChanged(boolean isKeyguardLocked) {
            if (!isKeyguardLocked) {
                RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$4$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        RhythmicEyeCareManager.AnonymousClass4.this.lambda$onKeyguardLockedStateChanged$0();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onKeyguardLockedStateChanged$0() {
            RhythmicEyeCareManager.this.updateState(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.RhythmicEyeCareManager$5, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass5 extends IProcessObserver.Stub {
        AnonymousClass5() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onForegroundActivitiesChanged$0() {
            RhythmicEyeCareManager.this.updateState(false);
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$5$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RhythmicEyeCareManager.AnonymousClass5.this.lambda$onForegroundActivitiesChanged$0();
                }
            });
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onProcessDied$1() {
            RhythmicEyeCareManager.this.updateState(false);
        }

        public void onProcessDied(int pid, int uid) {
            RhythmicEyeCareManager.this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$5$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    RhythmicEyeCareManager.AnonymousClass5.this.lambda$onProcessDied$1();
                }
            });
        }
    }

    public RhythmicEyeCareManager(Context context, Looper looper) {
        this.mHandler = new Handler(looper);
        registerReceiver(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mActivityTaskManager = ActivityTaskManager.getService();
    }

    private void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.DATE_CHANGED");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        context.registerReceiver(this.mRhythmicEyeCareAlarmReceiver, intentFilter);
    }

    private void enableRhythmicEyeCareMode() {
        if (this.mIsEnabled) {
            return;
        }
        Slog.i(TAG, "enableRhythmicEyeCareMode");
        this.mIsEnabled = true;
        registerForegroundAppObserver();
        this.mWindowManagerService.addKeyguardLockedStateListener(this.mKeyguardLockedStateListener);
        scheduleRhythmicAlarm();
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RhythmicEyeCareManager.this.lambda$enableRhythmicEyeCareMode$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$enableRhythmicEyeCareMode$0() {
        updateState(true);
    }

    private void disableRhythmicEyeCareMode() {
        if (!this.mIsEnabled) {
            return;
        }
        Slog.i(TAG, "disableRhythmicEyeCareMode");
        this.mRhythmicEyeCareListener.onRhythmicEyeCareChange(this.mAppType, getMinuteOfDay());
        this.mAppType = 0;
        this.mAppChanged = true;
        this.mIsEnabled = false;
        this.mComponentName = null;
        this.mAlarmIndex = 0;
        AlarmManager.OnAlarmListener onAlarmListener = this.mOnAlarmListener;
        if (onAlarmListener != null) {
            this.mAlarmManager.cancel(onAlarmListener);
        }
        unregisterForegroundAppObserver();
        this.mWindowManagerService.removeKeyguardLockedStateListener(this.mKeyguardLockedStateListener);
    }

    public void setRhythmicEyeCareListener(RhythmicEyeCareListener listener) {
        this.mRhythmicEyeCareListener = listener;
    }

    public void updateRhythmicAppCategoryList(List<String> imageAppList, List<String> readAppList) {
        this.mAppCategoryMapper.put(1, imageAppList);
        this.mAppCategoryMapper.put(2, readAppList);
    }

    public void setModeEnable(boolean enable) {
        if (enable) {
            enableRhythmicEyeCareMode();
        } else {
            disableRhythmicEyeCareMode();
        }
    }

    private void registerForegroundAppObserver() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            ActivityManager.getService().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Slog.e(TAG, "registerForegroundAppObserver failed.", e);
        }
    }

    private void unregisterForegroundAppObserver() {
        try {
            this.mActivityTaskManager.unregisterTaskStackListener(this.mTaskStackListener);
            ActivityManager.getService().unregisterProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Slog.e(TAG, "unregisterForegroundAppObserver failed.", e);
        }
    }

    private int getMinuteOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        return (hour * 60) + minute;
    }

    private int getNextAlarmTimeIndex() {
        int[] iArr;
        int currentTime = getMinuteOfDay();
        int index = 0;
        while (true) {
            iArr = this.mAlarmTimePoints;
            if (index >= iArr.length || iArr[index] > currentTime) {
                break;
            }
            index++;
        }
        if (index == iArr.length) {
            return 0;
        }
        return index;
    }

    private int getAppTypeByPkgName(String pkgName) {
        for (int i = 0; i < this.mAppCategoryMapper.size(); i++) {
            int appType = this.mAppCategoryMapper.keyAt(i);
            List<String> appList = this.mAppCategoryMapper.get(appType);
            if (appList != null && appList.contains(pkgName)) {
                return appType;
            }
        }
        return 0;
    }

    private long getAlarmInMills(int time) {
        int currentTime = getMinuteOfDay();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (currentTime >= time) {
            calendar.add(6, 1);
        }
        calendar.set(11, time / 60);
        calendar.set(12, time % 60);
        calendar.set(13, 0);
        calendar.set(14, 0);
        Slog.d(TAG, getAlarmDateString(calendar.getTimeInMillis()));
        return calendar.getTimeInMillis();
    }

    private String getAlarmDateString(long mills) {
        StringBuilder builder = new StringBuilder();
        builder.append("next alarm:");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mills);
        int month = calendar.get(2) + 1;
        builder.append(month).append("-");
        int day = calendar.get(5);
        builder.append(day).append("-");
        int hour = calendar.get(11);
        builder.append(hour).append("-");
        int minute = calendar.get(12);
        builder.append(minute).append("-");
        int second = calendar.get(13);
        builder.append(minute).append(second);
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleRhythmicAlarm() {
        Slog.d(TAG, "scheduleRhythmicAlarm: " + Arrays.toString(this.mAlarmTimePoints));
        if (this.mAlarmTimePoints.length == 0) {
            Slog.w(TAG, "no alarm exists");
        } else {
            this.mAlarmIndex = getNextAlarmTimeIndex();
            setAlarm();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAlarm() {
        long startTime = getAlarmInMills(this.mAlarmTimePoints[this.mAlarmIndex]);
        this.mAlarmManager.setExact(0, startTime, ALARM_TAG, this.mOnAlarmListener, this.mHandler);
    }

    private boolean updateForegroundAppType() {
        ComponentName topActivity = null;
        boolean topActivityFullScreenOrNotOccluded = true;
        int visibleTaskNum = 0;
        try {
            List<ActivityTaskManager.RootTaskInfo> infos = this.mActivityTaskManager.getAllRootTaskInfos();
            Iterator<ActivityTaskManager.RootTaskInfo> it = infos.iterator();
            while (true) {
                boolean z = true;
                if (!it.hasNext()) {
                    break;
                }
                ActivityTaskManager.RootTaskInfo info = it.next();
                if (info.displayId == 0 && info.visible && !Objects.isNull(info.topActivity)) {
                    int windoingMode = info.getWindowingMode();
                    if (!this.mIsDeskTopMode) {
                        if (visibleTaskNum == 0) {
                            topActivity = info.topActivity;
                            if (windoingMode != 1) {
                                z = false;
                            }
                            topActivityFullScreenOrNotOccluded = z;
                        }
                        visibleTaskNum += info.childTaskIds.length;
                    } else {
                        if (windoingMode == 1) {
                            topActivity = info.topActivity;
                            break;
                        }
                        if (topActivityFullScreenOrNotOccluded) {
                            if (windoingMode != 5) {
                                z = false;
                            }
                            topActivityFullScreenOrNotOccluded = z;
                        }
                    }
                }
            }
            if (visibleTaskNum <= 1 && topActivityFullScreenOrNotOccluded && !Objects.isNull(topActivity) && !topActivity.equals(this.mComponentName)) {
                this.mComponentName = topActivity;
                int appType = this.mPolicy.isKeyguardShowingAndNotOccluded() ? 0 : getAppTypeByPkgName(topActivity.getPackageName());
                return updateAppType(appType);
            }
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "updateForegroundAppType failed.", e);
            return false;
        }
    }

    private boolean updateAppType(int appType) {
        if (this.mAppType != appType) {
            this.mAppType = appType;
            this.mAppChanged = true;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateState(boolean forceApply) {
        if ((updateForegroundAppType() || forceApply) && this.mDisplayState == 2) {
            int minute = getMinuteOfDay();
            if (this.mAppChanged || this.mTime != minute) {
                this.mAppChanged = false;
                this.mTime = getMinuteOfDay();
                Slog.i(TAG, "applyChange, appType=" + this.mAppType + ", minute=" + minute);
                this.mRhythmicEyeCareListener.onRhythmicEyeCareChange(this.mAppType, minute);
            }
        }
    }

    public void notifyScreenStateChanged(int state) {
        if (this.mDisplayState != state) {
            this.mDisplayState = state;
            if (this.mIsEnabled && state == 2) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.display.RhythmicEyeCareManager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        RhythmicEyeCareManager.this.lambda$notifyScreenStateChanged$1();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyScreenStateChanged$1() {
        updateState(true);
    }

    public void updateDeskTopMode(boolean deskTopModeEnabled) {
        this.mIsDeskTopMode = deskTopModeEnabled;
    }
}
