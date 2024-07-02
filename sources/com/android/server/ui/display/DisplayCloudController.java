package com.android.server.ui.display;

import android.content.Context;
import android.os.SystemProperties;
import com.android.server.ui.event_status.ForegroundStatusHandler;
import com.android.server.ui.event_status.MultiScreenHandler;
import com.android.server.ui.utils.LogUtil;

/* loaded from: classes.dex */
public class DisplayCloudController {
    public static final String TAG = "UIService-DisplayCloudController";
    private static DisplayCloudController mInstance = null;
    private final Context mContext;
    private ForegroundStatusHandler mForegroundStatusHandler;
    private MultiScreenHandler mMultiScreenHandler;
    private DisplayODHandler mOD;

    private DisplayCloudController(Context context) {
        this.mContext = context;
        if (SystemProperties.getBoolean("ro.vendor.display.uiservice.enable", true)) {
            this.mOD = DisplayODHandler.getInstance(context);
        }
        this.mForegroundStatusHandler = ForegroundStatusHandler.getInstance(context);
        this.mMultiScreenHandler = MultiScreenHandler.getInstance(context);
    }

    public static synchronized DisplayCloudController getInstance(Context context) {
        DisplayCloudController displayCloudController;
        synchronized (DisplayCloudController.class) {
            LogUtil.logD(TAG, "getInstance");
            if (mInstance == null && context != null) {
                mInstance = new DisplayCloudController(context);
            }
            displayCloudController = mInstance;
        }
        return displayCloudController;
    }
}
