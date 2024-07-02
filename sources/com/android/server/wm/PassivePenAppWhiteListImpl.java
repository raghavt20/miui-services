package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;
import miui.util.ITouchFeature;
import org.json.JSONArray;

/* loaded from: classes.dex */
public class PassivePenAppWhiteListImpl {
    private static final int NOT_WHITE_LIST_TOP_APP = 0;
    private static final String PASSIVE_PEN_APP_WHILTE_LIST = "whiteList";
    private static final String PASSIVE_PEN_MODULE_NAME = "passivepen";
    public static final String TAG = "PassivePenAppWhiteListImpl";
    private static final int WHITE_LIST_TOP_APP = 1;
    private static PassivePenAppWhiteListImpl mPassivePenImpl;
    private int lastWhiteListTopApp;
    public ActivityTaskManagerService mAtmService;
    public Context mContext;
    protected boolean splitMode;
    private int whiteListTopApp;
    private static final Boolean PASSIVE_PEN_MODE_ENABLED = Boolean.valueOf(SystemProperties.getBoolean("ro.miui.passive_pen.enabled", false));
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private ArrayList<String> defaultWhiteList = new ArrayList<>();
    private ArrayList<String> codeWhiteList = new ArrayList<>();
    BroadcastReceiver mKeyguardStateReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.PassivePenAppWhiteListImpl.1
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String mAction = intent.getAction();
            boolean isLocked = PassivePenAppWhiteListImpl.this.mAtmService.isKeyguardLocked(0);
            Slog.d(PassivePenAppWhiteListImpl.TAG, " onReceive= " + mAction);
            switch (mAction.hashCode()) {
                case -2128145023:
                    if (mAction.equals("android.intent.action.SCREEN_OFF")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (mAction.equals("android.intent.action.SCREEN_ON")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 798292259:
                    if (mAction.equals("android.intent.action.BOOT_COMPLETED")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 823795052:
                    if (mAction.equals("android.intent.action.USER_PRESENT")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    if (PassivePenAppWhiteListImpl.this.whiteListTopApp == 1 && !isLocked && !PassivePenAppWhiteListImpl.this.splitMode) {
                        PassivePenAppWhiteListImpl.this.dispatchTopAppWhiteState(1);
                        return;
                    }
                    return;
                case 2:
                    if (PassivePenAppWhiteListImpl.this.whiteListTopApp == 1) {
                        PassivePenAppWhiteListImpl.this.dispatchTopAppWhiteState(0);
                        return;
                    }
                    return;
                case 3:
                    PassivePenAppWhiteListImpl.this.readLocalCloudControlData(context.getContentResolver(), PassivePenAppWhiteListImpl.PASSIVE_PEN_MODULE_NAME);
                    return;
                default:
                    return;
            }
        }
    };
    private IForegroundInfoListener.Stub mAppObserver = new IForegroundInfoListener.Stub() { // from class: com.android.server.wm.PassivePenAppWhiteListImpl.2
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) throws RemoteException {
            String str = foregroundInfo.mForegroundPackageName;
            Slog.d(PassivePenAppWhiteListImpl.TAG, " mForegroundPackageName= " + str);
            boolean whiteListApp = PassivePenAppWhiteListImpl.this.whiteListApp(str);
            if (PassivePenAppWhiteListImpl.this.blackAppAndWhiteAppChanged(str)) {
                Slog.d(PassivePenAppWhiteListImpl.TAG, " whiteListApp= " + whiteListApp);
                PassivePenAppWhiteListImpl passivePenAppWhiteListImpl = PassivePenAppWhiteListImpl.this;
                passivePenAppWhiteListImpl.splitMode = passivePenAppWhiteListImpl.mAtmService.isInSplitScreenWindowingMode();
                if (!PassivePenAppWhiteListImpl.this.splitMode) {
                    PassivePenAppWhiteListImpl.this.dispatchTopAppWhiteState(whiteListApp ? 1 : 0);
                }
            }
        }
    };

    public static PassivePenAppWhiteListImpl getInstance() {
        if (mPassivePenImpl == null) {
            mPassivePenImpl = new PassivePenAppWhiteListImpl();
        }
        return mPassivePenImpl;
    }

    private PassivePenAppWhiteListImpl() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityTaskManagerService atms, Context context) {
        if (PASSIVE_PEN_MODE_ENABLED.booleanValue()) {
            this.mAtmService = atms;
            this.mContext = context;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        if (PASSIVE_PEN_MODE_ENABLED.booleanValue()) {
            ProcessManager.registerForegroundInfoListener(this.mAppObserver);
            getPassivePenAppWhiteListFromXml();
            registerKeyguardReceiver();
            registerCloudControlObserver(this.mContext.getContentResolver(), PASSIVE_PEN_MODULE_NAME);
        }
    }

    private void registerKeyguardReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mKeyguardStateReceiver, filter);
    }

    private void getPassivePenAppWhiteListFromXml() {
        this.defaultWhiteList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409356)));
    }

    boolean dispatchTopAppWhiteState(int touchId, int mode, int value) {
        try {
            ITouchFeature touchFeature = ITouchFeature.getInstance();
            if (touchFeature != null) {
                return touchFeature.setTouchMode(touchId, mode, value);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    boolean dispatchTopAppWhiteState(int value) {
        try {
            ITouchFeature touchFeature = ITouchFeature.getInstance();
            if (touchFeature != null) {
                Slog.d(TAG, " dispatchTopAppWhiteState value = " + value);
                return touchFeature.setTouchMode(0, 23, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected boolean whiteListApp(String name) {
        ArrayList<String> arrayList;
        this.lastWhiteListTopApp = this.whiteListTopApp;
        if (this.defaultWhiteList.contains(name) || ((arrayList = this.codeWhiteList) != null && arrayList.contains(name))) {
            this.whiteListTopApp = 1;
            return true;
        }
        this.whiteListTopApp = 0;
        return false;
    }

    boolean blackAppAndWhiteAppChanged(String name) {
        if (this.lastWhiteListTopApp == this.whiteListTopApp) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSplitScreenExit() {
        if (PASSIVE_PEN_MODE_ENABLED.booleanValue()) {
            if (this.whiteListTopApp == 1) {
                dispatchTopAppWhiteState(1);
            }
            this.splitMode = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readLocalCloudControlData(ContentResolver contentResolver, String moduleName) {
        if (TextUtils.isEmpty(moduleName)) {
            Slog.e(TAG, "moduleName can not be null");
            return;
        }
        try {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(contentResolver, moduleName, PASSIVE_PEN_APP_WHILTE_LIST, (String) null);
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                this.codeWhiteList.clear();
                for (int i = 0; i < apps.length(); i++) {
                    this.codeWhiteList.add(apps.getString(i));
                }
                Slog.i(TAG, "cloud data for listpassivepen " + this.codeWhiteList.toString());
            }
        } catch (Exception e) {
            Slog.e(TAG, "Exception when readLocalCloudControlData  :", e);
        }
    }

    private void registerCloudControlObserver(final ContentResolver contentResolver, final String moduleName) {
        contentResolver.registerContentObserver(URI_CLOUD_ALL_DATA_NOTIFY, true, new ContentObserver(null) { // from class: com.android.server.wm.PassivePenAppWhiteListImpl.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                Slog.i(PassivePenAppWhiteListImpl.TAG, "cloud data has update");
                PassivePenAppWhiteListImpl.this.readLocalCloudControlData(contentResolver, moduleName);
            }
        });
    }
}
