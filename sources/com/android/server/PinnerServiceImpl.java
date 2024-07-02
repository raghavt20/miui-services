package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Slog;
import com.android.server.wm.ActivityStarterImpl;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.List;
import miui.util.TypefaceUtils;

/* loaded from: classes.dex */
public class PinnerServiceImpl implements PinnerServiceStub {
    private static final boolean DEBUG = false;
    private static final int MAX_NORMAL_APP_PIN_SIZE = 52428800;
    private static final String TAG = "PinnerServiceImpl";
    private static final ArrayList<String> sDefaultDynamicPinEnableFileList;
    private static final ArrayList<String> sDefaultDynamicPinEnablePKGList;
    private List<ApplicationInfo> mAllInfoList;
    private Context mContext;
    private String mLastPkgName = "com.miui.home";
    private PinnerService mPinnerService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PinnerServiceImpl> {

        /* compiled from: PinnerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PinnerServiceImpl INSTANCE = new PinnerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PinnerServiceImpl m260provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PinnerServiceImpl m259provideNewInstance() {
            return new PinnerServiceImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sDefaultDynamicPinEnablePKGList = arrayList;
        arrayList.add("com.tencent.mm");
        arrayList.add("com.ss.android.ugc.aweme");
        arrayList.add("com.smile.gifmaker");
        arrayList.add("com.kuaishou.nebula");
        arrayList.add("com.tencent.mobileqq");
        arrayList.add("tv.danmaku.bili");
        arrayList.add("com.tencent.qqlive");
        arrayList.add("com.ss.android.article.lite");
        arrayList.add("com.baidu.searchbox");
        arrayList.add("com.xunmeng.pinduoduo");
        arrayList.add("com.UCMobile");
        arrayList.add("com.dragon.read");
        arrayList.add("com.qiyi.video");
        arrayList.add("com.ss.android.article.video");
        arrayList.add("com.taobao.taobao");
        arrayList.add(ActivityStarterImpl.PACKAGE_NAME_ALIPAY);
        arrayList.add("com.tencent.mtt");
        arrayList.add("com.kmxs.reader");
        arrayList.add("com.youku.phone");
        arrayList.add("com.sina.weibo");
        arrayList.add("com.ss.android.ugc.live");
        arrayList.add("com.autonavi.minimap");
        arrayList.add("com.duowan.kiwi");
        arrayList.add("com.baidu.haokan");
        arrayList.add("com.tencent.news");
        arrayList.add("com.xingin.xhs");
        arrayList.add("air.tv.douyu.android");
        arrayList.add("com.alibaba.android.rimet");
        arrayList.add("com.tencent.qqmusic");
        arrayList.add("com.android.browser");
        arrayList.add("com.ss.android.ugc.aweme.lite");
        ArrayList<String> arrayList2 = new ArrayList<>();
        sDefaultDynamicPinEnableFileList = arrayList2;
        arrayList2.add("/system/lib64/libhwui.so");
        arrayList2.add("/vendor/lib64/hw/vulkan.adreno.so");
        arrayList2.add("/vendor/lib64/libllvm-glnext.so");
        arrayList2.add("/vendor/lib64/libgsl.so");
        arrayList2.add("/system/lib64/libEGL.so");
        arrayList2.add("/vendor/lib64/egl/libGLESv2_adreno.so");
    }

    public void onActivityChanged(String pkgName) {
        if (this.mLastPkgName.equals(pkgName)) {
            return;
        }
        if (getKeyFromPkgName(this.mLastPkgName) != 0) {
            this.mPinnerService.sendUnPinAppMessageForStub(getKeyFromPkgName(this.mLastPkgName));
        }
        if (getKeyFromPkgName(pkgName) != 0) {
            this.mPinnerService.sendPinAppMessageForStub(getKeyFromPkgName(pkgName), ActivityManager.getCurrentUser(), false);
        }
        this.mLastPkgName = pkgName;
    }

    public void init(PinnerService pinnerService, Context context) {
        this.mPinnerService = pinnerService;
        this.mContext = context;
        this.mAllInfoList = getAllInfoList();
    }

    public ApplicationInfo getInfoFromKey(int key) {
        for (ApplicationInfo info : this.mAllInfoList) {
            if (info.packageName.equals(getPkgNameFromKey(key))) {
                return info;
            }
        }
        List<ApplicationInfo> allInfoList = getAllInfoList();
        this.mAllInfoList = allInfoList;
        for (ApplicationInfo info2 : allInfoList) {
            if (info2.packageName.equals(getPkgNameFromKey(key))) {
                Slog.i(TAG, "new installed app at key " + key + "!");
                return info2;
            }
        }
        Slog.e(TAG, "error happens at key " + key + "!");
        return null;
    }

    private List<ApplicationInfo> getAllInfoList() {
        Context context = TypefaceUtils.getContext();
        PackageManager pm = context.getPackageManager();
        return pm.getInstalledApplications(8192);
    }

    public String getNameForKey(int key) {
        if (key <= 2) {
            Slog.e(TAG, "error key for key " + key + "!");
            return null;
        }
        return sDefaultDynamicPinEnablePKGList.get(key - 3);
    }

    public ArrayList<String> getFilesFromKey(int key) {
        return sDefaultDynamicPinEnableFileList;
    }

    public String getPkgNameFromKey(int key) {
        if (key <= 2) {
            Slog.e(TAG, "error key for key " + key + "!");
            return null;
        }
        return sDefaultDynamicPinEnablePKGList.get(key - 3);
    }

    public int getSizeLimitForKey(int key) {
        if (key <= 2 || key >= sDefaultDynamicPinEnablePKGList.size() + 3) {
            Slog.e(TAG, "error key for key " + key + "!");
            return 0;
        }
        return MAX_NORMAL_APP_PIN_SIZE;
    }

    public int getKeyFromPkgName(String name) {
        int i = 0;
        while (true) {
            ArrayList<String> arrayList = sDefaultDynamicPinEnablePKGList;
            if (i < arrayList.size()) {
                if (!arrayList.get(i).equals(name)) {
                    i++;
                } else {
                    return i + 3;
                }
            } else {
                return 0;
            }
        }
    }
}
