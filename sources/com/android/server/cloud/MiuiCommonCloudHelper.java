package com.android.server.cloud;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.server.MiuiCommonCloudHelperStub;
import com.android.server.cloud.MiuiCommonCloudHelper;
import com.android.server.wm.MiuiSizeCompatService;
import com.google.gson.Gson;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MiuiCommonCloudHelper extends MiuiCommonCloudHelperStub {
    private static final String CLOUD_BACKUP_FILE_ROOT_ELEMENT = "common-cloud-config";
    private static final String CLOUD_BACKUP_FILE_TEST_CRT_TAG = "cloud-control";
    private static final String CLOUD_CTRL_ITEM_TAG = "cloud-data-item";
    private static final String CLOUD_MODULE_BEAN_PATH = "moduleBeanPath";
    private static final String CLOUD_MODULE_NAME = "moduleName";
    private static final String CLOUD_MODULE_STRING = "moduleString";
    private static final String CLOUD_MODULE_VERSION = "moduleVersion";
    private static final String MODULE_DATA_VERSION = "mVersion";
    private static final String TAG = "MiuiCommonCloudHelper";
    private String mCloudFilePath;
    private Context mContext;
    private Gson mGson;
    private Handler mHandler;
    private HashMap<String, String> mModuleNameStrMap = new HashMap<>();
    private HashMap<String, String> mModuleNameBeanClassPathMap = new HashMap<>();
    private HashMap<String, String> mModuleNameDataVersionMap = new HashMap<>();
    private HashMap<String, Object> mModuleNameClassObjectMap = new HashMap<>();
    private ArrayList<MiuiCommonCloudHelperStub.Observer> mObservers = new ArrayList<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiCommonCloudHelper> {

        /* compiled from: MiuiCommonCloudHelper$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiCommonCloudHelper INSTANCE = new MiuiCommonCloudHelper();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiCommonCloudHelper m943provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiCommonCloudHelper m942provideNewInstance() {
            return new MiuiCommonCloudHelper();
        }
    }

    public void init(Context context, Handler handler, String cloudFilePath) {
        this.mContext = context;
        this.mGson = new Gson();
        this.mHandler = handler;
        this.mCloudFilePath = cloudFilePath;
    }

    public void initFromAssets(final String assetsConfigFilePath) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.cloud.MiuiCommonCloudHelper$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCommonCloudHelper.this.lambda$initFromAssets$0(assetsConfigFilePath);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initFromAssets$0(String assetsConfigFilePath) {
        loadLocalInitConfigFromAssets(assetsConfigFilePath);
        loadLocalCloudBackup(this.mCloudFilePath);
        parseStringToClassObject();
        notifyAllObservers();
    }

    public void initFromPath(final String configFilePath) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.cloud.MiuiCommonCloudHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCommonCloudHelper.this.lambda$initFromPath$1(configFilePath);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initFromPath$1(String configFilePath) {
        loadLocalInitConfigFromPath(configFilePath);
        loadLocalCloudBackup(this.mCloudFilePath);
        parseStringToClassObject();
        notifyAllObservers();
    }

    private void loadLocalInitConfigFromAssets(String assetsConfigFilePath) {
        AssetManager assetManager = Resources.getSystem().getAssets();
        try {
            InputStream inputStream = assetManager.open(assetsConfigFilePath);
            try {
                if (inputStream == null) {
                    Slog.e(TAG, "Local init config file " + assetsConfigFilePath + " does not exits ");
                } else {
                    loadLocalFile(inputStream);
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read local init config file " + assetsConfigFilePath, e);
        }
    }

    private void loadLocalInitConfigFromPath(String path) {
        try {
            InputStream inputStream = new FileInputStream(path);
            loadLocalFile(inputStream);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read local init config file " + path, e);
        }
    }

    private void loadLocalCloudBackup(String cloudPath) {
        File mCloudBackFile = new File(cloudPath);
        checkFileStatus(mCloudBackFile);
        try {
            FileInputStream inputStream = new FileInputStream(mCloudBackFile);
            try {
                loadLocalFile(inputStream);
                inputStream.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read local cloud_backup file ", e);
        }
    }

    private void loadLocalFile(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, StandardCharsets.UTF_8.name());
        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
            switch (eventType) {
                case 2:
                    String tagName = parser.getName();
                    if (CLOUD_CTRL_ITEM_TAG.equals(tagName)) {
                        String moduleName = parser.getAttributeValue(null, CLOUD_MODULE_NAME);
                        String moduleVersion = parser.getAttributeValue(null, CLOUD_MODULE_VERSION);
                        String moduleBeanPath = parser.getAttributeValue(null, CLOUD_MODULE_BEAN_PATH);
                        String moduleString = parser.getAttributeValue(null, CLOUD_MODULE_STRING);
                        Long lastStoredVersion = Long.valueOf(Long.parseLong(moduleVersion));
                        Long initVersion = Long.valueOf(Long.parseLong(this.mModuleNameDataVersionMap.getOrDefault(moduleName, "-1")));
                        if (lastStoredVersion.longValue() > initVersion.longValue()) {
                            this.mModuleNameStrMap.put(moduleName, moduleString);
                            this.mModuleNameDataVersionMap.put(moduleName, moduleVersion);
                            this.mModuleNameBeanClassPathMap.put(moduleName, moduleBeanPath);
                            Slog.d(TAG, "Last stored cloud data version: " + lastStoredVersion + " is higher than init data version: " + initVersion + ", so use last stored cloud data");
                            break;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
            }
        }
    }

    private void checkFileStatus(File file) {
        if (file != null) {
            try {
                if (file.exists()) {
                    return;
                }
            } catch (IOException e) {
                Slog.e(TAG, "Create local cloud_backup file fail " + e);
                return;
            }
        }
        file.createNewFile();
        Slog.d(TAG, "Local cloud_backup file dose not exist, create file end");
    }

    public void startCloudObserve() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(this.mHandler));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.cloud.MiuiCommonCloudHelper$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        AnonymousClass1(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            boolean changed = MiuiCommonCloudHelper.this.updateDataFromCloud();
            Slog.d(MiuiCommonCloudHelper.TAG, "Cloud data changed: " + changed);
            if (changed) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.cloud.MiuiCommonCloudHelper$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiCommonCloudHelper.AnonymousClass1.this.lambda$onChange$0();
                    }
                });
                MiuiCommonCloudHelper.this.notifyAllObservers();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0() {
            MiuiCommonCloudHelper miuiCommonCloudHelper = MiuiCommonCloudHelper.this;
            miuiCommonCloudHelper.syncLocalBackupFromCloud(miuiCommonCloudHelper.mCloudFilePath);
        }
    }

    private void parseStringToClassObject() {
        for (String moduleName : this.mModuleNameStrMap.keySet()) {
            Object classObj = null;
            String data = this.mModuleNameStrMap.get(moduleName);
            String beanPath = this.mModuleNameBeanClassPathMap.get(moduleName);
            if (moduleName != null && beanPath != null) {
                classObj = parseStringToClass(data, beanPath);
            }
            if (classObj != null) {
                this.mModuleNameClassObjectMap.put(moduleName, classObj);
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Object parseStringToClass(String data, String classPath) {
        char c;
        Class<?> clazz;
        try {
            switch (classPath.hashCode()) {
                case -1808118735:
                    if (classPath.equals("String")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case -1325958191:
                    if (classPath.equals("double")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 104431:
                    if (classPath.equals("int")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 3039496:
                    if (classPath.equals("byte")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 3052374:
                    if (classPath.equals("char")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 3327612:
                    if (classPath.equals("long")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 64711720:
                    if (classPath.equals("boolean")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 97526364:
                    if (classPath.equals("float")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 109413500:
                    if (classPath.equals("short")) {
                        c = 5;
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
                    clazz = Boolean.TYPE;
                    break;
                case 1:
                    clazz = Integer.TYPE;
                    break;
                case 2:
                    clazz = Float.TYPE;
                    break;
                case 3:
                    clazz = Character.TYPE;
                    break;
                case 4:
                    clazz = Byte.TYPE;
                    break;
                case 5:
                    clazz = Short.TYPE;
                    break;
                case 6:
                    clazz = Long.TYPE;
                    break;
                case 7:
                    clazz = Double.TYPE;
                    break;
                case '\b':
                    clazz = String.class;
                    break;
                default:
                    clazz = Class.forName(classPath);
                    break;
            }
            return this.mGson.fromJson(data, (Class) clazz);
        } catch (Exception e) {
            Slog.e(TAG, "parseStringToClass exception " + e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateDataFromCloud() {
        boolean changed = false;
        for (String moduleName : this.mModuleNameStrMap.keySet()) {
            changed |= updateCloudData(moduleName);
        }
        return changed;
    }

    private boolean updateCloudData(String moduleName) {
        String latestVersion = this.mModuleNameDataVersionMap.get(moduleName);
        String remoteVersion = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), moduleName, MODULE_DATA_VERSION, (String) null);
        List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), moduleName);
        if (remoteVersion == null) {
            Slog.d(TAG, " remoteVersion is null, don't update ");
            return false;
        }
        if (latestVersion == null || Long.parseLong(remoteVersion) <= Long.parseLong(latestVersion)) {
            return false;
        }
        Slog.d(TAG, " moduleName: " + moduleName + ", lastVersion: " + latestVersion + ", remoteVersion: " + remoteVersion + ", so update");
        return updateModuleData(moduleName, remoteVersion, cloudDataList);
    }

    private boolean updateModuleData(String moduleName, String remoteVersion, List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList) {
        String cloudData = cloudDataList.get(0).toString();
        String beanPath = this.mModuleNameBeanClassPathMap.get(moduleName);
        if (cloudData == null || beanPath == null) {
            Slog.e(TAG, "moduleName or beanPath is null");
            return false;
        }
        Object classObject = parseStringToClass(cloudData, beanPath);
        if (classObject == null) {
            Slog.e(TAG, moduleName + " parsed classObject is null, please check cloud data and bean properties");
            return false;
        }
        this.mModuleNameDataVersionMap.put(moduleName, remoteVersion);
        this.mModuleNameStrMap.put(moduleName, cloudData);
        this.mModuleNameClassObjectMap.put(moduleName, classObject);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncLocalBackupFromCloud(String cloudFilePath) {
        File mCloudBackFile = new File(cloudFilePath);
        checkFileStatus(mCloudBackFile);
        long startTime = SystemClock.uptimeMillis();
        try {
            BufferedOutputStream backUpBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(mCloudBackFile));
            try {
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(backUpBufferedOutputStream, StandardCharsets.UTF_8.name());
                serializer.startDocument(StandardCharsets.UTF_8.name(), true);
                serializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                serializer.startTag(null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                serializer.startTag(null, CLOUD_BACKUP_FILE_TEST_CRT_TAG);
                writeElementOfModuleListToXml(serializer, this.mModuleNameStrMap, CLOUD_CTRL_ITEM_TAG);
                serializer.endTag(null, CLOUD_BACKUP_FILE_TEST_CRT_TAG);
                serializer.endTag(null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                serializer.endDocument();
                backUpBufferedOutputStream.flush();
                Slog.d(TAG, "Write local cloud_backup file took " + (SystemClock.uptimeMillis() - startTime) + " ms ");
                backUpBufferedOutputStream.close();
            } finally {
            }
        } catch (IOException e) {
            if (mCloudBackFile.exists() && !mCloudBackFile.delete()) {
                Slog.w(TAG, "Failed to clean up mangled file: " + mCloudBackFile);
            }
            Slog.e(TAG, "Unable to write local cloud_backup file", e);
        }
    }

    private void writeElementOfModuleListToXml(final XmlSerializer serializer, HashMap<String, String> strMap, final String tagString) {
        strMap.forEach(new BiConsumer() { // from class: com.android.server.cloud.MiuiCommonCloudHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiCommonCloudHelper.this.lambda$writeElementOfModuleListToXml$2(serializer, tagString, (String) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$writeElementOfModuleListToXml$2(XmlSerializer serializer, String tagString, String key, String value) {
        if (value != null) {
            try {
                serializer.startTag(null, tagString);
                serializer.attribute(null, CLOUD_MODULE_NAME, key);
                serializer.attribute(null, CLOUD_MODULE_VERSION, this.mModuleNameDataVersionMap.get(key));
                serializer.attribute(null, CLOUD_MODULE_BEAN_PATH, this.mModuleNameBeanClassPathMap.get(key));
                serializer.attribute(null, CLOUD_MODULE_STRING, value);
                serializer.endTag(null, tagString);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write element of app list to xml" + e);
                e.printStackTrace();
            }
        }
    }

    public Object getDataByModuleName(String moduleName) {
        return this.mModuleNameClassObjectMap.get(moduleName);
    }

    private String getStringByModuleName(String moduleName) {
        return this.mModuleNameStrMap.get(moduleName);
    }

    public void registerObserver(MiuiCommonCloudHelperStub.Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        if (!this.mObservers.contains(observer)) {
            this.mObservers.add(observer);
        }
    }

    public void unregisterObserver(MiuiCommonCloudHelperStub.Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        this.mObservers.remove(observer);
    }

    public void notifyAllObservers() {
        Iterator<MiuiCommonCloudHelperStub.Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            MiuiCommonCloudHelperStub.Observer observer = it.next();
            observer.update();
        }
    }
}
