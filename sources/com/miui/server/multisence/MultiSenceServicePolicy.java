package com.miui.server.multisence;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.am.MiuiBoosterUtilsStub;
import com.miui.server.migard.MiGardService;
import com.miui.server.multisence.SingleWindowInfo;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.android.animation.internal.AnimTask;

/* loaded from: classes.dex */
public class MultiSenceServicePolicy {
    private static final String MCD_DF_PATH = "/data/system/mcd/mwdf";
    private static final String TAG = "MultiSenceServicePolicy";
    private static final int VRA_CMD_APP_CONTROL = 2;
    private static final int VRA_CMD_SYNC_WHITELIST = 1;
    String currentInputFocus;
    String dynamicSencePackage;
    String focusPackage;
    boolean isDynamicSenceStarting;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    private static IBinder mJoyoseService = null;
    private static final int FREEFORM_MOVE_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.freeform_move", 8000);
    private static final int FREEFORM_MOVE_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.freeform_move", 1);
    private static final int FREEFORM_RESIZE_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.freeform_resize", 8000);
    private static final int FREEFORM_RESIZE_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.freeform_resize", 1);
    private static final int FREEFORM_ENTERFULLSCREEN_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.freeform_enterfullscreen", 8000);
    private static final int FREEFORM_ENTERFULLSCREEN_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.freeform_enterfullscreen", 1);
    private static final int FREEFORM_ELUDE_TIMOUT = SystemProperties.getInt("persist.multisence.pri_time.freeform_elude", 1000);
    private static final int FREEFORM_ELUDE_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.freeform_elude", 1);
    private static final int SPLITSCREEN_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.splitscreen", 2000);
    private static final int SPLITSCREEN_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.splitscreen", 1);
    private static final int MWS_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.mws", SpeedTestModeServiceImpl.ENABLE_SPTM_MIN_MEMORY);
    private static final int MWS_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.mws", 1);
    private static final int DEFAULT_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.default", 1000);
    private static final int DEFAULT_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.default", 2);
    private static final int FW_PRIORITY_TIMEOUT = SystemProperties.getInt("persist.multisence.pri_time.floatingwindow_move", AnimTask.MAX_SINGLE_TASK_SIZE);
    private static final int FW_PRIORITY_LEVEL = SystemProperties.getInt("persist.multisence.pri_level.floatingwindow_move", 1);
    Map<String, SingleWindowInfo> schedWindows = new HashMap();
    private int mFreeformMoveRequestId = -1;
    private int mFreeformMoveBindCoreRequestId = -1;
    private int mFreeformResizeRequestId = -1;
    private int mFreeformResizeBindCoreRequestId = -1;
    private int mFreeformEnterFullScreenRequestId = -1;
    private int mFreeformEnterFullScreenBindCoreRequestId = -1;
    private int mFreeformEludeRequestId = -1;
    private int mFreeformEludeBindCoreRequestId = -1;
    private int mSplitscreenRequestId = -1;
    private int mSplitscreenBindCoreRequestId = -1;
    private int mMWSRequestId = -1;
    private int mMWSBindCoreRequestId = -1;
    private int mFWRequestId = -1;
    private int mFWBindCoreRequestId = -1;
    private ArrayList<Integer> mDefaultRequestIds = new ArrayList<>();
    private Set<String> mVrsWorkList = new HashSet();

