package com.android.server.clipboard;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardRuleInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerServiceStub;
import com.android.server.clipboard.ClipboardService;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.AccessController;
import com.miui.server.greeze.GreezeManagerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import miui.greeze.IGreezeManager;
import miui.os.Build;
import miui.security.WakePathChecker;

@MiuiStubHead(manifestName = "com.android.server.clipboard.ClipboardServiceStub$$")
/* loaded from: classes.dex */
public class ClipboardServiceStubImpl extends ClipboardServiceStub {
    private static final boolean IS_SUPPORT_SUPER_CLIPBOARD = true;
    private static final String MIUI_INPUT_NO_NEED_SHOW_POP = "miui_input_no_need_show_pop";
    private static final String TAG = "ClipboardServiceI";
    private static final List<String> sSuperClipboardPkgList;
    private static final List<String> sWebViewWhiteList;
    private AppOpsManager mAppOps;
    private Context mContext;
    private IGreezeManager mIGreezeManager = null;
    private ClipboardService mService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ClipboardServiceStubImpl> {

        /* compiled from: ClipboardServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ClipboardServiceStubImpl INSTANCE = new ClipboardServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ClipboardServiceStubImpl m937provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ClipboardServiceStubImpl m936provideNewInstance() {
            return new ClipboardServiceStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        sSuperClipboardPkgList = arrayList;
        arrayList.add("com.android.mms");
        arrayList.add("com.miui.notes");
        arrayList.add("com.miui.screenshot");
        arrayList.add("com.android.browser");
        arrayList.add("com.android.fileexplorer");
        arrayList.add(AccessController.PACKAGE_GALLERY);
        arrayList.add("com.android.email");
        ArrayList arrayList2 = new ArrayList();
        sWebViewWhiteList = arrayList2;
        arrayList2.add("com.yinxiang");
        arrayList2.add("com.evernote");
        arrayList2.add("com.youdao.note");
        arrayList2.add("com.flomo.app");
        arrayList2.add("com.yuque.mobile.android.app");
        arrayList2.add("com.denglin.moji");
        arrayList2.add("com.zhihu.android");
        arrayList2.add("cn.wps.moffice_eng");
    }

    public void init(ClipboardService clipboardService, AppOpsManager appOps) {
        this.mService = clipboardService;
        this.mContext = clipboardService.getContext();
        this.mAppOps = appOps;
        SystemProperties.set("persist.sys.support_super_clipboard", "1");
    }

    public int clipboardAccessResult(ClipData clip, String callerPkg, int callerUid, int callerUserId, int primaryClipUid, boolean userCall) {
        int resultMode;
        boolean z;
        notifyGreeze(primaryClipUid);
        CharSequence firstClipData = firstClipData(clip);
        boolean firstClipDataEmpty = firstClipData == null || firstClipData.toString().trim().length() == 0;
        if (!Build.IS_INTERNATIONAL_BUILD && !firstClipDataEmpty && !userCall && primaryClipUid != callerUid) {
            if (ClipboardChecker.getInstance().isAiClipboardEnable(this.mContext)) {
                String defaultIme = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "default_input_method", callerUserId);
                if (!TextUtils.isEmpty(defaultIme)) {
                    String imePkg = ComponentName.unflattenFromString(defaultIme).getPackageName();
                    if (imePkg.equals(callerPkg)) {
                        ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 1);
                        return 0;
                    }
                }
                int resultMode2 = this.mAppOps.checkOp(29, callerUid, callerPkg);
                boolean isSystem = UserHandle.getAppId(callerUid) < 10000;
                if (isSystem) {
                    resultMode = resultMode2;
                } else {
                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    resultMode = resultMode2;
                    ApplicationInfo appInfo = pmi.getApplicationInfo(callerPkg, 0L, 1000, UserHandle.getUserId(callerUid));
                    isSystem = (appInfo == null || (appInfo.flags & 1) == 0) ? false : true;
                }
                if (resultMode == 5 && !isSystem) {
                    ClipDescription description = clip.getDescription();
                    if (description != null) {
                        String[] imageMimeTypes = description.filterMimeTypes("image/*");
                        if (imageMimeTypes != null) {
                            ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                            return 1;
                        }
                    }
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        ClipData.Item item = clip.getItemAt(i);
                        if (item.getUri() != null) {
                            ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                            return 1;
                        }
                    }
                }
                int matchResult = ClipboardChecker.getInstance().matchClipboardRule(callerPkg, callerUid, firstClipData, clip.hashCode(), isSystem);
                if (resultMode == 0 || resultMode == 4) {
                    if (callerUid == 1000 || !isSystem) {
                        ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 1);
                        return 0;
                    }
                    if (matchResult != 3) {
                        if (!Build.IS_DEVELOPMENT_VERSION) {
                            z = true;
                        } else {
                            if (matchResult != 0) {
                                ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                                Log.i(TAG, "MIUILOG- Permission Denied when system app read clipboard, caller " + callerUid);
                                return 1;
                            }
                            z = true;
                        }
                        ClipboardChecker.getInstance().applyReadClipboardOperation(z, callerUid, callerPkg, 5);
                        return 0;
                    }
                    return 0;
                }
                if (primaryClipUid == 1000 && clip.getDescription() != null && TextUtils.equals(MIUI_INPUT_NO_NEED_SHOW_POP, clip.getDescription().getLabel())) {
                    ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                    Log.i(TAG, "MIUILOG- Permission Denied when read clipboard [CloudSet], caller " + callerUid);
                    return 1;
                }
                if (matchResult == 0) {
                    ClipboardChecker.getInstance().updateClipItemData(clip);
                    ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 5);
                    return 0;
                }
                if (matchResult == 1) {
                    ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                    Log.i(TAG, "MIUILOG- Permission Denied when read clipboard [Mismatch], caller " + callerUid);
                    return 1;
                }
                ClipboardChecker.getInstance().stashClip(callerUid, clip);
                return 3;
            }
        }
        if (Build.IS_INTERNATIONAL_BUILD || (!firstClipDataEmpty && userCall)) {
            ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, userCall ? 6 : 1);
            return 0;
        }
        return 0;
    }

    public ClipData waitUserChoice(String callerPkg, int callerUid) {
        Trace.beginSection("ai_read_clipboard");
        try {
            if (this.mAppOps.noteOp(29, callerUid, callerPkg) == 0) {
                return ClipboardChecker.getInstance().getStashClip(callerUid);
            }
            Trace.endSection();
            ClipboardChecker.getInstance().removeStashClipLater(callerUid);
            return this.EMPTY_CLIP;
        } finally {
            Trace.endSection();
            ClipboardChecker.getInstance().removeStashClipLater(callerUid);
        }
    }

    private IGreezeManager getGreeze() {
        if (this.mIGreezeManager == null) {
            this.mIGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        }
        return this.mIGreezeManager;
    }

    private void notifyGreeze(int uid) {
        int[] uids = {uid};
        try {
            if (getGreeze().isUidFrozen(uid)) {
                getGreeze().thawUids(uids, 1000, "clip");
            }
        } catch (Exception e) {
            Log.d(TAG, "ClipGreez err:");
        }
    }

    public boolean isUidFocused(int uid) {
        return ClipboardChecker.getInstance().hasStash(uid);
    }

    public ClipData getStashClipboardData() {
        checkPermissionPkg();
        return ClipboardChecker.getInstance().getClipItemData();
    }

    public void updateClipboardRuleData(List<ClipboardRuleInfo> ruleInfoList) {
        checkPermissionPkg();
        ClipboardChecker.getInstance().updateClipboardPatterns(ruleInfoList);
    }

    private void checkPermissionPkg() {
        String callingPackageName = ActivityManagerServiceStub.get().getPackageNameByPid(Binder.getCallingPid());
        if (!"com.lbe.security.miui".equals(callingPackageName)) {
            throw new SecurityException("Permission Denial: attempt to assess internal clipboard from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", pkg=" + callingPackageName);
        }
    }

    public Map getClipboardClickRuleData() {
        return ClipboardChecker.getInstance().getClipboardClickTrack();
    }

    private CharSequence firstClipData(ClipData clipData) {
        CharSequence paste = null;
        if (clipData != null) {
            long identity = Binder.clearCallingIdentity();
            for (int i = 0; i < clipData.getItemCount() && (paste = clipData.getItemAt(i).getText()) == null; i++) {
                try {
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
        return paste;
    }

    public boolean checkProviderWakePathForClipboard(String callerPkg, int callingUid, ProviderInfo providerInfo, int userId) {
        if (!TextUtils.isEmpty(callerPkg) && providerInfo != null) {
            if (!TextUtils.isEmpty(providerInfo.authority)) {
                ClipboardService.Clipboard clipboard = this.mService.getClipboardLocked(userId, this.mContext.getDeviceId());
                if (clipboard.primaryClip == null) {
                    Log.i(TAG, "checkProviderWakePathForClipboard: primaryClip is null");
                    return false;
                }
                if (!clipboard.activePermissionOwners.contains(callerPkg)) {
                    Log.i(TAG, "checkProviderWakePathForClipboard: " + callerPkg + "is not a activePermissionOwner");
                    return false;
                }
                if (!sSuperClipboardPkgList.contains(providerInfo.packageName)) {
                    Log.i(TAG, "checkProviderWakePathForClipboard: Package " + providerInfo.packageName + " is not in the white list");
                    return false;
                }
                int count = clipboard.primaryClip.getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = clipboard.primaryClip.getItemAt(i).getUri();
                    if (uri != null && TextUtils.equals(providerInfo.authority, uri.getAuthority())) {
                        Log.i(TAG, "checkProviderWakePathForClipboard: Package " + providerInfo.packageName + " can to waked by " + callerPkg);
                        WakePathChecker.getInstance().recordWakePathCall(callerPkg, providerInfo.packageName, 4, UserHandle.getUserId(callingUid), userId, true);
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public void onPackageUriPermissionRemoved(String pkg, int userId) {
        try {
            if (TextUtils.isEmpty(pkg)) {
                Log.i(TAG, "onPackageUriPermissionRemoved: package is empty");
                return;
            }
            if (userId == -10000) {
                Log.i(TAG, "onPackageUriPermissionRemoved: Attempt to remove active permission owner for invalid user: " + userId);
                return;
            }
            IActivityManager am = ActivityManager.getService();
            if (userId == -1) {
                int[] runningUserIds = am.getRunningUserIds();
                for (int id : runningUserIds) {
                    removeActivePermissionOwnerForPackage(pkg, id);
                }
                return;
            }
            if (userId != -2 && userId != -3) {
                removeActivePermissionOwnerForPackage(pkg, userId);
                return;
            }
            removeActivePermissionOwnerForPackage(pkg, am.getCurrentUserId());
        } catch (Exception e) {
            Log.e(TAG, "onPackageUriPermissionRemoved: " + e);
        }
    }

    private void removeActivePermissionOwnerForPackage(String pkg, int userId) {
        ClipboardService.Clipboard clipboard = this.mService.getClipboardLocked(userId, this.mContext.getDeviceId());
        if (TextUtils.equals(pkg, clipboard.mPrimaryClipPackage)) {
            clipboard.activePermissionOwners.clear();
        } else {
            clipboard.activePermissionOwners.remove(pkg);
        }
    }

    public ClipData modifyClipDataIfNeed(String pkg, ClipData clipData) {
        if (needModifyClipDataForWebView(pkg, clipData)) {
            return modifyClipDataForWebView(clipData);
        }
        return clipData;
    }

    private boolean needModifyClipDataForWebView(String pkg, ClipData clipData) {
        return sWebViewWhiteList.contains(pkg) && clipData.getItemCount() > 1;
    }

    private ClipData modifyClipDataForWebView(ClipData clipData) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < clipData.getItemCount(); i++) {
            CharSequence text = clipData.getItemAt(i).getText();
            builder.append(TextUtils.isEmpty(text) ? "" : text);
        }
        ClipData.Item item = new ClipData.Item(builder.toString());
        return new ClipData(clipData.getDescription(), item);
    }
}
