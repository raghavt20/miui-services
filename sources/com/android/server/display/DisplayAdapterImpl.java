package com.android.server.display;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.provider.Settings;
import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class DisplayAdapterImpl extends DisplayAdapterStub {
    private static final String EXTERNAL_DISPLAY_CONNECTED = "external_display_connected";
    private ActivityManagerInternal mActivityManagerInternal;
    private boolean mSystemReady;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayAdapterImpl> {

        /* compiled from: DisplayAdapterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayAdapterImpl INSTANCE = new DisplayAdapterImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayAdapterImpl m1012provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayAdapterImpl m1011provideNewInstance() {
            return new DisplayAdapterImpl();
        }
    }

    public void updateExternalDisplayStatus(DisplayDevice device, int event, Context context) {
        if (device.getDisplayDeviceInfoLocked().type == 2 && isSystemReady()) {
            if (event == 1) {
                Settings.System.putInt(context.getContentResolver(), EXTERNAL_DISPLAY_CONNECTED, 1);
            } else if (event == 3) {
                Settings.System.putInt(context.getContentResolver(), EXTERNAL_DISPLAY_CONNECTED, 0);
            }
        }
    }

    private boolean isSystemReady() {
        if (!this.mSystemReady) {
            if (this.mActivityManagerInternal == null) {
                this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            }
            this.mSystemReady = this.mActivityManagerInternal.isSystemReady();
        }
        return this.mSystemReady;
    }
}
