package com.android.server.am;

import android.content.pm.ApplicationInfo;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class ContentProviderRecordImpl implements ContentProviderRecordStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ContentProviderRecordImpl> {

        /* compiled from: ContentProviderRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ContentProviderRecordImpl INSTANCE = new ContentProviderRecordImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ContentProviderRecordImpl m467provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ContentProviderRecordImpl m466provideNewInstance() {
            return new ContentProviderRecordImpl();
        }
    }

    public boolean isReleaseNeeded(ApplicationInfo ai) {
        return ai.uid == 1000 && "com.android.settings".equals(ai.packageName);
    }
}
