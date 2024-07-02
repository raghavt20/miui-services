package com.miui.app;

import android.content.Context;

/* loaded from: classes.dex */
public interface SpeedTestModeServiceInternal {
    void init(Context context);

    boolean isSpeedTestMode();

    void onBootPhase();

    void reportOneKeyCleanEvent();
}
