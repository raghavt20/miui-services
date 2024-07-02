package com.android.server.display;

import android.os.IBinder;
import com.miui.base.MiuiStubRegistry;
import java.util.List;

/* loaded from: classes.dex */
public class DisplayFeatureManagerServiceImpl implements DisplayFeatureManagerServiceStub {
    private DisplayFeatureManagerInternal mDisplayFeatureInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayFeatureManagerServiceImpl> {

        /* compiled from: DisplayFeatureManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayFeatureManagerServiceImpl INSTANCE = new DisplayFeatureManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayFeatureManagerServiceImpl m1077provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayFeatureManagerServiceImpl m1076provideNewInstance() {
            return new DisplayFeatureManagerServiceImpl();
        }
    }

    public void init(DisplayFeatureManagerInternal displayFeatureInternal) {
        this.mDisplayFeatureInternal = displayFeatureInternal;
    }

    public void updateScreenEffect(int state) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.updateScreenEffect(state);
        }
    }

    public void updateDozeBrightness(long physicalDisplayId, int brightness) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.updateDozeBrightness(physicalDisplayId, brightness);
        }
    }

    public void updateBCBCState(int state) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.updateBCBCState(state);
        }
    }

    public void setVideoInformation(int pid, boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio, IBinder token) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.setVideoInformation(pid, bulletChatStatus, frameRate, width, height, compressionRatio, token);
        }
    }

    public void updateRhythmicAppCategoryList(List<String> imageAppList, List<String> readAppList) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.updateRhythmicAppCategoryList(imageAppList, readAppList);
        }
    }

    public void updateScreenGrayscaleState(int state) {
        DisplayFeatureManagerInternal displayFeatureManagerInternal = this.mDisplayFeatureInternal;
        if (displayFeatureManagerInternal != null) {
            displayFeatureManagerInternal.updateScreenGrayscaleState(state);
        }
    }
}
