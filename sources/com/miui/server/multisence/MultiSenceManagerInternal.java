package com.miui.server.multisence;

import java.util.Map;

/* loaded from: classes.dex */
public interface MultiSenceManagerInternal {
    void setUpdateReason(String str);

    void showAllWindowInfo(String str);

    void updateDynamicWindowsInfo(int i, int i2, int[] iArr, boolean z);

    void updateStaticWindowsInfo(Map<String, SingleWindowInfo> map);
}
