package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.util.Slog;

/* loaded from: classes.dex */
public class MemoryControlCloud {
    private static final String CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE = "cloud_memory_standard_appheap_enable";
    private static final String CLOUD_MEMORY_STANDARD_ENABLE = "cloud_memory_standard_enable";
    private static final String CLOUD_MEMORY_STANDARD_PROC_LIST = "cloud_memory_standard_proc_list";
    private static final String CLOUD_MEMORY_STANDARD_PROC_LIST_JSON = "cloud_memory_standard_proc_list_json";
    private static final String CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON = "cloud_memory_standard_appheap_list_json";
    private static final String CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME = "cloud_memory_standard_appheap_handle_time";
    private static final String CLOUD_SCREEN_OFF_STRATEGY = "cloud_memory_standard_screen_off_strategy";
    private static final String TAG = "MemoryStandardProcessControl";
    private MemoryStandardProcessControl mspc = null;

    public boolean initCloud(Context context, MemoryStandardProcessControl mspc) {
        this.mspc = mspc;
        getCloudSwitch(context);
        getCloudProcList(context);
        getCloudAppHeapList(context);
        getCloudAppHeapHandleTime(context);
        return true;
    }

    private void registerCloudEnableObserver(final Context context) {
        ContentObserver enableObserver = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                String str;
                if (uri == null || !uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_MEMORY_STANDARD_ENABLE)) || (str = Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MEMORY_STANDARD_ENABLE, -2)) == null || str.equals("")) {
                    return;
                }
                try {
                    MemoryControlCloud.this.mspc.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MEMORY_STANDARD_ENABLE, -2));
                } catch (Exception e) {
                    Slog.e(MemoryControlCloud.TAG, "cloud CLOUD_MEMORY_STANDARD_ENABLE check failed: " + e);
                    MemoryControlCloud.this.mspc.mEnable = false;
                }
                Slog.w(MemoryControlCloud.TAG, "cloud control set received: " + MemoryControlCloud.this.mspc.mEnable);
            }
        };
        ContentObserver strategyObserver = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_SCREEN_OFF_STRATEGY))) {
                    try {
                        MemoryControlCloud.this.mspc.mScreenOffStrategy = Integer.parseInt(Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_SCREEN_OFF_STRATEGY, -2));
                    } catch (Exception e) {
                        Slog.e(MemoryControlCloud.TAG, "cloud mScreenOffStrategy check failed: " + e);
                        MemoryControlCloud.this.mspc.mScreenOffStrategy = 0;
                        MemoryControlCloud.this.mspc.mEnable = false;
                    }
                    Slog.w(MemoryControlCloud.TAG, "cloud screen off strategy set received: " + MemoryControlCloud.this.mspc.mScreenOffStrategy);
                }
            }
        };
        ContentObserver appheapObserver = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE))) {
                    try {
                        MemoryControlCloud.this.mspc.mAppHeapEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE, -2));
                    } catch (Exception e) {
                        Slog.e(MemoryControlCloud.TAG, "cloud mAppHeapEnable check failed: " + e);
                        MemoryControlCloud.this.mspc.mAppHeapEnable = false;
                    }
                    Slog.w(MemoryControlCloud.TAG, "cloud app heap control received: " + MemoryControlCloud.this.mspc.mAppHeapEnable);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMORY_STANDARD_ENABLE), false, enableObserver, -2);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_SCREEN_OFF_STRATEGY), false, strategyObserver, -2);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE), false, appheapObserver, -2);
    }

    private void getCloudSwitch(Context context) {
        registerCloudEnableObserver(context);
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_ENABLE, -2) != null) {
            try {
                this.mspc.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_ENABLE, -2));
            } catch (Exception e) {
                Slog.e(TAG, "cloud CLOUD_MEMORY_STANDARD_ENABLE check failed: " + e);
                this.mspc.mEnable = false;
            }
            Slog.w(TAG, "set enable state from database: " + this.mspc.mEnable);
        }
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_SCREEN_OFF_STRATEGY, -2) != null) {
            try {
                this.mspc.mScreenOffStrategy = Integer.parseInt(Settings.System.getStringForUser(context.getContentResolver(), CLOUD_SCREEN_OFF_STRATEGY, -2));
            } catch (Exception e2) {
                Slog.e(TAG, "cloud mScreenOffStrategy check failed: " + e2);
                this.mspc.mScreenOffStrategy = 0;
                this.mspc.mEnable = false;
            }
            Slog.w(TAG, "set screen off strategy from database: " + this.mspc.mScreenOffStrategy);
        }
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE, -2) != null) {
            try {
                this.mspc.mAppHeapEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE, -2));
            } catch (Exception e3) {
                Slog.e(TAG, "cloud CLOUD_MEMORY_STANDARD_APPHEAP_ENABLE check failed: " + e3);
                this.mspc.mAppHeapEnable = false;
            }
            Slog.w(TAG, "set appheap enable state from database: " + this.mspc.mAppHeapEnable);
        }
    }

    private void registerCloudAppHeapHandleTimeObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME))) {
                    try {
                        String str = Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME, -2);
                        long handleTime = Long.parseLong(str);
                        MemoryControlCloud.this.mspc.mAppHeapHandleTime = handleTime;
                    } catch (Exception e) {
                        Slog.e(MemoryControlCloud.TAG, "cloud CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME check failed: " + e);
                    }
                    Slog.w(MemoryControlCloud.TAG, "set app heap handle time from database: " + MemoryControlCloud.this.mspc.mAppHeapHandleTime);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME), false, observer, -2);
    }

    private void getCloudAppHeapHandleTime(Context context) {
        registerCloudAppHeapHandleTimeObserver(context);
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME, -2) != null) {
            try {
                String str = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME, -2);
                long handleTime = Long.parseLong(str);
                this.mspc.mAppHeapHandleTime = handleTime;
            } catch (Exception e) {
                Slog.e(TAG, "cloud CLOUD_MRMORY_STANDARD_APPHEAP_HANDLE_TIME check failed: " + e);
            }
            Slog.w(TAG, "cloud app heap handle time received: " + this.mspc.mAppHeapHandleTime);
        }
    }

    private void registerCloudProcListObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                String str;
                if (uri == null || !uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_MEMORY_STANDARD_PROC_LIST_JSON)) || (str = Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MEMORY_STANDARD_PROC_LIST_JSON, -2)) == null || "".equals(str)) {
                    return;
                }
                MemoryControlCloud.this.mspc.callUpdateCloudConfig(str);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMORY_STANDARD_PROC_LIST_JSON), false, observer, -2);
    }

    private void getCloudProcList(Context context) {
        String str;
        registerCloudProcListObserver(context);
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_PROC_LIST_JSON, -2) == null || (str = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDARD_PROC_LIST_JSON, -2)) == null || "".equals(str)) {
            return;
        }
        this.mspc.callUpdateCloudConfig(str);
    }

    private void registerCloudAppHeapListObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mspc.mHandler) { // from class: com.android.server.am.MemoryControlCloud.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                String str;
                if (uri == null || !uri.equals(Settings.System.getUriFor(MemoryControlCloud.CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON)) || (str = Settings.System.getStringForUser(context.getContentResolver(), MemoryControlCloud.CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON, -2)) == null || "".equals(str)) {
                    return;
                }
                MemoryControlCloud.this.mspc.callUpdateCloudAppHeapConfig(str);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON), false, observer, -2);
    }

    private void getCloudAppHeapList(Context context) {
        String str;
        registerCloudAppHeapListObserver(context);
        if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON, -2) == null || (str = Settings.System.getStringForUser(context.getContentResolver(), CLOUD_MEMORY_STANDRAD_APPHEAP_LIST_JSON, -2)) == null || "".equals(str)) {
            return;
        }
        this.mspc.callUpdateCloudAppHeapConfig(str);
    }
}
