package com.android.server.wm;

import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.base.MiuiStubRegistry;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class WindowStateAnimatorImpl implements WindowStateAnimatorStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowStateAnimatorImpl> {

        /* compiled from: WindowStateAnimatorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowStateAnimatorImpl INSTANCE = new WindowStateAnimatorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WindowStateAnimatorImpl m2807provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WindowStateAnimatorImpl m2806provideNewInstance() {
            return new WindowStateAnimatorImpl();
        }
    }

    WindowStateAnimatorImpl() {
    }

    public boolean isAllowedDisableScreenshot(WindowState w) {
        return (w.mAttrs.extraFlags & SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX) != 0;
    }

    public boolean isNeedPrintLog(WindowState mWin) {
        if (mWin.mAttrs.type != 2027 && mWin.mAttrs.type != 2013 && mWin.mAttrs.type != 2024 && mWin.mAttrs.type != 2000 && mWin.mAttrs.type != 2019 && mWin.mAttrs.type != 3 && mWin.mAttrs.type != 2020 && mWin.mAttrs.type != 2005 && mWin.mAttrs.type != 2006) {
            return true;
        }
        return false;
    }

    public boolean needDisplaySyncTransaction(WindowState windowState) {
        if (windowState == null || windowState.mAttrs == null || (windowState.mAttrs.extraFlags & 8192) == 0) {
            return false;
        }
        return true;
    }
}
