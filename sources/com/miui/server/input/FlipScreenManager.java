package com.miui.server.input;

import android.content.Context;

/* loaded from: classes.dex */
public class FlipScreenManager {
    private static volatile FlipScreenManager sIntance;

    private FlipScreenManager(Context systemContext) {
    }

    public static FlipScreenManager getInstance(Context systemContext) {
        if (sIntance == null) {
            synchronized (FlipScreenManager.class) {
                if (sIntance == null) {
                    sIntance = new FlipScreenManager(systemContext);
                }
            }
        }
        return sIntance;
    }

    public void startService() {
    }
}
