package com.android.server.display;

import android.os.IBinder;
import java.util.List;

/* loaded from: classes.dex */
public abstract class DisplayFeatureManagerInternal {
    public abstract void setVideoInformation(int i, boolean z, float f, int i2, int i3, float f2, IBinder iBinder);

    public abstract void updateBCBCState(int i);

    public abstract void updateDozeBrightness(long j, int i);

    public abstract void updateRhythmicAppCategoryList(List<String> list, List<String> list2);

    public abstract void updateScreenEffect(int i);

    public abstract void updateScreenGrayscaleState(int i);
}
