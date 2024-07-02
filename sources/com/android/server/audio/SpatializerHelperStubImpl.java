package com.android.server.audio;

import android.media.AudioSystem;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class SpatializerHelperStubImpl implements SpatializerHelperStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SpatializerHelperStubImpl> {

        /* compiled from: SpatializerHelperStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SpatializerHelperStubImpl INSTANCE = new SpatializerHelperStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SpatializerHelperStubImpl m825provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SpatializerHelperStubImpl m824provideNewInstance() {
            return new SpatializerHelperStubImpl();
        }
    }

    public void setPropertyByMode(boolean isHeadTrackerAvailable, int mode) {
        if (isHeadTrackerAvailable) {
            if (mode == 1) {
                AudioSystem.setParameters("vendor.spatial.headtrack=1");
                return;
            } else {
                if (mode == -1) {
                    AudioSystem.setParameters("vendor.spatial.headtrack=0");
                    return;
                }
                return;
            }
        }
        AudioSystem.setParameters("vendor.spatial.headtrack=-1");
    }

    public void disablePropertyByMode(int mode) {
        if (mode == -1) {
            AudioSystem.setParameters("vendor.spatial.headtrack=0");
        }
    }
}
