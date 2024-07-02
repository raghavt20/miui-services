package com.android.server.wm;

import android.content.ClipData;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiCommonCloudServiceStub;
import com.miui.app.MiuiFreeDragServiceInternal;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiDragAndDropStubImpl implements MiuiDragAndDropStub {
    private static final String TAG = "MiuiDragAndDropStubImpl";
    private MiuiFreeDragServiceInternal mMFDSInternal = (MiuiFreeDragServiceInternal) LocalServices.getService(MiuiFreeDragServiceInternal.class);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDragAndDropStubImpl> {

        /* compiled from: MiuiDragAndDropStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDragAndDropStubImpl INSTANCE = new MiuiDragAndDropStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiDragAndDropStubImpl m2519provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiDragAndDropStubImpl m2518provideNewInstance() {
            return new MiuiDragAndDropStubImpl();
        }
    }

    MiuiDragAndDropStubImpl() {
    }

    public void checkMiuiDragAndDropMetaData(ActivityRecord ar) {
        MiuiFreeDragServiceInternal miuiFreeDragServiceInternal;
        ActivityInfo infoWithMetaData = ar.intent.resolveActivityInfo(ar.mAtmService.mContext.getPackageManager(), 128);
        Bundle metaData = infoWithMetaData == null ? null : infoWithMetaData.metaData;
        boolean z = false;
        if (metaData != null && metaData.getBoolean("miui.hasDragAndDropFeature", false)) {
            z = true;
        }
        ar.mHasDragAndDropFeature = z;
        if (ar.mHasDragAndDropFeature && ar.intent != null && ar.intent.getComponent() != null && (miuiFreeDragServiceInternal = this.mMFDSInternal) != null) {
            miuiFreeDragServiceInternal.notifyHasMiuiDragAndDropMetaData(ar.packageName, ar.intent.getComponent().getClassName());
        }
    }

    public void notifyDragDropResultToOneTrack(WindowState dragWindow, WindowState dropWindow, boolean result, ClipData clipData) {
        OneTrackDragDropHelper.getInstance().notifyDragDropResult(dragWindow, dropWindow, result, clipData);
    }

    public boolean getDragNotAllowPackages(WindowState callingWin) {
        DragNotAllowPackages packages = (DragNotAllowPackages) MiuiCommonCloudServiceStub.getInstance().getDataByModuleName("drag_not_allow_packages");
        ArraySet<String> dragNotAllowPackages = packages.getPackages();
        if (dragNotAllowPackages != null && dragNotAllowPackages.contains(callingWin.getOwningPackage())) {
            Slog.w(TAG, "Not allow dragging, package: " + callingWin.getOwningPackage());
            return true;
        }
        return false;
    }

    public boolean isMiuiFreeForm(WindowState callingWin, WindowManagerService mWmService) {
        ActivityRecord ar = callingWin.getActivityRecord();
        if (ar == null) {
            return false;
        }
        int arId = ar.getRootTaskId();
        boolean result = mWmService.mAtmService.mMiuiFreeFormManagerService.isInMiniFreeFormMode(arId);
        return result;
    }

    /* loaded from: classes.dex */
    public static class DragNotAllowPackages {
        private final ArraySet<String> packages = new ArraySet<>();
        private long version;

        public long getVersion() {
            return this.version;
        }

        public ArraySet<String> getPackages() {
            return this.packages;
        }
    }
}
