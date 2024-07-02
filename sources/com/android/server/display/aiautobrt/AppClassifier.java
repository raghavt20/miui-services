package com.android.server.display.aiautobrt;

import android.os.Environment;
import android.util.Singleton;
import android.util.Slog;
import com.android.server.display.aiautobrt.config.AppCategory;
import com.android.server.display.aiautobrt.config.AppCategoryConfig;
import com.android.server.display.aiautobrt.config.PackageInfo;
import com.android.server.display.aiautobrt.config.XmlParser;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class AppClassifier {
    private static final String APP_CATEGORY_CONFIG_DIR = "displayconfig";
    public static final int CATEGORY_FINANCE_LEARN = 4;
    public static final int CATEGORY_GAME = 1;
    public static final int CATEGORY_MAX = 9;
    public static final int CATEGORY_MUSIC_READ = 3;
    public static final int CATEGORY_NEWS = 5;
    public static final int CATEGORY_PHOTO = 7;
    public static final int CATEGORY_SHOPPING = 6;
    public static final int CATEGORY_SOCIAL = 9;
    public static final int CATEGORY_TRAVEL = 8;
    public static final int CATEGORY_UNDEFINED = 0;
    public static final int CATEGORY_VIDEO = 2;
    private static final String CLOUD_BACKUP_CONFIG_FILE = "cloud_app_brightness_category.xml";
    private static final String DEFAULT_CONFIG_FILE = "app_brightness_category.xml";
    private static final String ETC_DIR = "etc";
    private static final String TAG = "CbmController-AppClassifier";
    private static final Singleton<AppClassifier> sInstance = new Singleton<AppClassifier>() { // from class: com.android.server.display.aiautobrt.AppClassifier.1
        /* JADX INFO: Access modifiers changed from: protected */
        /* renamed from: create, reason: merged with bridge method [inline-methods] */
        public AppClassifier m1274create() {
            return new AppClassifier();
        }
    };
    private final List<PackageInfo> mAppCategoryInfo;
    private final HashMap<String, Integer> mCachedAppCategoryInfo;

    public static AppClassifier getInstance() {
        return (AppClassifier) sInstance.get();
    }

    private AppClassifier() {
        this.mAppCategoryInfo = new ArrayList();
        this.mCachedAppCategoryInfo = new HashMap<>();
        loadAppCategoryConfig();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void loadAppCategoryConfig() {
        AppCategoryConfig config = loadConfigFromFile();
        if (config != null) {
            Slog.d(TAG, "Update custom app category config.");
            for (AppCategory category : config.getCategory()) {
                this.mAppCategoryInfo.addAll(category.getPkg());
            }
        }
    }

    private AppCategoryConfig loadConfigFromFile() {
        File defaultFile = Environment.buildPath(Environment.getProductDirectory(), new String[]{ETC_DIR, "displayconfig", DEFAULT_CONFIG_FILE});
        File cloudFile = Environment.buildPath(Environment.getDataSystemDirectory(), new String[]{"displayconfig", CLOUD_BACKUP_CONFIG_FILE});
        AppCategoryConfig config = readFromConfig(cloudFile);
        if (config != null) {
            return config;
        }
        return readFromConfig(defaultFile);
    }

    private AppCategoryConfig readFromConfig(File configFile) {
        if (!configFile.exists()) {
            return null;
        }
        try {
            InputStream in = new BufferedInputStream(Files.newInputStream(configFile.toPath(), new OpenOption[0]));
            try {
                AppCategoryConfig read = XmlParser.read(in);
                in.close();
                return read;
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getAppCategoryId(String packageName) {
        if (this.mAppCategoryInfo.isEmpty() || packageName == null) {
            return 0;
        }
        int category = 0;
        if (this.mCachedAppCategoryInfo.containsKey(packageName)) {
            return this.mCachedAppCategoryInfo.get(packageName).intValue();
        }
        Iterator<PackageInfo> it = this.mAppCategoryInfo.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageInfo pkgInfo = it.next();
            if (packageName.equals(pkgInfo.getName())) {
                category = pkgInfo.getCateId();
                break;
            }
        }
        this.mCachedAppCategoryInfo.put(packageName, Integer.valueOf(category));
        return category;
    }

    public static String categoryToString(int category) {
        switch (category) {
            case 1:
                return "game";
            case 2:
                return "video";
            case 3:
                return "music_read";
            case 4:
                return "finance_learn";
            case 5:
                return "news";
            case 6:
                return "shopping";
            case 7:
                return "photo";
            case 8:
                return "travel";
            case 9:
                return "social";
            default:
                return NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI;
        }
    }
}
