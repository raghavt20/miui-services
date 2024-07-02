package com.miui.server.turbosched;

import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Slog;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import miui.turbosched.ITurboSchedManager;

/* loaded from: classes.dex */
public class FrameTurboAction {
    private static final String TAG = "TurboSched_FrameTurbo";
    private LocalLog mHistoryLog = new LocalLog(512);
    private String mCurrentTurboScene = "";
    private Map<String, LinkedList<String>> mActiveSceneWaitListMap = new HashMap();
    protected boolean mDebug = false;
    protected List<ITurboSchedManager.IFrameTurboSceneCallback> mFrameTurboSceneCallbacks = new ArrayList();
    protected Map<String, List<String>> mFrameTurboAppMap = initFrameTurboAppMap();
    protected Map<String, List<String>> mFrameTurboSceneWhiteListMap = initFrameTurboSceneWhiteListMap();

    private Map<String, List<String>> initFrameTurboAppMap() {
        Map<String, List<String>> appMap = new HashMap<>();
        String appListStr = SystemProperties.get("persist.sys.turbosched.frame_turbo.apps", "");
        if (appListStr != null && !appListStr.isEmpty()) {
            String[] appList = appListStr.split(",");
            for (String app : appList) {
                String[] appConfig = app.split(":");
                List<String> sceneList = new ArrayList<>();
                if (appConfig.length > 1) {
                    String[] scenes = appConfig[1].split("\\|");
                    Slog.d(TAG, "scenes: " + scenes[0] + ", size = " + scenes.length);
                    for (String scene : scenes) {
                        sceneList.add(scene);
                    }
                }
                appMap.put(appConfig[0], sceneList);
            }
        }
        if (this.mDebug) {
            Slog.d(TAG, "frame turbo init success, appMap: " + appMap.toString());
        }
        return appMap;
    }

    private Map<String, List<String>> initFrameTurboSceneWhiteListMap() {
        Map<String, List<String>> sceneMap = new HashMap<>();
        String sceneWhiteListStr = SystemProperties.get("persist.sys.turbosched.frame_turbo.scene_white_list", "");
        if (sceneWhiteListStr != null && !sceneWhiteListStr.isEmpty()) {
            String[] sceneWhiteList = sceneWhiteListStr.split(",");
            for (String scene : sceneWhiteList) {
                String[] sceneConfig = scene.split(":");
                List<String> sceneList = new ArrayList<>();
                if (sceneConfig.length > 1) {
                    String[] scenes = sceneConfig[1].split("\\|");
                    for (String s : scenes) {
                        sceneList.add(s);
                    }
                    sceneMap.put(sceneConfig[0], sceneList);
                }
            }
        }
        return sceneMap;
    }

