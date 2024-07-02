package com.miui.server.rtboost;

import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessListener;

/* loaded from: classes.dex */
public interface SchedBoostManagerInternal {
    void beginSchedThreads(int[] iArr, long j, String str, int i);

    void boostHomeAnim(long j, int i);

    boolean checkThreadBoost(int i);

    void enableSchedBoost(boolean z);

    void schedProcessBoost(WindowProcessListener windowProcessListener, String str, int i, int i2, int i3, long j);

    void setRenderThreadTid(WindowProcessController windowProcessController);

    void setThreadSavedPriority(int[] iArr, int i);

    void stopCurrentSchedBoost();
}
