package com.miui.server.sptm;

import android.os.SystemProperties;
import java.util.LinkedList;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SpeedTestModeState {
    public static final int FAST_SWITCH_OPS = 3;
    public static final int QUICK_SWITCH_OPS = 2;
    public static final int SLOW_SWITCH_OPS = 1;
    public static final List<String> SPTM_ENABLE_APP_LIST;
    private static final List<String> SPTM_ENABLE_APP_LIST_NEW;
    private static final List<String> SPTM_ENABLE_APP_LIST_OLD;
    private SpeedTestModeController mSpeedTestModeController;
    private int mFastSwitchCount = 0;
    private int mQuickSwitchCount = 0;
    private int mSpecifyAppCount = 0;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();

    static {
        LinkedList linkedList = new LinkedList();
        SPTM_ENABLE_APP_LIST_OLD = linkedList;
        linkedList.add("com.tencent.mobileqq");
        linkedList.add("com.tencent.mm");
        linkedList.add("com.sina.weibo");
        linkedList.add("com.taobao.taobao");
        LinkedList linkedList2 = new LinkedList();
        SPTM_ENABLE_APP_LIST_NEW = linkedList2;
        linkedList2.add("com.ss.android.ugc.aweme");
        linkedList2.add("com.quark.browser");
        linkedList2.add("com.xingin.xhs");
        linkedList2.add("com.dragon.read");
        if (SystemProperties.getBoolean("persist.sys.miui_sptm_new.enable", false)) {
            linkedList = linkedList2;
        }
        SPTM_ENABLE_APP_LIST = linkedList;
    }

    public SpeedTestModeState(SpeedTestModeController controller) {
        this.mSpeedTestModeController = controller;
    }

    public void addAppSwitchOps(int ops) {
        if (ops == 3) {
            this.mFastSwitchCount++;
            return;
        }
        if (ops == 2) {
            this.mQuickSwitchCount++;
        } else if (ops == 1) {
            setMode(false);
            reset();
        }
    }

    public void addAppSwitchOps(String pkgName, boolean isColdStart) {
        int i;
        if (!this.mSpeedTestModeService.getSPTMCloudEnable()) {
            return;
        }
        List<String> list = SPTM_ENABLE_APP_LIST;
        int appSize = list.size();
        if (isColdStart && (i = this.mSpecifyAppCount) < appSize && pkgName.equals(list.get(i))) {
            int i2 = this.mSpecifyAppCount + 1;
            this.mSpecifyAppCount = i2;
            if (i2 == appSize) {
                setMode(true);
            }
        }
    }

    private void reset() {
        this.mFastSwitchCount = 0;
        this.mQuickSwitchCount = 0;
        this.mSpecifyAppCount = 0;
    }

    private void setMode(boolean isSPTMode) {
        this.mSpeedTestModeController.setSpeedTestMode(isSPTMode);
    }
}
