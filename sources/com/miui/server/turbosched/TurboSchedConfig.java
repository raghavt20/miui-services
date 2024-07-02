package com.miui.server.turbosched;

import android.os.SystemProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class TurboSchedConfig {
    protected static final int ERROR_CODE_EXIST_TURBO_SCENE = -5;
    protected static final int ERROR_CODE_INTERNAL_EXCEPTION = -2;
    protected static final int ERROR_CODE_INVALID_PARAM = -3;
    protected static final int ERROR_CODE_NOT_ENABLE = -1;
    protected static final int ERROR_CODE_NOT_FOREGROUND = -6;
    protected static final int ERROR_CODE_NOT_IN_WHITE_LIST = -4;
    protected static final int ERROR_CODE_SUCCESS = 0;
    protected static final String PERSIST_KEY_FRAME_TURBO_APPS = "persist.sys.turbosched.frame_turbo.apps";
    protected static final String PERSIST_KEY_FRAME_TURBO_CURRENT_TURBO_SCENE = "persist.sys.turbosched.frame_turbo.current_turbo_scene";
    protected static final String PERSIST_KEY_FRAME_TURBO_SCENE_WHITE_LIST = "persist.sys.turbosched.frame_turbo.scene_white_list";
    protected static final String PERSIST_KEY_POLICY_LIST = "persist.sys.turbosched.policy_list";
    protected static final String POLICY_NAME_LITTLE_CORE = "lc";
    protected static final String POLICY_NAME_LINK = "link";
    protected static final String POLICY_NAME_BOOST_WITH_FREQUENCY = "bwf";
    protected static final String POLICY_NAME_PRIORITY = "priority";
    protected static final String POLICY_NAME_FRAME_TURBO = "frame_turbo";
    protected static List<String> POLICY_ALL = new ArrayList(Arrays.asList(POLICY_NAME_LINK, POLICY_NAME_BOOST_WITH_FREQUENCY, POLICY_NAME_PRIORITY, POLICY_NAME_FRAME_TURBO, POLICY_NAME_LINK));
    private static List<String> mPolicyList = new ArrayList(Arrays.asList(POLICY_NAME_LINK));

    /* JADX INFO: Access modifiers changed from: protected */
    public static void setPolicyList(List<String> policyList) {
        mPolicyList = policyList;
        SystemProperties.set(PERSIST_KEY_POLICY_LIST, String.join(",", policyList));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static List<String> getPolicyList() {
        return mPolicyList;
    }
}
