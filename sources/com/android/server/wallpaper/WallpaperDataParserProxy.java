package com.android.server.wallpaper;

import com.android.internal.util.JournaledFile;
import com.xiaomi.reflect.RefClass;
import com.xiaomi.reflect.RefMethod;
import com.xiaomi.reflect.annotation.MethodArguments;

/* loaded from: classes.dex */
public class WallpaperDataParserProxy {

    @MethodArguments(cls = {int.class})
    public static RefMethod<JournaledFile> makeJournaledFile;

    static {
        RefClass.attach(WallpaperDataParserProxy.class, WallpaperDataParser.class);
    }
}
