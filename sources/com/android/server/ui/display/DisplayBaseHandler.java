package com.android.server.ui.display;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BackgroundThread;
import com.android.server.MiuiCommonCloudHelperStub;
import com.android.server.ui.UIService;
import com.android.server.ui.event_status.ForegroundStatusHandler;
import com.android.server.ui.event_status.MultiScreenHandler;
import com.android.server.ui.utils.LogUtil;
import com.android.server.ui.utils.SubModule;
import com.android.server.ui.utils.WhitePackageInfo;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.ArrayList;
import java.util.Iterator;
import miui.process.ForegroundInfo;

/* loaded from: classes.dex */
public class DisplayBaseHandler extends Handler {
    public static final String TAG = "UIService-DisplayBaseHandler";
    private final int MSG_ID_CLOUD_CHANGE;
    private final int MSG_ID_FORGROUND_CHANGE;
    private final int MSG_ID_MULTISCREEN_CHANGE;
    private final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE;
    private String UI_SERVICE_CLOUD_FILE;
    private IBinder mFlinger;
    private ForegroundStatusHandler mForegroundStatusHandler;
    private int mKey;
    private MiuiCommonCloudHelperStub mMiuiCommonCloudHelper;
    private MultiScreenHandler mMultiScreenHandler;
    private ArrayList<String> mWhitePackageList;

    public DisplayBaseHandler() {
        super(UIService.getThread().getLooper());
        this.mKey = 0;
        this.UI_SERVICE_CLOUD_FILE = "";
        this.SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE = 31107;
        this.MSG_ID_CLOUD_CHANGE = 1;
        this.MSG_ID_FORGROUND_CHANGE = 2;
        this.MSG_ID_MULTISCREEN_CHANGE = 3;
    }

    public void init(Context context, int key, String cloud_file) {
        this.mKey = key;
        this.UI_SERVICE_CLOUD_FILE = cloud_file;
        MiuiCommonCloudHelperStub miuiCommonCloudHelperStub = MiuiCommonCloudHelperStub.getInstance();
        this.mMiuiCommonCloudHelper = miuiCommonCloudHelperStub;
        miuiCommonCloudHelperStub.init(context, BackgroundThread.getHandler(), Environment.getDownloadCacheDirectory() + "/" + cloud_file);
        this.mForegroundStatusHandler = ForegroundStatusHandler.getInstance(context);
        this.mMultiScreenHandler = MultiScreenHandler.getInstance(context);
        registerCloudObserver();
        registerFgEvent();
        registerMultiScreenEvent();
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                LogUtil.logI(TAG, "handleMessage MSG_ID_CLOUD_CHANGE");
                onCloudUpdate((ArrayList) msg.obj);
                return;
            case 2:
                LogUtil.logI(TAG, "handleMessage MSG_ID_FORGROUND_CHANGE");
                onForegroundChange((ForegroundInfo) msg.obj);
                return;
            case 3:
                LogUtil.logI(TAG, "handleMessage MSG_ID_MultiScreen_CHANGE");
                onMultiScreenChange(((Boolean) msg.obj).booleanValue());
                return;
            default:
                return;
        }
    }

    public void onCloudUpdate(ArrayList<String> WhitePackageList) {
    }

    public void onForegroundChange(ForegroundInfo appInfo) {
    }

    public void onMultiScreenChange(boolean IsMultiScreen) {
    }

    public void registerCloudObserver() {
        LogUtil.logD(TAG, "registerCloudObserver");
        this.mMiuiCommonCloudHelper.registerObserver(new MiuiCommonCloudHelperStub.Observer() { // from class: com.android.server.ui.display.DisplayBaseHandler.1
            public void update() {
                LogUtil.logI(DisplayBaseHandler.TAG, "cloud data update:");
                WhitePackageInfo whitePackageInfo = (WhitePackageInfo) DisplayBaseHandler.this.mMiuiCommonCloudHelper.getDataByModuleName(SubModule.SUB_MODULE_LIST.get(DisplayBaseHandler.this.mKey));
                DisplayBaseHandler.this.mWhitePackageList = whitePackageInfo.getWhitePackageList();
                if (whitePackageInfo != null) {
                    Iterator<String> it = whitePackageInfo.getWhitePackageList().iterator();
                    while (it.hasNext()) {
                        String data = it.next();
                        if (data != null) {
                            LogUtil.logI(DisplayBaseHandler.TAG, data);
                        }
                    }
                }
                DisplayBaseHandler displayBaseHandler = DisplayBaseHandler.this;
                displayBaseHandler.sendMessage(displayBaseHandler.obtainMessage(1, displayBaseHandler.mWhitePackageList));
            }
        });
        this.mMiuiCommonCloudHelper.initFromAssets(this.UI_SERVICE_CLOUD_FILE);
        this.mMiuiCommonCloudHelper.startCloudObserve();
    }

    public void registerFgEvent() {
        LogUtil.logD(TAG, "registerFgEvent");
        this.mForegroundStatusHandler.addFgStatusChangeCallback(this.mKey, new ForegroundStatusHandler.IForegroundInfoChangeCallback() { // from class: com.android.server.ui.display.DisplayBaseHandler.2
            @Override // com.android.server.ui.event_status.ForegroundStatusHandler.IForegroundInfoChangeCallback
            public void onChange(ForegroundInfo appInfo) {
                if (appInfo != null) {
                    LogUtil.logI(DisplayBaseHandler.TAG, "onChange: fg app = " + appInfo.mForegroundPackageName);
                    DisplayBaseHandler displayBaseHandler = DisplayBaseHandler.this;
                    displayBaseHandler.sendMessage(displayBaseHandler.obtainMessage(2, appInfo));
                }
            }
        });
    }

    public void registerMultiScreenEvent() {
        LogUtil.logD(TAG, "registerMultiScreenEvent");
        this.mMultiScreenHandler.addMultiScreenStatusChangeCallback(this.mKey, new MultiScreenHandler.IMultiScreenStatusChangeCallback() { // from class: com.android.server.ui.display.DisplayBaseHandler.3
            @Override // com.android.server.ui.event_status.MultiScreenHandler.IMultiScreenStatusChangeCallback
            public void onChange(boolean MultiScreenStatus) {
                LogUtil.logI(DisplayBaseHandler.TAG, "onChange: MultiScreenStatus = " + MultiScreenStatus);
                DisplayBaseHandler displayBaseHandler = DisplayBaseHandler.this;
                displayBaseHandler.sendMessage(displayBaseHandler.obtainMessage(3, Boolean.valueOf(MultiScreenStatus)));
            }
        });
    }

    public void notifySFMode(int cookie, int status, String pkg) {
        LogUtil.logD(TAG, "notifySFMode");
        if (this.mFlinger == null) {
            this.mFlinger = ServiceManager.getService(DumpSysInfoUtil.SURFACEFLINGER);
        }
        if (this.mFlinger != null) {
            LogUtil.logD(TAG, "getService succesd");
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(cookie);
            data.writeInt(status);
            data.writeString(pkg);
            try {
                try {
                    LogUtil.logD(TAG, "notify SurfaceFlinger");
                    this.mFlinger.transact(31107, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    LogUtil.logE(TAG, "Failed to notify SurfaceFlinger" + ex);
                }
            } finally {
                data.recycle();
            }
        }
    }
}
