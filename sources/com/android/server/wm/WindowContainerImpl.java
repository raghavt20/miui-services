package com.android.server.wm;

import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import com.miui.base.MiuiStubRegistry;
import java.util.List;

/* loaded from: classes.dex */
public class WindowContainerImpl implements WindowContainerStub {
    private Animation mSplitDimmer;
    private WindowContainer wc;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowContainerImpl> {

        /* compiled from: WindowContainerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowContainerImpl INSTANCE = new WindowContainerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WindowContainerImpl m2799provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WindowContainerImpl m2798provideNewInstance() {
            return new WindowContainerImpl();
        }
    }

    public void init(WindowContainer container) {
        this.wc = container;
        this.mSplitDimmer = null;
    }

    public void clearTransitionDimmer() {
        if (this.mSplitDimmer == null) {
            return;
        }
        AppTransitionStub.get().stopSplitDimmer(this.mSplitDimmer, this.wc);
        this.mSplitDimmer = null;
    }

    public void setSplitDimmer(Animation a) {
        clearTransitionDimmer();
        if (a instanceof AnimationSet) {
            List<Animation> animations = ((AnimationSet) a).getAnimations();
            if (animations != null && animations.size() > 0) {
                for (int i = 0; i < animations.size(); i++) {
                    Animation animation = animations.get(i);
                    Animation splitDimmer = AppTransitionStub.get().dimSplitDimmerAboveIfNeeded(animation, this.wc);
                    this.mSplitDimmer = splitDimmer;
                    if (splitDimmer != null) {
                        return;
                    }
                }
                return;
            }
            return;
        }
        this.mSplitDimmer = AppTransitionStub.get().dimSplitDimmerAboveIfNeeded(a, this.wc);
    }

    public Animation getSplitDimmer() {
        return this.mSplitDimmer;
    }
}
