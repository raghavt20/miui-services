package com.android.server.cloud;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.server.MiuiCommonCloudServiceStub;
import com.android.server.wm.MiuiSizeCompatService;
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
public class MiuiCloudParsingCollection {
    private static final String ATTR_CONFIG_FILE_NAME = "attributes_config.xml";
    private static final String CLOUD_BACKUP_FILE_NAME = "common_cloud_backup.xml";
    private static final String CLOUD_BACKUP_FILE_ROOT_ELEMENT = "common-cloud-config";
    private static final String CLOUD_BACKUP_FILE_TEST_CRT_TAG = "cloud-control";
    private static final String CLOUD_CTRL_ITEM_TAG = "cloud-data-item";
    private static final String CLOUD_MODULE_BEAN_PATH = "moduleBeanPath";
    private static final String CLOUD_MODULE_NAME = "moduleName";
    private static final String CLOUD_MODULE_STRING = "moduleString";
    private static final String CLOUD_MODULE_VERSION = "moduleVersion";
    private static final String MODULE_DATA_VERSION = "version";
    private static final String TAG = "MiuiCloudParsingCollection";
    private AssetManager mAssets;
    private File mCloudBackFile;
    private Context mContext;
    private HashMap<String, String> mModuleNameStrMap = new HashMap<>();
    private HashMap<String, String> mModuleNameBeanClassPathMap = new HashMap<>();
    private HashMap<String, String> mModuleNameDataVersionMap = new HashMap<>();
    private HashMap<String, Object> mModuleNameClassObjectMap = new HashMap<>();
    private ArrayList<MiuiCommonCloudServiceStub.Observer> mObservers = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiuiCloudParsingCollection(Context context) {
        this.mContext = context;
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.cloud.MiuiCloudParsingCollection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCloudParsingCollection.this.lambda$new$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initialization, reason: merged with bridge method [inline-methods] */
    public void lambda$new$0() {
        loadLocalInitConfig();
        loadLocalCloudBackup();
        parseStringToClassObject();
        notifyAllObservers();
    }

    private void loadLocalInitConfig() {
        AssetManager assets = Resources.getSystem().getAssets();
        this.mAssets = assets;
        try {
            InputStream inputStream = assets.open(ATTR_CONFIG_FILE_NAME);
            try {
                if (inputStream == null) {
                    Slog.e(TAG, "file attributes_config not exits ");
                } else {
                    loadLocalFile(inputStream);
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read attributes_config.xml ", e);
        }
    }

    private void loadLocalCloudBackup() {
        File file = new File(Environment.getDataSystemDeDirectory(), CLOUD_BACKUP_FILE_NAME);
        this.mCloudBackFile = file;
        checkFileStatus(file);
        try {
            FileInputStream inputStream = new FileInputStream(this.mCloudBackFile);
            try {
                loadLocalFile(inputStream);
                inputStream.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read common_cloud_backup.xml ", e);
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
                            Slog.d(TAG, "last stored cloud data version: " + lastStoredVersion + " is higher than init data version; " + initVersion + ", so use last stored cloud data");
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
                Slog.d(TAG, "create common_cloud_backup fail " + e);
                return;
            }
        }
        file.createNewFile();
        Slog.d(TAG, "common_cloud_backup not exist, create file end");
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

    public Object parseStringToClass(String data, String classPath) {
        try {
            Class<?> clazz = Class.forName(classPath);
            return JsonUtil.fromJson(data, clazz);
        } catch (Exception e) {
            Slog.e(TAG, "parseStringToClass exception " + e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDataFromCloud() {
        boolean changed = false;
        for (String moduleName : this.mModuleNameStrMap.keySet()) {
            changed |= updateCloudData(moduleName);
        }
        return changed;
    }

    private boolean updateCloudData(String moduleName) {
        String latestVersion = this.mModuleNameDataVersionMap.get(moduleName);
        String remoteVersion = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), moduleName, "version", (String) null);
        List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), moduleName);
        if (remoteVersion == null) {
            Slog.d(TAG, " remoteVersion is null, Don't update ");
            return false;
        }
        if (latestVersion == null || Long.parseLong(remoteVersion) <= Long.parseLong(latestVersion)) {
            return false;
        }
        Slog.d(TAG, " moduleName: " + moduleName + ", lastVersion: " + latestVersion + ", remoteVersion: " + remoteVersion + ", so update");
        return updateMultiMap(moduleName, remoteVersion, cloudDataList);
    }

    private boolean updateMultiMap(String moduleName, String remoteVersion, List<MiuiSettings.SettingsCloudData.CloudData> cloudDataList) {
        if (cloudDataList == null) {
            Slog.e(TAG, "cloudDataList is null, see early log for detail");
            return false;
        }
        String cloudData = cloudDataList.get(0).toString();
        String beanPath = this.mModuleNameBeanClassPathMap.get(moduleName);
        if (cloudData == null || beanPath == null) {
            Slog.e(TAG, "moduleName or beanPath is null");
            return false;
        }
        Object classObject = parseStringToClass(cloudData, beanPath);
        if (classObject == null) {
            Slog.e(TAG, moduleName + " parsed classObject is null, check cloud data and Bean properties");
            return false;
        }
        this.mModuleNameDataVersionMap.put(moduleName, remoteVersion);
        this.mModuleNameStrMap.put(moduleName, cloudData);
        this.mModuleNameClassObjectMap.put(moduleName, classObject);
        return true;
    }

    public void syncLocalBackupFromCloud() {
        File file = new File(Environment.getDataSystemDeDirectory(), CLOUD_BACKUP_FILE_NAME);
        this.mCloudBackFile = file;
        checkFileStatus(file);
        long startTime = SystemClock.uptimeMillis();
        try {
            BufferedOutputStream backUpBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(this.mCloudBackFile));
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
                Slog.d(TAG, "Write common_cloud_backup.xml took " + (SystemClock.uptimeMillis() - startTime) + " ms ");
                backUpBufferedOutputStream.close();
            } finally {
            }
        } catch (IOException e) {
            if (this.mCloudBackFile.exists() && !this.mCloudBackFile.delete()) {
                Slog.w(TAG, "Failed to clean up mangled file: " + this.mCloudBackFile);
            }
            Slog.e(TAG, "Unable to write cloud backup xml,current changes will be lost at reboot", e);
        }
    }

    private void writeElementOfModuleListToXml(final XmlSerializer serializer, HashMap<String, String> strMap, final String tagString) {
        strMap.forEach(new BiConsumer() { // from class: com.android.server.cloud.MiuiCloudParsingCollection$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiCloudParsingCollection.this.lambda$writeElementOfModuleListToXml$1(serializer, tagString, (String) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$writeElementOfModuleListToXml$1(XmlSerializer serializer, String tagString, String key, String value) {
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

    public String getStringByModuleName(String moduleName) {
        return this.mModuleNameStrMap.get(moduleName);
    }

    public void registerObserver(MiuiCommonCloudServiceStub.Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        if (!this.mObservers.contains(observer)) {
            this.mObservers.add(observer);
        }
    }

    public void unregisterObserver(MiuiCommonCloudServiceStub.Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        this.mObservers.remove(observer);
    }

    public void notifyAllObservers() {
        Iterator<MiuiCommonCloudServiceStub.Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            MiuiCommonCloudServiceStub.Observer observer = it.next();
            observer.update();
        }
    }
}
