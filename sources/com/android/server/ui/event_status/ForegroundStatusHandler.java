package com.android.server.ui.event_status;

import android.content.Context;
import android.os.RemoteException;
import android.util.SparseArray;
import com.android.server.ui.utils.LogUtil;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class ForegroundStatusHandler {
    private static final String TAG = "UIService-ForegroundStatusHandler";
    private static ForegroundStatusHandler mInstance = null;
    private IForegroundInfoListener.Stub mAppObserver;
    private final SparseArray<IForegroundInfoChangeCallback> mCallbacks = new SparseArray<>();
    private final Context mContext;
    private ForegroundInfo mCurrentFgAppInfo;

    /* loaded from: classes.dex */
    public interface IForegroundInfoChangeCallback {
        void onChange(ForegroundInfo foregroundInfo);
    }

    public static synchronized ForegroundStatusHandler getInstance(Context context) {
        ForegroundStatusHandler foregroundStatusHandler;
        synchronized (ForegroundStatusHandler.class) {
            LogUtil.logD(TAG, "getInstance");
            if (mInstance == null && context != null) {
                mInstance = new ForegroundStatusHandler(context);
            }
            foregroundStatusHandler = mInstance;
        }
        return foregroundStatusHandler;
    }

    private ForegroundStatusHandler(Context context) {
        this.mCurrentFgAppInfo = null;
        IForegroundInfoListener.Stub stub = new IForegroundInfoListener.Stub() { // from class: com.android.server.ui.event_status.ForegroundStatusHandler.1
            public void onForegroundInfoChanged(ForegroundInfo appInfo) throws RemoteException {
                LogUtil.logD(ForegroundStatusHandler.TAG, "onForegroundInfoChanged");
                if (appInfo == null) {
                    LogUtil.logI(ForegroundStatusHandler.TAG, "appInfo is null");
                } else {
                    ForegroundStatusHandler.this.mCurrentFgAppInfo = appInfo;
                    ForegroundStatusHandler.this.updataAppInfo();
                }
            }
        };
        this.mAppObserver = stub;
        this.mContext = context;
        ProcessManager.registerForegroundInfoListener(stub);
        this.mCurrentFgAppInfo = ProcessManager.getForegroundInfo();
    }

    public void addFgStatusChangeCallback(int key, IForegroundInfoChangeCallback callback) {
        if (callback == null) {
            return;
        }
        this.mCallbacks.put(key, callback);
    }

    public void updataAppInfo() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            int key = this.mCallbacks.keyAt(i);
            this.mCallbacks.get(key).onChange(this.mCurrentFgAppInfo);
        }
    }
}
