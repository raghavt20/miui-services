package miui.app;

import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* loaded from: classes.dex */
public class StorageRestrictedPathManager {
    public static final String SPLIT_MULTI_PATH = ";";
    private static final String TAG = "StorageRestrictedPathManager";
    private static StorageRestrictedPathManager sInstance;
    private final Set<String> mGalleryPathSet = new HashSet();
    private final Set<String> mSocialityPathSet = new HashSet();
    private static final String KEY_ISOLATED_GALLERY_PATH = "key_isolated_gallery_path";
    private static final Uri URI_ISOLATED_GALLERY_PATH = Settings.Global.getUriFor(KEY_ISOLATED_GALLERY_PATH);
    private static final String KEY_ISOLATED_SOCIALITY_PATH = "key_isolated_sociality_path";
    private static final Uri URI_ISOLATED_SOCIALITY_PATH = Settings.Global.getUriFor(KEY_ISOLATED_SOCIALITY_PATH);

    public static synchronized StorageRestrictedPathManager getInstance() {
        StorageRestrictedPathManager storageRestrictedPathManager;
        synchronized (StorageRestrictedPathManager.class) {
            if (sInstance == null) {
                sInstance = new StorageRestrictedPathManager();
            }
            storageRestrictedPathManager = sInstance;
        }
        return storageRestrictedPathManager;
    }

    private StorageRestrictedPathManager() {
    }

    public void init(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) { // from class: miui.app.StorageRestrictedPathManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (StorageRestrictedPathManager.URI_ISOLATED_GALLERY_PATH.equals(uri)) {
                    StorageRestrictedPathManager storageRestrictedPathManager = StorageRestrictedPathManager.this;
                    storageRestrictedPathManager.updatePolicy(resolver, StorageRestrictedPathManager.KEY_ISOLATED_GALLERY_PATH, storageRestrictedPathManager.mGalleryPathSet);
                } else if (StorageRestrictedPathManager.URI_ISOLATED_SOCIALITY_PATH.equals(uri)) {
                    StorageRestrictedPathManager storageRestrictedPathManager2 = StorageRestrictedPathManager.this;
                    storageRestrictedPathManager2.updatePolicy(resolver, StorageRestrictedPathManager.KEY_ISOLATED_SOCIALITY_PATH, storageRestrictedPathManager2.mSocialityPathSet);
                }
            }
        };
        resolver.registerContentObserver(URI_ISOLATED_GALLERY_PATH, false, observer);
        resolver.registerContentObserver(URI_ISOLATED_SOCIALITY_PATH, false, observer);
        updatePolicy(resolver, KEY_ISOLATED_GALLERY_PATH, this.mGalleryPathSet);
        updatePolicy(resolver, KEY_ISOLATED_SOCIALITY_PATH, this.mSocialityPathSet);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePolicy(ContentResolver resolver, String key, Set<String> targetSet) {
        targetSet.clear();
        String rawPath = Settings.Global.getString(resolver, key);
        if (!TextUtils.isEmpty(rawPath)) {
            String[] galleryPathArray = rawPath.split(SPLIT_MULTI_PATH);
            targetSet.addAll((Collection) Stream.of((Object[]) galleryPathArray).collect(Collectors.toSet()));
            Log.i(TAG, "policy has filled with " + rawPath);
        }
    }

    public Set<String> getRestrictedPathSet(int type) {
        switch (type) {
            case 10034:
                return this.mGalleryPathSet;
            case 10035:
                return this.mSocialityPathSet;
            default:
                return new HashSet();
        }
    }

    public static boolean isDenyAccessGallery(Context context, String packageName, int uid, Intent intent) {
        long identity = Binder.clearCallingIdentity();
        try {
            AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
            boolean isAllowGallery = isAllowAccessSpecialPath(aom, 10034, packageName, uid, intent);
            if (!isAllowGallery) {
                return true;
            }
            return true ^ isAllowAccessSpecialPath(aom, 10035, packageName, uid, intent);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static boolean isAllowAccessSpecialPath(AppOpsManager aom, int code, String packageName, int uid, Intent intent) {
        Set<String> restrictedArray = getInstance().getRestrictedPathSet(code);
        if (restrictedArray == null || restrictedArray.size() == 0) {
            return true;
        }
        int mode = aom.checkOpNoThrow(code, uid, packageName);
        if (mode != 0) {
            if (intent == null || intent.getClipData() == null) {
                return false;
            }
            ClipData data = intent.getClipData();
            for (int i = 0; i < data.getItemCount(); i++) {
                Uri uri = data.getItemAt(i).getUri();
                if (uri != null) {
                    for (String restricted : restrictedArray) {
                        if (uri.getLastPathSegment().toLowerCase().startsWith(restricted.toLowerCase())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
