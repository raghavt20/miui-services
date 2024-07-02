package com.android.server.tof;

import java.util.List;

/* loaded from: classes.dex */
public abstract class TofManagerInternal {
    public abstract void onDefaultDisplayFocusChanged(String str);

    public abstract void onEarlyInteractivityChange(boolean z);

    public abstract void updateGestureAppConfig(List<String> list);
}
