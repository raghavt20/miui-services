package com.android.server.am;

import android.content.ComponentName;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.spc.PressureStateSettings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.rtboost.SchedBoostService;
import com.miui.server.smartpower.IAppState;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class OomAdjusterImpl extends OomAdjusterStub {
    public static final String ADJ_TYPE_BIND_IMPROVE = "bind-fix";
    public static final String ADJ_TYPE_CAMERA_IMPROVE = "camera-improve";
    public static final String ADJ_TYPE_GAME_IMPROVE = "game-improve";
    public static final String ADJ_TYPE_GAME_IMPROVE_DOWNLOAD = "game-improve-download";
    public static final String ADJ_TYPE_GAME_IMPROVE_PAYMENT = "game-improve-payment";
    public static final String ADJ_TYPE_PREVIOUS_IMPROVE = "previous-improve";
    public static final String ADJ_TYPE_PREVIOUS_IMPROVE_SCALE = "previous-improve-scale";
    public static final String ADJ_TYPE_RESIDENT_IMPROVE = "resident-improveAdj";
    public static final String ADJ_TYPE_WIDGET_DEGENERATE = "widget-degenerate";
    private static final int BACKGROUND_APP_COUNT_LIMIT;
    private static final int GAME_APP_BACKGROUND_STATE = 2;
    private static final int GAME_APP_TOP_TO_BACKGROUND_STATE = 4;
    private static final int GAME_SCENE_DEFAULT_STATE = 0;
    private static final int GAME_SCENE_DOWNLOAD_STATE = 2;
    private static final int GAME_SCENE_PLAYING_STATE = 3;
    private static final int GAME_SCENE_WATCHING_STATE = 4;
    private static final int HIGH_MEMORY_RESIDENT_APP_COUNT;
    private static boolean IMPROVE_RESIDENT_APP_ADJ_ENABLE = false;
    public static boolean LIMIT_BIND_VEISIBLE_ENABLED = false;
    private static final int LOW_MEMORY_RESIDENT_APP_COUNT;
    private static final int MAX_APP_WEEK_LAUNCH_COUNT = 21;
    private static final int MAX_APP_WEEK_TOP_TIME = 7200000;
    private static final int MAX_GAME_DOWNLOAD_TIME = 1200000;
    private static final int MAX_PAYMENT_SCENE_TIME = 120000;
    private static final int MAX_PREVIOUS_GAME_TIME = 600000;
    private static final int MAX_PREVIOUS_PROTECT_TIME = 1200000;
    private static final int MAX_PREVIOUS_TIME = 300000;
    private static final int MIDDLE_MEMORY_RESIDENT_APP_COUNT;
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PACKAGE_NAME_MAGE_PAYMENT_SDK = "com.xiaomi.gamecenter.sdk.service";
    private static final String PACKAGE_NAME_WECHAT = "com.tencent.mm";
    private static final int PREVIOUS_APP_CRITICAL_ADJ = 701;
    private static final int PREVIOUS_APP_MAJOR_ADJ = 702;
    private static final int PREVIOUS_APP_MINOR_ADJ = 730;
    private static final int PREVIOUS_PROTECT_CRITICAL_COUNT = PressureStateSettings.PREVIOUS_PROTECT_CRITICAL_COUNT;
    private static String QQ_PLAYER = null;
    private static final int RESIDENT_APP_COUNT;
    private static final int RESIDENT_APP_COUNT_LIMIT;
    private static boolean SCALE_BACKGROUND_APP_ADJ_ENABLE = false;
    private static final long TOTAL_MEMORY;
    private static final boolean UNTRUSTEDAPP_BG_ENABLED = false;
    private static final int WIDGET_PROTECT_TIME = 3000;
    private static RunningAppRecordMap mRunningGameMap;
    private static final ArraySet<String> mWidgetProcBCWhiteList;
    private static List<String> sPaymentAppList;
    private static List<String> sPreviousBackgroundAppList;
    private static List<String> sPreviousResidentAppList;
    private static Map<String, List<String>> sSubProcessAdjBindList;
    private static List<String> skipMoveCgroupList;
    private String mForegroundPkg = "";
    private String mForegroundResultToProc = "";
    private SmartPowerServiceInternal mSmartPowerService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<OomAdjusterImpl> {

        /* compiled from: OomAdjusterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final OomAdjusterImpl INSTANCE = new OomAdjusterImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public OomAdjusterImpl m548provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public OomAdjusterImpl m547provideNewInstance() {
            return new OomAdjusterImpl();
        }
    }

    static {
        ArraySet<String> arraySet = new ArraySet<>();
        mWidgetProcBCWhiteList = arraySet;
        LIMIT_BIND_VEISIBLE_ENABLED = SystemProperties.getBoolean("persist.sys.spc.bindvisible.enabled", true);
        ArrayList arrayList = new ArrayList();
        skipMoveCgroupList = arrayList;
        arrayList.add(PACKAGE_NAME_WECHAT);
        skipMoveCgroupList.add("com.tencent.mobileqq");
        mRunningGameMap = new RunningAppRecordMap();
        sSubProcessAdjBindList = new HashMap();
        sPreviousBackgroundAppList = new ArrayList();
        BACKGROUND_APP_COUNT_LIMIT = getBackgroundAppCount();
        SCALE_BACKGROUND_APP_ADJ_ENABLE = SystemProperties.getBoolean("persist.sys.spc.scale.backgorund.app.enable", false);
        IMPROVE_RESIDENT_APP_ADJ_ENABLE = SystemProperties.getBoolean("persist.sys.spc.resident.app.enable", false);
        int i = SystemProperties.getInt("persist.sys.miui.resident.app.count", 0);
        RESIDENT_APP_COUNT = i;
        TOTAL_MEMORY = Process.getTotalMemory() >> 30;
        HIGH_MEMORY_RESIDENT_APP_COUNT = i + 10;
        MIDDLE_MEMORY_RESIDENT_APP_COUNT = i + 5;
        LOW_MEMORY_RESIDENT_APP_COUNT = i + 2;
        RESIDENT_APP_COUNT_LIMIT = getResidentAppCount();
        sPreviousResidentAppList = new ArrayList();
        QQ_PLAYER = "com.tencent.qqmusic:QQPlayerService";
        sPaymentAppList = new ArrayList();
        arraySet.add("android.appwidget.action.APPWIDGET_UPDATE");
        arraySet.add("miui.appwidget.action.APPWIDGET_UPDATE");
        sPaymentAppList.add(PACKAGE_NAME_WECHAT);
        sPaymentAppList.add(ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        sPaymentAppList.add(PACKAGE_NAME_MAGE_PAYMENT_SDK);
    }

    private static int getBackgroundAppCount() {
        long totalMemByte = Process.getTotalMemory();
        if (totalMemByte > 12 * FormatBytesUtil.GB) {
            return 15;
        }
        if (totalMemByte > 8 * FormatBytesUtil.GB) {
            return 10;
        }
        return 0;
    }

    private static int getResidentAppCount() {
        long j = TOTAL_MEMORY;
        if (j > 6 && j <= 8) {
            return LOW_MEMORY_RESIDENT_APP_COUNT;
        }
        if (j > 8 && j <= 12) {
            return MIDDLE_MEMORY_RESIDENT_APP_COUNT;
        }
        if (j > 12) {
            return HIGH_MEMORY_RESIDENT_APP_COUNT;
        }
        return 0;
    }

    public static OomAdjusterImpl getInstance() {
        return (OomAdjusterImpl) OomAdjusterStub.getInstance();
    }

    public void onSystemReady() {
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
    }

    private final boolean computePreviousAdj(ProcessRecord app, int procState, int adj) {
        if (!SmartPowerPolicyConstants.TESTSUITSPECIFIC && app.hasActivities() && !Process.isIsolated(app.uid) && adj > 700) {
            ProcessStateRecord appState = app.mState;
            if (computeOomAdjForPaymentScene(app, procState) || computeOomAdjForGameApp(app, procState)) {
                return true;
            }
            long backgroundTime = SystemClock.uptimeMillis() - appState.getLastTopTime();
            if (backgroundTime > 300000) {
                return IMPROVE_RESIDENT_APP_ADJ_ENABLE && computeOomAdjForResidentApp(app, procState);
            }
            if (SCALE_BACKGROUND_APP_ADJ_ENABLE) {
                computeOomAdjForBackgroundApp(app, procState);
                return true;
            }
            modifyProcessRecordAdj(appState, false, PREVIOUS_APP_MAJOR_ADJ, procState, ADJ_TYPE_PREVIOUS_IMPROVE);
            return true;
        }
        return false;
    }

    private boolean computeOomAdjForBackgroundApp(ProcessRecord app, int procState) {
        int tempAdj;
        updateBackgroundAppList();
        if (sPreviousBackgroundAppList.contains(app.processName)) {
            int index = sPreviousBackgroundAppList.indexOf(app.processName);
            int tempAdj2 = BACKGROUND_APP_COUNT_LIMIT;
            if (index < tempAdj2) {
                tempAdj = (index * 2) + 700;
            } else {
                tempAdj = (tempAdj2 * 2) + 700;
            }
            modifyProcessRecordAdj(app.mState, false, tempAdj, procState, "ADJ_TYPE_PREVIOUS_IMPROVE_SCALE");
            return true;
        }
        return false;
    }

    private void updateBackgroundAppList() {
        ArrayList<IAppState.IRunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses();
        ArrayList<String> backgroundRunningAppList = new ArrayList<>();
        Iterator<IAppState.IRunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            IAppState.IRunningProcess runningProc = it.next();
            if (runningProc.getProcessName().equals(runningProc.getPackageName()) && runningProc.hasActivity()) {
                backgroundRunningAppList.add(runningProc.getProcessName());
            }
        }
        List<IAppState> AppStateList = this.mSmartPowerService.getAllAppState();
        List<IAppState> backgroundAppStateList = new ArrayList<>();
        for (IAppState appState : AppStateList) {
            if (backgroundRunningAppList.contains(appState.getPackageName()) && !appState.isSystemApp() && !isSpecialApp(appState.getPackageName())) {
                backgroundAppStateList.add(appState);
            }
        }
        Collections.sort(backgroundAppStateList, new Comparator<IAppState>() { // from class: com.android.server.am.OomAdjusterImpl.1
            @Override // java.util.Comparator
            public int compare(IAppState o1, IAppState o2) {
                long res = o1.getLastTopTime() - o2.getLastTopTime();
                if (res > 0) {
                    return -1;
                }
                if (res == 0) {
                    return 0;
                }
                return 1;
            }
        });
        sPreviousBackgroundAppList.clear();
        for (int i = 0; i < backgroundAppStateList.size(); i++) {
            String theProcess = backgroundAppStateList.get(i).getPackageName();
            if (!sPreviousBackgroundAppList.contains(theProcess)) {
                sPreviousBackgroundAppList.add(theProcess);
            }
        }
    }

    private boolean isSpecialApp(String packageName) {
        return packageName.equals("com.android.camera") || packageName.equals(PACKAGE_NAME_WECHAT) || SystemPressureController.getInstance().isGameApp(packageName);
    }

    public void dumpPreviousBackgroundApps(PrintWriter pw, String prefix) {
        if (sPreviousBackgroundAppList.size() > 0) {
            pw.println("BGAL:");
            for (int i = 0; i < sPreviousBackgroundAppList.size(); i++) {
                pw.print(prefix);
                pw.println(sPreviousBackgroundAppList.get(i));
            }
        }
    }

    private boolean isRunningWidgetBroadcast(ProcessRecord app) {
        BroadcastRecord brc;
        BroadcastProcessQueue brcProcessQueue = ActivityManagerServiceImpl.getInstance().getBroadcastProcessQueue(app.processName, app.uid);
        if (brcProcessQueue != null && brcProcessQueue.isActive() && (brc = brcProcessQueue.getActive()) != null && brc.intent != null && mWidgetProcBCWhiteList.contains(brc.intent.getAction())) {
            return true;
        }
        return false;
    }

    private final boolean computeWidgetAdj(ProcessRecord app, int adj) {
        if (app.processName == null || !app.processName.endsWith(":widgetProvider")) {
            return false;
        }
        if (adj != 0 || !isRunningWidgetBroadcast(app)) {
            app.mState.setLastSwitchToTopTime(0L);
        } else {
            if (app.mState.getLastSwitchToTopTime() <= 0) {
                app.mState.setLastSwitchToTopTime(SystemClock.elapsedRealtime());
                return false;
            }
            if (SystemClock.elapsedRealtime() - app.mState.getLastSwitchToTopTime() < 3000) {
                return false;
            }
        }
        ProcessStateRecord prcState = app.mState;
        modifyProcessRecordAdj(prcState, prcState.isCached(), 999, 19, ADJ_TYPE_WIDGET_DEGENERATE);
        return true;
    }

    private boolean computeOomAdjForResidentApp(ProcessRecord app, int procState) {
        SmartPowerServiceInternal smartPowerServiceInternal = this.mSmartPowerService;
        if (smartPowerServiceInternal != null) {
            updateResidentAppList(smartPowerServiceInternal.updateAllAppUsageStats());
        }
        if (sPreviousResidentAppList.contains(app.processName)) {
            modifyProcessRecordAdj(app.mState, false, PREVIOUS_APP_MINOR_ADJ, procState, ADJ_TYPE_RESIDENT_IMPROVE);
            return true;
        }
        return false;
    }

    private boolean improveOomAdjForCamera(ProcessRecord app, ProcessRecord topApp, int adj, int procState) {
        if (topApp != null && app.processName != null && adj > 400 && app != topApp && app.hasActivities() && TextUtils.equals(topApp.processName, "com.android.camera") && app.processName.equals(this.mForegroundResultToProc)) {
            modifyProcessRecordAdj(app.mState, app.mState.isCached(), 400, 13, ADJ_TYPE_CAMERA_IMPROVE);
            return true;
        }
        return false;
    }

    private boolean computeOomAdjForGameApp(ProcessRecord app, int procState) {
        ProcessStateRecord appState = app.mState;
        long backgroundTime = SystemClock.uptimeMillis() - appState.getLastTopTime();
        if (!SystemPressureController.getInstance().isGameApp(app.info.packageName)) {
            return false;
        }
        if (MemoryFreezeStub.getInstance().isNeedProtect(app)) {
            modifyProcessRecordAdj(appState, false, PREVIOUS_APP_CRITICAL_ADJ, procState, "game-improveAdj(memory-freeze)");
            return true;
        }
        if (backgroundTime <= 600000) {
            int gameCount = mRunningGameMap.size();
            int gameIndex = mRunningGameMap.getIndexForKey(app.info.packageName);
            int i = PREVIOUS_PROTECT_CRITICAL_COUNT;
            if (gameCount > i && gameIndex >= i) {
                return false;
            }
            modifyProcessRecordAdj(appState, false, PREVIOUS_APP_CRITICAL_ADJ, procState, ADJ_TYPE_GAME_IMPROVE);
            return true;
        }
        if (backgroundTime <= 1200000) {
            int gameScene = mRunningGameMap.getForKey(app.info.packageName).intValue();
            switch (gameScene) {
                case 2:
                    modifyProcessRecordAdj(appState, false, PREVIOUS_APP_MAJOR_ADJ, procState, ADJ_TYPE_GAME_IMPROVE_DOWNLOAD);
                    return true;
                case 3:
                case 4:
                default:
                    return false;
            }
        }
        mRunningGameMap.removeForKey(app.info.packageName);
        return false;
    }

    private void improveOomAdjForAudioProcess(ProcessRecord app) {
        if (QQ_PLAYER.equals(app.processName)) {
            ProcessStateRecord prcState = app.mState;
            if (SystemPressureController.getInstance().isAudioOrGPSProc(app.uid, app.getPid()) && !app.mServices.hasForegroundServices()) {
                prcState.setMaxAdj(200);
            } else {
                prcState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
            }
        }
    }

    public boolean isPaymentScene(ProcessRecord app) {
        if (sPaymentAppList.contains(this.mForegroundPkg) && this.mSmartPowerService.isProcessInTaskStack(app.info.uid, app.getPid()) && this.mSmartPowerService.isProcessInTaskStack(PACKAGE_NAME_MAGE_PAYMENT_SDK)) {
            return true;
        }
        return false;
    }

    private boolean computeOomAdjForPaymentScene(ProcessRecord app, int procState) {
        long backgroundTime = SystemClock.uptimeMillis() - app.mState.getLastTopTime();
        if (SmartPowerSettings.GAME_PAY_PROTECT_ENABLED && isPaymentScene(app) && backgroundTime <= 120000) {
            modifyProcessRecordAdj(app.mState, false, 400, procState, ADJ_TYPE_GAME_IMPROVE_PAYMENT);
            return true;
        }
        return false;
    }

    private void modifyProcessRecordAdj(ProcessStateRecord prcState, boolean cached, int CurRawAdj, int procState, String AdjType) {
        prcState.setCached(cached);
        prcState.setCurRawAdj(CurRawAdj);
        prcState.setCurRawProcState(procState);
        prcState.setAdjType(AdjType);
    }

    public boolean computeOomAdjLocked(ProcessRecord app, ProcessRecord topApp, int adj, int procState, boolean cycleReEval) {
        IAppState appState;
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            String isTop = app == topApp ? "true" : "false";
            Slog.i("ActivityManager", "computeOomAdjLocked processName = " + app.processName + " rawAdj=" + adj + " maxAdj=" + app.mState.getMaxAdj() + " procState=" + procState + " maxProcState=" + app.mState.mMaxProcState + " adjType=" + app.mState.getAdjType() + " isTop=" + isTop + " cycleReEval=" + cycleReEval);
        }
        if (adj < 0) {
            return false;
        }
        if (this.mSmartPowerService != null && app.info.uid > 10000 && adj > 700 && isImportantSubProc(app.info.packageName, app.processName) && (appState = this.mSmartPowerService.getAppState(app.info.uid)) != null && app.info.packageName.equals(appState.getPackageName())) {
            app.mState.setCurRawAdj(Math.max(appState.getMainProcAdj(), 200));
            app.mState.setAdjType(ADJ_TYPE_BIND_IMPROVE);
            return true;
        }
        boolean isChangeAdj = computeWidgetAdj(app, adj) || improveOomAdjForCamera(app, topApp, adj, procState);
        if (isChangeAdj) {
            return true;
        }
        if (!app.isKilled() && !app.isKilledByAm() && app.getThread() != null && adj > 0) {
            switch (procState) {
                case 10:
                case 15:
                case 16:
                case 17:
                case 18:
                    if (adj >= 1001 || app.mServices.hasAboveClient()) {
                        isChangeAdj = computePreviousAdj(app, procState, adj);
                        break;
                    }
                    break;
                case 19:
                    if (adj > 800 && ProcessProphetStub.getInstance().isNeedProtect(app)) {
                        app.mState.setCurRawAdj(ScreenRotationAnimationImpl.COVER_EGE);
                        app.mState.setAdjType("proc-prophet");
                        isChangeAdj = true;
                        break;
                    }
                    break;
            }
            improveOomAdjForAudioProcess(app);
        }
        return isChangeAdj;
    }

    public void updateResidentAppList(List<IAppState> appList) {
        if (appList == null) {
            return;
        }
        List<IAppState> residentList = new ArrayList<>();
        for (IAppState appState : appList) {
            if (!appState.isSystemApp() && !isSpecialApp(appState.getPackageName()) && appState.hasActivityApp() && appState.getLaunchCount() > 21 && appState.getTotalTimeInForeground() > 7200000) {
                residentList.add(appState);
            }
        }
        Collections.sort(residentList, new Comparator<IAppState>() { // from class: com.android.server.am.OomAdjusterImpl.2
            @Override // java.util.Comparator
            public int compare(IAppState o1, IAppState o2) {
                long res = o1.getTotalTimeInForeground() - o2.getTotalTimeInForeground();
                if (res > 0) {
                    return -1;
                }
                if (res == 0) {
                    return 0;
                }
                return 1;
            }
        });
        sPreviousResidentAppList.clear();
        for (int i = 0; i < Math.min(RESIDENT_APP_COUNT_LIMIT, residentList.size()); i++) {
            sPreviousResidentAppList.add(residentList.get(i).getPackageName());
        }
    }

    public void dumpPreviousResidentApps(PrintWriter pw, String prefix) {
        if (sPreviousResidentAppList.size() > 0) {
            pw.println("PRAL:");
            for (int i = 0; i < sPreviousResidentAppList.size(); i++) {
                pw.print(prefix);
                pw.println(sPreviousResidentAppList.get(i));
            }
        }
    }

    public boolean isImportantSubProc(String pkgName, String procName) {
        List<String> procList = sSubProcessAdjBindList.get(pkgName);
        if (procList != null && procList.contains(procName)) {
            return true;
        }
        return false;
    }

    public boolean shouldSkipDueToBind(ProcessRecord app, ProcessRecord client) {
        if (isImportantSubProc(client.info.packageName, client.processName) && app.processName.equals(app.info.packageName)) {
            return true;
        }
        return false;
    }

    public void applyOomAdjLocked(ProcessRecord app) {
        SmartPowerServiceInternal smartPowerServiceInternal = this.mSmartPowerService;
        if (smartPowerServiceInternal != null) {
            smartPowerServiceInternal.applyOomAdjLocked(app);
        }
    }

    public int computeBindServiceAdj(ProcessRecord app, int adj, ConnectionRecord connectService) {
        ProcessRecord client = connectService.binding.client;
        ProcessStateRecord cstate = client.mState;
        if (!ProcessManagerInternal.checkCtsProcess(app.processName) && LIMIT_BIND_VEISIBLE_ENABLED) {
            int clientAdj = cstate.getCurRawAdj();
            boolean isSystem = (app.info.flags & 129) != 0;
            if ((connectService.flags & 100663296) == 0 && clientAdj >= 0) {
                if ((connectService.flags & 1) != 0 && clientAdj < 200) {
                    if (isProcessPerceptible(client.uid, client.processName) || SmartPowerPolicyConstants.TESTSUITSPECIFIC) {
                        return 250;
                    }
                }
                return adj;
            }
            if (isSystem) {
                int adj2 = Math.max(clientAdj, 100);
                return adj2;
            }
            int adj3 = Math.max(clientAdj, 200);
            return adj3;
        }
        return Math.max(cstate.getCurRawAdj(), 100);
    }

    public static boolean isCacheProcessState(int procState) {
        return procState == 16 || procState == 17 || procState == 18;
    }

    public void compactBackgroundProcess(ProcessRecord proc) {
        ProcessStateRecord state = proc.mState;
        if (state.getCurProcState() != state.getSetProcState()) {
            int oldState = state.getSetProcState();
            int newState = state.getCurProcState();
            if (!isCacheProcessState(oldState) && isCacheProcessState(newState)) {
                SystemPressureController.getInstance().compactBackgroundProcess(proc.uid, proc.processName);
            }
        }
    }

    public boolean isSetUntrustedBgGroup(ProcessRecord app) {
        return false;
    }

    public void updateGameSceneRecordMap(String packageName, int gameScene, int appState) {
        switch (appState) {
            case 2:
            case 4:
                mRunningGameMap.put(packageName, Integer.valueOf(gameScene));
                return;
            case 3:
            default:
                return;
        }
    }

    public void setProperThreadPriority(ProcessRecord app, int pid, int renderThreadTid, int prio) {
        boolean boosted = RealTimeModeControllerImpl.get().checkThreadBoost(pid);
        if (!boosted) {
            super.setProperThreadPriority(app, pid, renderThreadTid, prio);
        } else {
            RealTimeModeControllerImpl.get().setThreadSavedPriority(new int[]{pid, renderThreadTid}, prio);
            Slog.d(SchedBoostService.TAG, app.processName + " already boosted, skip boost priority");
        }
    }

    public void resetProperThreadPriority(ProcessRecord app, int tid, int prio) {
        boolean boosted = RealTimeModeControllerImpl.get().checkThreadBoost(tid);
        if (tid != 0 && !boosted) {
            super.resetProperThreadPriority(app, tid, prio);
        } else {
            RealTimeModeControllerImpl.get().setThreadSavedPriority(new int[]{tid}, prio);
            Slog.d(SchedBoostService.TAG, app.processName + " boosting, skip reset priority");
        }
    }

    public void updateProcessAdjBindList(String data) {
        if (!TextUtils.isEmpty(data)) {
            sSubProcessAdjBindList.clear();
            String[] pkgString = data.split(",");
            for (int i = 0; i < pkgString.length; i++) {
                if (!TextUtils.isEmpty(pkgString[i])) {
                    String[] appString = pkgString[i].split("-");
                    if (appString.length == 2 && !TextUtils.isEmpty(appString[0]) && !TextUtils.isEmpty(appString[1])) {
                        String[] procString = appString[1].split("\\+");
                        List<String> procList = new ArrayList<>();
                        for (int j = 0; j < procString.length; j++) {
                            if (!TextUtils.isEmpty(procString[j])) {
                                procList.add(procString[j]);
                            }
                        }
                        sSubProcessAdjBindList.put(appString[0], procList);
                    }
                }
            }
            Slog.d("ProcCloudControl", "subprocess adj bind list cloud control received: " + data);
        }
    }

    public void foregroundInfoChanged(String packageName, ComponentName component, String resultToProc) {
        if (!this.mForegroundPkg.equals(packageName)) {
            if (SystemPressureController.getInstance().isGameApp(packageName)) {
                mRunningGameMap.put(0, packageName, 0);
            }
            this.mForegroundPkg = packageName;
            this.mForegroundResultToProc = resultToProc;
        }
    }

    public void notifyProcessDied(ProcessRecord app) {
        if (app.info.packageName.equals(app.processName)) {
            mRunningGameMap.removeForKey(app.info.packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RunningAppRecordMap {
        private final List<String> mList;
        private final ArrayMap<String, Integer> mMap;

        private RunningAppRecordMap() {
            this.mMap = new ArrayMap<>();
            this.mList = new ArrayList();
        }

        public Integer getForKey(String packageName) {
            Integer value = this.mMap.get(packageName);
            return Integer.valueOf(value != null ? value.intValue() : -1);
        }

        public int getIndexForKey(String packageName) {
            return this.mList.indexOf(packageName);
        }

        public void put(String packageName, Integer gameScence) {
            if (!this.mList.contains(packageName)) {
                this.mList.add(packageName);
            }
            this.mMap.put(packageName, gameScence);
        }

        public void put(int index, String packageName, Integer gameScence) {
            if (this.mList.contains(packageName)) {
                this.mList.remove(packageName);
            }
            this.mList.add(index, packageName);
            this.mMap.put(packageName, gameScence);
        }

        public void removeForKey(String packageName) {
            this.mMap.remove(packageName);
            this.mList.remove(packageName);
        }

        public void clear() {
            this.mMap.clear();
            this.mList.clear();
        }

        public List<String> getList() {
            return this.mList;
        }

        public int size() {
            return this.mList.size();
        }
    }

    private boolean isProcessPerceptible(int uid, String processName) {
        SmartPowerServiceInternal smartPowerServiceInternal = this.mSmartPowerService;
        if (smartPowerServiceInternal != null) {
            return smartPowerServiceInternal.isProcessPerceptible(uid, processName);
        }
        return false;
    }
}
