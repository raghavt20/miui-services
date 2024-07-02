package com.miui.server.stability;

import android.content.Context;

/* loaded from: classes.dex */
public interface StabilityLocalServiceInternal {
    void captureDumpLog();

    void crawlLogsByPower();

    void initContext(Context context);

    void startMemoryMonitor();
}
