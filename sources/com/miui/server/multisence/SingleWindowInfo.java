package com.miui.server.multisence;

import android.graphics.Rect;
import android.os.Process;
import android.os.Trace;
import com.android.server.am.MiuiBoosterUtilsStub;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class SingleWindowInfo implements Comparable<SingleWindowInfo> {
    private static final String TAG = "SingleWindowInfo";
    private int FPS_LIMIT_DEFAULT;
    private int FPS_LIMIT_IN_FREEFORM_MINI;
    private int FPS_LIMIT_IN_FULLSCREEN_COMMON;
    private boolean isCovered;
    private boolean isFPSLimited;
    private boolean isHighPriority;
    private boolean isInputFocused;
    private boolean isVisible;
    private boolean isVrsCovered;
    private int layerOrder;
    private WindowChangeState mChangeStatus;
    private WindowForm mForm;
    private String mPackageName;
    private Rect mRect;
    private Set<ScheduleStatus> mScheduleStatus;
    private AppType mType;
    private String mWindowMode;
    private int myPid;
    private int myRenderThreadTid;
    private int myUid;
    private int windowCount;

    /* loaded from: classes.dex */
    public enum AppType {
        UNKNOWN,
        COMMON,
        GAME,
        VIDEO,
        HOME
    }

    /* loaded from: classes.dex */
    public enum WindowForm {
        UNKNOWN,
        MUTIL_FREEDOM,
        MUTIL_FREEDOM_MINI,
        MUTIL_FREEDOM_PIN,
        MUTIL_FULLSCREEN_SINGLE,
        MUTIL_FULLSCREEN_PARALLEL,
        MUTIL_FULLSCREEN_SPLIT,
        MUTIL_VIDEO
    }

    public SingleWindowInfo setPid(int pid) {
        this.myPid = pid;
        return this;
    }

    public SingleWindowInfo setUid(int uid) {
        this.myUid = uid;
        return this;
    }

    public SingleWindowInfo setRenderThreadTid(int tid) {
        this.myRenderThreadTid = tid;
        return this;
    }

    public SingleWindowInfo() {
        this.mForm = WindowForm.UNKNOWN;
        this.mType = AppType.UNKNOWN;
        this.mWindowMode = "unknown";
        this.isInputFocused = false;
        this.isHighPriority = false;
        this.isVisible = false;
        this.isCovered = false;
        this.isVrsCovered = false;
        this.mChangeStatus = WindowChangeState.UNKNOWN;
        this.mScheduleStatus = new HashSet();
        this.FPS_LIMIT_DEFAULT = 60;
        this.FPS_LIMIT_IN_FREEFORM_MINI = 30;
        this.FPS_LIMIT_IN_FULLSCREEN_COMMON = 60;
        this.isFPSLimited = false;
        this.layerOrder = -1;
        this.windowCount = 1;
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG, "new a SingleWindowInfo object.");
    }

    public SingleWindowInfo(String name, AppType type) {
        this.mForm = WindowForm.UNKNOWN;
        this.mType = AppType.UNKNOWN;
        this.mWindowMode = "unknown";
        this.isInputFocused = false;
        this.isHighPriority = false;
        this.isVisible = false;
        this.isCovered = false;
        this.isVrsCovered = false;
        this.mChangeStatus = WindowChangeState.UNKNOWN;
        this.mScheduleStatus = new HashSet();
        this.FPS_LIMIT_DEFAULT = 60;
        this.FPS_LIMIT_IN_FREEFORM_MINI = 30;
        this.FPS_LIMIT_IN_FULLSCREEN_COMMON = 60;
        this.isFPSLimited = false;
        this.layerOrder = -1;
        this.windowCount = 1;
        this.mPackageName = name;
        if (name.equals("com.miui.home")) {
            this.mType = AppType.HOME;
        } else {
            this.mType = type;
        }
    }

    public SingleWindowInfo setCovered(boolean covered) {
        this.isCovered = covered;
        return this;
    }

    public SingleWindowInfo setVrsCovered(boolean covered) {
        this.isVrsCovered = covered;
        return this;
    }

    public boolean isCovered() {
        return this.isCovered;
    }

    public boolean isVrsCovered() {
        return this.isVrsCovered;
    }

    public AppType getAppType() {
        return this.mType;
    }

    public WindowForm getWindowForm() {
        return this.mForm;
    }

    public SingleWindowInfo setgetWindowForm(WindowForm form) {
        this.mForm = form;
        return this;
    }

    public SingleWindowInfo setFPSLimited(boolean isLimited) {
        this.isFPSLimited = isLimited;
        return this;
    }

    public SingleWindowInfo setVisiable(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    public boolean isFPSLimited() {
        return this.isFPSLimited;
    }

    public String getWindowingModeString() {
        return this.mWindowMode;
    }

    public boolean isInFreeform() {
        return this.mWindowMode.equals("freeform");
    }

    public int getLimitFPS() {
        return this.FPS_LIMIT_IN_FREEFORM_MINI;
    }

    public SingleWindowInfo setLayerOrder(int order) {
        this.layerOrder = order;
        return this;
    }

    public int getLayerOrder() {
        return this.layerOrder;
    }

    public SingleWindowInfo setWindowCount(int count) {
        this.windowCount = count;
        return this;
    }

    public int getWindowCount() {
        return this.windowCount;
    }

    public Rect getRectValue() {
        return this.mRect;
    }

    public SingleWindowInfo setRectValue(Rect rect) {
        this.mRect = rect;
        return this;
    }

    @Override // java.lang.Comparable
    public int compareTo(SingleWindowInfo window) {
        return window.getLayerOrder() - getLayerOrder();
    }

    public SingleWindowInfo setWindowingModeString(String mode) {
        this.mWindowMode = mode;
        return this;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public SingleWindowInfo setFocused(boolean focused) {
        this.isInputFocused = focused;
        return this;
    }

    public boolean isInputFocused() {
        return this.isInputFocused;
    }

    public boolean isHighPriority() {
        return this.isHighPriority;
    }

    public SingleWindowInfo setSchedPriority(boolean priority) {
        this.isHighPriority = priority;
        return this;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    /* loaded from: classes.dex */
    public enum WindowChangeState {
        UNKNOWN(-1),
        NOT_CHANGED(0),
        RESET(1),
        CHANGED(2);

        final int id;

        WindowChangeState(int id) {
            this.id = id;
        }
    }

    /* loaded from: classes.dex */
    public enum ScheduleStatus {
        UNKNOWN(0),
        DYN_FPS_SET(1),
        VRS_RESET(2),
        VRS_SET(3);

        final int id;

        ScheduleStatus(int id) {
            this.id = id;
        }
    }

    public SingleWindowInfo setSchedStatus(ScheduleStatus status) {
        if (this.mScheduleStatus.contains(status)) {
            return this;
        }
        this.mScheduleStatus.add(status);
        return this;
    }

    public boolean checkSchedStatus(ScheduleStatus status) {
        if (this.mScheduleStatus.contains(status)) {
            return true;
        }
        return false;
    }

    public SingleWindowInfo doSched() {
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG, toString());
        if (this.isHighPriority) {
            Trace.traceBegin(32L, "doSched app: " + this.mPackageName + " pid: " + this.myPid + " high priority: " + this.isHighPriority);
            int callingUid = Process.myUid();
            if (this.myPid <= 0) {
                MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG, "Pid recorded error");
                return this;
            }
            MiuiBoosterUtilsStub.getInstance().requestBindCore(callingUid, new int[]{this.myPid}, 3, 2000);
            Trace.traceEnd(32L);
        }
        return this;
    }

    public SingleWindowInfo setChangeStatus(WindowChangeState status) {
        this.mChangeStatus = status;
        return this;
    }

    public WindowChangeState getChangeStatus() {
        return this.mChangeStatus;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("AppInfo[");
        stringBuilder.append("name: ").append(this.mPackageName);
        stringBuilder.append(", ");
        stringBuilder.append("uid: ").append(this.myUid);
        stringBuilder.append(", ");
        stringBuilder.append("pid: ").append(this.myPid);
        stringBuilder.append(", ");
        stringBuilder.append("isHighPriority: ").append(this.isHighPriority);
        stringBuilder.append(", ");
        stringBuilder.append("window state: ").append(this.mForm);
        stringBuilder.append(", ");
        stringBuilder.append("covered: ").append(this.isCovered ? "y" : "n");
        stringBuilder.append(", ");
        stringBuilder.append("sched: ");
        for (ScheduleStatus sSched : this.mScheduleStatus) {
            stringBuilder.append(Integer.toString(sSched.id) + " ");
        }
        return stringBuilder.toString();
    }

    /* loaded from: classes.dex */
    public static class FreeFormInfo {
        public int mPid;
        public Rect mRect;
        public WindowForm mWindowForm;

        public FreeFormInfo(WindowForm windowForm, Rect rect, int pid) {
            this.mWindowForm = windowForm;
            this.mRect = rect;
            this.mPid = pid;
        }
    }
}
