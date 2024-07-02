package com.android.server.biometrics.sensors.face;

import android.os.RemoteException;
import android.system.ErrnoException;
import android.view.Surface;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class FaceServiceStubImpl implements FaceServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FaceServiceStubImpl> {

        /* compiled from: FaceServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FaceServiceStubImpl INSTANCE = new FaceServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public FaceServiceStubImpl m862provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public FaceServiceStubImpl m861provideNewInstance() {
            return new FaceServiceStubImpl();
        }
    }

    public void stopGetFrame() {
        MiuiFaceHidl.getInstance().stopGetFrame();
    }

    public boolean setPreviewNotifyCallback() {
        try {
            return MiuiFaceHidl.getInstance().setPreviewNotifyCallback();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } catch (ErrnoException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    public void setEnrollArea(int left, int top, int right, int bottom) {
        try {
            MiuiFaceHidl.getInstance().setEnrollArea(left, top, right, bottom);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setDetectArea(int left, int top, int right, int bottom) {
        try {
            MiuiFaceHidl.getInstance().setDetectArea(left, top, right, bottom);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setEnrollStep(int steps) {
        try {
            MiuiFaceHidl.getInstance().setEnrollStep(steps);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setEnrollSurface(Surface surface) {
        MiuiFaceHidl.getInstance().setEnrollSurface(surface);
    }
}
