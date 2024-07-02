package com.android.server.wallpaper;

import android.content.ComponentName;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class WallpaperManagerServiceProxy {

    @MethodArguments(cls = {ComponentName.class, WallpaperData.class})
    public static RefMethod<Boolean> changingToSame;

    @MethodArguments(cls = {WallpaperData.class})
    public static RefMethod<Void> notifyWallpaperChanged;

    static {
        RefClass.attach(WallpaperManagerServiceProxy.class, WallpaperManagerService.class);
    }
}
