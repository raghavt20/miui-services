package com.android.server.am;

import com.miui.server.smartpower.IAppState;

/* loaded from: classes.dex */
public interface MiuiMemoryServiceInternal {
    public static final int COMPACT_MODE_CAMERA = 0;
    public static final int COMPACT_MODE_LMKD = 3;
    public static final int COMPACT_MODE_PMC = 4;
    public static final int COMPACT_MODE_SCREENOFF_CHARGING = 2;
    public static final int COMPACT_MODE_SPTM = 1;
    public static final int VMPRESS_LEVEL_CRITICAL = 2;
    public static final int VMPRESS_LEVEL_LOW = 0;
    public static final int VMPRESS_LEVEL_MEDIUM = 1;
    public static final int VMPRESS_LEVEL_SUPER_CRITICAL = 3;

    void interruptProcCompaction(int i);

    void interruptProcsCompaction();

    boolean isCompactNeeded(IAppState.IRunningProcess iRunningProcess, int i);

    void performCompaction(String str, int i);

    void runGlobalCompaction(int i);

    void runProcCompaction(IAppState.IRunningProcess iRunningProcess, int i);

    void runProcsCompaction(int i);

    void setAppStartingMode(boolean z);

    void writeLmkd(boolean z);
}
