package com.android.server.pm;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.logging.EventLogTags;
import com.android.internal.util.XmlUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.pm.PackageManagerCloudHelper;
import com.android.server.pm.ResilientAtomicFile;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class PackageManagerCloudHelper {
    private static final String CLOUD_DATA_VERSION = "version";
    private static final String CLOUD_MODULE_NAME = "block_update";
    private static final String TAG = PackageManagerCloudHelper.class.getSimpleName();
    private Context mContext;
    private PackageManagerService mPms;
    private File mPreviousSettingsFilename;
    private File mSettingsFilename;
    private File mSettingsReserveCopyFilename;
    private File mSystemDir;
    private final Set<String> mCloudNotSupportUpdateSystemApps = new HashSet();
    private final Object mNotSupportUpdateLock = new Object();
    private int mLastCloudConfigVersion = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<String> getCloudNotSupportUpdateSystemApps() {
        return this.mCloudNotSupportUpdateSystemApps;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageManagerCloudHelper(PackageManagerService pms, Context context) {
        this.mPms = pms;
        this.mContext = context;
        initPackageCloudSettings();
    }

    private void initPackageCloudSettings() {
        this.mSystemDir = new File(Environment.getDataDirectory(), "system");
        this.mSettingsFilename = new File(this.mSystemDir, "packagemanger_cloud.xml");
        this.mSettingsReserveCopyFilename = new File(this.mSystemDir, "packagemanger_cloud.xml.reservecopy");
        this.mPreviousSettingsFilename = new File(this.mSystemDir, "packagemanger_cloud-backup.xml");
        readCloudSetting();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCloudDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(this.mPms.mHandler));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.PackageManagerCloudHelper$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        AnonymousClass1(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            PackageManagerCloudHelper packageManagerCloudHelper = PackageManagerCloudHelper.this;
            boolean isCloudConfigChanged = packageManagerCloudHelper.isCloudConfigUpdate(packageManagerCloudHelper.mContext);
            Slog.i(PackageManagerCloudHelper.TAG, "MiuiSettings notify cloud data is changed. Whether is applying new package cloud data : " + isCloudConfigChanged);
            if (isCloudConfigChanged) {
                PackageManagerCloudHelper.this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerCloudHelper$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerCloudHelper.AnonymousClass1.this.lambda$onChange$0();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0() {
            PackageManagerCloudHelper.this.readCloudData();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCloudConfigUpdate(Context context) {
        String dataVersion = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), CLOUD_MODULE_NAME, "version", (String) null);
        if (TextUtils.isEmpty(dataVersion)) {
            Slog.d(TAG, "data version is empty, or control module block_update is not exist");
            return false;
        }
        int remoteVersion = Integer.parseInt(dataVersion);
        if (remoteVersion <= this.mLastCloudConfigVersion) {
            Slog.d(TAG, "cloud data is not modify, don't need update. data version = " + remoteVersion);
            return false;
        }
        this.mLastCloudConfigVersion = remoteVersion;
        Slog.d(TAG, "package manager cloud data new version " + remoteVersion);
        return true;
    }

    private ResilientAtomicFile getCloudSettingsFile() {
        return new ResilientAtomicFile(this.mSettingsFilename, this.mPreviousSettingsFilename, this.mSettingsReserveCopyFilename, 432, "package manager cloud settings", (ResilientAtomicFile.ReadEventLogger) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readCloudData() {
        List<MiuiSettings.SettingsCloudData.CloudData> dataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), CLOUD_MODULE_NAME);
        if (dataList == null || dataList.size() == 0) {
            return;
        }
        try {
            Set<String> pkgs = new HashSet<>();
            for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                String json = data.toString();
                if (!TextUtils.isEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray jsonArray = jsonObject.getJSONArray("packageName");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String pkg = jsonArray.getString(i);
                        if (!TextUtils.isEmpty(pkg)) {
                            pkgs.add(pkg);
                        }
                    }
                }
            }
            synchronized (this.mNotSupportUpdateLock) {
                this.mCloudNotSupportUpdateSystemApps.clear();
                this.mCloudNotSupportUpdateSystemApps.addAll(pkgs);
            }
            this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerCloudHelper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerCloudHelper.this.lambda$readCloudData$0();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: writeCloudSetting, reason: merged with bridge method [inline-methods] */
    public void lambda$readCloudData$0() {
        long startTime = SystemClock.uptimeMillis();
        Set<String> pkgs = new HashSet<>();
        synchronized (this.mNotSupportUpdateLock) {
            pkgs.addAll(this.mCloudNotSupportUpdateSystemApps);
        }
        ResilientAtomicFile atomicFile = getCloudSettingsFile();
        FileOutputStream str = null;
        try {
            try {
                str = atomicFile.startWrite();
                TypedXmlSerializer serializer = Xml.resolveSerializer(str);
                serializer.startDocument((String) null, true);
                serializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                serializer.startTag((String) null, "package-cloudconfig");
                serializer.startTag((String) null, "packages");
                serializer.attributeInt((String) null, "version", this.mLastCloudConfigVersion);
                for (String packageName : pkgs) {
                    serializer.startTag((String) null, "package");
                    serializer.attribute((String) null, "name", packageName);
                    serializer.endTag((String) null, "package");
                }
                serializer.endTag((String) null, "packages");
                serializer.endTag((String) null, "package-cloudconfig");
                serializer.endDocument();
                atomicFile.finishWrite(str);
                EventLogTags.writeCommitSysConfigFile("package_cloud", SystemClock.uptimeMillis() - startTime);
            } catch (IOException e) {
                this.mLastCloudConfigVersion = 0;
                Slog.e(TAG, "Unable to write package manager cloud settings: " + e.getMessage());
                if (str != null) {
                    atomicFile.failWrite(str);
                }
            }
            if (atomicFile != null) {
                atomicFile.close();
            }
        } catch (Throwable th) {
            if (atomicFile != null) {
                try {
                    atomicFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void readCloudSetting() {
        FileInputStream str;
        int type;
        long startTime = SystemClock.uptimeMillis();
        ResilientAtomicFile atomicFile = getCloudSettingsFile();
        try {
            try {
                str = atomicFile.openRead();
            } catch (Exception e) {
                Slog.e(TAG, "readNotSupportUpdateConfig fail: " + e.getMessage());
                this.mLastCloudConfigVersion = 0;
                atomicFile.failRead((FileInputStream) null, e);
                readCloudSetting();
            }
            if (str != null) {
                Set<String> pkgs = new HashSet<>();
                TypedXmlPullParser parser = Xml.resolvePullParser(str);
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Slog.e(TAG, "No start tag found in package manager cloud settings");
                    if (atomicFile != null) {
                        atomicFile.close();
                        return;
                    }
                    return;
                }
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if ("packages".equals(tagName)) {
                            String version = parser.getAttributeValue((String) null, "version");
                            if (TextUtils.isEmpty(version)) {
                                Slog.e(TAG, "read block-update config version is null");
                                break;
                            } else {
                                this.mLastCloudConfigVersion = Integer.parseInt(version);
                                readPackageNames(pkgs, parser);
                            }
                        } else {
                            Slog.w(TAG, "Unknown element under <package-cloudconfig>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                str.close();
                Slog.d(TAG, "read packageCloudSetting took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                if (atomicFile != null) {
                    atomicFile.close();
                    return;
                }
                return;
            }
            if (atomicFile != null) {
                atomicFile.close();
            }
        } catch (Throwable th) {
            if (atomicFile != null) {
                try {
                    atomicFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void readPackageNames(Set<String> pkgs, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if ("package".equals(tagName)) {
                    String pkgName = parser.getAttributeValue((String) null, "name");
                    if (TextUtils.isEmpty(pkgName)) {
                        Slog.e(TAG, "read block-update config pkgName is null");
                        break;
                    }
                    pkgs.add(pkgName);
                } else {
                    Slog.w(TAG, "Unknown element under <packages>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        synchronized (this.mNotSupportUpdateLock) {
            this.mCloudNotSupportUpdateSystemApps.clear();
            this.mCloudNotSupportUpdateSystemApps.addAll(pkgs);
        }
    }
}
