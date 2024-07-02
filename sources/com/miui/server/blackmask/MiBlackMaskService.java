package com.miui.server.blackmask;

import android.content.Context;
import com.android.server.SystemService;
import com.android.server.wm.MiuiFreezeStub;
import miui.blackmask.IMiBlackMask;

/* loaded from: classes.dex */
public class MiBlackMaskService extends IMiBlackMask.Stub {
    public static final String SERVICE_NAME = "miblackmask";
    private static final String TAG = "MiBlackMaskService";
    private final Context mContext;

    private MiBlackMaskService(Context context) {
        this.mContext = context;
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiBlackMaskService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiBlackMaskService(context);
        }

        public void onStart() {
            publishBinderService(MiBlackMaskService.SERVICE_NAME, this.mService);
        }
    }

    public void finishAndRemoveSplashScreen(String packageName, int uid) {
        MiuiFreezeStub.getInstance().finishAndRemoveSplashScreen(packageName, uid);
    }
}
