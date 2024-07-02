package com.android.server.audio;

import android.os.SystemProperties;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class SoundDoseHelperStubImpl implements SoundDoseHelperStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SoundDoseHelperStubImpl> {

        /* compiled from: SoundDoseHelperStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SoundDoseHelperStubImpl INSTANCE = new SoundDoseHelperStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SoundDoseHelperStubImpl m823provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SoundDoseHelperStubImpl m822provideNewInstance() {
            return new SoundDoseHelperStubImpl();
        }
    }

    public int updateSafeMediaVolumeIndex(int safeMediaVolumeIndex) {
        int propSafeMediaVolumeIndex = SystemProperties.getInt("ro.config.safe_media_volume_index", -1);
        if (propSafeMediaVolumeIndex != -1) {
            return propSafeMediaVolumeIndex * 10;
        }
        return safeMediaVolumeIndex;
    }
}
