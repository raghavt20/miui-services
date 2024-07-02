package com.android.server.ui;

import android.content.Context;
import android.os.HandlerThread;
import com.android.server.SystemService;
import com.android.server.ui.display.DisplayCloudController;
import com.android.server.ui.utils.LogUtil;

/* loaded from: classes.dex */
public class UIService {
    private static final String TAG = "UIService";
    private static HandlerThread mThread;
    private Context mContext;
    private DisplayCloudController mDisplayCloudController;

    private static void ensureThreadLocked() {
        if (mThread == null) {
            HandlerThread handlerThread = new HandlerThread(TAG, 0);
            mThread = handlerThread;
            handlerThread.start();
        }
    }

    public static synchronized HandlerThread getThread() {
        HandlerThread handlerThread;
        synchronized (UIService.class) {
            ensureThreadLocked();
            handlerThread = mThread;
        }
        return handlerThread;
    }

    private UIService(Context context) {
        LogUtil.logD(TAG, "start uiservice");
        this.mContext = context;
        this.mDisplayCloudController = DisplayCloudController.getInstance(context);
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final UIService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new UIService(context);
        }

        public void onStart() {
        }
    }
}
