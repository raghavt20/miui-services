package com.android.server.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.provider.Settings;
import android.util.Slog;
import android.widget.RemoteViews;
import com.android.server.LocalServices;
import com.android.server.appwidget.AppWidgetServiceImpl;
import com.android.server.wm.WindowManagerInternal;
import com.google.android.collect.Sets;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.List;
import java.util.Set;
import miui.security.SecurityManagerInternal;

@MiuiStubHead(manifestName = "com.android.server.appwidget.MiuiAppWidgetServiceStub$$")
/* loaded from: classes.dex */
public class MiuiAppWidgetServiceImpl extends MiuiAppWidgetServiceStub {
    public static final String ENABLED_WIDGETS = "enabled_widgets";
    private static final String PKG_PA = "com.miui.personalassistant";
    private static String TAG = "MiuiAppWidgetServiceImpl";
    private boolean mDebuggable;
    private SecurityManagerInternal mSecurityManagerInternal;
    private WindowManagerInternal mWindowManagerInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiAppWidgetServiceImpl> {

        /* compiled from: MiuiAppWidgetServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiAppWidgetServiceImpl INSTANCE = new MiuiAppWidgetServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiAppWidgetServiceImpl m723provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiAppWidgetServiceImpl m722provideNewInstance() {
            return new MiuiAppWidgetServiceImpl();
        }
    }

    public boolean isForMiui(IPackageManager pm, AppWidgetProviderInfo info, int userId) {
        if (info == null || info.provider == null) {
            return false;
        }
        try {
            ActivityInfo activityInfo = pm.getReceiverInfo(info.provider, 128L, userId);
            if (activityInfo != null && activityInfo.metaData != null) {
                return activityInfo.metaData.getBoolean("miuiWidget", false);
            }
        } catch (Exception e) {
            Slog.e(TAG, "isForMiui", e);
        }
        return false;
    }

    public void updateWidgetPackagesLocked(Context ctx, List<AppWidgetServiceImpl.Provider> mProviders, int userId) {
        Set<String> pkgSet = Sets.newHashSet();
        int N = mProviders.size();
        for (int index = 0; index < N; index++) {
            AppWidgetServiceImpl.Provider provider = mProviders.get(index);
            if (provider.getUserId() == userId && provider.widgets.size() > 0) {
                pkgSet.add(provider.info.provider.getPackageName());
            }
        }
        StringBuilder sb = null;
        for (String pkg : pkgSet) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(':');
            }
            sb.append(pkg);
        }
        long identity = Binder.clearCallingIdentity();
        Settings.Secure.putStringForUser(ctx.getContentResolver(), ENABLED_WIDGETS, sb != null ? sb.toString() : "", userId);
        Binder.restoreCallingIdentity(identity);
    }

    public boolean isPACallingAndFocused(int callingUid, String callingPackage) {
        if (this.mWindowManagerInternal == null) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        boolean isUidFocused = this.mWindowManagerInternal.isUidFocused(callingUid);
        boolean isPA = PKG_PA.equals(callingPackage);
        Slog.i(TAG, "MIUILOG-PersonalAssistant isPACallingAndFocused: isUidFocused=" + isUidFocused + ", isPA=" + isPA);
        return isPA && isUidFocused;
    }

    public void onWidgetProviderAddedOrChanged(IPackageManager pm, AppWidgetProviderInfo info, int userId) {
        if (isForMiui(pm, info, userId) || info == null || info.provider == null) {
            return;
        }
        if (this.mSecurityManagerInternal == null) {
            this.mSecurityManagerInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        SecurityManagerInternal securityManagerInternal = this.mSecurityManagerInternal;
        if (securityManagerInternal != null) {
            securityManagerInternal.recordAppBehaviorAsync(31, info.provider.getPackageName(), 1L, info.provider.flattenToShortString());
        }
    }

    public boolean loadMaskWidgetView(ApplicationInfo appInfo, RemoteViews views, Context context) {
        Drawable drawable = appInfo.loadIcon(context.getPackageManager());
        if (drawable != null) {
            Bitmap bitmap = convertDrawableToBitmap(drawable);
            views.setImageViewBitmap(16909758, bitmap);
            return true;
        }
        return false;
    }

    private static Bitmap convertDrawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public boolean checkDebugParams(String[] args) {
        String opt;
        if (args.length <= 0 || !"logging".equals(args[0])) {
            return false;
        }
        int opti = 1;
        boolean debug = false;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0) {
            opti++;
            if ("enable-text".equals(opt)) {
                debug = true;
            } else if ("disable-text".equals(opt)) {
                debug = false;
            } else if ("DEBUG".equals(opt)) {
                this.mDebuggable = debug;
                return true;
            }
        }
        return true;
    }

    public boolean isDebuggable() {
        return this.mDebuggable;
    }
}
