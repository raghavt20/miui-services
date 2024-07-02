package com.miui.server.smartpower;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.ProcessRecord;
import com.android.server.am.SmartPowerService;
import com.miui.server.multisence.MultiSenceConfig;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.rtboost.SchedBoostManagerInternal;

/* loaded from: classes.dex */
public class SmartBoostPolicyManager {
    public static final int BOOST_TYPE_APP_TRANSITION = 2;
    public static final int BOOST_TYPE_MULTI_TASK = 1;
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static final String TAG = "SmartPower.BoostPolicy";
    private final Context mContext;
    private final Handler mHandler;
    private ProcessManagerInternal mProcessManagerInternal;
    private SchedBoostController mSchedBoostController;

    public SmartBoostPolicyManager(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
    }

    public void init() {
    }

    public void systemReady() {
        this.mProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        this.mSchedBoostController = new SchedBoostController();
    }

    public void beginSchedThreads(int[] tids, int callingPid, int boostType) {
        this.mSchedBoostController.beginSchedThreads(tids, callingPid, 2000L, boostType);
    }

    public void beginSchedThreads(int[] tids, int callingPid, long duration, int boostType) {
        this.mSchedBoostController.beginSchedThreads(tids, callingPid, duration, boostType);
    }

    public void stopCurrentSchedBoost(int[] tids, int callingPid, int boostType) {
        this.mSchedBoostController.beginSchedThreads(tids, callingPid, 0L, boostType);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String boostTypeToString(int boostType) {
        switch (boostType) {
            case 1:
                return "MULTI_TASK";
            case 2:
                return "APP_TRANSITION";
            default:
                return "UNKNOWN";
        }
    }

    public void setMultiSenceEnable(boolean enable) {
        SchedBoostController.MULTI_SENCE_ENABLE = enable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getProcessNameByPid(int pid) {
        ProcessRecord record = this.mProcessManagerInternal.getProcessRecordByPid(pid);
        return record != null ? record.processName : "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SchedBoostController {
        public static boolean MULTI_SENCE_ENABLE = SystemProperties.getBoolean(MultiSenceConfig.PROP_MULTISENCE_ENABLE, false);
        private static final long SCHED_BOOST_DEFAULT_DURATION = 2000;
        private static final long SCHED_BOOST_STOP_DURATION = 0;
        private final SchedBoostManagerInternal mSchedBoostManagerInternal = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);

        public SchedBoostController() {
        }

        public boolean isEanble() {
            return !MULTI_SENCE_ENABLE;
        }

        public void beginSchedThreads(int[] tids, int callingPid, long duration, int boostType) {
            if (!isEanble()) {
                return;
            }
            int schedBoostMode = getScheduleBoostMode(boostType);
            String procName = SmartBoostPolicyManager.this.getProcessNameByPid(callingPid);
            if (SmartBoostPolicyManager.DEBUG) {
                Slog.d(SmartBoostPolicyManager.TAG, "beginSchedThreads: tids=" + tids + ", duration=" + duration + ", boostType=" + SmartBoostPolicyManager.this.boostTypeToString(boostType) + ", schedBoostMode=" + schedBoostMode + ", procName=" + procName);
            }
            this.mSchedBoostManagerInternal.beginSchedThreads(tids, duration, procName, schedBoostMode);
        }

        public int getScheduleBoostMode(int boostType) {
            switch (boostType) {
                case 1:
                    return 9;
                default:
                    return 0;
            }
        }
    }
}
