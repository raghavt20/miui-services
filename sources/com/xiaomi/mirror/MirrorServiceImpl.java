package com.xiaomi.mirror;

import android.net.Uri;
import android.view.PointerIcon;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.xiaomi.mirror.service.MirrorService;

@MiuiStubHead(manifestName = "com.xiaomi.mirror.MirrorServiceStub$$")
/* loaded from: classes.dex */
public class MirrorServiceImpl extends MirrorServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MirrorServiceImpl> {

        /* compiled from: MirrorServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MirrorServiceImpl INSTANCE = new MirrorServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MirrorServiceImpl m3828provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MirrorServiceImpl m3827provideNewInstance() {
            return new MirrorServiceImpl();
        }
    }

    public boolean isEnabled() {
        MirrorService mirrorService = MirrorService.get();
        return (mirrorService == null || mirrorService.getDelegatePid() == 0) ? false : true;
    }

    public boolean isGrantAllowed(Uri uri, String targetPkg) {
        return "com.xiaomi.mirror.remoteprovider".equals(uri.getAuthority()) || targetPkg.equals(MirrorService.get().getDelegatePackageName());
    }

    public void setAllowGrant(boolean allowGrant) {
        MirrorService.get().setAllowGrant(allowGrant);
    }

    public boolean dragDropActiveLocked() {
        if (MirrorService.get().getSystemReady()) {
            return MirrorService.get().getDragDropController().dragDropActiveLocked();
        }
        return false;
    }

    public void sendDragStartedIfNeededLocked(Object window) {
        if (MirrorService.get().getSystemReady()) {
            MirrorService.get().getDragDropController().sendDragStartedIfNeededLocked(window);
        }
    }

    public boolean getAllowGrant() {
        return MirrorService.get().getAllowGrant();
    }

    public void notifyPointerIconChanged(int iconId, PointerIcon customIcon) {
        MirrorService.get().notifyPointerIconChanged(iconId, customIcon);
    }

    public boolean dragDropActiveLockedWithMirrorEnabled() {
        if (isEnabled()) {
            return dragDropActiveLocked();
        }
        return false;
    }
}
