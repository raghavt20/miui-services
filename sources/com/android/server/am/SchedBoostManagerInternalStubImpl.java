package com.android.server.am;

import com.android.server.LocalServices;
import com.android.server.wm.WindowProcessController;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.rtboost.SchedBoostManagerInternal;

/* loaded from: classes.dex */
public class SchedBoostManagerInternalStubImpl implements SchedBoostManagerInternalStub {
    private SchedBoostManagerInternal mSchedBoostService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SchedBoostManagerInternalStubImpl> {

        /* compiled from: SchedBoostManagerInternalStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SchedBoostManagerInternalStubImpl INSTANCE = new SchedBoostManagerInternalStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SchedBoostManagerInternalStubImpl m677provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SchedBoostManagerInternalStubImpl m676provideNewInstance() {
            return new SchedBoostManagerInternalStubImpl();
        }
    }

    public void setSchedMode(String procName, int pid, int rtid, int schedMode, long timeout) {
        getSchedBoostService().beginSchedThreads(new int[]{pid, rtid}, timeout, procName, schedMode);
    }

    public void setRenderThreadTid(WindowProcessController wpc) {
        getSchedBoostService().setRenderThreadTid(wpc);
    }

    public void boostHomeAnim(long duration, int mode) {
        getSchedBoostService().boostHomeAnim(duration, mode);
    }

    private SchedBoostManagerInternal getSchedBoostService() {
        if (this.mSchedBoostService == null) {
            this.mSchedBoostService = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        }
        return this.mSchedBoostService;
    }
}
