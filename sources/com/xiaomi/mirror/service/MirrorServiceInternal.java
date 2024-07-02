package com.xiaomi.mirror.service;

import android.content.ClipData;
import android.net.Uri;
import com.android.server.LocalServices;
import java.util.List;

/* loaded from: classes.dex */
public abstract class MirrorServiceInternal {
    static MirrorServiceInternal sInstance = null;

    public abstract int getDelegatePid();

    public abstract boolean isNeedFinishAnimator();

    public abstract void notifyDragFinish(String str, boolean z);

    public abstract void notifyDragResult(boolean z);

    public abstract void notifyDragStart(int i, int i2, ClipData clipData, int i3);

    public abstract void notifyDragStart(ClipData clipData, int i, int i2, int i3);

    public abstract void onDelegatePermissionReleased(List<Uri> list);

    public abstract boolean tryToShareDrag(String str, int i, ClipData clipData);

    public static MirrorServiceInternal getInstance() {
        if (sInstance == null) {
            sInstance = (MirrorServiceInternal) LocalServices.getService(MirrorServiceInternal.class);
        }
        return sInstance;
    }
}
