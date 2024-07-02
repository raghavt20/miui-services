package com.android.server.display;

import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class DisplayGroupImpl extends DisplayGroupStub {
    private int mAlwaysUnlockedCount;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayGroupImpl> {

        /* compiled from: DisplayGroupImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayGroupImpl INSTANCE = new DisplayGroupImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayGroupImpl m1079provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayGroupImpl m1078provideNewInstance() {
            return new DisplayGroupImpl();
        }
    }

    public void updateAlwaysUnlockedCountIfNeeded(LogicalDisplay display, boolean isAdd) {
        if (hasAlwaysUnLockedFlag(display)) {
            if (isAdd) {
                this.mAlwaysUnlockedCount++;
            } else {
                this.mAlwaysUnlockedCount--;
            }
        }
    }

    public boolean isAlwaysOnLocked() {
        return this.mAlwaysUnlockedCount > 0;
    }

    private boolean hasAlwaysUnLockedFlag(LogicalDisplay display) {
        if (display != null) {
            int flags = display.getDisplayInfoLocked().flags;
            if ((flags & 512) != 0) {
                return true;
            }
            return false;
        }
        return false;
    }
}
