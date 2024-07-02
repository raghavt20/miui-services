package com.miui.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IMiuiProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.Display;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.MiuiFgThread;
import com.android.server.am.ProcessUtils;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import miui.util.CustomizeUtil;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public final class MiuiCompatModePackages {
    private static final String ATTR_CONFIG_NOTIFY_SUGGEST_APPS = "notifySuggestApps";
    private static final String MODULE_CUTOUT_MODE = "cutout_mode";
    private static final int MSG_DONT_SHOW_AGAIN = 105;
    private static final int MSG_ON_APP_LAUNCH = 104;
    private static final int MSG_READ = 101;
    private static final int MSG_REGISTER_OBSERVER = 102;
    private static final int MSG_UNREGISTER_OBSERVER = 103;
    private static final int MSG_UPDATE_CLOUD_DATA = 108;
    private static final int MSG_WRITE = 100;
    private static final int MSG_WRITE_CUTOUT_MODE = 107;
    private static final int MSG_WRITE_SPECIAL_MODE = 106;
    private static final String TAG = "MiuiCompatModePackages";
    private static final String TAG_NAME_CONFIG = "config";
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private AlertDialog mAlertDialog;
    private BroadcastReceiver mBroadcastReceiver;
    private final ContentObserver mCloudDataObserver;
    private final Context mContext;
    private final AtomicFile mCutoutModeFile;
    private float mDefaultAspect;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final Handler mMainHandler;
    private boolean mNotifySuggestApps;
    private IMiuiProcessObserver mProcessObserver;
    private final HashSet<String> mRestrictList;
    private final AtomicFile mSpecialModeFile;
    private final HashSet<String> mSupportNotchList;
    private final Object mLock = new Object();
    private final HashMap<String, Integer> mPackages = new HashMap<>();
    private final HashMap<String, Integer> mDefaultType = new HashMap<>();
    private final HashMap<String, Integer> mNotchConfig = new HashMap<>();
    private final HashMap<String, Integer> mNotchSpecialModePackages = new HashMap<>();
    private final HashMap<String, Integer> mUserCutoutModePackages = new HashMap<>();
    private final HashMap<String, Integer> mCloudCutoutModePackages = new HashMap<>();
    private final HashSet<String> mSuggestList = new HashSet<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CompatHandler extends Handler {
        public CompatHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    MiuiCompatModePackages.this.saveCompatModes();
                    return;
                case 101:
                    MiuiCompatModePackages.this.readCutoutModeConfig();
                    MiuiCompatModePackages.this.readSpecialModeConfig();
                    MiuiCompatModePackages.this.readPackagesConfig();
                    MiuiCompatModePackages.this.readSuggestApps();
                    return;
                case 102:
                    MiuiCompatModePackages.this.handleRegisterObservers();
                    return;
                case 103:
                    MiuiCompatModePackages.this.handleUnregisterObservers();
                    return;
                case 104:
                    return;
                case 105:
                    if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                        MiuiCompatModePackages.this.handleDontShowAgain();
                        return;
                    }
                    return;
                case 106:
                    MiuiCompatModePackages.this.saveSpecialModeFile();
                    return;
                case 107:
                    MiuiCompatModePackages.this.saveCutoutModeFile();
                    return;
                case 108:
                    MiuiCompatModePackages.this.updateCloudData();
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiCompatModePackages(Context context) {
        HashSet<String> hashSet = new HashSet<>();
        this.mRestrictList = hashSet;
        this.mSupportNotchList = new HashSet<>();
        this.mNotifySuggestApps = true;
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.MiuiCompatModePackages.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String pkg;
                String action = intent.getAction();
                Uri data = intent.getData();
                if (data != null && (pkg = data.getSchemeSpecificPart()) != null) {
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        MiuiCompatModePackages.this.handleUpdatePackage(pkg);
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        MiuiCompatModePackages.this.handleRemovePackage(pkg);
                    }
                }
            }
        };
        this.mContext = context;
        hashSet.add("android.dpi.cts");
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mFile = new AtomicFile(new File(systemDir, "miui-packages-compat.xml"));
        this.mSpecialModeFile = new AtomicFile(new File(systemDir, "miui-specail-mode-v2.xml"));
        this.mCutoutModeFile = new AtomicFile(new File(systemDir, "cutout-mode.xml"));
        CompatHandler compatHandler = new CompatHandler(MiuiFgThread.getHandler().getLooper());
        this.mHandler = compatHandler;
        compatHandler.sendEmptyMessage(101);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, MiuiFgThread.getHandler());
        getDeviceAspect();
        this.mCloudDataObserver = new ContentObserver(compatHandler) { // from class: com.miui.server.MiuiCompatModePackages.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiCompatModePackages.this.updateCloudDataAsync();
            }
        };
        Handler handler = new Handler(Looper.getMainLooper());
        this.mMainHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.MiuiCompatModePackages.2
            @Override // java.lang.Runnable
            public void run() {
                MiuiCompatModePackages.this.mContext.getContentResolver().registerContentObserver(MiuiCompatModePackages.URI_CLOUD_ALL_DATA_NOTIFY, false, MiuiCompatModePackages.this.mCloudDataObserver, -1);
                MiuiCompatModePackages.this.mCloudDataObserver.onChange(false);
            }
        });
    }

    public void updateCloudDataAsync() {
        this.mHandler.removeMessages(108);
        this.mHandler.sendEmptyMessage(108);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudData() {
        Log.d(TAG, "updateCloudData");
        synchronized (this.mLock) {
            this.mCloudCutoutModePackages.clear();
        }
        List<MiuiSettings.SettingsCloudData.CloudData> dataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), MODULE_CUTOUT_MODE);
        if (dataList == null || dataList.size() == 0) {
            return;
        }
        try {
            HashMap<String, Integer> pkgs = new HashMap<>();
            for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                String json = data.toString();
                if (!TextUtils.isEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    String pkg = jsonObject.optString("pkg");
                    int mode = jsonObject.optInt("mode");
                    if (!TextUtils.isEmpty(pkg)) {
                        pkgs.put(pkg, Integer.valueOf(mode));
                    }
                }
            }
            synchronized (this.mLock) {
                this.mCloudCutoutModePackages.putAll(pkgs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readSpecialModeConfig() {
        XmlPullParser parser;
        int eventType;
        String pkg;
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mSpecialModeFile.openRead();
                    parser = Xml.newPullParser();
                    parser.setInput(fis, StandardCharsets.UTF_8.name());
                    eventType = parser.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = parser.next();
                    }
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                Slog.w(TAG, "Error reading compat-packages", e2);
                if (fis == null) {
                    return;
                } else {
                    fis.close();
                }
            }
            if (eventType == 1) {
                if (fis != null) {
                    try {
                        fis.close();
                        return;
                    } catch (IOException e3) {
                        return;
                    }
                }
                return;
            }
            HashMap<String, Integer> pkgs = new HashMap<>();
            String tagName = parser.getName();
            if ("special-mode".equals(tagName)) {
                int eventType2 = parser.next();
                do {
                    if (eventType2 == 2) {
                        String tagName2 = parser.getName();
                        if (parser.getDepth() == 2 && "pkg".equals(tagName2) && (pkg = parser.getAttributeValue(null, "name")) != null) {
                            String mode = parser.getAttributeValue(null, "mode");
                            int modeInt = 0;
                            if (mode != null) {
                                try {
                                    modeInt = Integer.parseInt(mode);
                                } catch (NumberFormatException e4) {
                                }
                            }
                            pkgs.put(pkg, Integer.valueOf(modeInt));
                        }
                    }
                    eventType2 = parser.next();
                } while (eventType2 != 1);
            }
            synchronized (this.mLock) {
                this.mNotchSpecialModePackages.putAll(pkgs);
            }
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e5) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readCutoutModeConfig() {
        XmlPullParser parser;
        int eventType;
        String pkg;
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mCutoutModeFile.openRead();
                    parser = Xml.newPullParser();
                    parser.setInput(fis, StandardCharsets.UTF_8.name());
                    eventType = parser.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = parser.next();
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error reading compat-packages", e);
                    if (fis == null) {
                        return;
                    } else {
                        fis.close();
                    }
                }
                if (eventType == 1) {
                    if (fis != null) {
                        try {
                            fis.close();
                            return;
                        } catch (IOException e2) {
                            return;
                        }
                    }
                    return;
                }
                HashMap<String, Integer> pkgs = new HashMap<>();
                String tagName = parser.getName();
                if ("cutout-mode".equals(tagName)) {
                    int eventType2 = parser.next();
                    do {
                        if (eventType2 == 2) {
                            String tagName2 = parser.getName();
                            if (parser.getDepth() == 2 && "pkg".equals(tagName2) && (pkg = parser.getAttributeValue(null, "name")) != null) {
                                String mode = parser.getAttributeValue(null, "mode");
                                if (mode != null) {
                                    try {
                                        int modeInt = Integer.parseInt(mode);
                                        pkgs.put(pkg, Integer.valueOf(modeInt));
                                    } catch (NumberFormatException e3) {
                                    }
                                }
                            }
                        }
                        eventType2 = parser.next();
                    } while (eventType2 != 1);
                }
                synchronized (this.mLock) {
                    this.mUserCutoutModePackages.putAll(pkgs);
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readPackagesConfig() {
        XmlPullParser parser;
        int eventType;
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mFile.openRead();
                    parser = Xml.newPullParser();
                    parser.setInput(fis, StandardCharsets.UTF_8.name());
                    eventType = parser.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = parser.next();
                    }
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                Slog.w(TAG, "Error reading compat-packages", e2);
                if (fis == null) {
                    return;
                } else {
                    fis.close();
                }
            }
            if (eventType == 1) {
                if (fis != null) {
                    try {
                        fis.close();
                        return;
                    } catch (IOException e3) {
                        return;
                    }
                }
                return;
            }
            HashMap<String, Integer> pkgs = new HashMap<>();
            if ("compat-packages".equals(parser.getName())) {
                int eventType2 = parser.next();
                do {
                    if (eventType2 == 2) {
                        String tagName = parser.getName();
                        if (parser.getDepth() == 2) {
                            if ("pkg".equals(tagName)) {
                                String pkg = parser.getAttributeValue(null, "name");
                                if (pkg != null) {
                                    String mode = parser.getAttributeValue(null, "mode");
                                    int modeInt = 0;
                                    if (mode != null) {
                                        try {
                                            modeInt = Integer.parseInt(mode);
                                        } catch (NumberFormatException e4) {
                                        }
                                    }
                                    pkgs.put(pkg, Integer.valueOf(modeInt));
                                }
                            } else if (TAG_NAME_CONFIG.equals(tagName)) {
                                String notifySuggestApps = parser.getAttributeValue(null, ATTR_CONFIG_NOTIFY_SUGGEST_APPS);
                                this.mNotifySuggestApps = Boolean.valueOf(notifySuggestApps).booleanValue();
                            }
                        }
                    }
                    eventType2 = parser.next();
                } while (eventType2 != 1);
            }
            synchronized (this.mLock) {
                this.mPackages.putAll(pkgs);
            }
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e5) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readSuggestApps() {
        String[] arr = this.mContext.getResources().getStringArray(285409437);
        Collections.addAll(this.mSuggestList, arr);
    }

    private float getPackageMode(String packageName) {
        Integer mode;
        synchronized (this.mLock) {
            mode = this.mPackages.get(packageName);
        }
        return mode != null ? mode.intValue() : getDefaultMode(packageName);
    }

    private int getSpecialMode(String packageName) {
        Integer mode;
        synchronized (this.mLock) {
            mode = this.mNotchSpecialModePackages.get(packageName);
        }
        if (mode != null) {
            return mode.intValue();
        }
        return 0;
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(100);
        Message msg = this.mHandler.obtainMessage(100);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    private void scheduleWriteSpecialMode() {
        this.mHandler.removeMessages(106);
        Message msg = this.mHandler.obtainMessage(106);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    private void scheduleWriteCutoutMode() {
        this.mHandler.removeMessages(107);
        Message msg = this.mHandler.obtainMessage(107);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    void saveCompatModes() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mPackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "compat-packages");
                    fastXmlSerializer.startTag(null, TAG_NAME_CONFIG);
                    fastXmlSerializer.attribute(null, ATTR_CONFIG_NOTIFY_SUGGEST_APPS, String.valueOf(this.mNotifySuggestApps));
                    fastXmlSerializer.endTag(null, TAG_NAME_CONFIG);
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        boolean restrict = mode > 0;
                        if (restrict != isDefaultRestrict(pkg) && getDefaultAspectType(pkg) != 1) {
                            fastXmlSerializer.startTag(null, "pkg");
                            fastXmlSerializer.attribute(null, "name", pkg);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                            fastXmlSerializer.endTag(null, "pkg");
                        }
                    }
                    fastXmlSerializer.endTag(null, "compat-packages");
                    fastXmlSerializer.endDocument();
                    this.mFile.finishWrite(fos);
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e1) {
                Slog.w(TAG, "Error writing compat packages", e1);
                if (fos != null) {
                    this.mFile.failWrite(fos);
                }
                if (fos == null) {
                    return;
                } else {
                    fos.close();
                }
            }
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e2) {
        }
    }

    void saveSpecialModeFile() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mNotchSpecialModePackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mSpecialModeFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "special-mode");
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        boolean special = mode > 0;
                        if (special) {
                            fastXmlSerializer.startTag(null, "pkg");
                            fastXmlSerializer.attribute(null, "name", pkg);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                            fastXmlSerializer.endTag(null, "pkg");
                        }
                    }
                    fastXmlSerializer.endTag(null, "special-mode");
                    fastXmlSerializer.endDocument();
                    this.mSpecialModeFile.finishWrite(fos);
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e1) {
                Slog.w(TAG, "Error writing compat packages", e1);
                if (fos != null) {
                    this.mSpecialModeFile.failWrite(fos);
                }
                if (fos == null) {
                    return;
                } else {
                    fos.close();
                }
            }
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e2) {
        }
    }

    void saveCutoutModeFile() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mUserCutoutModePackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mCutoutModeFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "cutout-mode");
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        fastXmlSerializer.startTag(null, "pkg");
                        fastXmlSerializer.attribute(null, "name", pkg);
                        fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                        fastXmlSerializer.endTag(null, "pkg");
                    }
                    fastXmlSerializer.endTag(null, "cutout-mode");
                    fastXmlSerializer.endDocument();
                    this.mCutoutModeFile.finishWrite(fos);
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception e1) {
                    Slog.w(TAG, "Error writing cutout packages", e1);
                    if (fos != null) {
                        this.mCutoutModeFile.failWrite(fos);
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            } catch (IOException e) {
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }

    private boolean isDefaultRestrict(String pkg) {
        int type = getDefaultAspectType(pkg);
        return type == 4 || type == 5;
    }

    private float getDeviceAspect() {
        float f = this.mDefaultAspect;
        float ratio = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        if (f <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            Display display = this.mContext.getDisplay();
            Point point = new Point();
            display.getRealSize(point);
            int min = Math.min(point.x, point.y);
            int max = Math.max(point.x, point.y);
            if (min != 0) {
                ratio = (max * 1.0f) / min;
            }
            this.mDefaultAspect = ratio;
        }
        return this.mDefaultAspect;
    }

    private int getDefaultMode(String str) {
        return isDefaultRestrict(str) ? 1 : 0;
    }

    private void removePackage(String packageName) {
        boolean realRemove = false;
        synchronized (this.mLock) {
            this.mDefaultType.remove(packageName);
            if (this.mPackages.containsKey(packageName)) {
                this.mPackages.remove(packageName);
                realRemove = true;
            }
        }
        if (realRemove) {
            scheduleWrite();
        }
    }

    private void removeSpecialModePackage(String packageName) {
        boolean realRemove = false;
        synchronized (this.mLock) {
            this.mNotchConfig.remove(packageName);
            if (this.mNotchSpecialModePackages.containsKey(packageName)) {
                this.mNotchSpecialModePackages.remove(packageName);
                realRemove = true;
            }
        }
        if (realRemove) {
            scheduleWriteSpecialMode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemovePackage(String packageName) {
        removePackage(packageName);
        removeSpecialModePackage(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdatePackage(String packageName) {
        synchronized (this.mLock) {
            this.mDefaultType.remove(packageName);
            this.mNotchConfig.remove(packageName);
        }
        boolean isDefaultRestrict = isDefaultRestrict(packageName);
        boolean isRestrict = isRestrictAspect(packageName);
        if (isDefaultRestrict == isRestrict || getDefaultAspectType(packageName) == 1) {
            Slog.i(TAG, "package " + packageName + " updated, removing config");
            removePackage(packageName);
        }
    }

    public float getAspectRatio(String pkg) {
        if (isRestrictAspect(pkg)) {
            return CustomizeUtil.RESTRICT_ASPECT_RATIO;
        }
        return 3.0f;
    }

    public int getNotchConfig(String packageName) {
        Integer mode;
        int config = 0;
        synchronized (this.mLock) {
            if (this.mNotchSpecialModePackages.containsKey(packageName) && (mode = this.mNotchSpecialModePackages.get(packageName)) != null) {
                config = mode.intValue() != 0 ? 128 : 0;
            }
        }
        return config | getDefaultNotchConfig(packageName);
    }

    private int getDefaultNotchConfig(String packageName) {
        synchronized (this.mLock) {
            if (this.mNotchConfig.containsKey(packageName)) {
                return this.mNotchConfig.get(packageName).intValue();
            }
            int type = resolveNotchConfig(packageName);
            synchronized (this.mLock) {
                this.mNotchConfig.put(packageName, Integer.valueOf(type));
            }
            return type;
        }
    }

    private int resolveNotchConfig(String packageName) {
        Bundle metadata;
        if (this.mSupportNotchList.contains(packageName)) {
            return 1792;
        }
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 128L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null || (metadata = ai.metaData) == null) {
            return 0;
        }
        String notch = metadata.getString("notch.config");
        if (TextUtils.isEmpty(notch)) {
            return 0;
        }
        int config = 0 | 256;
        if (notch.contains("portrait")) {
            config |= 512;
        }
        if (notch.contains("landscape")) {
            return config | 1024;
        }
        return config;
    }

    public void setNotchSpecialMode(String pkg, boolean special) {
        boolean oldSpecail = isNotchSpecailMode(pkg);
        if (special != oldSpecail) {
            synchronized (this.mLock) {
                this.mNotchSpecialModePackages.put(pkg, Integer.valueOf(special ? 1 : 0));
            }
            scheduleWriteSpecialMode();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
        }
    }

    private boolean isNotchSpecailMode(String pkg) {
        return getSpecialMode(pkg) != 0;
    }

    public void setCutoutMode(String pkg, int mode) {
        synchronized (this.mLock) {
            int oldMode = this.mUserCutoutModePackages.getOrDefault(pkg, 0).intValue();
            if (oldMode == mode) {
                return;
            }
            this.mUserCutoutModePackages.put(pkg, Integer.valueOf(mode));
            scheduleWriteCutoutMode();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
        }
    }

    public int getCutoutMode(String pkg) {
        synchronized (this.mLock) {
            if (this.mUserCutoutModePackages.containsKey(pkg)) {
                return this.mUserCutoutModePackages.get(pkg).intValue();
            }
            synchronized (this.mLock) {
                if (this.mCloudCutoutModePackages.containsKey(pkg)) {
                    return this.mCloudCutoutModePackages.get(pkg).intValue();
                }
                int flag = getDefaultNotchConfig(pkg);
                if ((flag & 1792) != 1792) {
                    return 0;
                }
                return 1;
            }
        }
    }

    public int getDefaultAspectType(String packageName) {
        synchronized (this.mLock) {
            if (this.mDefaultType.containsKey(packageName)) {
                return this.mDefaultType.get(packageName).intValue();
            }
            int type = resolveDefaultAspectType(packageName);
            synchronized (this.mLock) {
                this.mDefaultType.put(packageName, Integer.valueOf(type));
            }
            return type;
        }
    }

    private int resolveDefaultAspectType(String packageName) {
        if ("jp.netstar.familysmile".equals(packageName) || "jp.softbank.mb.parentalcontrols".equals(packageName)) {
            return 1;
        }
        if (this.mRestrictList.contains(packageName)) {
            return 4;
        }
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 128L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return 0;
        }
        Bundle metadata = ai.metaData;
        float aspect = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        if (metadata != null) {
            aspect = metadata.getFloat("android.max_aspect");
        }
        if (aspect >= getDeviceAspect()) {
            return 1;
        }
        if (this.mSuggestList.contains(packageName)) {
            return 3;
        }
        return 5;
    }

    public boolean isRestrictAspect(String packageName) {
        return getPackageMode(packageName) != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    public void setRestrictAspect(String pkg, boolean restrict) {
        boolean curRestrict = isRestrictAspect(pkg);
        if (restrict != curRestrict) {
            synchronized (this.mLock) {
                this.mPackages.put(pkg, Integer.valueOf(restrict ? 1 : 0));
            }
            scheduleWrite();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRegisterObservers() {
        if (!this.mNotifySuggestApps) {
            return;
        }
        this.mProcessObserver = new AppLaunchObserver() { // from class: com.miui.server.MiuiCompatModePackages.4
            @Override // com.miui.server.MiuiCompatModePackages.AppLaunchObserver
            protected void onFirstLaunch(String packageName) {
                MiuiCompatModePackages.this.mHandler.removeMessages(104);
                MiuiCompatModePackages.this.mHandler.sendMessageDelayed(Message.obtain(MiuiCompatModePackages.this.mHandler, 104, packageName), 500L);
            }
        };
        try {
            Slog.i(TAG, "registering process observer...");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            this.mProcessObserver = null;
            Slog.e(TAG, "error when registering process observer", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnregisterObservers() {
        if (this.mProcessObserver != null) {
            Slog.i(TAG, "unregistering process observer...");
            try {
                try {
                    ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error when unregistering process observer", e);
                }
            } finally {
                this.mProcessObserver = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDontShowAgain() {
        this.mNotifySuggestApps = false;
        this.mHandler.sendEmptyMessage(103);
        this.mHandler.sendEmptyMessage(100);
    }

    private void gotoMaxAspectSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
            intent.putExtra(":settings:show_fragment", "com.android.settings.MaxAspectRatioSettings");
            intent.addFlags(268435456);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "error when goto max aspect settings", e);
        }
    }

    /* loaded from: classes.dex */
    private static abstract class AppLaunchObserver extends IMiuiProcessObserver {
        private HashSet<Integer> mRunningFgActivityProcesses;

        protected abstract void onFirstLaunch(String str);

        private AppLaunchObserver() {
            this.mRunningFgActivityProcesses = new HashSet<>();
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities && !this.mRunningFgActivityProcesses.contains(Integer.valueOf(pid))) {
                this.mRunningFgActivityProcesses.add(Integer.valueOf(pid));
                String packageName = ProcessUtils.getPackageNameByPid(pid);
                onFirstLaunch(packageName);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onImportanceChanged(int pid, int uid, int importance) {
        }

        public void onProcessDied(int pid, int uid) {
            this.mRunningFgActivityProcesses.remove(Integer.valueOf(pid));
        }

        public void onProcessStateChanged(int pid, int uid, int procState) {
        }
    }
}