    private boolean parseCommandArgs(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args.length < 2) {
            return false;
        }
        String command = args[1];
        if (args.length == 2) {
            boolean result = parseCommandWithoutArgs(fd, pw, command);
            return result;
        }
        boolean result2 = parseCommandWithArgs(fd, pw, command, args);
        return result2;
    }

    private boolean parseCommandWithoutArgs(FileDescriptor fd, PrintWriter pw, String command) {
        char c;
        switch (command.hashCode()) {
            case 926934164:
                if (command.equals("history")) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                this.mHistoryLog.dump(fd, pw, new String[0]);
                return true;
            default:
                return false;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean parseCommandWithArgs(FileDescriptor fd, PrintWriter pw, String command, String[] args) {
        char c;
        switch (command.hashCode()) {
            case -1298848381:
                if (command.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 95458899:
                if (command.equals("debug")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 109254796:
                if (command.equals("scene")) {
                    c = 2;
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
                boolean result = parseEnableCommand(args, pw);
                return result;
            case 1:
                boolean result2 = parseDebugCommand(args, pw);
                return result2;
            case 2:
                boolean result3 = parseSceneCommand(args, pw);
                return result3;
            default:
                return false;
        }
    }

    private boolean parseEnableCommand(String[] args, PrintWriter pw) {
        boolean enable = !args[2].equals("0");
        List<String> policyList = TurboSchedConfig.getPolicyList();
        if (enable) {
            if (!policyList.contains("frame_turbo")) {
                policyList.add("frame_turbo");
                TurboSchedConfig.setPolicyList(policyList);
            }
            pw.println("enable frame turbo success");
        } else {
            if (policyList.contains("frame_turbo")) {
                policyList.remove("frame_turbo");
                TurboSchedConfig.setPolicyList(policyList);
            }
            pw.println("disable frame turbo success");
        }
        return true;
    }

    private boolean parseDebugCommand(String[] args, PrintWriter pw) {
        this.mDebug = !args[2].equals("0");
        pw.println("set debug: " + this.mDebug);
        return true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x007a, code lost:
    
        if (r9.equals("whitelist") != false) goto L28;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseSceneCommand(java.lang.String[] r12, java.io.PrintWriter r13) {
        /*
            Method dump skipped, instructions count: 314
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.FrameTurboAction.parseSceneCommand(java.lang.String[], java.io.PrintWriter):boolean");
    }

    private boolean checkSceneFormat(String scene) {
        String[] appScenes = scene.split(",");
        for (String appScene : appScenes) {
            String[] appSceneConfig = appScene.split(":");
            if (appSceneConfig.length < 2) {
                return false;
            }
            String[] scenes = appSceneConfig[0].split("#");
            if (scenes.length < 2) {
                return false;
            }
        }
        return true;
    }

    private boolean testScene(boolean start, List<String> sceneList) {
        if (sceneList.size() > 1) {
            return false;
        }
        updateSceneWhiteList(start, sceneList);
        updateTriggerList(start, sceneList);
        updateMarkScene(start, sceneList);
        return true;
    }

    private void updateTriggerList(boolean add, List<String> frameCongfigs) {
        for (String config : frameCongfigs) {
            if (config != null && !config.contains(":")) {
                if (this.mDebug) {
                    Slog.d(TAG, "invalid config: " + config);
                }
            } else {
                String[] appConfig = config.split(":");
                enableFrameTurboInternal(add, appConfig[0], appConfig[1]);
            }
        }
    }

    private boolean updateMarkScene(boolean enable, List<String> frameConfigs) {
        if (frameConfigs.size() > 1) {
            return false;
        }
        String[] appConfig = frameConfigs.get(0).split(":");
        if (appConfig.length < 2) {
            return false;
        }
        int code = markScene(appConfig[0], appConfig[1], enable);
        return code == 0;
    }

    private void updateSceneWhiteList(boolean add, List<String> sceneList) {
        for (String scene : sceneList) {
            String[] sceneConfig = scene.split(":");
            if (sceneConfig.length > 1) {
                String[] scenes = sceneConfig[1].split("\\|");
                List<String> sceneWhiteList = this.mFrameTurboSceneWhiteListMap.get(sceneConfig[0]);
                for (String s : scenes) {
                    if (add) {
                        if (sceneWhiteList == null) {
                            sceneWhiteList = new ArrayList();
                        }
                        if (!sceneWhiteList.contains(s)) {
                            sceneWhiteList.add(s);
                        }
                    } else if (sceneWhiteList != null && sceneWhiteList.contains(s)) {
                        sceneWhiteList.remove(s);
                    }
                }
                if (sceneWhiteList != null && !sceneWhiteList.isEmpty()) {
                    this.mFrameTurboSceneWhiteListMap.put(sceneConfig[0], sceneWhiteList);
                } else {
                    this.mFrameTurboSceneWhiteListMap.remove(sceneConfig[0]);
                }
            }
        }
        String whiteListStr = "";
        for (String key : this.mFrameTurboSceneWhiteListMap.keySet()) {
            List<String> sceneWhiteList2 = this.mFrameTurboSceneWhiteListMap.get(key);
            if (sceneWhiteList2 != null && !sceneWhiteList2.isEmpty()) {
                whiteListStr = whiteListStr + key + ":" + TextUtils.join("|", sceneWhiteList2) + ", ";
            }
        }
        if (whiteListStr.length() > 0) {
            whiteListStr = whiteListStr.substring(0, whiteListStr.length() - 2);
        }
        SystemProperties.set("persist.sys.turbosched.frame_turbo.scene_white_list", whiteListStr);
    }

    private void printCommandResult(PrintWriter pw, String result) {
        pw.println("--------------------command result-----------------------");
        pw.println(result);
    }

    private int checkPermission(String packageName, String sceneId) {
        if (packageName == null || packageName.length() == 0 || !this.mFrameTurboSceneWhiteListMap.containsKey(packageName)) {
            if (this.mDebug) {
                Slog.d(TAG, "TS [MARK] : failed, R: not in white list, packageName: " + packageName + ",sceneId: " + sceneId);
            }
            this.mHistoryLog.log("TS [MARK] : failed, R: not in white list, packageName: " + packageName + ",sceneId: " + sceneId);
            return -4;
        }
        if (sceneId == null || sceneId.length() == 0 || this.mFrameTurboSceneWhiteListMap.get(packageName) == null || !this.mFrameTurboSceneWhiteListMap.get(packageName).contains(sceneId)) {
            if (this.mDebug) {
                Slog.d(TAG, "TS [MARK] : failed, R: not in white list, packageName: " + packageName + ",sceneId: " + sceneId);
            }
            this.mHistoryLog.log("TS [MARK] : failed, R: not in white list, packageName: " + packageName + ",sceneId: " + sceneId);
            return -4;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean parseFrameTurboCommand(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args.length >= 2) {
            if (parseCommandArgs(fd, pw, args)) {
                return true;
            }
            pw.println("wrong command");
            return true;
        }
        pw.println("--------------------config-------------------------------");
        boolean isEnable = TurboSchedConfig.getPolicyList().contains("frame_turbo");
        pw.println("enable: " + isEnable);
        pw.println("debug: " + this.mDebug);
        pw.println("current scene: " + this.mCurrentTurboScene);
        Map<String, List<String>> map = this.mFrameTurboAppMap;
        if (map != null && map.size() > 0) {
            pw.println("-----------------turbo trigger list------------------------");
            for (Map.Entry<String, List<String>> entry : this.mFrameTurboAppMap.entrySet()) {
                String valueStr = "";
                for (String value : entry.getValue()) {
                    valueStr = valueStr + value + "|";
                }
                if (valueStr.endsWith("|")) {
                    valueStr = valueStr.substring(0, valueStr.length() - 1);
                }
                pw.println(entry.getKey() + ":" + valueStr);
            }
        }
        Map<String, List<String>> map2 = this.mFrameTurboSceneWhiteListMap;
        if (map2 != null && map2.size() > 0) {
            pw.println("--------------------scene white list---------------------");
            for (Map.Entry<String, List<String>> entry2 : this.mFrameTurboSceneWhiteListMap.entrySet()) {
                String valueStr2 = "";
                for (String value2 : entry2.getValue()) {
                    valueStr2 = valueStr2 + value2 + "|";
                }
                if (valueStr2.endsWith("|")) {
                    valueStr2 = valueStr2.substring(0, valueStr2.length() - 1);
                }
                pw.println(entry2.getKey() + ":" + valueStr2);
            }
        }
        Map<String, LinkedList<String>> map3 = this.mActiveSceneWaitListMap;
        if (map3 != null && map3.size() > 0) {
            pw.println("--------------------active scene wait set---------------------");
            for (Map.Entry<String, LinkedList<String>> entry3 : this.mActiveSceneWaitListMap.entrySet()) {
                String valueStr3 = "";
                Iterator<String> it = entry3.getValue().iterator();
                while (it.hasNext()) {
                    String value3 = it.next();
                    valueStr3 = valueStr3 + value3 + "|";
                }
                if (valueStr3.endsWith("|")) {
                    valueStr3 = valueStr3.substring(0, valueStr3.length() - 1);
                }
                pw.println(entry3.getKey() + ":" + valueStr3);
            }
        }
        return true;
    }

    protected void enableFrameTurboInternal(boolean enable, String processThread, String sceneListStr) {
        if (enable) {
            if (this.mFrameTurboAppMap.containsKey(processThread)) {
                List<String> sceneList = this.mFrameTurboAppMap.get(processThread);
                if (sceneListStr != null) {
                    String[] sceneArray = sceneListStr.split(",");
                    for (int i = 0; i < sceneArray.length; i++) {
                        if (!sceneList.contains(sceneArray[i])) {
                            sceneList.add(sceneArray[i]);
                        }
                    }
                }
            } else {
                List<String> sceneList2 = new ArrayList<>();
                if (sceneListStr != null) {
                    for (String str : sceneListStr.split(",")) {
                        sceneList2.add(str);
                    }
                    this.mFrameTurboAppMap.put(processThread, sceneList2);
                }
            }
        } else if (this.mFrameTurboAppMap.containsKey(processThread)) {
            List<String> sceneList3 = this.mFrameTurboAppMap.get(processThread);
            if (sceneListStr != null) {
                String[] sceneArray2 = sceneListStr.split(",");
                for (int i2 = 0; i2 < sceneArray2.length; i2++) {
                    if (sceneList3.contains(sceneArray2[i2])) {
                        sceneList3.remove(sceneArray2[i2]);
                    }
                    if (sceneList3.size() == 0) {
                        this.mFrameTurboAppMap.remove(processThread);
                    }
                }
            }
        }
        String frameTurboApps = "";
        for (Map.Entry<String, List<String>> entry : this.mFrameTurboAppMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            frameTurboApps = frameTurboApps + key + ":" + TextUtils.join("|", value) + ",";
        }
        if (frameTurboApps.length() > 0) {
            frameTurboApps = frameTurboApps.substring(0, frameTurboApps.length() - 1);
        }
        SystemProperties.set("persist.sys.turbosched.frame_turbo.apps", frameTurboApps);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void registerSceneCallback(ITurboSchedManager.IFrameTurboSceneCallback callback) {
        if (this.mDebug) {
            Slog.d(TAG, "registerSceneCallback");
        }
        synchronized (this.mFrameTurboSceneCallbacks) {
            if (!this.mFrameTurboSceneCallbacks.contains(callback)) {
                this.mFrameTurboSceneCallbacks.add(callback);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void unregisterSceneCallback(ITurboSchedManager.IFrameTurboSceneCallback callback) {
        if (this.mDebug) {
            Slog.d(TAG, "unregisterSceneCallback");
        }
        synchronized (this.mFrameTurboSceneCallbacks) {
            if (this.mFrameTurboSceneCallbacks.contains(callback)) {
                this.mFrameTurboSceneCallbacks.remove(callback);
            }
        }
    }

    protected void notifySceneChanged(String packageName, String sceneId, boolean start) {
        if (this.mDebug) {
            Slog.d(TAG, "notifySceneChanged, packageName: " + packageName + ",sceneId: " + sceneId + ",start: " + start + ",mFrameTurboSceneCallbacks: " + this.mFrameTurboSceneCallbacks.size());
        }
        if (start) {
            this.mCurrentTurboScene = packageName + ":" + sceneId;
        } else {
            this.mCurrentTurboScene = "";
        }
        SystemProperties.set("persist.sys.turbosched.frame_turbo.current_turbo_scene", this.mCurrentTurboScene);
        synchronized (this.mFrameTurboSceneCallbacks) {
            if (this.mFrameTurboSceneCallbacks.size() == 0) {
                return;
            }
            ITurboSchedManager.IFrameTurboSceneCallback[] copyCallbacks = (ITurboSchedManager.IFrameTurboSceneCallback[]) this.mFrameTurboSceneCallbacks.toArray(new ITurboSchedManager.IFrameTurboSceneCallback[0]);
            for (ITurboSchedManager.IFrameTurboSceneCallback iFrameTurboSceneCallback : copyCallbacks) {
                try {
                    iFrameTurboSceneCallback.onSceneChanged(packageName, sceneId, start);
                } catch (RemoteException e) {
                    Slog.e(TAG, "notifySceneChanged, RemoteException: " + e.getMessage());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int markScene(String packageName, String sceneId, boolean start) {
        int code;
        if (start && (code = checkPermission(packageName, sceneId)) != 0) {
            return code;
        }
        if (start) {
            if (!"".equals(this.mCurrentTurboScene)) {
                LinkedList<String> wList = this.mActiveSceneWaitListMap.get(packageName);
                if (wList == null) {
                    wList = new LinkedList<>();
                }
                if (!wList.contains(sceneId)) {
                    wList.add(sceneId);
                }
                this.mActiveSceneWaitListMap.put(packageName, wList);
                return -5;
            }
            notifySceneChanged(packageName, sceneId, start);
            return 0;
        }
        if (!start) {
            notifySceneChanged(packageName, sceneId, start);
            LinkedList<String> wList2 = this.mActiveSceneWaitListMap.get(packageName);
            if (wList2 != null) {
                if (wList2.contains(sceneId)) {
                    wList2.remove(sceneId);
                }
                if (wList2.size() > 0) {
                    String nextScene = wList2.getFirst();
                    notifySceneChanged(packageName, nextScene, true);
                    wList2.removeFirst();
                }
                if (wList2.size() == 0) {
                    this.mActiveSceneWaitListMap.remove(packageName);
                } else {
                    this.mActiveSceneWaitListMap.put(packageName, wList2);
                }
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int triggerFrameTurbo(String packageName, boolean turbo, List<String> uiThreads, List<String> scenes) {
        for (String uiThread : uiThreads) {
            String processThread = packageName + "#" + uiThread;
            if (scenes == null || scenes.size() == 0) {
                return -3;
            }
            String sceneList = TextUtils.join("|", scenes);
            enableFrameTurboInternal(turbo, processThread, sceneList);
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateWhiteList(String[] pkgScenes) {
        this.mFrameTurboSceneWhiteListMap.clear();
        if (pkgScenes.length > 0) {
            for (String pkgScene : pkgScenes) {
                String[] pkgSceneInfo = pkgScene.split(":");
                if (pkgSceneInfo.length == 2) {
                    String pkgName = pkgSceneInfo[0];
                    List<String> sceneList = this.mFrameTurboSceneWhiteListMap.get(pkgName);
                    if (sceneList == null) {
                        sceneList = new ArrayList();
                    }
                    String[] scenes = pkgSceneInfo[1].split("\\|");
                    for (String scene : scenes) {
                        sceneList.add(scene);
                    }
                    if (sceneList.size() == 0) {
                        this.mFrameTurboSceneWhiteListMap.remove(pkgName);
                    } else {
                        this.mFrameTurboSceneWhiteListMap.put(pkgName, sceneList);
                    }
                }
            }
            String whiteListStr = "";
            for (String key : this.mFrameTurboSceneWhiteListMap.keySet()) {
                List<String> sceneWhiteList = this.mFrameTurboSceneWhiteListMap.get(key);
                if (sceneWhiteList != null && !sceneWhiteList.isEmpty()) {
                    whiteListStr = whiteListStr + key + ":" + TextUtils.join("|", sceneWhiteList) + ", ";
                }
            }
            if (whiteListStr.length() > 0) {
                whiteListStr = whiteListStr.substring(0, whiteListStr.length() - 2);
            }
            SystemProperties.set("persist.sys.turbosched.frame_turbo.scene_white_list", whiteListStr);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onForegroundActivitiesChanged(String processName, boolean foregroundActivities) {
        if (!foregroundActivities) {
            if ("".equals(this.mCurrentTurboScene)) {
                return;
            }
            String[] sceneInfo = this.mCurrentTurboScene.split(":");
            if (sceneInfo.length != 2 || !sceneInfo[0].equals(processName)) {
                return;
            }
            String sceneId = sceneInfo[1];
            notifySceneChanged(processName, sceneId, false);
            LinkedList<String> wList = this.mActiveSceneWaitListMap.get(processName);
            if (wList == null) {
                wList = new LinkedList<>();
            }
            wList.addFirst(sceneId);
            this.mActiveSceneWaitListMap.put(processName, wList);
            this.mCurrentTurboScene = "";
            return;
        }
        LinkedList<String> wList2 = this.mActiveSceneWaitListMap.get(processName);
        if (wList2 != null) {
            if (wList2.size() > 0) {
                String nextScene = wList2.getFirst();
                notifySceneChanged(processName, nextScene, true);
                wList2.removeFirst();
            }
            if (wList2.size() == 0) {
                this.mActiveSceneWaitListMap.remove(processName);
            } else {
                this.mActiveSceneWaitListMap.put(processName, wList2);
            }
        }
    }
}
