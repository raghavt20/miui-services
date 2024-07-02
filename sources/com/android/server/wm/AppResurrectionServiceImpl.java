package com.android.server.wm;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.FgThread;
import com.android.server.MiuiBgThread;
import com.android.server.am.IProcessPolicy;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.wm.AppResurrectionServiceImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import miui.util.DeviceLevel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.wm.AppResurrectionServiceStub$$")
/* loaded from: classes.dex */
public class AppResurrectionServiceImpl extends AppResurrectionServiceStub {
    private static final String ATTR_REBORN_CHILDREN_SIZE = "reborn_children_size";
    private static final String CLOUD_KEY_NAME = "KeyAppResurrection";
    private static final String CLOUD_MODULE_NAME = "ModuleAppResurrection";
    public static final String TAG = "AppResurrectionServiceImpl";
    private static final String TAG_REBORN_ACTIVITY = "reborn_activity";
    private static final ArrayList<String> sDefaultAppResurrectionEnableActivityList;
    private static final ArrayList<String> sDefaultAppResurrectionEnablePKGList;
    private static final JSONObject sDefaultPkg2MaxChildCountJObject;
    private static final ArrayList<String> sDefaultRebootResurrectionEnableActivityList;
    private static final ArrayList<String> sDefaultRebootResurrectionEnablePkgList;
    private static final ArrayList<String> sDefaultResurrectionEnableReasonList;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private HashMap<String, String> mPkg2KillReasonMap;
    private HashMap<String, Integer> mPkg2StartThemeMap;
    private ContentResolver mResolver;
    private boolean DEBUG = false;
    private final Handler mLoggerHandler = FgThread.getHandler();
    private final Handler mMIUIBgHandler = MiuiBgThread.getHandler();
    private AppResurrectionCloudManager mCloudManager = null;
    private AppResurrectionTrackManager mTrackManager = null;
    private volatile String mRebornPkg = "";
    private File mLocalSpFile = new File(new File(Environment.getDataDirectory(), "system"), "app_resurrection_pkg2theme.xml");
    private boolean mIsDeviceSupport = false;
    private boolean mCloudAppResurrectionEnable = true;
    private boolean mCloudRebootResurrectionEnable = true;
    private String mCloudAppResurrectionName = "local";
    private long mCloudAppResurrectionVersion = 0;
    private JSONArray mCloudAppResurrectionPKGJArray = null;
    private JSONArray mCloudAppResurrectionActivityJArray = null;
    private JSONArray mCloudRebootResurrectionPKGJArray = null;
    private JSONArray mCloudRebootResurrectionActivityJArray = null;
    private JSONObject mCloudPkg2MaxChildCountJObject = null;
    private int mCloudAppResurrectionMaxChildCount = 3;
    private int mCloudAppResurrectionInactiveDurationHour = 36;
    private long mCloudAppResurrectionLaunchTimeThresholdMillis = 3500;
    private String mCPULevel = IProcessPolicy.REASON_UNKNOWN;
    private boolean mIsLoadDone = false;
    private boolean mIsTestMode = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppResurrectionServiceImpl> {

        /* compiled from: AppResurrectionServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppResurrectionServiceImpl INSTANCE = new AppResurrectionServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AppResurrectionServiceImpl m2448provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AppResurrectionServiceImpl m2447provideNewInstance() {
            return new AppResurrectionServiceImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sDefaultAppResurrectionEnablePKGList = arrayList;
        arrayList.add("com.youku.phone");
        arrayList.add("com.qiyi.video");
        arrayList.add("com.hunantv.imgo.activity");
        arrayList.add("tv.danmaku.bili");
        arrayList.add("com.tencent.qqlive");
        arrayList.add("com.qiyi.video.lite");
        ArrayList<String> arrayList2 = new ArrayList<>();
        sDefaultAppResurrectionEnableActivityList = arrayList2;
        arrayList2.add("com.tencent.qqlive/.ona.activity.VideoDetailActivity");
        JSONObject jSONObject = new JSONObject();
        sDefaultPkg2MaxChildCountJObject = jSONObject;
        try {
            jSONObject.put("com.tencent.qqlive", 2);
        } catch (JSONException e) {
        }
        ArrayList<String> arrayList3 = new ArrayList<>();
        sDefaultResurrectionEnableReasonList = arrayList3;
        arrayList3.add(IProcessPolicy.REASON_AUTO_IDLE_KILL);
        arrayList3.add(IProcessPolicy.REASON_AUTO_POWER_KILL);
        ArrayList<String> arrayList4 = new ArrayList<>();
        sDefaultRebootResurrectionEnablePkgList = arrayList4;
        arrayList4.add("com.youku.phone");
        arrayList4.add("com.tencent.qqlive");
        arrayList4.add("com.hunantv.imgo.activity");
        arrayList4.add("tv.danmaku.bili");
        arrayList4.add("com.qiyi.video.lite");
        ArrayList<String> arrayList5 = new ArrayList<>();
        sDefaultRebootResurrectionEnableActivityList = arrayList5;
        arrayList5.add("tv.danmaku.bili/.ui.video.VideoDetailsActivity");
        arrayList5.add("tv.danmaku.bili/com.bilibili.ship.theseus.all.UnitedBizDetailsActivity");
        arrayList5.add("tv.danmaku.bili/com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity");
        arrayList5.add("com.hunantv.imgo.activity/com.mgtv.ui.player.VodPlayerPageActivity");
        arrayList5.add("com.hunantv.imgo.activity/com.mgtv.ui.videoplay.MGVideoPlayActivity");
        arrayList5.add("com.youku.phone/com.youku.ui.activity.DetailActivity");
        arrayList5.add("com.tencent.qqlive/.ona.activity.VideoDetailActivity");
        arrayList5.add("com.qiyi.video.lite/.videoplayer.activity.PlayerV2Activity");
    }

    public void init(Context context) {
        this.mContext = context;
        boolean isDeviceSupport = isDeviceSupport();
        this.mIsDeviceSupport = isDeviceSupport;
        if (!isDeviceSupport) {
            return;
        }
        this.mIsTestMode = SystemProperties.getBoolean("persist.sys.test_app_resurrection.enable", false);
        this.mPkg2StartThemeMap = new HashMap<>();
        this.mPkg2KillReasonMap = new HashMap<>();
        this.mCloudManager = new AppResurrectionCloudManager(context);
        this.mTrackManager = new AppResurrectionTrackManager(context);
        ContentResolver contentResolver = context.getContentResolver();
        this.mResolver = contentResolver;
        contentResolver.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(this.mMIUIBgHandler) { // from class: com.android.server.wm.AppResurrectionServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                Slog.i(AppResurrectionServiceImpl.TAG, "apprescloud i on Change:");
                AppResurrectionServiceImpl.this.lambda$init$1();
            }
        });
        this.mBroadcastReceiver = new AnonymousClass2();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppResurrectionServiceImpl.this.lambda$init$0();
            }
        });
        this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AppResurrectionServiceImpl.this.lambda$init$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.AppResurrectionServiceImpl$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_PRESENT".equals(action) && !AppResurrectionServiceImpl.this.mIsLoadDone) {
                AppResurrectionServiceImpl.this.mIsLoadDone = true;
                AppResurrectionServiceImpl.this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppResurrectionServiceImpl.AnonymousClass2.this.lambda$onReceive$0();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onReceive$0() {
            AppResurrectionServiceImpl.this.loadDataFromSP();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$0() {
        checkCPULevel(3000L, 3500L, 3500L, 3500L);
    }

    public int getAllowChildCount(String pkg) {
        JSONObject jSONObject = this.mCloudPkg2MaxChildCountJObject;
        if (jSONObject != null) {
            if (jSONObject.has(pkg)) {
                return this.mCloudPkg2MaxChildCountJObject.optInt(pkg, this.mCloudAppResurrectionMaxChildCount);
            }
        } else {
            JSONObject jSONObject2 = sDefaultPkg2MaxChildCountJObject;
            if (jSONObject2.has(pkg)) {
                return jSONObject2.optInt(pkg, this.mCloudAppResurrectionMaxChildCount);
            }
        }
        return this.mCloudAppResurrectionMaxChildCount;
    }

    public boolean isResurrectionEnable(String pkg) {
        if (this.DEBUG) {
            Slog.d(TAG, "isResurrectionEnable, start " + pkg);
        }
        if (TextUtils.isEmpty(pkg) || !this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable) {
            return false;
        }
        JSONArray jSONArray = this.mCloudAppResurrectionPKGJArray;
        if (jSONArray == null) {
            if (sDefaultAppResurrectionEnablePKGList.contains(pkg)) {
                if (this.DEBUG) {
                    Slog.d(TAG, "isResurrectionEnable, End " + pkg + " in default, ret true");
                }
                return true;
            }
        } else if (isKeyInJArray(jSONArray, pkg)) {
            if (this.DEBUG) {
                Slog.d(TAG, "isResurrectionEnable, End " + pkg + " in cloud, ret true");
            }
            return true;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "isResurrectionEnable, End " + pkg + " ret false");
        }
        return false;
    }

    public boolean isResurrectionEnable(String pkg, String reason, List<ActivityManager.RunningTaskInfo> tasks) {
        boolean ret;
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable || !isResurrectionEnable(pkg)) {
            return false;
        }
        if (tasks != null) {
            tasks.size();
            for (ActivityManager.RunningTaskInfo taskInfo : tasks) {
                if (taskInfo != null && taskInfo.baseActivity != null && taskInfo.topActivity != null) {
                    if (taskInfo.baseActivity.getPackageName().equals(pkg)) {
                        if (this.DEBUG) {
                            Slog.d(TAG, "isResE, start " + pkg + ", numAct:" + taskInfo.numActivities + ", taskInfo=" + taskInfo);
                        }
                        if (isPKGInAppResurrectionActivityList(pkg)) {
                            ComponentName topActivityComp = taskInfo.topActivity;
                            if (topActivityComp != null) {
                                String topActivityStr = topActivityComp.flattenToShortString();
                                if (!TextUtils.isEmpty(topActivityStr)) {
                                    if (this.DEBUG) {
                                        Slog.d(TAG, "isResE, " + pkg + ", topActivityStr:" + topActivityStr);
                                    }
                                    JSONArray jSONArray = this.mCloudAppResurrectionActivityJArray;
                                    if (jSONArray != null) {
                                        if (!isKeyInJArray(jSONArray, topActivityStr)) {
                                            Slog.v(TAG, "isResE, " + pkg + ", " + topActivityStr + " not in Clist, ret f, setRemove t");
                                            return false;
                                        }
                                        if (this.DEBUG) {
                                            Slog.v(TAG, "isResE, " + topActivityStr + ", topActivity in Clist, go on");
                                        }
                                    } else {
                                        if (!sDefaultAppResurrectionEnableActivityList.contains(topActivityStr)) {
                                            Slog.v(TAG, "isResE, " + pkg + ", " + topActivityStr + " not in Dlist, ret f, setRemove t");
                                            return false;
                                        }
                                        if (this.DEBUG) {
                                            Slog.v(TAG, "isResE, " + topActivityStr + ", topActivity in DList, go on");
                                        }
                                    }
                                }
                            }
                        } else if (this.DEBUG) {
                            Slog.v(TAG, "isResE, " + pkg + ", not in activity list, go on");
                        }
                        int allowChildCount = getAllowChildCount(pkg);
                        if (taskInfo.numActivities < 2) {
                            Slog.v(TAG, "isResE, " + pkg + ", " + taskInfo.numActivities + " too less, ret f, setRemove t");
                            return false;
                        }
                        if (taskInfo.numActivities > allowChildCount) {
                            Slog.v(TAG, "isResE, " + pkg + ", " + taskInfo.numActivities + " over allow " + allowChildCount + ", ret f, setRemove t");
                            return false;
                        }
                        String basePkg = taskInfo.baseActivity.getPackageName();
                        String topPkg = taskInfo.topActivity.getPackageName();
                        if (!TextUtils.isEmpty(basePkg) && !TextUtils.isEmpty(topPkg) && !basePkg.equals(topPkg)) {
                            Slog.v(TAG, "isResE, " + pkg + " not " + topPkg + " ret f, setRemove t");
                            return false;
                        }
                        if (sDefaultResurrectionEnableReasonList.contains(reason)) {
                            HashMap<String, String> hashMap = this.mPkg2KillReasonMap;
                            if (hashMap != null) {
                                if (hashMap.size() > 200) {
                                    this.mPkg2KillReasonMap.clear();
                                }
                                this.mPkg2KillReasonMap.put(pkg, reason);
                            }
                            ret = true;
                        } else {
                            ret = false;
                        }
                        Slog.v(TAG, "isResE, final " + pkg + ", reason:" + reason + ", ret:" + ret + " setRemove:" + (!ret));
                        return ret;
                    }
                    if (this.DEBUG) {
                        Slog.d(TAG, "isResE, start not equal, pkg: " + pkg + ", taskInfo.baseActivity.getPackageName():" + taskInfo.baseActivity.getPackageName() + ", continue");
                    }
                } else {
                    Slog.v(TAG, "isResE error, pkg:" + pkg);
                }
            }
        } else if (this.DEBUG) {
            Slog.v(TAG, "isResE, tasks == null");
        }
        Slog.v(TAG, "isResE, End " + pkg + ", reason:" + reason + ", ret: false, setRemove: ture");
        return false;
    }

    public boolean keepTaskInRecent(Task task) {
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable || task == null) {
            return false;
        }
        if (task.getRootActivity() == null) {
            if (this.DEBUG) {
                Slog.d(TAG, "keepTaskInRecent ret false, task.getRootActivity() == null task:" + task);
            }
            return false;
        }
        String rootActivityPKG = task.getRootActivity().packageName;
        if (!isResurrectionEnable(rootActivityPKG)) {
            if (this.DEBUG) {
                Slog.d(TAG, "keepTaskInRecent ret false, !isResurrectionEnable rootActivityPKG:" + rootActivityPKG);
            }
            return false;
        }
        if (task.getChildCount() < 2) {
            Slog.d(TAG, "keepTaskInRecent ret false, task.getChildCount() <2:" + task.getChildCount() + ", " + rootActivityPKG);
            return false;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "keepTaskInRecent go on:" + task);
        }
        long hoursInMs = TimeUnit.HOURS.toMillis(this.mCloudAppResurrectionInactiveDurationHour);
        long inactiveHours = TimeUnit.MILLISECONDS.toHours(task.getInactiveDuration());
        if (task.getInactiveDuration() > hoursInMs) {
            Slog.d(TAG, "keepTaskInRecent ret false, task.getInactiveDuration > hours :" + task.getInactiveDuration() + ", 36H:" + hoursInMs + ", inactiveHours=" + inactiveHours + ", " + rootActivityPKG);
            return false;
        }
        int allowChildCount = getAllowChildCount(rootActivityPKG);
        if (task.getChildCount() > allowChildCount) {
            Slog.d(TAG, "keepTaskInRecent ret false, task.getChildCount():" + task.getChildCount() + " > allow:" + allowChildCount + ", " + rootActivityPKG);
            return false;
        }
        if (isPKGInAppResurrectionActivityList(rootActivityPKG)) {
            if (this.DEBUG) {
                Slog.d(TAG, "keepTaskInRecent pkg In ActivityList");
            }
            String topMostActivity = "";
            ActivityRecord arTopMost = task.getTopMostActivity();
            if (arTopMost != null) {
                if (this.DEBUG) {
                    Slog.d(TAG, "keepTaskInRecent arTopMost=" + arTopMost.shortComponentName);
                }
                topMostActivity = arTopMost.shortComponentName;
            }
            if (TextUtils.isEmpty(topMostActivity)) {
                Slog.d(TAG, "keepTaskInRecent topMostActivity is empty, ret false, " + rootActivityPKG);
                return false;
            }
            JSONArray jSONArray = this.mCloudAppResurrectionActivityJArray;
            if (jSONArray != null) {
                if (!isKeyInJArray(jSONArray, topMostActivity)) {
                    Slog.d(TAG, "keepTaskInRecent topMostActivity is not in Clist, ret false, " + rootActivityPKG);
                    return false;
                }
            } else if (!sDefaultAppResurrectionEnableActivityList.contains(topMostActivity)) {
                Slog.d(TAG, "keepTaskInRecent topMostActivity is not in Dlist, ret false, " + rootActivityPKG);
                return false;
            }
        }
        if (!isChildrenActivityFromSamePkg(task)) {
            Slog.d(TAG, "keepTaskInRecent !isChildrenActivityFromSamePkg, ret false, " + rootActivityPKG);
            return false;
        }
        if (this.DEBUG) {
            Slog.v(TAG, "keepTaskInRecent task:" + task);
            return true;
        }
        return true;
    }

    public int getSplashScreenTheme(String packageName, int resolvedTheme, boolean newTask, boolean taskSwitch, boolean processRunning, boolean startActivity) {
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable) {
            return resolvedTheme;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "getSplashScreenResolvedTheme, pkg:" + packageName + ", resolvedTheme:" + resolvedTheme + ", newTask:" + newTask + ", taskSwitch:" + taskSwitch + ",processRunning:" + processRunning + ", startActivity:" + startActivity);
        }
        if (newTask && taskSwitch && !processRunning && startActivity) {
            HashMap<String, Integer> hashMap = this.mPkg2StartThemeMap;
            if (hashMap != null) {
                if (hashMap.size() > 50) {
                    this.mPkg2StartThemeMap.clear();
                }
                this.mPkg2StartThemeMap.put(packageName, Integer.valueOf(resolvedTheme));
            }
            return resolvedTheme;
        }
        if (!newTask && taskSwitch && !processRunning) {
            HashMap<String, Integer> hashMap2 = this.mPkg2StartThemeMap;
            if (hashMap2 != null && hashMap2.containsKey(packageName)) {
                int retResolvedTheme = this.mPkg2StartThemeMap.get(packageName).intValue();
                if (this.DEBUG) {
                    Slog.d(TAG, "getSplashScreenResolvedTheme reborn, get store " + packageName + ", retResolvedTheme:" + retResolvedTheme);
                }
                this.mRebornPkg = packageName;
                return retResolvedTheme;
            }
            Slog.d(TAG, "getSplashScreenResolvedTheme, no get " + packageName + ", resolvedTheme:" + resolvedTheme);
            return resolvedTheme;
        }
        if (!newTask && !taskSwitch && processRunning && !startActivity) {
            if (this.DEBUG) {
                Slog.d(TAG, "getSplashScreenResolvedTheme, back to main, resolvedTheme = 0");
                return 0;
            }
            return 0;
        }
        return resolvedTheme;
    }

    public boolean updateStartingWindowResolvedTheme(final String packageName, final int resolvedTheme, boolean newTask, boolean taskSwitch, boolean processRunning, boolean startActivity) {
        HashMap<String, Integer> hashMap;
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable) {
            return false;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "updateSplashScreenResolvedTheme, start pkg:" + packageName + ", resolvedTheme:" + resolvedTheme + ", newTask:" + newTask + ", taskSwitch:" + taskSwitch + ",processRunning:" + processRunning + ", startActivity:" + startActivity);
        }
        if (!newTask || !taskSwitch || processRunning || !startActivity || (hashMap = this.mPkg2StartThemeMap) == null) {
            return false;
        }
        if (hashMap.size() > 50) {
            this.mPkg2StartThemeMap.clear();
        }
        this.mPkg2StartThemeMap.put(packageName, Integer.valueOf(resolvedTheme));
        this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                AppResurrectionServiceImpl.this.lambda$updateStartingWindowResolvedTheme$2(packageName, resolvedTheme);
            }
        });
        if (this.DEBUG) {
            Slog.d(TAG, "updateSplashScreenResolvedTheme, final pkg:" + packageName + ", resolvedTheme:" + resolvedTheme + ", newTask:" + newTask + ", taskSwitch:" + taskSwitch + ",processRunning:" + processRunning + ", startActivity:" + startActivity);
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateStartingWindowResolvedTheme$2(String packageName, int resolvedTheme) {
        saveOneDataToSP(packageName, Integer.valueOf(resolvedTheme));
    }

    public int getStartingWindowResolvedTheme(String packageName, boolean newTask, boolean taskSwitch, boolean processRunning, boolean startActivity) {
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable) {
            return 0;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "getStartingWindowResolvedTheme, start newTask=" + newTask + ", taskSwitch=" + taskSwitch + ", processRunning=" + processRunning + ", startActivity=" + startActivity + ", mRebornPkg=" + this.mRebornPkg);
        }
        if (!newTask && taskSwitch && !processRunning) {
            HashMap<String, Integer> hashMap = this.mPkg2StartThemeMap;
            if (hashMap != null) {
                if (hashMap.containsKey(packageName)) {
                    int retResolvedTheme = this.mPkg2StartThemeMap.get(packageName).intValue();
                    Slog.v(TAG, "getStartingWindowResolvedTheme, get stored " + packageName + ", retResolvedTheme:" + retResolvedTheme);
                    this.mRebornPkg = packageName;
                    return retResolvedTheme;
                }
                if (this.DEBUG) {
                    Slog.d(TAG, "getStartingWindowResolvedTheme, no get " + packageName + ", ret 0");
                }
            }
        } else if (!newTask && !taskSwitch && processRunning && !startActivity && this.DEBUG) {
            Slog.d(TAG, "getStartingWindowResolvedTheme, back to main");
        }
        return 0;
    }

    public void notifyWindowsDrawn(final long windowsDrawnDelay, ActivityRecord lastLaunchedActivity, final ActivityRecord curTransitionActivity, final ComponentName curComponent) {
        long j;
        String killReason;
        if (!this.mIsDeviceSupport || !this.mCloudAppResurrectionEnable || curTransitionActivity == null) {
            return;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "notifyWindowsDrawn, windowsDrawnDelay = " + windowsDrawnDelay + ", lastLaunchedActivity=" + lastLaunchedActivity + ", curTransitionActivity=" + curTransitionActivity + ", mRebornPkg=" + this.mRebornPkg + ", curComponent:" + curComponent.flattenToShortString());
        }
        if (this.DEBUG) {
            Slog.d(TAG, "notifyWindowsDrawn, caller = " + Debug.getCallers(35));
        }
        Trace.traceBegin(8L, "AppResurrection_notifyWindowsDrawn");
        if (!this.mRebornPkg.equals(curTransitionActivity.packageName)) {
            j = 8;
        } else {
            HashMap<String, String> hashMap = this.mPkg2KillReasonMap;
            if (hashMap != null && hashMap.containsKey(this.mRebornPkg)) {
                String killReason2 = this.mPkg2KillReasonMap.get(this.mRebornPkg);
                killReason = killReason2;
            } else {
                killReason = IProcessPolicy.REASON_UNKNOWN;
            }
            final String fKillReason = killReason;
            if (curComponent != null) {
                this.mLoggerHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppResurrectionServiceImpl.this.lambda$notifyWindowsDrawn$3(curComponent, windowsDrawnDelay, fKillReason);
                    }
                });
                j = 8;
                this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppResurrectionServiceImpl.this.lambda$notifyWindowsDrawn$4(curComponent, windowsDrawnDelay, fKillReason);
                    }
                });
            } else {
                j = 8;
                this.mLoggerHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppResurrectionServiceImpl.this.lambda$notifyWindowsDrawn$5(curTransitionActivity, windowsDrawnDelay, fKillReason);
                    }
                });
                this.mMIUIBgHandler.post(new Runnable() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppResurrectionServiceImpl.this.lambda$notifyWindowsDrawn$6(curTransitionActivity, windowsDrawnDelay, fKillReason);
                    }
                });
            }
            checkLaunchTimeOverThreshold(this.mRebornPkg, windowsDrawnDelay);
            this.mRebornPkg = "";
        }
        Trace.traceEnd(j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyWindowsDrawn$3(ComponentName curComponent, long windowsDrawnDelay, String fKillReason) {
        logAppResurrectionDisplayed(curComponent.flattenToShortString(), windowsDrawnDelay, fKillReason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyWindowsDrawn$4(ComponentName curComponent, long windowsDrawnDelay, String fKillReason) {
        trackAppResurrectionDisplayed(curComponent.flattenToShortString(), windowsDrawnDelay, fKillReason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyWindowsDrawn$5(ActivityRecord curTransitionActivity, long windowsDrawnDelay, String fKillReason) {
        logAppResurrectionDisplayed(curTransitionActivity.packageName, windowsDrawnDelay, fKillReason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyWindowsDrawn$6(ActivityRecord curTransitionActivity, long windowsDrawnDelay, String fKillReason) {
        trackAppResurrectionDisplayed(curTransitionActivity.packageName, windowsDrawnDelay, fKillReason);
    }

    private void checkLaunchTimeOverThreshold(String pkg, long windowsDrawnDelay) {
        if (this.DEBUG) {
            Slog.d(TAG, "checkLaunchTimeOverThreshold  enter delay:" + windowsDrawnDelay + ", pkg: " + pkg);
        }
        if (windowsDrawnDelay > this.mCloudAppResurrectionLaunchTimeThresholdMillis) {
            JSONArray jSONArray = this.mCloudAppResurrectionPKGJArray;
            if (jSONArray == null) {
                ArrayList<String> arrayList = sDefaultAppResurrectionEnablePKGList;
                if (arrayList.contains(pkg)) {
                    arrayList.remove(pkg);
                    Slog.d(TAG, "checkLaunchTimeOverThreshold = " + windowsDrawnDelay + ", D: " + pkg);
                    return;
                }
                return;
            }
            if (isKeyInJArray(jSONArray, pkg)) {
                removeKeyFromJArray(this.mCloudAppResurrectionPKGJArray, pkg);
                Slog.d(TAG, "checkLaunchTimeOverThreshold = " + windowsDrawnDelay + ", C: " + pkg);
            }
        }
    }

    public boolean isSaveTask(String pkg) {
        return false;
    }

    public boolean isRestoreTask(String pkg) {
        return false;
    }

    public void dump(PrintWriter pw, String prefix) {
        if (pw == null) {
            return;
        }
        pw.print(prefix);
        pw.println("AppResurDevice:" + this.mIsDeviceSupport);
        pw.print(prefix);
        pw.println("AppResurName:" + this.mCloudAppResurrectionName);
        pw.print(prefix);
        pw.println("AppResurVer:" + this.mCloudAppResurrectionVersion);
        pw.print(prefix);
        pw.println("AppResurEnable:" + this.mCloudAppResurrectionEnable);
        pw.print(prefix);
        pw.println("AppResurCPU:" + this.mCPULevel);
        pw.print(prefix);
        pw.println("AppResurLaunchThres:" + this.mCloudAppResurrectionLaunchTimeThresholdMillis);
        pw.print(prefix);
        pw.println("AppResurMaxChildCount:" + this.mCloudAppResurrectionMaxChildCount);
        pw.print(prefix);
        pw.println("AppResurInactiveDuraH:" + this.mCloudAppResurrectionInactiveDurationHour);
        if (this.mCloudPkg2MaxChildCountJObject != null) {
            pw.print(prefix);
            pw.println("AppResurPkg2MaxChildC:" + this.mCloudPkg2MaxChildCountJObject);
        } else {
            JSONObject jSONObject = sDefaultPkg2MaxChildCountJObject;
            if (jSONObject != null) {
                pw.print(prefix);
                pw.println("AppResurPkg2MaxChildD:" + jSONObject);
            }
        }
        if (this.mCloudAppResurrectionPKGJArray != null) {
            pw.print(prefix);
            pw.println("AppResurPkgCArray:" + this.mCloudAppResurrectionPKGJArray.toString());
        } else {
            pw.print(prefix);
            pw.println("AppResurPkgDArray:" + printList(sDefaultAppResurrectionEnablePKGList));
        }
        if (this.mCloudAppResurrectionActivityJArray != null) {
            pw.print(prefix);
            pw.println("AppResurActivityCArray:" + this.mCloudAppResurrectionActivityJArray.toString());
        } else {
            pw.print(prefix);
            pw.println("AppResurActivityDArray:" + printList(sDefaultAppResurrectionEnableActivityList));
        }
        pw.print(prefix);
        pw.println("ReResurEnable:" + this.mCloudRebootResurrectionEnable);
        if (this.mCloudRebootResurrectionPKGJArray != null) {
            pw.print(prefix);
            pw.println("ReResurPkgCArray:" + this.mCloudRebootResurrectionPKGJArray.toString());
        } else {
            pw.print(prefix);
            pw.println("ReResurPkgDArray:" + printList(sDefaultRebootResurrectionEnablePkgList));
        }
        if (this.mCloudRebootResurrectionActivityJArray != null) {
            pw.print(prefix);
            pw.println("ReResurActivityCArray:" + this.mCloudRebootResurrectionActivityJArray.toString());
        } else {
            pw.print(prefix);
            pw.println("ReResurActivityDArray:" + printList(sDefaultRebootResurrectionEnableActivityList));
        }
    }

    private String printList(ArrayList<String> list) {
        if (list == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            sb.append(pkg);
            sb.append(", ");
        }
        String ret = sb.toString();
        return ret;
    }

    private boolean isKeyInJArray(JSONArray jArray, String key) {
        if (TextUtils.isEmpty(key) || jArray == null) {
            return false;
        }
        int len = jArray.length();
        for (int i = 0; i < len; i++) {
            if (key.equals(jArray.optString(i, ""))) {
                return true;
            }
        }
        return false;
    }

    private boolean removeKeyFromJArray(JSONArray jArray, String key) {
        if (TextUtils.isEmpty(key) || jArray == null) {
            return false;
        }
        int len = jArray.length();
        for (int index = 0; index < len; index++) {
            if (key.equals(jArray.optString(index, ""))) {
                jArray.remove(index);
                return true;
            }
        }
        return false;
    }

    private boolean isDeviceSupport() {
        boolean isDeviceSupport = SystemProperties.getBoolean("persist.sys.app_resurrection.enable", false);
        return isDeviceSupport;
    }

    private boolean isChildrenActivityFromSamePkg(Task task) {
        final String rootActivityPKG = task.getRootActivity().packageName;
        if (this.DEBUG) {
            Slog.d(TAG, "isChildrenActivitySameFromPkg ,rootActivityPKG:" + rootActivityPKG + ", task.getRootActivity():" + task.getRootActivity());
        }
        ActivityRecord notSamePKG = task.getActivity(new Predicate() { // from class: com.android.server.wm.AppResurrectionServiceImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean lambda$isChildrenActivityFromSamePkg$7;
                lambda$isChildrenActivityFromSamePkg$7 = AppResurrectionServiceImpl.this.lambda$isChildrenActivityFromSamePkg$7(rootActivityPKG, (ActivityRecord) obj);
                return lambda$isChildrenActivityFromSamePkg$7;
            }
        });
        if (notSamePKG != null) {
            if (this.DEBUG) {
                Slog.d(TAG, "isChildrenActivitySameFromPkg notSamePKGAR=" + notSamePKG + ", ret false");
                return false;
            }
            return false;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "isChildrenActivitySameFromPkg notSamePKGAR == null, ret true");
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$isChildrenActivityFromSamePkg$7(String rootActivityPKG, ActivityRecord r) {
        boolean ret = !rootActivityPKG.equals(r.packageName);
        if (this.DEBUG) {
            Slog.d(TAG, "isChildrenActivitySameFromPkg getActivity, r.packageName:" + r.packageName + ", rootActivityPKG=" + rootActivityPKG + ", ret=" + ret);
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: getCloudData, reason: merged with bridge method [inline-methods] */
    public void lambda$init$1() {
        MiuiSettings.SettingsCloudData.CloudData cloudData;
        Context context = this.mContext;
        if (context == null || (cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), CLOUD_KEY_NAME, (String) null, (String) null, false)) == null) {
            return;
        }
        JSONObject cloudDataJson = cloudData.json();
        if (cloudDataJson != null) {
            Slog.v(TAG, "getCloudData cloudDataJson !=null");
            if (this.DEBUG) {
                Slog.v(TAG, "getCloudData cloudDataJson:" + cloudDataJson);
            }
            this.mCloudAppResurrectionEnable = cloudDataJson.optBoolean("app_resur_enable", false);
            this.mCloudAppResurrectionVersion = cloudDataJson.optLong(AmapExtraCommand.VERSION_KEY, 0L);
            this.mCloudAppResurrectionName = cloudDataJson.optString("name", "local");
            this.mCloudAppResurrectionMaxChildCount = cloudDataJson.optInt("app_resur_max_child_count", 3);
            this.mCloudPkg2MaxChildCountJObject = cloudDataJson.optJSONObject("app_resur_max_child_map");
            this.mCloudAppResurrectionInactiveDurationHour = cloudDataJson.optInt("app_resur_inactive_hour", 36);
            long highCPULaunchTimeThreshold = cloudDataJson.optLong("app_resur_high_cpu_launch_time_threshold", 3000L);
            long middleCPULaunchTimeThreshold = cloudDataJson.optLong("app_resur_middle_cpu_launch_time_threshold", 3500L);
            long lowCPULaunchTimeThreshold = cloudDataJson.optLong("app_resur_low_cpu_launch_time_threshold", 3500L);
            checkCPULevel(highCPULaunchTimeThreshold, middleCPULaunchTimeThreshold, lowCPULaunchTimeThreshold, lowCPULaunchTimeThreshold);
            JSONArray appResurJArray = cloudDataJson.optJSONArray("app_resur_pkg");
            if (appResurJArray != null) {
                this.mCloudAppResurrectionPKGJArray = appResurJArray;
            }
            JSONArray appResurActivityJArray = cloudDataJson.optJSONArray("app_resur_activity");
            if (appResurActivityJArray != null) {
                this.mCloudAppResurrectionActivityJArray = appResurActivityJArray;
            }
            JSONArray appRebootResurJArray = cloudDataJson.optJSONArray("reboot_resur_pkg");
            if (appRebootResurJArray != null) {
                this.mCloudRebootResurrectionPKGJArray = appRebootResurJArray;
            }
            JSONArray appRebootResurActivityJArray = cloudDataJson.optJSONArray("reboot_resur_activity");
            if (appRebootResurActivityJArray != null) {
                this.mCloudRebootResurrectionActivityJArray = appRebootResurActivityJArray;
            }
            this.mCloudRebootResurrectionEnable = cloudDataJson.optBoolean("reboot_resur_enable", false);
            return;
        }
        Slog.v(TAG, "getCloudData cloudDataJson == null");
    }

    private void checkCPULevel(long high, long middle, long low, long unknown) {
        if (this.DEBUG) {
            Slog.d(TAG, "DeviceLevel.getDeviceLevel(DeviceLevel.DEV_STANDARD_VER, DeviceLevel.CPU): " + DeviceLevel.getDeviceLevel(1, DeviceLevel.CPU));
        }
        if (this.DEBUG) {
            Slog.d(TAG, "DeviceLevel.getDeviceLevel(DeviceLevel.DEV_STANDARD_VER, DeviceLevel.GPU): " + DeviceLevel.getDeviceLevel(1, DeviceLevel.GPU));
        }
        if (this.DEBUG) {
            Slog.d(TAG, "DeviceLevel.getDeviceLevel(DeviceLevel.DEV_STANDARD_VER, DeviceLevel.RAM): " + DeviceLevel.getDeviceLevel(1, DeviceLevel.RAM));
        }
        if (isCPUHighLevelDevice()) {
            this.mCPULevel = "High";
            this.mCloudAppResurrectionLaunchTimeThresholdMillis = high;
            if (this.DEBUG) {
                Slog.d(TAG, "checkCPULevel ret high");
                return;
            }
            return;
        }
        if (isCPUMiddleLevelDevice()) {
            this.mCPULevel = "Middle";
            this.mCloudAppResurrectionLaunchTimeThresholdMillis = middle;
            if (this.DEBUG) {
                Slog.d(TAG, "checkCPULevel ret middle");
                return;
            }
            return;
        }
        if (isCPULowLevelDevice()) {
            this.mCPULevel = "Low";
            this.mCloudAppResurrectionLaunchTimeThresholdMillis = low;
            if (this.DEBUG) {
                Slog.d(TAG, "checkCPULevel ret low");
                return;
            }
            return;
        }
        if (isCPUUnknownLevelDevice()) {
            this.mCPULevel = IProcessPolicy.REASON_UNKNOWN;
            this.mCloudAppResurrectionLaunchTimeThresholdMillis = unknown;
            if (this.DEBUG) {
                Slog.d(TAG, "checkCPULevel ret unknown");
            }
        }
    }

    private boolean isCPUHighLevelDevice() {
        return DeviceLevel.getDeviceLevel(1, DeviceLevel.CPU) == DeviceLevel.HIGH;
    }

    private boolean isCPUMiddleLevelDevice() {
        return DeviceLevel.getDeviceLevel(1, DeviceLevel.CPU) == DeviceLevel.MIDDLE;
    }

    private boolean isCPULowLevelDevice() {
        return DeviceLevel.getDeviceLevel(1, DeviceLevel.CPU) == DeviceLevel.LOW;
    }

    private boolean isCPUUnknownLevelDevice() {
        return DeviceLevel.getDeviceLevel(1, DeviceLevel.CPU) == DeviceLevel.UNKNOWN;
    }

    private void logAppResurrectionDisplayed(String shortComponentName, long delay, String killReason) {
        if (this.DEBUG) {
            Slog.d(TAG, "logAppResurrectionDisplayed, delay=" + delay + ", comp = " + shortComponentName + ", killReason= " + killReason);
        }
        EventLog.writeEvent(1030440, "[" + shortComponentName + "," + delay + "," + killReason + "]");
    }

    private void trackAppResurrectionDisplayed(String shortComponentName, long delay, String killReason) {
        if (this.DEBUG) {
            Slog.d(TAG, "trackAppResurrectionDisplayed, delay=" + delay + ", comp = " + shortComponentName + ", killReason=" + killReason);
        }
        this.mTrackManager.sendTrack(shortComponentName, delay, killReason);
    }

    private boolean isPKGInAppResurrectionActivityList(String PKG) {
        JSONArray jSONArray = this.mCloudAppResurrectionActivityJArray;
        if (jSONArray != null) {
            int len = jSONArray.length();
            for (int i = 0; i < len; i++) {
                String activityStr = this.mCloudAppResurrectionActivityJArray.optString(i, "");
                if (activityStr.startsWith(PKG + "/")) {
                    return true;
                }
            }
            return false;
        }
        Iterator<String> it = sDefaultAppResurrectionEnableActivityList.iterator();
        while (it.hasNext()) {
            String activityStr2 = it.next();
            if (activityStr2.startsWith(PKG + "/")) {
                return true;
            }
        }
        return false;
    }

    private void saveOneDataToSP(String pkg, Integer theme) {
        Context context;
        if (this.DEBUG) {
            Slog.d(TAG, "saveOneDataToSP, pkg=" + pkg + ", theme = " + theme);
        }
        try {
            context = this.mContext;
        } catch (Exception e) {
            Slog.w(TAG, "saveAllDataToSP Exception e:" + e.getMessage());
        }
        if (context == null) {
            return;
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(this.mLocalSpFile, 0).edit();
        if (!TextUtils.isEmpty(pkg)) {
            editor.putInt(pkg, theme.intValue());
        }
        editor.apply();
        if (this.DEBUG) {
            Slog.d(TAG, "saveOneDataToSP done");
        }
    }

    private void saveAllDataToSP() {
        if (this.DEBUG) {
            Slog.d(TAG, "saveAllDataToSP");
        }
        try {
            Context context = this.mContext;
            if (context == null) {
                return;
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(this.mLocalSpFile, 0).edit();
            HashMap<String, Integer> hashMap = this.mPkg2StartThemeMap;
            if (hashMap != null) {
                for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    if (!TextUtils.isEmpty(key)) {
                        editor.putInt(key, value.intValue());
                    }
                }
            }
            editor.apply();
        } catch (Exception e) {
            Slog.w(TAG, "saveAllDataToSP Exception e:" + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadDataFromSP() {
        Context context;
        if (this.DEBUG) {
            Slog.d(TAG, "loadDataFromSP start");
        }
        try {
            context = this.mContext;
        } catch (Exception e) {
            Slog.w(TAG, "loadDataFromSP Exception e:" + e.getMessage());
        }
        if (context == null) {
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(this.mLocalSpFile, 0);
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            String pkg = entry.getKey();
            Integer theme = (Integer) entry.getValue();
            if (this.mPkg2StartThemeMap != null && !TextUtils.isEmpty(pkg)) {
                if (this.DEBUG) {
                    Slog.d(TAG, "loadDataFromSP pkg=" + pkg + ", theme=" + theme);
                }
                this.mPkg2StartThemeMap.put(pkg, theme);
            }
        }
        if (this.DEBUG) {
            Slog.d(TAG, "loadDataFromSP done");
        }
    }

    public void saveActivityToXml(ActivityRecord r, TypedXmlSerializer out, int index) {
        if (r.isPersistable()) {
            try {
                out.startTag((String) null, TAG_REBORN_ACTIVITY + index);
                r.saveToXml(out);
                out.endTag((String) null, TAG_REBORN_ACTIVITY + index);
            } catch (Exception e) {
                e.printStackTrace();
                Slog.e("TaskPersister", "savetoxml failed at pkg:" + r.packageName + " activity:" + r.toString());
            }
        }
    }

    public boolean isActivityTag(String tag) {
        if (tag.contains(TAG_REBORN_ACTIVITY)) {
            return true;
        }
        return false;
    }

    public int getActivityIndex(String tag) {
        if (isActivityTag(tag)) {
            StringTokenizer stringTokenizer = new StringTokenizer(tag, TAG_REBORN_ACTIVITY);
            int index = Integer.parseInt(stringTokenizer.nextToken());
            return index;
        }
        return -1;
    }

    public boolean isRebootResurrectionEnable(String pkg) {
        if (this.mIsTestMode) {
            return true;
        }
        if (!this.mIsDeviceSupport || !this.mCloudRebootResurrectionEnable) {
            return false;
        }
        JSONArray jSONArray = this.mCloudRebootResurrectionPKGJArray;
        if (jSONArray == null) {
            if (sDefaultRebootResurrectionEnablePkgList.contains(pkg)) {
                return true;
            }
        } else if (isKeyInJArray(jSONArray, pkg)) {
            return true;
        }
        return false;
    }

    public boolean isRebootResurrectionActivity(String activityName) {
        if (this.mIsTestMode) {
            return true;
        }
        if (!this.mIsDeviceSupport || !this.mCloudRebootResurrectionEnable) {
            return false;
        }
        JSONArray jSONArray = this.mCloudRebootResurrectionActivityJArray;
        if (jSONArray == null) {
            if (sDefaultRebootResurrectionEnableActivityList.contains(activityName)) {
                return true;
            }
        } else if (isKeyInJArray(jSONArray, activityName)) {
            return true;
        }
        return false;
    }
}
