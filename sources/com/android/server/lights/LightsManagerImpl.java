package com.android.server.lights;

import android.content.res.Resources;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import miui.os.DeviceFeature;

@MiuiStubHead(manifestName = "com.android.server.lights.LightsManagerStub$$")
/* loaded from: classes.dex */
public class LightsManagerImpl extends LightsManagerStub {
    private static final boolean SUPPORT_HBM = Resources.getSystem().getBoolean(285540481);
    private static final String TAG = "LightsManagerImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LightsManagerImpl> {

        /* compiled from: LightsManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LightsManagerImpl INSTANCE = new LightsManagerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LightsManagerImpl m1665provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LightsManagerImpl m1664provideNewInstance() {
            return new LightsManagerImpl();
        }
    }

    public int brightnessToColor(int id, int brightness, int lastColor) {
        if (id == 0 && DeviceFeature.BACKLIGHT_BIT > 8 && DeviceFeature.BACKLIGHT_BIT <= 14) {
            if (brightness < 0) {
                Slog.e(TAG, "invalid backlight " + brightness + " !!!");
                return lastColor;
            }
            if (SUPPORT_HBM) {
                return brightness & 16383;
            }
            return brightness & 8191;
        }
        int color = brightness & 255;
        return color | (color << 16) | (-16777216) | (color << 8);
    }
}
