package com.miui.server.input.magicpointer;

import android.hardware.display.DisplayViewport;
import android.view.PointerIcon;
import com.android.server.am.ProcessRecord;
import com.android.server.policy.WindowManagerPolicy;
import java.util.List;

/* loaded from: classes.dex */
public abstract class MiuiMagicPointerServiceInternal {
    public abstract void handleAppDied(int i, ProcessRecord processRecord);

    public abstract boolean needInterceptSetCustomPointerIcon(PointerIcon pointerIcon);

    public abstract boolean needInterceptSetPointerIconType(int i);

    public abstract void onFocusedWindowChanged(WindowManagerPolicy.WindowState windowState);

    public abstract void onUserChanged(int i);

    public abstract void setDisplayViewports(List<DisplayViewport> list);

    public abstract void setMagicPointerVisibility(boolean z);

    public abstract void systemReady();

    public abstract void updateMagicPointerPosition(float f, float f2);

    public abstract void updatePointerDisplayId(int i);
}
