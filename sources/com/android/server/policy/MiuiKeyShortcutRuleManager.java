package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.content.PackageMonitor;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.shortcut.MiuiGestureRuleManager;
import com.android.server.input.shortcut.MiuiShortcutOperatorCustomManager;
import com.android.server.input.shortcut.ShortcutOneTrackHelper;
import com.android.server.input.shortcut.combinationkeyrule.MiuiCombinationRuleManager;
import com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyInfo;
import com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyRuleManager;
import com.android.server.pm.UserManagerInternal;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import miui.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiKeyShortcutRuleManager {
    private static final String ATTRIBUTE_ACTION = "action";
    private static final String ATTRIBUTE_COMBINATION_KEY = "combinationKey";
    private static final String ATTRIBUTE_FUNCTION = "function";
    private static final String ATTRIBUTE_FUNCTIONS = "functions";
    private static final String ATTRIBUTE_GLOBAL = "global";
    private static final String ATTRIBUTE_PRIMARY_KEY = "primaryKey";
    private static final String ATTRIBUTE_REGION = "region";
    public static final int ATTRIBUTE_REGION_TYPE_ALL = 2;
    public static final int ATTRIBUTE_REGION_TYPE_CN = 0;
    public static final int ATTRIBUTE_REGION_TYPE_GLOBAL = 1;
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String MIUI_SETTINGS_PACKAGE = "com.android.settings";
    private static final String SHORTCUT_CONFIG_NAME = "miui_shortcut_config.json";
    private static final String SHORTCUT_CONFIG_PATH_DEFAULT = "system_ext/etc/input/";
    private static final String SHORTCUT_CONFIG_PATH_EXTRA = "product/etc/input/";
    private static final String SHORTCUT_TYPE_COMBINATION_KEY_RULE = "combinationKeyRules";
    private static final String SHORTCUT_TYPE_GESTURE_RULE = "gestureRules";
    private static final String SHORTCUT_TYPE_SINGLE_KEY_RULE = "singleKeyRules";
    private static final String SINGLE_KEY_RULE_ATTRIBUTE_LONG_PRESS_TIME_OUT = "longPressTimeOut";
    private static final String SINGLE_KEY_RULE_ATTRIBUTE_MAX_COUNT = "maxCount";
    private static final String SINGLE_KEY_RULE_TYPE_MULTI_PRESS = "multiPress";
    private static final String SINGLE_KEY_RULE_TYPE_SINGLE_PRESS = "singlePress";
    private static final String TAG = "MiuiKeyShortcutRuleManager";
    private static MiuiKeyShortcutRuleManager sMiuiKeyShortcutRuleManager;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId;
    private final Handler mHandler;
    private final Set<Integer> mInitForUsers;
    private final KeyCombinationManager mKeyCombinationManager;
    private final MiuiCombinationRuleManager mMiuiCombinationRuleManager;
    private final MiuiGestureRuleManager mMiuiGestureManager;
    private final MiuiShortcutOperatorCustomManager mMiuiShortcutOperatorCustomManager;
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private final MiuiSingleKeyRuleManager mMiuiSingleKeyRuleManager;
    private final SingleKeyGestureDetector mSingleKeyGestureDetector;

    private MiuiKeyShortcutRuleManager(Context context, SingleKeyGestureDetector singleKeyGestureDetector, KeyCombinationManager combinationManager, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        Handler handler = MiuiInputThread.getHandler();
        this.mHandler = handler;
        int[] userIds = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
        this.mInitForUsers = new HashSet(Arrays.asList((Integer[]) Arrays.stream(userIds).boxed().toArray(new IntFunction() { // from class: com.android.server.policy.MiuiKeyShortcutRuleManager$$ExternalSyntheticLambda0
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return MiuiKeyShortcutRuleManager.lambda$new$0(i);
            }
        })));
        this.mCurrentUserId = UserHandle.myUserId();
        this.mSingleKeyGestureDetector = singleKeyGestureDetector;
        this.mKeyCombinationManager = combinationManager;
        LocalServices.addService(MiuiKeyShortcutRuleManager.class, this);
        this.mMiuiShortcutTriggerHelper = miuiShortcutTriggerHelper;
        this.mMiuiSingleKeyRuleManager = MiuiSingleKeyRuleManager.getInstance(context, handler, miuiShortcutTriggerHelper, singleKeyGestureDetector);
        this.mMiuiCombinationRuleManager = MiuiCombinationRuleManager.getInstance(context, handler, combinationManager);
        this.mMiuiGestureManager = MiuiGestureRuleManager.getInstance(context, handler);
        MiuiShortcutOperatorCustomManager miuiShortcutOperatorCustomManager = MiuiShortcutOperatorCustomManager.getInstance(context);
        this.mMiuiShortcutOperatorCustomManager = miuiShortcutOperatorCustomManager;
        initMiuiKeyShortcut();
        miuiShortcutOperatorCustomManager.initShortcut();
        ShortcutOneTrackHelper.getInstance(context).setUploadShortcutAlarm(true);
        registerPackageChangeReceivers();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer[] lambda$new$0(int x$0) {
        return new Integer[x$0];
    }

    private void registerPackageChangeReceivers() {
        PackageMonitor packageMonitor = new PackageMonitor() { // from class: com.android.server.policy.MiuiKeyShortcutRuleManager.1
            public void onPackageDataCleared(String packageName, int uid) {
                if (MiuiKeyShortcutRuleManager.MIUI_SETTINGS_PACKAGE.equals(packageName)) {
                    Slog.i(MiuiKeyShortcutRuleManager.TAG, "onPackageDataCleared uid=" + uid);
                    MiuiKeyShortcutRuleManager.this.resetShortcutSettings();
                }
                super.onPackageDataCleared(packageName, uid);
            }
        };
        packageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
    }

    public static synchronized MiuiKeyShortcutRuleManager getInstance(Context context, SingleKeyGestureDetector singleKeyGestureDetector, KeyCombinationManager keyCombinationManager, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper) {
        MiuiKeyShortcutRuleManager miuiKeyShortcutRuleManager;
        synchronized (MiuiKeyShortcutRuleManager.class) {
            if (sMiuiKeyShortcutRuleManager == null) {
                sMiuiKeyShortcutRuleManager = new MiuiKeyShortcutRuleManager(context, singleKeyGestureDetector, keyCombinationManager, miuiShortcutTriggerHelper);
            }
            miuiKeyShortcutRuleManager = sMiuiKeyShortcutRuleManager;
        }
        return miuiKeyShortcutRuleManager;
    }

    private void initMiuiKeyShortcut() {
        String shortcutConfigString = getShortcutConfig(SHORTCUT_CONFIG_PATH_DEFAULT, SHORTCUT_CONFIG_NAME);
        String shortcutConfigStringExtra = getShortcutConfig(SHORTCUT_CONFIG_PATH_EXTRA, SHORTCUT_CONFIG_NAME);
        String defaultShortcutConfigType = "defaultShortcutMapConfig";
        String extraShortcutConfigType = "extraShortcutMapConfig";
        Map<String, String> shortcutConfigMap = new HashMap<String, String>(defaultShortcutConfigType, shortcutConfigString, extraShortcutConfigType, shortcutConfigStringExtra) { // from class: com.android.server.policy.MiuiKeyShortcutRuleManager.2
            final /* synthetic */ String val$defaultShortcutConfigType;
            final /* synthetic */ String val$extraShortcutConfigType;
            final /* synthetic */ String val$shortcutConfigString;
            final /* synthetic */ String val$shortcutConfigStringExtra;

            {
                this.val$defaultShortcutConfigType = defaultShortcutConfigType;
                this.val$shortcutConfigString = shortcutConfigString;
                this.val$extraShortcutConfigType = extraShortcutConfigType;
                this.val$shortcutConfigStringExtra = shortcutConfigStringExtra;
                put(defaultShortcutConfigType, shortcutConfigString);
                put(extraShortcutConfigType, shortcutConfigStringExtra);
            }
        };
        List<String> shortcutTypeList = new ArrayList<>(Arrays.asList(SHORTCUT_TYPE_SINGLE_KEY_RULE, SHORTCUT_TYPE_COMBINATION_KEY_RULE, SHORTCUT_TYPE_GESTURE_RULE));
        Map<String, Map<String, List<JSONObject>>> shortcutJsonObjectMap = getShortcutJsonObjectMap(shortcutConfigMap, shortcutTypeList);
        Map<String, List<JSONObject>> defaultShortcutConfigMap = shortcutJsonObjectMap.get("defaultShortcutMapConfig");
        if (defaultShortcutConfigMap != null && defaultShortcutConfigMap.size() != 0) {
            initSingleKeyRule(defaultShortcutConfigMap.get(SHORTCUT_TYPE_SINGLE_KEY_RULE));
            initCombinationKeyRule(defaultShortcutConfigMap.get(SHORTCUT_TYPE_COMBINATION_KEY_RULE));
            initGestureRule(defaultShortcutConfigMap.get(SHORTCUT_TYPE_GESTURE_RULE));
        }
        Map<String, List<JSONObject>> extraShortcutConfigMap = shortcutJsonObjectMap.get("extraShortcutMapConfig");
        if (extraShortcutConfigMap != null && extraShortcutConfigMap.size() != 0) {
            initSingleKeyRule(extraShortcutConfigMap.get(SHORTCUT_TYPE_SINGLE_KEY_RULE));
            initCombinationKeyRule(extraShortcutConfigMap.get(SHORTCUT_TYPE_COMBINATION_KEY_RULE));
            initGestureRule(extraShortcutConfigMap.get(SHORTCUT_TYPE_GESTURE_RULE));
        }
        this.mMiuiSingleKeyRuleManager.initSingleKeyRule();
        this.mMiuiCombinationRuleManager.initCombinationKeyRule();
        this.mMiuiGestureManager.initGestureRule();
    }

    private Map<String, Map<String, List<JSONObject>>> getShortcutJsonObjectMap(Map<String, String> shortcutConfigMap, List<String> shortcutTypeList) {
        if (shortcutConfigMap == null || shortcutConfigMap.size() == 0 || shortcutTypeList == null || shortcutTypeList.size() == 0) {
            MiuiInputLog.major("shortcut config is null");
            return null;
        }
        Map<String, Map<String, List<JSONObject>>> shortcutJsonObjectMap = new HashMap<>();
        for (Map.Entry<String, String> shortcutConfigMapEntry : shortcutConfigMap.entrySet()) {
            String configType = shortcutConfigMapEntry.getKey();
            String config = shortcutConfigMapEntry.getValue();
            Map<String, List<JSONObject>> shortcutJsonObjectMapForType = new HashMap<>();
            if (!TextUtils.isEmpty(configType) && !TextUtils.isEmpty(config) && configType.length() != 0 && config.length() != 0) {
                try {
                    JSONObject jsonObject = new JSONObject(config);
                    for (String shortcutType : shortcutTypeList) {
                        if (jsonObject.has(shortcutType)) {
                            JSONArray jsonArray = jsonObject.getJSONArray(shortcutType);
                            List<JSONObject> jsonObjectList = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                jsonObjectList.add(jsonArray.getJSONObject(i));
                            }
                            shortcutJsonObjectMapForType.put(shortcutType, jsonObjectList);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            shortcutJsonObjectMap.put(configType, shortcutJsonObjectMapForType);
        }
        return shortcutJsonObjectMap;
    }

    private void initGestureRule(List<JSONObject> gestureJsonObjectList) {
        if (gestureJsonObjectList == null || gestureJsonObjectList.size() == 0) {
            return;
        }
        for (JSONObject gestureJSONObject : gestureJsonObjectList) {
            String action = getStringAttributeValueByJsonObject(gestureJSONObject, ATTRIBUTE_ACTION);
            String function = getStringAttributeByFunctionJSONObject(gestureJSONObject, ATTRIBUTE_FUNCTION);
            MiuiGestureRule miuiGestureRule = new MiuiGestureRule(this.mContext, this.mHandler, action, function, this.mCurrentUserId);
            this.mMiuiGestureManager.addRule(action, miuiGestureRule);
        }
    }

    private void initCombinationKeyRule(List<JSONObject> combinationJsonObjectList) {
        if (combinationJsonObjectList == null || combinationJsonObjectList.size() == 0) {
            return;
        }
        for (JSONObject combinationJSONObject : combinationJsonObjectList) {
            int primaryKey = getIntAttributeValueByJsonObject(combinationJSONObject, ATTRIBUTE_PRIMARY_KEY);
            int combinationKey = getIntAttributeValueByJsonObject(combinationJSONObject, ATTRIBUTE_COMBINATION_KEY);
            String action = getStringAttributeValueByJsonObject(combinationJSONObject, ATTRIBUTE_ACTION);
            String function = getStringAttributeByFunctionJSONObject(combinationJSONObject, ATTRIBUTE_FUNCTION);
            MiuiCombinationRule combinationRule = this.mMiuiCombinationRuleManager.getMiuiCombinationRule(primaryKey, combinationKey, action, function, this.mCurrentUserId);
            this.mMiuiCombinationRuleManager.addRule(action, combinationRule);
            if (!this.mMiuiCombinationRuleManager.shouldHoldOnAOSPLogic(combinationRule)) {
                this.mKeyCombinationManager.addRule(combinationRule);
            }
        }
    }

    private void initSingleKeyRule(List<JSONObject> singleKeyJsonObjectList) {
        if (singleKeyJsonObjectList == null || singleKeyJsonObjectList.size() == 0) {
            return;
        }
        for (JSONObject singleKeyObject : singleKeyJsonObjectList) {
            int primaryKey = getIntAttributeValueByJsonObject(singleKeyObject, ATTRIBUTE_PRIMARY_KEY);
            MiuiSingleKeyInfo miuiSingleKeyInfo = getMiuiSingleKeyInfo(singleKeyObject);
            MiuiSingleKeyRule miuiSingleKeyRule = this.mMiuiSingleKeyRuleManager.getMiuiSingleKeyRule(primaryKey, miuiSingleKeyInfo, this.mCurrentUserId);
            if (miuiSingleKeyRule != null) {
                this.mMiuiSingleKeyRuleManager.addRule(miuiSingleKeyInfo, miuiSingleKeyRule);
                this.mSingleKeyGestureDetector.addRule(miuiSingleKeyRule);
            }
        }
    }

    private MiuiSingleKeyInfo getMiuiSingleKeyInfo(JSONObject singleKeyObject) {
        int primaryKey;
        long longPressTimeOut;
        Map<String, String> actionAndDefaultFunctionMap = new HashMap<>();
        Map<String, Integer> actionMaxCountMap = new HashMap<>();
        Map<String, String> actionMapForType = new HashMap<>();
        int primaryKey2 = 0;
        long longPressTimeOut2 = this.mMiuiShortcutTriggerHelper.getDefaultLongPressTimeOut();
        try {
            primaryKey2 = getIntAttributeValueByJsonObject(singleKeyObject, ATTRIBUTE_PRIMARY_KEY);
            try {
                if (!singleKeyObject.has(SINGLE_KEY_RULE_TYPE_SINGLE_PRESS)) {
                    primaryKey = primaryKey2;
                } else {
                    JSONArray singlePressArray = singleKeyObject.getJSONArray(SINGLE_KEY_RULE_TYPE_SINGLE_PRESS);
                    int i = 0;
                    while (i < singlePressArray.length()) {
                        JSONObject singlePressJSONObject = singlePressArray.getJSONObject(i);
                        String action = getStringAttributeValueByJsonObject(singlePressJSONObject, ATTRIBUTE_ACTION);
                        JSONArray singlePressArray2 = singlePressArray;
                        primaryKey = primaryKey2;
                        try {
                            actionAndDefaultFunctionMap.put(action, getStringAttributeByFunctionJSONObject(singlePressJSONObject, ATTRIBUTE_FUNCTION));
                            long configLongPressTimeOut = getLongAttributeByFunctionJSONObject(singlePressJSONObject, SINGLE_KEY_RULE_ATTRIBUTE_LONG_PRESS_TIME_OUT);
                            longPressTimeOut2 = configLongPressTimeOut > 0 ? configLongPressTimeOut : longPressTimeOut2;
                            actionMapForType.put(getStringAttributeValueByJsonObject(singlePressJSONObject, "type"), action);
                            actionMaxCountMap.put(action, Integer.valueOf(getIntAttributeValueByJsonObject(singlePressJSONObject, SINGLE_KEY_RULE_ATTRIBUTE_MAX_COUNT)));
                            i++;
                            singlePressArray = singlePressArray2;
                            primaryKey2 = primaryKey;
                        } catch (JSONException e) {
                            e = e;
                            primaryKey2 = primaryKey;
                            e.printStackTrace();
                            primaryKey = primaryKey2;
                            longPressTimeOut = longPressTimeOut2;
                            return new MiuiSingleKeyInfo(primaryKey, actionAndDefaultFunctionMap, longPressTimeOut, actionMaxCountMap, actionMapForType);
                        }
                    }
                    primaryKey = primaryKey2;
                }
                if (singleKeyObject.has(SINGLE_KEY_RULE_TYPE_MULTI_PRESS)) {
                    JSONArray multiPressArray = singleKeyObject.getJSONArray(SINGLE_KEY_RULE_TYPE_MULTI_PRESS);
                    for (int i2 = 0; i2 < multiPressArray.length(); i2++) {
                        JSONObject multiPressJSONObject = multiPressArray.getJSONObject(i2);
                        String action2 = multiPressJSONObject.getString(ATTRIBUTE_ACTION);
                        actionAndDefaultFunctionMap.put(action2, getStringAttributeByFunctionJSONObject(multiPressJSONObject, ATTRIBUTE_FUNCTION));
                        actionMaxCountMap.put(action2, Integer.valueOf(getIntAttributeValueByJsonObject(multiPressJSONObject, SINGLE_KEY_RULE_ATTRIBUTE_MAX_COUNT)));
                        actionMapForType.put(getStringAttributeValueByJsonObject(multiPressJSONObject, "type"), action2);
                    }
                }
                longPressTimeOut = longPressTimeOut2;
            } catch (JSONException e2) {
                e = e2;
            }
        } catch (JSONException e3) {
            e = e3;
        }
        return new MiuiSingleKeyInfo(primaryKey, actionAndDefaultFunctionMap, longPressTimeOut, actionMaxCountMap, actionMapForType);
    }

    private JSONObject getFunctionJSONObject(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.length() != 0 && jsonObject.has(ATTRIBUTE_FUNCTIONS)) {
            try {
                JSONArray functionsArray = jsonObject.getJSONArray(ATTRIBUTE_FUNCTIONS);
                for (int i = 0; i < functionsArray.length(); i++) {
                    JSONObject functionJSONObject = functionsArray.getJSONObject(i);
                    if (isLegaDataInCurrentRule(functionJSONObject)) {
                        return functionJSONObject;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private long getLongAttributeByFunctionJSONObject(JSONObject jsonObject, String attribute) {
        return getLongAttributeValueByJsonObject(getFunctionJSONObject(jsonObject), attribute);
    }

    private String getStringAttributeByFunctionJSONObject(JSONObject jsonObject, String attribute) {
        return getStringAttributeValueByJsonObject(getFunctionJSONObject(jsonObject), attribute);
    }

    private boolean isLegaDataInCurrentRule(JSONObject jsonObject) {
        int global = getIntAttributeValueByJsonObject(jsonObject, ATTRIBUTE_GLOBAL);
        String regions = getStringAttributeValueByJsonObject(jsonObject, "region");
        if (TextUtils.isEmpty(regions)) {
            return (!Build.IS_INTERNATIONAL_BUILD && global == 0) || (1 == global && Build.IS_INTERNATIONAL_BUILD) || 2 == global;
        }
        List<String> regionList = Arrays.asList(regions.split(","));
        return regionList.contains(MiuiShortcutTriggerHelper.CURRENT_DEVICE_REGION) || regionList.contains(MiuiShortcutTriggerHelper.CURRENT_DEVICE_CUSTOMIZED_REGION);
    }

    /* JADX WARN: Failed to find 'out' block for switch in B:14:0x0026. Please report as an issue. */
    private long getLongAttributeValueByJsonObject(JSONObject jsonObject, String attribute) {
        char c;
        if (jsonObject == null || jsonObject.length() == 0 || TextUtils.isEmpty(attribute)) {
            Slog.i(TAG, "get int attribute fail,jsonObject is null or attribute is null");
            return -1L;
        }
        try {
            switch (attribute.hashCode()) {
                case -1814935526:
                    if (attribute.equals(SINGLE_KEY_RULE_ATTRIBUTE_LONG_PRESS_TIME_OUT)) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
        } catch (JSONException e) {
            Slog.i(TAG, e.toString());
        }
        switch (c) {
            case 0:
                if (jsonObject.has(attribute)) {
                    return jsonObject.getLong(SINGLE_KEY_RULE_ATTRIBUTE_LONG_PRESS_TIME_OUT);
                }
                return -1L;
            default:
                Slog.i(TAG, "current attribute is invalid,attribute=" + attribute);
                return -1L;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int getIntAttributeValueByJsonObject(JSONObject jsonObject, String attribute) {
        char c;
        if (jsonObject == null || jsonObject.length() == 0 || TextUtils.isEmpty(attribute)) {
            Slog.i(TAG, "get int attribute fail,jsonObject is null or attribute is null");
            return -1;
        }
        try {
            switch (attribute.hashCode()) {
                case -1274920707:
                    if (attribute.equals(ATTRIBUTE_PRIMARY_KEY)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1243020381:
                    if (attribute.equals(ATTRIBUTE_GLOBAL)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -9648880:
                    if (attribute.equals(ATTRIBUTE_COMBINATION_KEY)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 382106123:
                    if (attribute.equals(SINGLE_KEY_RULE_ATTRIBUTE_MAX_COUNT)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    if (jsonObject.has(attribute)) {
                        return jsonObject.getInt(attribute);
                    }
                    return -1;
                case 2:
                    if (jsonObject.has(attribute)) {
                        return jsonObject.getInt(attribute);
                    }
                    return 0;
                case 3:
                    if (jsonObject.has(attribute)) {
                        return jsonObject.getInt(attribute);
                    }
                    return 1;
                default:
                    Slog.i(TAG, "current attribute is invalid,attribute=" + attribute);
                    return -1;
            }
        } catch (JSONException e) {
            Slog.i(TAG, e.toString());
            return -1;
        }
    }

    private String getStringAttributeValueByJsonObject(JSONObject jsonObject, String attribute) {
        if (jsonObject == null || jsonObject.length() == 0 || TextUtils.isEmpty(attribute)) {
            Slog.i(TAG, "get string attribute fail,jsonObject is null or attribute is null");
            return null;
        }
        try {
            if (jsonObject.has(attribute)) {
                return jsonObject.getString(attribute);
            }
            return null;
        } catch (JSONException e) {
            Slog.i(TAG, e.toString());
            return null;
        }
    }

    private String getShortcutConfig(String filePtah, String fileName) {
        File localFile = new File(filePtah + fileName);
        if (localFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(localFile);
                try {
                    String readFully = readFully(inputStream);
                    inputStream.close();
                    return readFully;
                } finally {
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private String readFully(FileInputStream inputStream) throws IOException {
        OutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = inputStream.read(buffer);
        while (read >= 0) {
            baos.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        return baos.toString();
    }

    public void onUserSwitch(int currentUserId) {
        this.mCurrentUserId = currentUserId;
        boolean isNewUser = !this.mInitForUsers.contains(Integer.valueOf(currentUserId));
        if (isNewUser) {
            this.mInitForUsers.add(Integer.valueOf(currentUserId));
        }
        this.mMiuiShortcutTriggerHelper.onUserSwitch(currentUserId, isNewUser);
        this.mMiuiSingleKeyRuleManager.onUserSwitch(currentUserId, isNewUser);
        this.mMiuiCombinationRuleManager.onUserSwitch(currentUserId, isNewUser);
        this.mMiuiGestureManager.onUserSwitch(currentUserId, isNewUser);
        this.mMiuiShortcutOperatorCustomManager.onUserSwitch(currentUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetShortcutSettings() {
        this.mMiuiSingleKeyRuleManager.resetShortcutSettings();
        this.mMiuiCombinationRuleManager.resetDefaultFunction();
        this.mMiuiGestureManager.resetDefaultFunction();
        this.mMiuiShortcutTriggerHelper.resetMiuiShortcutSettings();
        this.mMiuiShortcutOperatorCustomManager.initShortcut();
    }

    public MiuiCombinationRule getCombinationRule(String action) {
        return this.mMiuiCombinationRuleManager.getCombinationRule(action);
    }

    public MiuiSingleKeyRule getSingleKeyRule(int primaryKey) {
        return this.mMiuiSingleKeyRuleManager.getSingleKeyRuleForPrimaryKey(primaryKey);
    }

    public MiuiGestureRule getGestureRule(String action) {
        return this.mMiuiGestureManager.getGestureManager(action);
    }

    public String getFunction(String action) {
        if (this.mMiuiSingleKeyRuleManager.hasActionInSingleKeyMap(action)) {
            return this.mMiuiSingleKeyRuleManager.getFunction(action);
        }
        if (this.mMiuiCombinationRuleManager.hasActionInCombinationKeyMap(action)) {
            return this.mMiuiCombinationRuleManager.getFunction(action);
        }
        if (this.mMiuiGestureManager.hasActionInGestureRuleMap(action)) {
            return this.mMiuiGestureManager.getFunction(action);
        }
        return null;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        this.mMiuiSingleKeyRuleManager.dump(prefix2, pw);
        pw.print(prefix2);
        this.mMiuiCombinationRuleManager.dump(prefix2, pw);
        pw.print(prefix2);
        this.mMiuiGestureManager.dump(prefix2, pw);
        pw.print(prefix2);
        this.mMiuiShortcutTriggerHelper.dump(prefix2, pw);
    }

    public void updatePolicyFlag(int policyFlags) {
        this.mMiuiSingleKeyRuleManager.updatePolicyFlag(policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean skipKeyGesutre(KeyEvent event) {
        return this.mMiuiShortcutTriggerHelper.skipKeyGesutre(event);
    }
}
