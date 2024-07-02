package com.android.server.app;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import org.json.JSONArray;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.app.PreCacheUtilStubImpl$$")
/* loaded from: classes.dex */
public class PreCacheUtilStubImpl extends PreCacheUtilStub {
    private static final String CLOUD_ALL_DATA_CHANGE_URI = "content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify";
    private static final String CLOUD_PRE_CACHE_APPS_LIST = "app_list";
    private static final String CLOUD_PRE_CACHE_MODULE = "precache";
    private static final int MAX_APP_LIST_PROP_NUM = 10;
    private static final int MAX_PROP_LENGTH = 90;
    private static final String PRE_CACHE_APPS_STR_NUM_PROP = "persist.sys.precache.number";
    private static final String PRE_CACHE_APPS_STR_PROP = "persist.sys.precache.appstrs";
    private static final String TAG = "PreCacheUtilStubImpl";
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PreCacheUtilStubImpl> {

        /* compiled from: PreCacheUtilStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PreCacheUtilStubImpl INSTANCE = new PreCacheUtilStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PreCacheUtilStubImpl m711provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PreCacheUtilStubImpl m710provideNewInstance() {
            return new PreCacheUtilStubImpl();
        }
    }

    protected PreCacheUtilStubImpl() {
    }

    public void registerPreCacheObserver(Context context, Handler handler) {
        this.mContext = context;
        context.getContentResolver().registerContentObserver(Uri.parse(CLOUD_ALL_DATA_CHANGE_URI), false, new PreCacheContentObserver(handler), -2);
        Slog.d(TAG, "registerContentObserver");
    }

    /* loaded from: classes.dex */
    private class PreCacheContentObserver extends ContentObserver {
        public PreCacheContentObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && uri.equals(Uri.parse(PreCacheUtilStubImpl.CLOUD_ALL_DATA_CHANGE_URI))) {
                PreCacheUtilStubImpl.this.updatePreCacheSetting();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePreCacheSetting() {
        MiuiSettings.SettingsCloudData.CloudData sData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), CLOUD_PRE_CACHE_MODULE, (String) null, (String) null, false);
        if (sData == null) {
            Slog.d(TAG, "no data in precache");
            return;
        }
        try {
            JSONObject jsonData = sData.json();
            if (!jsonData.has(CLOUD_PRE_CACHE_APPS_LIST)) {
                Slog.d(TAG, "no app_list");
                return;
            }
            JSONArray appArray = jsonData.getJSONArray(CLOUD_PRE_CACHE_APPS_LIST);
            if (appArray.length() == 0) {
                Slog.d(TAG, "get member in app_list");
                return;
            }
            Slog.d(TAG, "app number: " + appArray.length());
            int propNum = 0;
            String propValue = "";
            for (int i = 0; i < appArray.length(); i++) {
                String curApp = appArray.getString(i);
                if (propValue.length() + curApp.length() < MAX_PROP_LENGTH) {
                    if (propValue.length() == 0) {
                        propValue = curApp;
                    } else {
                        propValue = propValue + "," + curApp;
                    }
                } else {
                    if (propNum > 10) {
                        Slog.d(TAG, "prop number not enough, cur: " + propNum + ", max:10");
                        return;
                    }
                    propNum++;
                    String propKey = PRE_CACHE_APPS_STR_PROP + String.valueOf(propNum);
                    SystemProperties.set(propKey, propValue);
                    Slog.d(TAG, propKey + "=" + propValue);
                    propValue = curApp;
                }
            }
            int i2 = propValue.length();
            if (i2 > 0) {
                propNum++;
                String propKey2 = PRE_CACHE_APPS_STR_PROP + String.valueOf(propNum);
                SystemProperties.set(propKey2, propValue);
                Slog.d(TAG, propKey2 + "=" + propValue);
            }
            SystemProperties.set(PRE_CACHE_APPS_STR_NUM_PROP, String.valueOf(propNum));
            Slog.d(TAG, "persist.sys.precache.number=" + String.valueOf(propNum));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
