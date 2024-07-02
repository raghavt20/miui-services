package com.miui.server.stability;

import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class GpuMemoryUsageInfo {
    private long nativeTotalSize;
    private long runtimeTotalSize;
    private long totalSize;
    private ArrayList<GpuMemoryProcUsageInfo> usageList = new ArrayList<>();

    public void add(GpuMemoryProcUsageInfo procInfo) {
        this.usageList.add(procInfo);
    }

    public ArrayList<GpuMemoryProcUsageInfo> getList() {
        return this.usageList;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public void setRuntimeTotalSize(long runtimeTotalSize) {
        this.runtimeTotalSize = runtimeTotalSize;
    }

    public long getRuntimeTotalSize() {
        return this.runtimeTotalSize;
    }

    public void setNativeTotalSize(long nativeTotalSize) {
        this.nativeTotalSize = nativeTotalSize;
    }

    public long getNativeTotalSize() {
        return this.nativeTotalSize;
    }

    public String toString(int gpuType) {
        StringBuilder sb = new StringBuilder();
        if (gpuType == 1) {
            sb.append("KgslMemory");
        } else if (gpuType == 2) {
            sb.append("MaliMemory");
        } else {
            return "Error Info";
        }
        sb.append(" info total: " + this.totalSize + "kB RuntimeTotalSize=" + this.runtimeTotalSize + "kB NativeTotalSize=" + this.nativeTotalSize + "kB\n");
        Iterator<GpuMemoryProcUsageInfo> it = this.usageList.iterator();
        while (it.hasNext()) {
            GpuMemoryProcUsageInfo info = it.next();
            sb.append(info.toString(gpuType) + "\n");
        }
        return sb.toString();
    }
}
