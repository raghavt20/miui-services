package com.miui.server.sentinel;

/* loaded from: classes.dex */
public class FdUsageInfo extends ProcUsageInfo {
    private long fd_amount;
    private String usageInfo;

    public long getFd_amount() {
        return this.fd_amount;
    }

    public void setFd_amount(long fd_amount) {
        this.fd_amount = fd_amount;
    }

    public String getUsageInfo() {
        return this.usageInfo;
    }

    public void setUsageInfo(String usageInfo) {
        this.usageInfo = usageInfo;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proc info Name: " + getName() + "Pid = " + getPid() + "Thread Amount size = " + this.fd_amount);
        return sb.toString();
    }
}