    public MultiSenceServicePolicy() {
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, "new MultiSenceServicePolicy");
    }

    public void windowInfoPerPorcessing(Map<String, SingleWindowInfo> inWindows) {
        List<SingleWindowInfo> freeformWindows = new ArrayList<>();
        for (String appName : inWindows.keySet()) {
            SingleWindowInfo app = inWindows.get(appName);
            if (app.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM) {
                freeformWindows.add(app);
            }
        }
        limitFreeFormRes(freeformWindows);
    }

    public void updateSchedPolicy(Map<String, SingleWindowInfo> oldWindows, Map<String, SingleWindowInfo> inWindows) {
        this.schedWindows.clear();
        for (String name_in : inWindows.keySet()) {
            SingleWindowInfo inWindow = inWindows.get(name_in);
            if (oldWindows.containsKey(name_in)) {
                SingleWindowInfo oldWindow = oldWindows.get(name_in);
                updateExistedWindowPolicy(this.schedWindows, inWindow, oldWindow);
                oldWindows.remove(name_in);
            } else {
                updateNewWindowPolicy(this.schedWindows, inWindow);
            }
        }
        for (String name_left : oldWindows.keySet()) {
            SingleWindowInfo leftWindow = oldWindows.get(name_left);
            updateLeftWindowPolicy(this.schedWindows, leftWindow);
        }
    }

    private void updateLeftWindowPolicy(Map<String, SingleWindowInfo> schedList, SingleWindowInfo leftWindow) {
        String name = leftWindow.getPackageName();
        if (name == null) {
            return;
        }
        leftWindow.setChangeStatus(SingleWindowInfo.WindowChangeState.RESET);
        this.schedWindows.put(name, leftWindow);
    }

    private void updateNewWindowPolicy(Map<String, SingleWindowInfo> schedList, SingleWindowInfo newWindow) {
        String name = newWindow.getPackageName();
        int count = newWindow.getWindowCount();
        if (name == null) {
            return;
        }
        if (newWindow.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM_MINI) {
            newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_SET);
            if (count == 1) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.DYN_FPS_SET);
            }
        }
        if (newWindow.isCovered()) {
            if (count == 1) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.DYN_FPS_SET);
            }
            if (newWindow.isVrsCovered()) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_SET);
            }
        }
        newWindow.setChangeStatus(SingleWindowInfo.WindowChangeState.CHANGED);
        this.schedWindows.put(name, newWindow);
    }

    private void updateExistedWindowPolicy(Map<String, SingleWindowInfo> schedList, SingleWindowInfo newWindow, SingleWindowInfo oldWindow) {
        String name = newWindow.getPackageName();
        int count = newWindow.getWindowCount();
        String oldName = oldWindow.getPackageName();
        if (name != null && !name.equals(oldName)) {
            return;
        }
        boolean isChanged = false;
        if (newWindow.isInputFocused() != oldWindow.isInputFocused()) {
            isChanged = true;
        }
        if (!newWindow.getWindowingModeString().equals(oldWindow.getWindowingModeString())) {
            isChanged = true;
        }
        if (!newWindow.isVrsCovered() && oldWindow.isVrsCovered()) {
            newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_RESET);
            isChanged = true;
        }
        if (newWindow.isCovered()) {
            if (count == 1) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.DYN_FPS_SET);
            }
            if (newWindow.isVrsCovered() && !oldWindow.isVrsCovered()) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_SET);
            }
            isChanged = true;
        }
        if (newWindow.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM_MINI) {
            if (count == 1) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.DYN_FPS_SET);
            }
            if (oldWindow.getWindowForm() != SingleWindowInfo.WindowForm.MUTIL_FREEDOM_MINI) {
                newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_SET);
            }
            isChanged = true;
        } else if (oldWindow.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM_MINI) {
            newWindow.setSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_RESET);
            isChanged = true;
        }
        newWindow.setChangeStatus(isChanged ? SingleWindowInfo.WindowChangeState.CHANGED : SingleWindowInfo.WindowChangeState.NOT_CHANGED);
        this.schedWindows.put(name, newWindow);
    }

    public void reset() {
        resetDynFps();
        resetVrs();
    }

    public void resetDynFps() {
        MultiSenceServiceUtils.writeToFile(MCD_DF_PATH, "\n");
    }

    public void resetVrs() {
        StringBuilder vrsResetCmd = new StringBuilder("");
        int count = 0;
        for (String appName : this.mVrsWorkList) {
            if (setVrsOff(vrsResetCmd, appName)) {
                count++;
            }
        }
        vrsResetCmd.append("\n");
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, vrsResetCmd.toString());
        if (count <= 0) {
            return;
        }
        setCommonVrsParams(2, vrsResetCmd.toString());
        this.mVrsWorkList.clear();
    }

    public void doSched() {
        perfSenceSched();
        showSchedWindowInfo();
        singleAppSched();
    }

    private void perfSenceSched() {
        frameDynamicFpsSched();
        frameVrsSched();
    }

    private boolean setVrsOn(StringBuilder cmdList, String name) {
        if (cmdList == null) {
            return false;
        }
        cmdList.append(name + "#1\n");
        if (!this.mVrsWorkList.contains(name)) {
            this.mVrsWorkList.add(name);
            return true;
        }
        return true;
    }

    private boolean setVrsOff(StringBuilder cmdList, String name) {
        if (cmdList == null) {
            return false;
        }
        cmdList.append(name + "#0\n");
        if (this.mVrsWorkList.contains(name)) {
            this.mVrsWorkList.remove(name);
            return true;
        }
        return true;
    }

    private void frameDynamicFpsSched() {
        SingleWindowInfo app;
        if (!MultiSenceConfig.DYN_FPS_ENABLED) {
            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_DYN_FPS || MultiSenceConfig.DEBUG_CONFIG, "dyn fps is not supported by multisence config");
            return;
        }
        StringBuilder dynFpsCmd = new StringBuilder("");
        for (String appName : this.schedWindows.keySet()) {
            if (appName != null && (app = this.schedWindows.get(appName)) != null && app.getChangeStatus() != SingleWindowInfo.WindowChangeState.RESET && app.checkSchedStatus(SingleWindowInfo.ScheduleStatus.DYN_FPS_SET)) {
                dynFpsCmd.append(appName + " " + app.getLimitFPS() + "\n");
            }
        }
        if (dynFpsCmd.length() == 0) {
            dynFpsCmd.append("\n");
        }
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_DYN_FPS, "App fps cmd: " + dynFpsCmd.toString());
        MultiSenceServiceUtils.writeToFile(MCD_DF_PATH, dynFpsCmd.toString());
    }

    private void frameVrsSched() {
        SingleWindowInfo app;
        if (!MultiSenceConfig.VRS_ENABLE) {
            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS || MultiSenceConfig.DEBUG_CONFIG, "vrs is not supported by multisence config");
            return;
        }
        StringBuilder vrsCmd = new StringBuilder("");
        int count = 0;
        for (String appName : this.schedWindows.keySet()) {
            if (appName != null && (app = this.schedWindows.get(appName)) != null) {
                if (!MultiSenceConfig.getInstance().checkVrsSupport(appName)) {
                    MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, appName + " is not it vrs whitelist.");
                } else if (app.getChangeStatus() == SingleWindowInfo.WindowChangeState.RESET) {
                    if (setVrsOff(vrsCmd, appName)) {
                        count++;
                    }
                } else if (app.checkSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_RESET)) {
                    if (setVrsOff(vrsCmd, appName)) {
                        count++;
                    }
                } else if (app.checkSchedStatus(SingleWindowInfo.ScheduleStatus.VRS_SET) && setVrsOn(vrsCmd, appName)) {
                    count++;
                }
            }
        }
        vrsCmd.append("\n");
        showVrsWorkApp();
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, Integer.toString(count) + " comonds for vrs-sched: " + vrsCmd.toString());
        if (count <= 0) {
            return;
        }
        setCommonVrsParams(2, vrsCmd.toString());
    }

    public void limitFreeFormRes(List<SingleWindowInfo> schedFreeForm) {
        boolean z = true;
        if (!MultiSenceConfig.POLICY_WINDOWSIZE_ENABLE) {
            if (!MultiSenceConfig.DEBUG_POLICY_BASE && !MultiSenceConfig.DEBUG_CONFIG) {
                z = false;
            }
            MultiSenceServiceUtils.msLogD(z, "windowsize policy is not supported by multisence config");
            return;
        }
        Collections.sort(schedFreeForm);
        int N = schedFreeForm.size();
        for (int i = 0; i < N; i++) {
            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, "schedFreeForm " + schedFreeForm.get(i).getPackageName() + " : " + schedFreeForm.get(i).getLayerOrder() + " : " + schedFreeForm.get(i).getRectValue().toString());
        }
        if (N <= 1) {
            return;
        }
        for (int i2 = 0; i2 < N - 1; i2++) {
            SingleWindowInfo app = schedFreeForm.get(i2);
            if (!app.isHighPriority()) {
                List<Rect> overLayList = new ArrayList<>();
                int curArea = app.getRectValue().width() * app.getRectValue().height();
                for (int j = i2 + 1; j < N; j++) {
                    SingleWindowInfo app_tmp = schedFreeForm.get(j);
                    Rect result = findOverlay(app.getRectValue(), app_tmp.getRectValue());
                    if (result != null) {
                        overLayList.add(result);
                        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, "above layer is " + app_tmp.getPackageName() + ", overlay info " + result.toString());
                    }
                }
                float scale = getOverLayScale(curArea, overLayList);
                MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, app.getPackageName() + " is covered : " + (100.0f * scale) + "%");
                if (scale >= 0.9d && scale <= 1.0f) {
                    app.getPackageName();
                    app.setCovered(true);
                    float unCoveredArea = curArea * (1.0f - scale);
                    MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, app.getPackageName() + " is uncovered : " + unCoveredArea);
                    if (unCoveredArea < 50000.0f) {
                        app.setVrsCovered(true);
                    }
                }
            } else {
                return;
            }
        }
    }

    public float getOverLayScale(int curArea, List<Rect> overLayList) {
        int overArea;
        int N = overLayList.size();
        if (N == 0) {
            overArea = 0;
        } else if (N == 1) {
            overArea = overLayList.get(0).height() * overLayList.get(0).width();
        } else {
            int[][] overLayArray = convertToArray(overLayList);
            overArea = rectangleArea(overLayArray);
        }
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, "curArea: " + curArea + ", overArea: " + overArea);
        return overArea / curArea;
    }

    public int[][] convertToArray(List<Rect> overLayList) {
        int N = overLayList.size();
        int[][] arr = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, N, 4);
        for (int i = 0; i < N; i++) {
            arr[i][0] = overLayList.get(i).left;
            arr[i][1] = overLayList.get(i).top;
            arr[i][2] = overLayList.get(i).right;
            arr[i][3] = overLayList.get(i).bottom;
        }
        return arr;
    }

    public int rectangleArea(int[][] rs) {
        char c;
        List<Integer> list = new ArrayList<>();
        int length = rs.length;
        int i = 0;
        int i2 = 0;
        while (true) {
            c = 2;
            if (i2 >= length) {
                break;
            }
            int[] info = rs[i2];
            list.add(Integer.valueOf(info[0]));
            list.add(Integer.valueOf(info[2]));
            i2++;
        }
        Collections.sort(list);
        long ans = 0;
        int i3 = 1;
        while (i3 < list.size()) {
            int a = list.get(i3 - 1).intValue();
            int b = list.get(i3).intValue();
            int len = b - a;
            if (len != 0) {
                List<int[]> lines = new ArrayList<>();
                int length2 = rs.length;
                for (int i4 = i; i4 < length2; i4++) {
                    int[] info2 = rs[i4];
                    if (info2[i] <= a && b <= info2[c]) {
                        lines.add(new int[]{info2[1], info2[3]});
                    }
                }
                Collections.sort(lines, new Comparator() { // from class: com.miui.server.multisence.MultiSenceServicePolicy$$ExternalSyntheticLambda0
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return MultiSenceServicePolicy.lambda$rectangleArea$0((int[]) obj, (int[]) obj2);
                    }
                });
                long tot = 0;
                long l = -1;
                long r = -1;
                for (int[] cur : lines) {
                    int a2 = a;
                    if (cur[i] > r) {
                        tot += r - l;
                        i = 0;
                        long l2 = cur[0];
                        r = cur[1];
                        l = l2;
                    } else {
                        i = 0;
                        if (cur[1] > r) {
                            r = cur[1];
                        }
                    }
                    a = a2;
                }
                ans += len * (tot + (r - l));
            }
            i3++;
            c = 2;
        }
        int i5 = (int) ans;
        return i5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$rectangleArea$0(int[] l1, int[] l2) {
        int i;
        int i2;
        if (l1[0] != l2[0]) {
            i = l1[0];
            i2 = l2[0];
        } else {
            i = l1[1];
            i2 = l2[1];
        }
        return i - i2;
    }

    public Rect findOverlay(Rect rect1, Rect rect2) {
        int x1 = Math.max(rect1.left, rect2.left);
        int y1 = Math.max(rect1.top, rect2.top);
        int x2 = Math.min(rect1.right, rect2.right);
        int y2 = Math.min(rect1.bottom, rect2.bottom);
        if (x1 < x2 && y1 < y2) {
            return new Rect(x1, y1, x2, y2);
        }
        return null;
    }

    private void singleAppSched() {
        for (String key : this.schedWindows.keySet()) {
            this.schedWindows.get(key).setChangeStatus(SingleWindowInfo.WindowChangeState.NOT_CHANGED);
        }
    }

    public void updateInputFocus(String newInputFocus) {
        if (newInputFocus == null || "".equals(newInputFocus)) {
            LOG_IF_DEBUG("updateInputFocus: fail, null");
        } else if (!newInputFocus.equals(this.currentInputFocus)) {
            this.currentInputFocus = newInputFocus;
            LOG_IF_DEBUG("input focus changed: " + newInputFocus);
            calculateFocusAndPolicy();
        }
    }

    public void updatePowerMode(boolean isPowerMode) {
        LOG_IF_DEBUG("updatePowerMode: isPowerMode= " + isPowerMode);
        MiuiBoosterUtilsStub.getInstance().notifyPowerModeChanged(isPowerMode);
    }

    private void calculateFocusAndPolicy() {
        String newFocus;
        if (!MultiSenceConfig.FREMAPREDICT_ENABLE) {
            LOG_IF_DEBUG("framepredict is not support by multisence");
            return;
        }
        LOG_IF_DEBUG("calculateFocusAndPolicy: currentInputFocus=" + this.currentInputFocus + " isDynamicSenceStarting=" + this.isDynamicSenceStarting + " dynamicSencePackage" + this.dynamicSencePackage);
        if (!this.isDynamicSenceStarting) {
            newFocus = this.currentInputFocus;
        } else {
            newFocus = this.dynamicSencePackage;
        }
        if (newFocus == null || "".equals(newFocus)) {
            LOG_IF_DEBUG("calculateFocusAndPolicy: fail, newFocus is null");
        } else if (!newFocus.equals(this.focusPackage)) {
            LOG_IF_DEBUG("final focus changed: " + newFocus + " old focus=" + this.focusPackage);
            this.focusPackage = newFocus;
            MiuiBoosterUtilsStub.getInstance().notifyForegroundAppChanged(this.focusPackage);
        }
    }

    public void dynamicSenceSchedFreeformActionMove(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mFreeformMoveRequestId == -1) {
                this.mFreeformMoveRequestId = requestThreadPriority(uid, tids, FREEFORM_MOVE_PRIORITY_TIMEOUT, FREEFORM_MOVE_PRIORITY_LEVEL);
                LOG_IF_DEBUG("FreeformMoveRequestId gains: " + this.mFreeformMoveRequestId);
            }
            if (this.mFreeformMoveBindCoreRequestId == -1) {
                this.mFreeformMoveBindCoreRequestId = requestBindCore(uid, tids, FREEFORM_MOVE_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("FreeformMoveBindCoreRequestId gains: " + this.mFreeformMoveBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mFreeformMoveRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mFreeformMoveRequestId);
            this.mFreeformMoveRequestId = -1;
        }
        if (this.mFreeformMoveBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mFreeformMoveBindCoreRequestId);
            this.mFreeformMoveBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedFreeformActionResize(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mFreeformResizeRequestId == -1) {
                this.mFreeformResizeRequestId = requestThreadPriority(uid, tids, FREEFORM_RESIZE_PRIORITY_TIMEOUT, FREEFORM_RESIZE_PRIORITY_LEVEL);
                LOG_IF_DEBUG("FreeformResizeRequestId gains: " + this.mFreeformResizeRequestId);
            }
            if (this.mFreeformResizeBindCoreRequestId == -1) {
                this.mFreeformResizeBindCoreRequestId = requestBindCore(uid, tids, FREEFORM_RESIZE_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("FreeformResizeBindCoreRequestId gains: " + this.mFreeformResizeBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mFreeformResizeRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mFreeformResizeRequestId);
            this.mFreeformResizeRequestId = -1;
        }
        if (this.mFreeformResizeBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mFreeformResizeBindCoreRequestId);
            this.mFreeformResizeBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedFreeformActionEnterFullScreen(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mFreeformEnterFullScreenRequestId == -1) {
                this.mFreeformEnterFullScreenRequestId = requestThreadPriority(uid, tids, FREEFORM_ENTERFULLSCREEN_PRIORITY_TIMEOUT, FREEFORM_ENTERFULLSCREEN_PRIORITY_LEVEL);
                LOG_IF_DEBUG("FreeformEnterFullScreenRequestId gains: " + this.mFreeformEnterFullScreenRequestId);
            }
            if (this.mFreeformEnterFullScreenBindCoreRequestId == -1) {
                this.mFreeformEnterFullScreenBindCoreRequestId = requestBindCore(uid, tids, FREEFORM_ENTERFULLSCREEN_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("FreeformEnterFullScreenBindCoreRequestId gains: " + this.mFreeformEnterFullScreenBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mFreeformEnterFullScreenRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mFreeformEnterFullScreenRequestId);
            this.mFreeformEnterFullScreenRequestId = -1;
        }
        if (this.mFreeformEnterFullScreenBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mFreeformEnterFullScreenBindCoreRequestId);
            this.mFreeformEnterFullScreenBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedFreeformActionElude(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mFreeformEludeRequestId == -1) {
                this.mFreeformEludeRequestId = requestThreadPriority(uid, tids, FREEFORM_ELUDE_TIMOUT, FREEFORM_ELUDE_PRIORITY_LEVEL);
                LOG_IF_DEBUG("freeform-elude proi requset id gains: " + this.mFreeformEludeRequestId);
            }
            if (this.mFreeformEludeBindCoreRequestId == -1) {
                this.mFreeformEludeBindCoreRequestId = requestBindCore(uid, tids, FREEFORM_ELUDE_TIMOUT, 4);
                LOG_IF_DEBUG("freeform-elude bindcore requset id gains: " + this.mFreeformEludeBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mFreeformEludeRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mFreeformEludeRequestId);
            this.mFreeformEludeRequestId = -1;
        }
        if (this.mFreeformEludeBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mFreeformEludeBindCoreRequestId);
            this.mFreeformEludeBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedSplitscreenctionDivider(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mSplitscreenRequestId == -1) {
                this.mSplitscreenRequestId = requestThreadPriority(uid, tids, SPLITSCREEN_PRIORITY_TIMEOUT, SPLITSCREEN_PRIORITY_LEVEL);
                LOG_IF_DEBUG("SplitscreenRequestId gains: " + this.mSplitscreenRequestId);
            }
            if (this.mSplitscreenBindCoreRequestId == -1) {
                this.mSplitscreenBindCoreRequestId = requestBindCore(uid, tids, SPLITSCREEN_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("SplitscreenBindCoreRequestId gains: " + this.mSplitscreenBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mSplitscreenRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mSplitscreenRequestId);
            this.mSplitscreenRequestId = -1;
        }
        if (this.mSplitscreenBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mSplitscreenBindCoreRequestId);
            this.mSplitscreenBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedMWSActionMove(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mMWSRequestId == -1) {
                this.mMWSRequestId = requestThreadPriority(uid, tids, MWS_PRIORITY_TIMEOUT, MWS_PRIORITY_LEVEL);
                LOG_IF_DEBUG("MWSRequestId gains: " + this.mMWSRequestId);
            }
            if (this.mMWSBindCoreRequestId == -1) {
                this.mMWSBindCoreRequestId = requestBindCore(uid, tids, MWS_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("MWSBindCoreRequestId gains: " + this.mMWSBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mMWSRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mMWSRequestId);
            this.mMWSRequestId = -1;
        }
        if (this.mMWSBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mMWSBindCoreRequestId);
            this.mMWSBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedFloatingWindowMove(int uid, int[] tids, boolean isStarted) {
        calculateFocusAndPolicy();
        if (isStarted) {
            if (this.mFWRequestId == -1) {
                this.mFWRequestId = requestThreadPriority(uid, tids, FW_PRIORITY_TIMEOUT, FW_PRIORITY_LEVEL);
                LOG_IF_DEBUG("FWMoveRequestId gains: " + this.mFWRequestId);
            }
            if (this.mFWBindCoreRequestId == -1) {
                this.mFWBindCoreRequestId = requestBindCore(uid, tids, FW_PRIORITY_TIMEOUT, 4);
                LOG_IF_DEBUG("FWBindCoreRequestId gains: " + this.mFWBindCoreRequestId);
                return;
            }
            return;
        }
        if (this.mFWRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, this.mFWRequestId);
            this.mFWRequestId = -1;
        }
        if (this.mFWBindCoreRequestId != -1) {
            MiuiBoosterUtilsStub.getInstance().cancelBindCore(uid, this.mFWBindCoreRequestId);
            this.mFWBindCoreRequestId = -1;
        }
    }

    public void dynamicSenceSchedDefault(int uid, int[] tids, boolean isStarted, String packageName) {
        this.isDynamicSenceStarting = isStarted;
        this.dynamicSencePackage = packageName;
        calculateFocusAndPolicy();
        if (isStarted) {
            int requsetId = MiuiBoosterUtilsStub.getInstance().requestThreadPriority(uid, tids, DEFAULT_PRIORITY_TIMEOUT, DEFAULT_PRIORITY_LEVEL);
            if (!this.mDefaultRequestIds.contains(Integer.valueOf(requsetId))) {
                this.mDefaultRequestIds.add(Integer.valueOf(requsetId));
                LOG_IF_DEBUG("Default requestId gains: " + requsetId);
                return;
            }
            return;
        }
        if (!this.mDefaultRequestIds.isEmpty()) {
            Iterator<Integer> it = this.mDefaultRequestIds.iterator();
            while (it.hasNext()) {
                int defaultRequestId = it.next().intValue();
                LOG_IF_DEBUG("defaultRequestId cancel: " + defaultRequestId);
                MiuiBoosterUtilsStub.getInstance().cancelThreadPriority(uid, defaultRequestId);
            }
            this.mDefaultRequestIds.clear();
        }
    }

    private int requestThreadPriority(int uid, int[] tids, int timeout, int level) {
        return MiuiBoosterUtilsStub.getInstance().requestThreadPriority(uid, tids, timeout, level);
    }

    private int requestBindCore(int uid, int[] tids, int timeout, int level) {
        return MiuiBoosterUtilsStub.getInstance().requestBindCore(uid, tids, level, timeout);
    }

    public void setCommonVrsParams(int cmd, String params) {
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, "start send VRS cmd to joyose.");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                if (mJoyoseService == null) {
                    IBinder service = ServiceManager.getService(MiGardService.JOYOSE_NAME);
                    mJoyoseService = service;
                    if (service == null) {
                        MultiSenceServiceUtils.msLogW(MultiSenceConfig.DEBUG_VRS, "not find joyose service");
                        if (data != null) {
                            data.recycle();
                        }
                        if (reply != null) {
                            reply.recycle();
                            return;
                        }
                        return;
                    }
                }
                data.writeInterfaceToken("com.xiaomi.joyose.IJoyoseInterface");
                data.writeInt(cmd);
                data.writeString(params);
                mJoyoseService.transact(24, data, reply, 0);
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            } catch (Exception e) {
                mJoyoseService = null;
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    private void showSchedWindowInfo() {
        for (String name : this.schedWindows.keySet()) {
            SingleWindowInfo app = this.schedWindows.get(name);
            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_POLICY_BASE, app.toString());
        }
    }

    private void showVrsWorkApp() {
        if (!MultiSenceConfig.DEBUG_VRS) {
            return;
        }
        StringBuilder str = new StringBuilder("VRS works in " + Integer.toString(this.mVrsWorkList.size()) + " apps:");
        for (String name : this.mVrsWorkList) {
            str.append(" " + name);
        }
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, str.toString());
    }

    private void LOG_IF_DEBUG(String log) {
        if (DEBUG) {
            Slog.d(TAG, log);
        }
    }
}
