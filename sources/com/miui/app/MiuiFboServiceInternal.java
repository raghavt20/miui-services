package com.miui.app;

/* loaded from: classes.dex */
public interface MiuiFboServiceInternal {
    void deliverMessage(String str, int i, long j);

    boolean getDueToScreenWait();

    boolean getGlobalSwitch();

    boolean getNativeIsRunning();

    void setBatteryInfos(int i, int i2, int i3);

    void setScreenStatus(boolean z);
}
