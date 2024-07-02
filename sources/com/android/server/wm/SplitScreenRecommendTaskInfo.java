package com.android.server.wm;

/* loaded from: classes.dex */
public class SplitScreenRecommendTaskInfo {
    private static final String TAG = "SplitScreenRecommendTaskInfo";
    private String mPkgName;
    private long mSwitchTime;
    private Task mTask;
    private int mTaskId;

    public String getPkgName() {
        return this.mPkgName;
    }

    public long getSwitchTime() {
        return this.mSwitchTime;
    }

    public int getTaskId() {
        return this.mTaskId;
    }

    public Task getTask() {
        return this.mTask;
    }

    public SplitScreenRecommendTaskInfo(int taskId, String packageName, long time, Task task) {
        this.mTaskId = taskId;
        this.mPkgName = packageName;
        this.mSwitchTime = time;
        this.mTask = task;
    }
}
