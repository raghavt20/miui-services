package com.miui.server.greeze;

import android.app.ActivityManager;
import android.os.Build;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.miui.server.process.ProcessManagerInternal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.process.ActiveUidInfo;
import miui.process.ProcessManager;
import miui.process.RunningProcessInfo;

/* loaded from: classes.dex */
public class GreezeServiceUtils {
    public static int GREEZER_MODULE_UNKNOWN = GreezeManagerInternal.GREEZER_MODULE_UNKNOWN;
    public static int GREEZER_MODULE_POWER = GreezeManagerInternal.GREEZER_MODULE_POWER;
    public static int GREEZER_MODULE_PERFORMANCE = GreezeManagerInternal.GREEZER_MODULE_PERFORMANCE;
    public static int GREEZER_MODULE_GAME = GreezeManagerInternal.GREEZER_MODULE_GAME;
    public static int GREEZER_MODULE_PRELOAD = GreezeManagerInternal.GREEZER_MODULE_PRELOAD;
    public static int GREEZER_MODULE_ALL = GreezeManagerInternal.GREEZER_MODULE_ALL;
    private static List<String> mGameReportPkgs = new ArrayList(Arrays.asList("com.tencent.tmgp.pubgmhd", "com.tencent.tmgp.sgame", "com.miHoYo.ys.mi", "com.miHoYo.Yuanshen", "com.miHoYo.ys.bilibili", "com.tencent.lolm", "com.tencent.jkchess"));
    private static List<String> mSupportDevice = new ArrayList(Arrays.asList("liuqin", "pipa", "ishtar", "yuechu", "yudi", "corot", "vermeer", "duchamp", "manet"));
    private static HashMap<String, Integer> mGameReportUids = new HashMap<>();
    public static String TAG = "GreezeServiceUtils";

    public static Set<Integer> getAudioUid() {
        Set<Integer> activeUids = new ArraySet<>();
        try {
            List<ActiveUidInfo> uidInfos = ProcessManager.getActiveUidInfo(3);
            for (ActiveUidInfo info : uidInfos) {
                activeUids.add(Integer.valueOf(info.uid));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get active audio info from ProcessManager", e);
        }
        return activeUids;
    }

    public static Set<Integer> getIMEUid() {
        Set<Integer> uids = new ArraySet<>();
        try {
            InputMethodManagerInternal imm = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            List<InputMethodInfo> inputMetheds = imm.getInputMethodListAsUser(UserHandle.myUserId());
            for (InputMethodInfo info : inputMetheds) {
                uids.add(Integer.valueOf(info.getServiceInfo().applicationInfo.uid));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get IME Uid from InputMethodManagerInternal", e);
        }
        return uids;
    }

    public static Set<Integer> getGameUids() {
        HashSet<Integer> ret = new HashSet<>();
        for (Integer uid : mGameReportUids.values()) {
            ret.add(uid);
        }
        return ret;
    }

    public static SparseArray<List<RunningProcess>> getUidMap() {
        SparseArray<List<RunningProcess>> uidMap = new SparseArray<>();
        List<RunningProcess> list = getProcessList();
        for (RunningProcess proc : list) {
            int uid = proc.uid;
            List<RunningProcess> procs = uidMap.get(uid);
            if (procs == null) {
                procs = new ArrayList();
                uidMap.put(uid, procs);
            }
            if (mSupportDevice.contains(Build.DEVICE) && proc.processName != null && mGameReportPkgs.contains(proc.processName) && !mGameReportUids.values().contains(Integer.valueOf(uid))) {
                mGameReportUids.put(proc.processName, Integer.valueOf(uid));
            }
            procs.add(proc);
        }
        return uidMap;
    }

    public static List<RunningProcess> getProcessList() {
        List<RunningProcess> procs = new ArrayList<>();
        try {
            ProcessManagerInternal pmi = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            List<RunningProcessInfo> list = pmi.getAllRunningProcessInfo();
            for (RunningProcessInfo info : list) {
                if (info != null) {
                    procs.add(new RunningProcess(info));
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get RunningProcessInfo from ProcessManager", e);
        }
        return procs;
    }

    public static List<RunningProcess> getProcessListFromAMS(ActivityManagerService ams) {
        List<RunningProcess> procs = new ArrayList<>();
        try {
            List<ActivityManager.RunningAppProcessInfo> list = ams.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (info != null) {
                    procs.add(new RunningProcess(info));
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get RunningProcessInfo from ProcessManager", e);
        }
        return procs;
    }
}
