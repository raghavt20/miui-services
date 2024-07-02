package com.miui.server.stability;

import android.content.Context;
import com.android.server.LocalServices;
import com.android.server.SystemService;

/* loaded from: classes.dex */
public class StabilityLocalService implements StabilityLocalServiceInternal {
    private static final String TAG = StabilityLocalService.class.getSimpleName();
    private static StabilityMemoryMonitor mStabilityMemoryMonitor = StabilityMemoryMonitor.getInstance();

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            LocalServices.addService(StabilityLocalServiceInternal.class, new StabilityLocalService());
        }
    }

    @Override // com.miui.server.stability.StabilityLocalServiceInternal
    public void captureDumpLog() {
        DumpSysInfoUtil.captureDumpLog();
    }

    @Override // com.miui.server.stability.StabilityLocalServiceInternal
    public void crawlLogsByPower() {
        DumpSysInfoUtil.crawlLogsByPower();
    }

    @Override // com.miui.server.stability.StabilityLocalServiceInternal
    public void initContext(Context context) {
        mStabilityMemoryMonitor.initContext(context);
    }

    @Override // com.miui.server.stability.StabilityLocalServiceInternal
    public void startMemoryMonitor() {
        mStabilityMemoryMonitor.startMemoryMonitor();
    }
}
