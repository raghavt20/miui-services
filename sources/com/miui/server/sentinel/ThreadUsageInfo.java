package com.miui.server.sentinel;

/* loaded from: classes.dex */
public class ThreadUsageInfo extends ProcUsageInfo {
    private long thread_amount;
    private String usageInfo;

    public long getThreadAmount() {
        return this.thread_amount;
    }

    public void setThreadAmount(long thread_amount) {
        this.thread_amount = thread_amount;
    }

    public String getUsageInfo() {
        return this.usageInfo;
    }

    public void setUsageInfo(String usageInfo) {
        this.usageInfo = usageInfo;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proc info Name: " + getName() + "Pid = " + getPid() + "Thread Amount size = " + this.thread_amount);
        return sb.toString();
    }
}
