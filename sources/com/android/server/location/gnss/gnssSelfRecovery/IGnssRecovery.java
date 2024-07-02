package com.android.server.location.gnss.gnssSelfRecovery;

/* loaded from: classes.dex */
public interface IGnssRecovery {
    boolean removeMockLocation();

    void restartGnss();

    boolean setFullTracking(boolean z);
}
