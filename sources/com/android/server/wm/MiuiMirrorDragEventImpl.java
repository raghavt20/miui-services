package com.android.server.wm;

import android.content.ClipData;
import android.os.UserHandle;
import android.view.DragEvent;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiMirrorDragEventImpl implements MiuiMirrorDragEventStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMirrorDragEventImpl> {

        /* compiled from: MiuiMirrorDragEventImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMirrorDragEventImpl INSTANCE = new MiuiMirrorDragEventImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiMirrorDragEventImpl m2560provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiMirrorDragEventImpl m2559provideNewInstance() {
            return new MiuiMirrorDragEventImpl();
        }
    }

    public DragEvent getDragEvent(WindowState newWin, float touchX, float touchY, ClipData data, DragState dragState, DragEvent event) {
        DragAndDropPermissionsHandler dragAndDropPermissions;
        if (!"com.xiaomi.mirror".equals(newWin.getOwningPackage())) {
            return event;
        }
        int targetUserId = UserHandle.getUserId(newWin.getOwningUid());
        int flags = GetDragEventProxy.mFlags.get(dragState);
        if ((flags & 256) != 0 && (GetDragEventProxy.DRAG_FLAGS_URI_ACCESS.get((Object) null) & flags) != 0 && GetDragEventProxy.mData.get(dragState) != null) {
            dragAndDropPermissions = new DragAndDropPermissionsHandler(((WindowManagerService) GetDragEventProxy.mService.get(dragState)).mGlobalLock, (ClipData) GetDragEventProxy.mData.get(dragState), GetDragEventProxy.mUid.get(dragState), newWin.getOwningPackage(), flags & GetDragEventProxy.DRAG_FLAGS_URI_PERMISSIONS.get((Object) null), GetDragEventProxy.mSourceUserId.get(dragState), targetUserId);
        } else {
            dragAndDropPermissions = null;
        }
        DragEvent event2 = (DragEvent) GetDragEventProxy.obtainDragEvent.invoke(dragState, new Object[]{1, Float.valueOf(touchX), Float.valueOf(touchY), GetDragEventProxy.mData.get(dragState), false, dragAndDropPermissions});
        return event2;
    }
}
