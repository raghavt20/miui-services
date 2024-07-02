package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.CloudControlPreinstallService;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class OpenBrowserWithUrlImpl implements OpenBrowserWithUrlStub {
    private static final String IS_SHORTCUT = "isShortCut";
    private static final String KEY_CLOUD = "openBrowserWithUrl";
    private static final String KEY_ENABLE = "openbrowserwithurl_enable";
    private static final String MODULE_CLOUD = "kamiCloudDataConfig";
    private static final String TAG = "KamiOpenBrowserWithUrl";
    private static String mAppName;
    private static String mDocUrlActivity;
    private static String mUrlRedirect;
    private static String mVersionCode;
    private boolean isSwitch = true;
    private Context mContext;
    private HandlerThread mExecuteThread;
    private static HashMap<String, String> mDeviceList = new HashMap<>();
    private static List<String> mUrlActivityList = new ArrayList();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<OpenBrowserWithUrlImpl> {

        /* compiled from: OpenBrowserWithUrlImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final OpenBrowserWithUrlImpl INSTANCE = new OpenBrowserWithUrlImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public OpenBrowserWithUrlImpl m2737provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public OpenBrowserWithUrlImpl m2736provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.wm.OpenBrowserWithUrlImpl is marked as singleton");
        }
    }

    public void init(final Context context) {
        if (context == null) {
            return;
        }
        try {
            initializeParam();
            this.mContext = context;
            HandlerThread handlerThread = new HandlerThread("kami_open_browser_thread");
            this.mExecuteThread = handlerThread;
            handlerThread.start();
            final Handler handler = new Handler(this.mExecuteThread.getLooper());
            final ContentObserver kamiCloudOberver = new ContentObserver(handler) { // from class: com.android.server.wm.OpenBrowserWithUrlImpl.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    OpenBrowserWithUrlImpl.this.updateCloudData(context);
                }
            };
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.android.server.wm.OpenBrowserWithUrlImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, kamiCloudOberver);
                    handler.post(new Runnable() { // from class: com.android.server.wm.OpenBrowserWithUrlImpl.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            OpenBrowserWithUrlImpl.this.updateCloudData(context);
                        }
                    });
                }
            });
            Slog.i(TAG, "init success");
        } catch (Exception e) {
            Slog.e(TAG, "init error :", e);
        }
    }

    public void initializeParam() {
        mAppName = "com.ss.android.lark.kami";
        mVersionCode = "2147483647";
        mUrlRedirect = "https://www.f.mioffice.cn/suite/passport/inbound/redirect";
        mUrlActivityList.addAll(Arrays.asList("com.ss.android.lark.kami/com.bytedance.lark.webview.container.impl.WebContainerMainProcessActivity", "com.ss.android.lark.kami/com.bytedance.ee.bear.document.DocActivity", "com.ss.android.lark.kami/com.bytedance.ee.bear.wikiv2.WikiActivity"));
        mDocUrlActivity = "com.ss.android.lark.kami/com.bytedance.ee.bear.basesdk.DocRouteActivity";
        Slog.i(TAG, "initializeParam success");
    }

    public boolean getSwitch() {
        return this.isSwitch;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateCloudData$0(String v) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudData(Context context) {
        Stream.of((Object[]) new String[]{mAppName, mVersionCode, mUrlRedirect, mDocUrlActivity}).forEach(new Consumer() { // from class: com.android.server.wm.OpenBrowserWithUrlImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OpenBrowserWithUrlImpl.lambda$updateCloudData$0((String) obj);
            }
        });
        mUrlActivityList.clear();
        mDeviceList.clear();
        String data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), MODULE_CLOUD, KEY_CLOUD, "");
        String deviceName = SystemProperties.get("ro.product.name");
        if (!TextUtils.isEmpty(data)) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                String switchStatus = jsonObject.optString("switchStatus");
                if ("0".equals(switchStatus)) {
                    this.isSwitch = false;
                    Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "0");
                    Slog.i(TAG, "switchStatus is zero, not updating cloud data");
                    return;
                }
                String localAppVersionCode = String.valueOf(this.mContext.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode());
                mVersionCode = jsonObject.optString("versionCode");
                if (Integer.parseInt(localAppVersionCode) >= Integer.parseInt(mVersionCode)) {
                    this.isSwitch = false;
                    Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "0");
                    Slog.i(TAG, "app versionCode is greater than cloudData, not updating cloud data");
                    return;
                }
                JSONObject deviceArray = jsonObject.optJSONObject(CloudControlPreinstallService.ConnectEntity.DEVICE);
                Iterator<String> keys_device = deviceArray.keys();
                while (keys_device.hasNext()) {
                    String key = keys_device.next();
                    String value = deviceArray.getString(key);
                    mDeviceList.put(key, value);
                }
                if (isTablet() && "0".equals(mDeviceList.get(deviceName))) {
                    this.isSwitch = false;
                    Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "0");
                    Slog.i(TAG, "the current device is in the blacklist, not updating cloud data");
                    return;
                }
                if (isTablet() || "1".equals(mDeviceList.get(deviceName))) {
                    this.isSwitch = true;
                    Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "1");
                    mAppName = jsonObject.optString("appName");
                    mUrlRedirect = jsonObject.optString("urlRedirect");
                    mDocUrlActivity = jsonObject.optString("docUrlActivity");
                    JSONObject urlActivity = jsonObject.optJSONObject("urlActivity");
                    Iterator<String> keys_url = urlActivity.keys();
                    while (keys_url.hasNext()) {
                        String value2 = urlActivity.getString(keys_url.next());
                        mUrlActivityList.add(value2);
                    }
                    Slog.i(TAG, "updateCloudData success");
                    return;
                }
                this.isSwitch = false;
                Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "0");
                Slog.i(TAG, "the current device is not in the whitelist, not updating cloud data");
                return;
            } catch (Exception e) {
                Slog.e(TAG, "updateCloudData error :", e);
                return;
            }
        }
        try {
            initializeParam();
            if (isTablet()) {
                this.isSwitch = true;
                Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "1");
                Slog.i(TAG, "cloud data is null and device is tablet, open by default");
            } else {
                this.isSwitch = false;
                Settings.System.putString(this.mContext.getContentResolver(), KEY_ENABLE, "0");
                Slog.i(TAG, "cloud data is null and device is not tablet, off by default");
            }
        } catch (Exception ex) {
            Slog.e(TAG, "reset switch error :", ex);
        }
    }

    public Intent openBrowserWithUrl(Intent intent, String callingPackage) {
        long start;
        String localAppVersionCode;
        String url;
        try {
            start = SystemClock.uptimeMillis();
            localAppVersionCode = String.valueOf(this.mContext.getPackageManager().getPackageInfo(callingPackage, 0).getLongVersionCode());
        } catch (Exception e) {
            Slog.e(TAG, "openBrowserWithUrl error :", e);
        }
        if (this.isSwitch && this.mContext != null && mAppName.equals(callingPackage) && Integer.parseInt(localAppVersionCode) < Integer.parseInt(mVersionCode)) {
            if (intent.getComponent() != null && intent.getExtras() != null) {
                if (intent.getExtras().get("url") == null || !mUrlActivityList.contains(intent.getComponent().flattenToShortString())) {
                    if (intent.getExtras().get("doc_url") != null && mDocUrlActivity.equals(intent.getComponent().flattenToShortString())) {
                        url = String.valueOf(intent.getExtras().get("doc_url"));
                    } else {
                        url = null;
                    }
                } else {
                    url = String.valueOf(intent.getExtras().get("url"));
                }
                if (url != null && !url.contains(mUrlRedirect)) {
                    Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                    ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveActivity(browserIntent, 65536);
                    if (resolveInfo == null) {
                        return intent;
                    }
                    browserIntent.addFlags(268435456);
                    browserIntent.setPackage(resolveInfo.activityInfo.packageName);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(IS_SHORTCUT, true);
                    browserIntent.putExtras(bundle);
                    checkSlow(start, 50L, "check openBrowserWithUrl start time");
                    Slog.i(TAG, "openBrowserWithUrl success");
                    return browserIntent;
                }
                return intent;
            }
            return intent;
        }
        return intent;
    }

    public boolean isTablet() {
        return SystemProperties.get("ro.build.characteristics") != null && SystemProperties.get("ro.build.characteristics").contains("tablet");
    }

    private void checkSlow(long startTime, long threshold, String where) {
        long took = SystemClock.uptimeMillis() - startTime;
        if (took > threshold) {
            Slog.w(TAG, "Slow operation: " + where + " took " + took + "ms.");
        }
    }
}
