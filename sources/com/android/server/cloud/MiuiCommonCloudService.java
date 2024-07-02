package com.android.server.cloud;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.MiuiCommonCloudServiceStub;
import com.android.server.cloud.MiuiCommonCloudService;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.MiuiCommonCloudServiceStub$$")
/* loaded from: classes.dex */
public class MiuiCommonCloudService extends MiuiCommonCloudServiceStub {
    private static final String TAG = "MiuiCommonCloudService";
    private Context mContext;
    private MiuiCloudParsingCollection mMiuiCloudParsingCollection;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiCommonCloudService> {

        /* compiled from: MiuiCommonCloudService$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiCommonCloudService INSTANCE = new MiuiCommonCloudService();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiCommonCloudService m946provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiCommonCloudService m945provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.cloud.MiuiCommonCloudService is marked as singleton");
        }
    }

    public void init(Context context) {
        this.mContext = context;
        this.mMiuiCloudParsingCollection = new MiuiCloudParsingCollection(context);
        registerCloudDataObserver();
    }

    private void registerCloudDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(BackgroundThread.getHandler()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.cloud.MiuiCommonCloudService$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        AnonymousClass1(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            boolean changed = MiuiCommonCloudService.this.mMiuiCloudParsingCollection.updateDataFromCloud();
            Slog.d(MiuiCommonCloudService.TAG, "Cloud Data changed: " + changed);
            if (changed) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.cloud.MiuiCommonCloudService$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiCommonCloudService.AnonymousClass1.this.lambda$onChange$0();
                    }
                });
                MiuiCommonCloudService.this.mMiuiCloudParsingCollection.notifyAllObservers();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0() {
            MiuiCommonCloudService.this.mMiuiCloudParsingCollection.syncLocalBackupFromCloud();
        }
    }

    public void registerObserver(MiuiCommonCloudServiceStub.Observer observer) {
        this.mMiuiCloudParsingCollection.registerObserver(observer);
    }

    public void unregisterObserver(MiuiCommonCloudServiceStub.Observer observer) {
        this.mMiuiCloudParsingCollection.unregisterObserver(observer);
    }

    public Object getDataByModuleName(String moduleName) {
        return this.mMiuiCloudParsingCollection.getDataByModuleName(moduleName);
    }

    public String getStringByModuleName(String moduleName) {
        return this.mMiuiCloudParsingCollection.getStringByModuleName(moduleName);
    }
}
