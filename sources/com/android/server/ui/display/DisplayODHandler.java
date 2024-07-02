package com.android.server.ui.display;

import android.content.Context;
import com.android.server.ui.utils.LogUtil;
import com.android.server.ui.utils.SubModule;
import java.util.ArrayList;
import miui.process.ForegroundInfo;

/* loaded from: classes.dex */
public class DisplayODHandler extends DisplayBaseHandler {
    public static final String TAG = "UIService-DisplayODHandler";
    private static DisplayODHandler mInstance = null;
    private final int COOKIE_OD_MODE;
    private final String UI_SERVICE_CLOUD_FILE = "ui_service_od_config.xml";
    private ForegroundInfo mAppInfo;
    private final Context mContext;
    private boolean mIsMultiScreen;
    private int mIsODSwitch;
    private final int mKey;
    private final Object mOdLocked;
    private ArrayList<String> mOdPackages;

    private DisplayODHandler(Context context) {
        int i = SubModule.ID_OD;
        this.mKey = i;
        this.COOKIE_OD_MODE = 999;
        this.mAppInfo = null;
        this.mIsMultiScreen = false;
        this.mIsODSwitch = 0;
        this.mOdPackages = new ArrayList<>(10);
        this.mOdLocked = new Object();
        this.mContext = context;
        init(context, i, "ui_service_od_config.xml");
    }

    public static synchronized DisplayODHandler getInstance(Context context) {
        DisplayODHandler displayODHandler;
        synchronized (DisplayODHandler.class) {
            LogUtil.logD(TAG, "getInstance");
            if (mInstance == null && context != null) {
                mInstance = new DisplayODHandler(context);
            }
            displayODHandler = mInstance;
        }
        return displayODHandler;
    }

    @Override // com.android.server.ui.display.DisplayBaseHandler
    public void onCloudUpdate(ArrayList<String> WhitePackageList) {
        LogUtil.logD(TAG, "onCloudUpdate");
        if (WhitePackageList != null) {
            this.mOdPackages = WhitePackageList;
        }
    }

    @Override // com.android.server.ui.display.DisplayBaseHandler
    public void onForegroundChange(ForegroundInfo appInfo) {
        this.mAppInfo = appInfo;
        LogUtil.logD(TAG, "mForegroundPackageName = " + this.mAppInfo.mForegroundPackageName);
        ODSwitch();
    }

    @Override // com.android.server.ui.display.DisplayBaseHandler
    public void onMultiScreenChange(boolean IsMultiScreen) {
        this.mIsMultiScreen = IsMultiScreen;
        LogUtil.logD(TAG, "mIsMultiScreen is " + this.mIsMultiScreen);
        ODSwitch();
    }

    private void ODSwitch() {
        int status;
        if (this.mAppInfo == null || this.mOdPackages == null) {
            return;
        }
        synchronized (this.mOdLocked) {
            boolean tempSwitch = this.mOdPackages.contains(this.mAppInfo.mForegroundPackageName);
            if (tempSwitch && !this.mIsMultiScreen) {
                status = 1;
            } else {
                status = 0;
            }
            if (status != this.mIsODSwitch) {
                this.mIsODSwitch = status;
                notifySFMode(999, status, this.mAppInfo.mForegroundPackageName);
                LogUtil.logI(TAG, "onForegroundChanged for OD value=" + status + " mForegroundPackageName=" + this.mAppInfo.mForegroundPackageName + " tempSwitch=" + tempSwitch);
            }
        }
    }

    public void onDestroy() {
    }
}
