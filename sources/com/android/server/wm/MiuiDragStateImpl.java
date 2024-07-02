package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;
import com.xiaomi.mirror.service.MirrorServiceInternal;

/* loaded from: classes.dex */
public class MiuiDragStateImpl implements DragStateStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDragStateImpl> {

        /* compiled from: MiuiDragStateImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDragStateImpl INSTANCE = new MiuiDragStateImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiDragStateImpl m2521provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiDragStateImpl m2520provideNewInstance() {
            return new MiuiDragStateImpl();
        }
    }

    public boolean needFinishAnimator() {
        return MirrorServiceInternal.getInstance().isNeedFinishAnimator();
    }
}
