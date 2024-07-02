package com.android.server.wm;

import android.content.ClipData;
import android.view.DragEvent;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefInt;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.RefObject;
import com.xiaomi.reflect.annotation.FieldArguments;
import com.xiaomi.reflect.annotation.MethodQualifiedArguments;

/* loaded from: classes.dex */
public class GetDragEventProxy {

    @FieldArguments
    public static RefInt DRAG_FLAGS_URI_ACCESS;

    @FieldArguments
    public static RefInt DRAG_FLAGS_URI_PERMISSIONS;

    @FieldArguments
    public static RefObject<ClipData> mData;

    @FieldArguments
    public static RefInt mFlags;

    @FieldArguments
    public static RefObject<WindowManagerService> mService;

    @FieldArguments
    public static RefInt mSourceUserId;

    @FieldArguments
    public static RefInt mUid;

    @MethodQualifiedArguments(classNames = {"int", "float", "float", "android.content.ClipData", "boolean", "com.android.internal.view.IDragAndDropPermissions"})
    public static RefMethod<DragEvent> obtainDragEvent;

    static {
        RefClass.attach(GetDragEventProxy.class, "com.android.server.wm.DragState");
    }
}
