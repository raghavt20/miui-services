package com.android.server.uri;

import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.miui.base.MiuiStubRegistry;
import com.miui.xspace.XSpaceManagerStub;

/* loaded from: classes.dex */
public class UriGrantsManagerServiceImpl implements UriGrantsManagerServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<UriGrantsManagerServiceImpl> {

        /* compiled from: UriGrantsManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final UriGrantsManagerServiceImpl INSTANCE = new UriGrantsManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public UriGrantsManagerServiceImpl m2379provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public UriGrantsManagerServiceImpl m2378provideNewInstance() {
            return new UriGrantsManagerServiceImpl();
        }
    }

    public ArrayMap<GrantUri, UriPermission> getPerms(SparseArray<ArrayMap<GrantUri, UriPermission>> mGrantedUriPermissions, int uid, GrantUri grantUri) {
        ArrayMap<GrantUri, UriPermission> perms = mGrantedUriPermissions.get(uid);
        ArrayMap<GrantUri, UriPermission> userPerms = XSpaceManagerStub.getInstance().canCrossUser(grantUri.sourceUserId, UserHandle.getUserId(uid)) ? mGrantedUriPermissions.get(UserHandle.getUid(grantUri.sourceUserId, uid)) : null;
        if (userPerms != null && perms != null) {
            perms.putAll((ArrayMap<? extends GrantUri, ? extends UriPermission>) userPerms);
            return perms;
        }
        if (userPerms != null) {
            return userPerms;
        }
        return perms;
    }
}
