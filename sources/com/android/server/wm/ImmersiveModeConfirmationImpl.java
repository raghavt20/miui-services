package com.android.server.wm;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ImmersiveModeConfirmationImpl implements ImmersiveModeConfirmationStub {
    private static final String FACTORY_BUILD_SIGN = "1";
    private static final String IS_FACTORY_BUILD = SystemProperties.get("ro.boot.factorybuild", "0");
    private static final String TAG = "ImmersiveModeConfirmation";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ImmersiveModeConfirmationImpl> {

        /* compiled from: ImmersiveModeConfirmationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ImmersiveModeConfirmationImpl INSTANCE = new ImmersiveModeConfirmationImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ImmersiveModeConfirmationImpl m2481provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ImmersiveModeConfirmationImpl m2480provideNewInstance() {
            return new ImmersiveModeConfirmationImpl();
        }
    }

    ImmersiveModeConfirmationImpl() {
    }

    public boolean reloadFromSetting(Context cxt) {
        return PolicyControl.reloadFromSetting(cxt);
    }

    public boolean disableImmersiveConfirmation(String pkg) {
        return PolicyControl.disableImmersiveConfirmation(pkg);
    }

    public void dump(String prefix, PrintWriter pw) {
        PolicyControl.dump(prefix, pw);
    }

    public void handleMessageShow(ImmersiveModeConfirmation immersiveModeConfirmation, int what, boolean isDebug) {
        String str = IS_FACTORY_BUILD;
        if (TextUtils.isEmpty(str) || !TextUtils.equals(str, "1")) {
            if (isDebug) {
                Slog.d(TAG, "Not factory build");
            }
            ImmersiveModeConfirmationProxy.handleShow.invoke(immersiveModeConfirmation, new Object[]{Integer.valueOf(what)});
        } else if (isDebug) {
            Slog.d(TAG, "factory build not show immersive mode confirmation");
        }
    }
}
