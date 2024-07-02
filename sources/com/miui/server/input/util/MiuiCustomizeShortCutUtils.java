package com.miui.server.input.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.Xml;
import android.view.KeyboardShortcutInfo;
import com.android.internal.logging.EventLogTags;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.MiuiBgThread;
import com.android.server.input.KeyboardCombinationManagerStubImpl;
import com.android.server.wm.MiuiSizeCompatService;
import com.miui.server.input.custom.InputMiuiDesktopMode;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MiuiCustomizeShortCutUtils {
    public static final int ALL_SHORTCUT = 0;
    public static final int APP_SHORTCUT = 2;
    public static final String ATTRIBUTE_ALT = "alt";
    public static final String ATTRIBUTE_CLASSNAME = "className";
    public static final String ATTRIBUTE_CTRL = "ctrl";
    public static final String ATTRIBUTE_CUSTOMIZED = "customized";
    public static final String ATTRIBUTE_ENABLE = "enable";
    public static final String ATTRIBUTE_KEYCODE = "keycode";
    public static final String ATTRIBUTE_LEFT_ALT = "lalt";
    public static final String ATTRIBUTE_META = "meta";
    public static final String ATTRIBUTE_PACKAGENAME = "packageName";
    public static final String ATTRIBUTE_RIGHT_ALT = "ralt";
    public static final String ATTRIBUTE_SHIFT = "shift";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final int SYSTEM_SHORTCUT = 1;
    private static final int UPDATE_ADD = 0;
    private static final int UPDATE_DEL = 2;
    private static final int UPDATE_MODIFY = 1;
    private static final int UPDATE_RESET_APP = 3;
    private static final int UPDATE_RESET_SYSTEM = 4;
    private static volatile MiuiCustomizeShortCutUtils mInstance;
    private Context mContext;
    private final String TAG = "MiuiCustomizeShortCutUtils";
    private final String TAG_1 = "ShortcutInfos";
    private final String TAG_2 = "ShortcutInfo";
    private LongSparseArray<MiuiKeyboardShortcutInfo> mShortcutInfos = new LongSparseArray<>();
    private List<KeyboardShortcutInfo> mKeyboardShortcutInfos = new ArrayList();
    private List<KeyboardShortcutInfo> mDefaultShortcutInfos = new ArrayList();
    private List<KeyboardShortcutInfo> mDefaultSystemShortcutInfos = new ArrayList();
    private List<KeyboardShortcutInfo> mDefaultAppShortcutInfos = new ArrayList();
    private final String mShortCutFileName = "miuishortcuts";
    private volatile boolean mInterceptShortcutKey = false;
    private boolean mNeedLoadInfoForDefaultRes = true;
    private final File mFile = new File(Environment.getDataSystemDirectory(), "miuishortcuts.xml");
    private final File mBackupFile = new File(Environment.getDataSystemDirectory(), "miuishortcuts-backup.xml");
    private final ReflectionUtil mReflectionUtil = new ReflectionUtil();

    private MiuiCustomizeShortCutUtils(Context context) {
        this.mContext = context;
        final IntentFilter local_switch = new IntentFilter();
        local_switch.addAction("android.intent.action.LOCALE_CHANGED");
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCustomizeShortCutUtils.this.lambda$new$0(local_switch);
            }
        });
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCustomizeShortCutUtils.this.lambda$new$1();
            }
        });
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCustomizeShortCutUtils.this.lambda$new$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(IntentFilter local_switch) {
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Slog.d("MiuiCustomizeShortCutUtils", "update all shortcuts because languages is changed");
                MiuiCustomizeShortCutUtils.this.lambda$new$1();
            }
        }, UserHandle.ALL, local_switch, null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$2() {
        loadFromDefaultRes(true);
    }

    public static MiuiCustomizeShortCutUtils getInstance(Context context) {
        if (mInstance == null) {
            synchronized (MiuiCustomizeShortCutUtils.class) {
                if (mInstance == null) {
                    mInstance = new MiuiCustomizeShortCutUtils(context);
                }
            }
        }
        return mInstance;
    }

    public List<KeyboardShortcutInfo> getKeyboardShortcutInfo() {
        List<KeyboardShortcutInfo> miuiDeskModeKeyboardShortcutInfos = InputMiuiDesktopMode.getMuiDeskModeKeyboardShortcutInfo(this.mContext, this.mKeyboardShortcutInfos);
        if (miuiDeskModeKeyboardShortcutInfos != null) {
            return miuiDeskModeKeyboardShortcutInfos;
        }
        return this.mKeyboardShortcutInfos;
    }

    public LongSparseArray<MiuiKeyboardShortcutInfo> getMiuiKeyboardShortcutInfo() {
        return this.mShortcutInfos;
    }

    public List<KeyboardShortcutInfo> getDefaultKeyboardShortcutInfos() {
        return this.mDefaultShortcutInfos;
    }

    public void updateKeyboardShortcut(KeyboardShortcutInfo info, int type) {
        MiuiKeyboardShortcutInfo shortcutInfo = this.mReflectionUtil.invokeObject(info);
        lambda$new$1();
        switch (type) {
            case 0:
                this.mShortcutInfos.put(shortcutInfo.getShortcutKeyCode(), shortcutInfo);
                KeyboardCombinationManagerStubImpl.getInstance().addRule(shortcutInfo);
                break;
            case 1:
                this.mShortcutInfos.put(shortcutInfo.getHistoryKeyCode(), shortcutInfo);
                KeyboardCombinationManagerStubImpl.getInstance().updateRule(shortcutInfo);
                break;
            case 2:
                this.mShortcutInfos.delete(shortcutInfo.getShortcutKeyCode());
                KeyboardCombinationManagerStubImpl.getInstance().removeRule(shortcutInfo);
                break;
            case 3:
                resetShortCut(2);
                KeyboardCombinationManagerStubImpl.getInstance().resetRule();
                break;
            case 4:
                resetShortCut(1);
                KeyboardCombinationManagerStubImpl.getInstance().resetRule();
                break;
        }
        writeShortCuts();
        lambda$new$1();
    }

    public void enableAutoRemoveShortCutWhenAppRemove() {
        Slog.i("MiuiCustomizeShortCutUtils", "enableAutoRemoveShortCutWhenAppRemove");
        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCustomizeShortCutUtils.this.lambda$enableAutoRemoveShortCutWhenAppRemove$3(packageFilter);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$enableAutoRemoveShortCutWhenAppRemove$3(IntentFilter packageFilter) {
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.miui.server.input.util.MiuiCustomizeShortCutUtils.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Slog.d("MiuiCustomizeShortCutUtils", "enableAutoRemoveShortCutWhenAppRemove: onReceive");
                Uri data = intent.getData();
                if (data == null) {
                    Slog.w("MiuiCustomizeShortCutUtils", "Cannot handle package broadcast with null data");
                } else {
                    String packageName = data.getSchemeSpecificPart();
                    MiuiCustomizeShortCutUtils.this.delShortCutByPackage(packageName);
                }
            }
        }, UserHandle.ALL, packageFilter, null, null);
    }

    private void loadFromDefaultRes(boolean onlyInitDefault) {
        try {
            if (!this.mNeedLoadInfoForDefaultRes) {
                return;
            }
            int resId = this.mContext.getResources().getIdentifier("miuishortcuts", "xml", "android.miui");
            XmlResourceParser parser = this.mContext.getResources().getXml(resId);
            XmlUtils.beginDocument(parser, "ShortcutInfos");
            ArrayList<MiuiKeyboardShortcutInfo> infos = new ArrayList<>();
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1 || !"ShortcutInfo".equals(parser.getName())) {
                    break;
                }
                MiuiKeyboardShortcutInfo info = readFromParser(parser);
                if (!onlyInitDefault) {
                    if (info.getType() == 0) {
                        if (checkAppInstalled(this.mContext, info.getPackageName())) {
                            this.mShortcutInfos.put(info.getShortcutKeyCode(), info);
                        }
                    } else {
                        this.mShortcutInfos.put(info.getShortcutKeyCode(), info);
                    }
                }
                infos.add(info);
            }
            this.mDefaultShortcutInfos.clear();
            this.mDefaultAppShortcutInfos.clear();
            this.mDefaultSystemShortcutInfos.clear();
            Iterator<MiuiKeyboardShortcutInfo> it = infos.iterator();
            while (it.hasNext()) {
                MiuiKeyboardShortcutInfo info2 = it.next();
                KeyboardShortcutInfo keyboardShortcutInfo = this.mReflectionUtil.reflectObject(info2);
                this.mDefaultShortcutInfos.add(keyboardShortcutInfo);
                if (info2.getType() == 0) {
                    this.mDefaultAppShortcutInfos.add(keyboardShortcutInfo);
                    if (!onlyInitDefault && checkAppInstalled(this.mContext, info2.getPackageName())) {
                        this.mKeyboardShortcutInfos.add(keyboardShortcutInfo);
                    }
                } else {
                    this.mDefaultSystemShortcutInfos.add(keyboardShortcutInfo);
                    if (!onlyInitDefault) {
                        this.mKeyboardShortcutInfos.add(keyboardShortcutInfo);
                    }
                }
            }
            this.mNeedLoadInfoForDefaultRes = false;
        } catch (IOException | XmlPullParserException e) {
            Slog.w("MiuiCustomizeShortCutUtils", "Got exception parsing bookmarks.", e);
        }
    }

    public boolean checkAppInstalled(Context context, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(pkgName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* renamed from: loadShortCuts, reason: merged with bridge method [inline-methods] */
    public void lambda$new$1() {
        Slog.i("MiuiCustomizeShortCutUtils", "loadShortCuts");
        FileInputStream str = null;
        if (this.mBackupFile.exists()) {
            try {
                str = new FileInputStream(this.mBackupFile);
                Slog.w("MiuiCustomizeShortCutUtils", "Need to read from backup settings file");
                if (this.mFile.exists()) {
                    Slog.w("MiuiCustomizeShortCutUtils", "Cleaning up settings file " + this.mFile);
                    this.mFile.delete();
                }
            } catch (IOException e) {
            }
        }
        this.mShortcutInfos.clear();
        this.mKeyboardShortcutInfos.clear();
        try {
            if (str == null) {
                try {
                    if (!this.mFile.exists()) {
                        Slog.w("MiuiCustomizeShortCutUtils", "No shortcuts xml file found");
                        this.mNeedLoadInfoForDefaultRes = true;
                        loadFromDefaultRes(false);
                        Slog.i("MiuiCustomizeShortCutUtils", "load shortcuts  from res successfully");
                        return;
                    }
                    str = new FileInputStream(this.mFile);
                } catch (IOException | XmlPullParserException e2) {
                    this.mFile.delete();
                    Slog.wtf("MiuiCustomizeShortCutUtils", "Error reading miui shortcuts settings", e2);
                }
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(str, StandardCharsets.UTF_8.name());
            XmlUtils.beginDocument(parser, "ShortcutInfos");
            ArrayList<MiuiKeyboardShortcutInfo> infos = new ArrayList<>();
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1) {
                    break;
                }
                if (!parser.getName().equals("ShortcutInfo")) {
                    Slog.w("MiuiCustomizeShortCutUtils", "read xml tagName error." + parser.getName());
                    break;
                } else {
                    MiuiKeyboardShortcutInfo info = readFromParser(parser);
                    this.mShortcutInfos.put(info.getShortcutKeyCode(), info);
                    infos.add(info);
                }
            }
            Iterator<MiuiKeyboardShortcutInfo> it = infos.iterator();
            while (it.hasNext()) {
                KeyboardShortcutInfo keyboardShortcutInfo = this.mReflectionUtil.reflectObject(it.next());
                this.mKeyboardShortcutInfos.add(keyboardShortcutInfo);
            }
            Slog.i("MiuiCustomizeShortCutUtils", "load shortcuts from xml successfully");
        } finally {
            IoUtils.closeQuietly(str);
        }
    }

    public void writeShortCuts() {
        long startTime = SystemClock.uptimeMillis();
        if (this.mFile.exists()) {
            if (!this.mBackupFile.exists()) {
                if (!this.mFile.renameTo(this.mBackupFile)) {
                    Slog.wtf("MiuiCustomizeShortCutUtils", "Unable to backup miui shortcut settings,  current changes will be lost at reboot");
                    return;
                }
            } else {
                this.mFile.delete();
                Slog.w("MiuiCustomizeShortCutUtils", "Preserving older settings backup");
            }
        }
        try {
            FileOutputStream fstr = new FileOutputStream(this.mFile);
            try {
                BufferedOutputStream out = new BufferedOutputStream(fstr);
                try {
                    XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(out, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "ShortcutInfos");
                    for (int i = 0; i < this.mShortcutInfos.size(); i++) {
                        writeToParser(this.mShortcutInfos.valueAt(i), fastXmlSerializer);
                    }
                    fastXmlSerializer.endTag(null, "ShortcutInfos");
                    fastXmlSerializer.endDocument();
                    out.flush();
                    FileUtils.sync(fstr);
                    out.close();
                    this.mBackupFile.delete();
                    EventLogTags.writeCommitSysConfigFile("miuishortcuts", SystemClock.uptimeMillis() - startTime);
                    out.close();
                    fstr.close();
                } finally {
                }
            } finally {
            }
        } catch (Exception e) {
            Slog.wtf("MiuiCustomizeShortCutUtils", "Unable to write miui shortcut settings, current changes will be lost at reboot", e);
            if (this.mFile.exists() && !this.mFile.delete()) {
                Slog.wtf("MiuiCustomizeShortCutUtils", "Failed to clean up mangled file: " + this.mFile);
            }
        }
    }

    public void resetShortCut(int type) {
        List<KeyboardShortcutInfo> defaultShortcutInfos;
        if (type != 1 && type != 2) {
            return;
        }
        LongSparseArray<MiuiKeyboardShortcutInfo> shortcutInfos = this.mShortcutInfos.clone();
        for (int i = 0; i < shortcutInfos.size(); i++) {
            MiuiKeyboardShortcutInfo info = shortcutInfos.valueAt(i);
            if ((type == 1 && info.getType() != 0) || (type == 2 && info.getType() == 0)) {
                this.mShortcutInfos.delete(info.getShortcutKeyCode());
                KeyboardShortcutInfo keyboardShortcutInfo = this.mReflectionUtil.reflectObject(info);
                this.mKeyboardShortcutInfos.remove(keyboardShortcutInfo);
            }
        }
        new ArrayList();
        if (type == 2) {
            defaultShortcutInfos = this.mDefaultAppShortcutInfos;
        } else {
            defaultShortcutInfos = this.mDefaultSystemShortcutInfos;
        }
        for (KeyboardShortcutInfo info2 : defaultShortcutInfos) {
            if (type != 2 || checkAppInstalled(this.mContext, info2.getPackageName())) {
                this.mKeyboardShortcutInfos.add(info2);
                MiuiKeyboardShortcutInfo shortcutInfo = this.mReflectionUtil.invokeObject(info2);
                this.mShortcutInfos.append(shortcutInfo.getShortcutKeyCode(), shortcutInfo);
            }
        }
    }

    public boolean isShortcutExist(long shortcutCode) {
        return this.mShortcutInfos.get(shortcutCode) != null;
    }

    public boolean addShortcut(MiuiKeyboardShortcutInfo info) {
        if (this.mShortcutInfos.get(info.getShortcutKeyCode()) != null) {
            return false;
        }
        this.mShortcutInfos.put(info.getShortcutKeyCode(), info);
        return true;
    }

    public void delShortCutByShortcutCode(long shortcutCode) {
        this.mShortcutInfos.delete(shortcutCode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void delShortCutByPackage(String packageName) {
        for (int i = 0; i < this.mKeyboardShortcutInfos.size(); i++) {
            KeyboardShortcutInfo info = this.mKeyboardShortcutInfos.get(i);
            if (info.getPackageName() != null && info.getPackageName().equals(packageName)) {
                this.mShortcutInfos.delete(info.getShortcutKeyCode());
                this.mKeyboardShortcutInfos.remove(info);
                writeShortCuts();
                Slog.i("MiuiCustomizeShortCutUtils", "delShortCutByPackage: " + packageName);
                return;
            }
        }
    }

    public MiuiKeyboardShortcutInfo getShortcut(long shortcutCode) {
        return this.mShortcutInfos.get(shortcutCode);
    }

    private MiuiKeyboardShortcutInfo readFromParser(XmlPullParser in) {
        boolean meta = "true".equals(in.getAttributeValue(null, ATTRIBUTE_META));
        boolean shift = "true".equals(in.getAttributeValue(null, ATTRIBUTE_SHIFT));
        boolean ctrl = "true".equals(in.getAttributeValue(null, ATTRIBUTE_CTRL));
        boolean alt = "true".equals(in.getAttributeValue(null, ATTRIBUTE_ALT));
        boolean rctrl = "true".equals(in.getAttributeValue(null, ATTRIBUTE_RIGHT_ALT));
        boolean lctrl = "true".equals(in.getAttributeValue(null, ATTRIBUTE_LEFT_ALT));
        int keyCode = Integer.parseInt(in.getAttributeValue(null, ATTRIBUTE_KEYCODE));
        long shortcutCode = meta ? 281474976710656L | 0 : 0L;
        long shortcutCode2 = shift ? 4294967296L | shortcutCode : shortcutCode;
        long shortcutCode3 = alt ? 8589934592L | shortcutCode2 : shortcutCode2;
        long shortcutCode4 = ctrl ? 17592186044416L | shortcutCode3 : shortcutCode3;
        long shortcutCode5 = rctrl ? 137438953472L | shortcutCode4 : shortcutCode4;
        long shortcutCode6 = (lctrl ? 68719476736L | shortcutCode5 : shortcutCode5) | keyCode;
        int type = Integer.parseInt(in.getAttributeValue(null, ATTRIBUTE_TYPE));
        boolean enable = Boolean.parseBoolean(in.getAttributeValue(null, ATTRIBUTE_ENABLE));
        boolean customized = Boolean.parseBoolean(in.getAttributeValue(null, ATTRIBUTE_CUSTOMIZED));
        MiuiKeyboardShortcutInfo info = new MiuiKeyboardShortcutInfo(shortcutCode6, enable, type, customized);
        if (type == 0) {
            String packageName = in.getAttributeValue(null, "packageName");
            String className = in.getAttributeValue(null, ATTRIBUTE_CLASSNAME);
            info.setAppInfo(packageName, className);
        }
        return info;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println("MiuiCustomizeShortCutUtils");
        pw.println(prefix);
        pw.print("mShortcutInfos =");
        for (int i = 0; i < this.mShortcutInfos.size(); i++) {
            pw.println(this.mShortcutInfos.valueAt(i).toString());
        }
    }

    public void writeToParser(MiuiKeyboardShortcutInfo info, XmlSerializer out) {
        try {
            out.startTag(null, "ShortcutInfo");
            boolean z = true;
            out.attribute(null, ATTRIBUTE_META, String.valueOf(((info.getShortcutKeyCode() >> 32) & 65536) == 65536));
            out.attribute(null, ATTRIBUTE_SHIFT, String.valueOf(((info.getShortcutKeyCode() >> 32) & 1) == 1));
            out.attribute(null, ATTRIBUTE_ALT, String.valueOf(((info.getShortcutKeyCode() >> 32) & 2) == 2));
            out.attribute(null, ATTRIBUTE_CTRL, String.valueOf(((info.getShortcutKeyCode() >> 32) & 4096) == 4096));
            out.attribute(null, ATTRIBUTE_RIGHT_ALT, String.valueOf(((info.getShortcutKeyCode() >> 32) & 32) == 32));
            if (((info.getShortcutKeyCode() >> 32) & 16) != 16) {
                z = false;
            }
            out.attribute(null, ATTRIBUTE_LEFT_ALT, String.valueOf(z));
            out.attribute(null, ATTRIBUTE_KEYCODE, String.valueOf(info.getShortcutKeyCode() & 65535));
            out.attribute(null, ATTRIBUTE_ENABLE, "" + info.isEnable());
            out.attribute(null, ATTRIBUTE_TYPE, "" + info.getType());
            if (info.getType() == 0) {
                out.attribute(null, "packageName", "" + info.getPackageName());
                out.attribute(null, ATTRIBUTE_CLASSNAME, "" + info.getClassName());
            }
            out.attribute(null, ATTRIBUTE_CUSTOMIZED, "" + info.isCustomized());
            out.endTag(null, "ShortcutInfo");
        } catch (Exception e) {
            Slog.d("MiuiCustomizeShortCutUtils", e.toString());
        }
    }

    /* loaded from: classes.dex */
    public class MiuiKeyboardShortcutInfo implements Comparable {
        private String mClassName;
        private boolean mCustomized;
        private boolean mEnable;
        private long mHistoryKeyCode;
        private String mPackageName;
        private long mShortcutKeyCode;
        private int mType;

        public MiuiKeyboardShortcutInfo() {
        }

        public MiuiKeyboardShortcutInfo(long shortcutKeyCode, boolean enable, int type, boolean customized) {
            this.mShortcutKeyCode = shortcutKeyCode;
            this.mEnable = enable;
            this.mType = type;
            this.mCustomized = customized;
        }

        public MiuiKeyboardShortcutInfo(KeyboardShortcutInfo info) {
        }

        public void setShortcutKeyCode(long shortcutKeyCode) {
            this.mShortcutKeyCode = shortcutKeyCode;
        }

        public void setAppInfo(String packageName, String className) {
            this.mPackageName = packageName;
            this.mClassName = className;
        }

        public void setHistoryKeyCode(long historyKeyCode) {
            this.mHistoryKeyCode = historyKeyCode;
        }

        public long getShortcutKeyCode() {
            return this.mShortcutKeyCode;
        }

        public boolean isEnable() {
            return this.mEnable;
        }

        public boolean isCustomized() {
            return this.mCustomized;
        }

        public int getType() {
            return this.mType;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public String getClassName() {
            return this.mClassName;
        }

        public long getHistoryKeyCode() {
            return this.mHistoryKeyCode;
        }

        private String getShortcutLabel() {
            if (this.mShortcutKeyCode == 0) {
                return "";
            }
            long keyCode = this.mShortcutKeyCode;
            StringBuilder shortcutLabel = new StringBuilder();
            if ((281474976710656L & keyCode) != 0) {
                keyCode &= -281474976710657L;
                shortcutLabel.append("meta ");
            }
            if ((8589934592L & keyCode) != 0) {
                keyCode &= -8589934593L;
                shortcutLabel.append("alt ");
            }
            if ((17592186044416L & keyCode) != 0) {
                keyCode &= -17592186044417L;
                shortcutLabel.append("ctrl ");
            }
            if ((4294967296L & keyCode) != 0) {
                keyCode &= -4294967297L;
                shortcutLabel.append("shift ");
            }
            shortcutLabel.append(keyCode);
            return shortcutLabel.toString();
        }

        public String toString() {
            return "MiuiKeyboardShortcutInfo{mShortcutKeyCode=" + getShortcutLabel() + ", mEnable=" + this.mEnable + ", mType=" + this.mType + ", mPackageName='" + this.mPackageName + "', mClassName='" + this.mClassName + "', mHistoryKeyCode=" + this.mHistoryKeyCode + ", mCustomized=" + this.mCustomized + '}';
        }

        @Override // java.lang.Comparable
        public int compareTo(Object o) {
            MiuiKeyboardShortcutInfo info = (MiuiKeyboardShortcutInfo) o;
            int i = this.mType;
            int i2 = info.mType;
            if (i - i2 < 0) {
                return 1;
            }
            if (i - i2 > 0) {
                return -1;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ReflectionUtil {
        private Method mGetClassName;
        private Method mGetHistoryKeyCode;
        private Method mGetPackageName;
        private Method mGetShortcutKeyCode;
        private Method mGetType;
        private Method mIsCustomized;
        private Method mIsEnable;
        private Method mSetAppInfo;
        private Method mSetCustomized;
        private Method mSetEnable;
        private Method mSetShortcutKeyCode;
        private Method mSetType;

        ReflectionUtil() {
            init();
        }

        public void init() {
            try {
                this.mGetShortcutKeyCode = KeyboardShortcutInfo.class.getMethod("getShortcutKeyCode", new Class[0]);
                this.mIsEnable = KeyboardShortcutInfo.class.getMethod("isActive", new Class[0]);
                this.mGetType = KeyboardShortcutInfo.class.getMethod("getType", new Class[0]);
                this.mGetPackageName = KeyboardShortcutInfo.class.getMethod("getPackageName", new Class[0]);
                this.mGetClassName = KeyboardShortcutInfo.class.getMethod("getClassName", new Class[0]);
                this.mGetHistoryKeyCode = KeyboardShortcutInfo.class.getMethod("getHistoryKeyCode", new Class[0]);
                this.mIsCustomized = KeyboardShortcutInfo.class.getMethod("isCustomized", new Class[0]);
                this.mSetShortcutKeyCode = KeyboardShortcutInfo.class.getMethod("setShortcutKeyCode", Long.TYPE);
                this.mSetEnable = KeyboardShortcutInfo.class.getMethod("setActive", Boolean.TYPE);
                this.mSetType = KeyboardShortcutInfo.class.getMethod("setType", Integer.TYPE);
                this.mSetAppInfo = KeyboardShortcutInfo.class.getMethod("setAppInfo", String.class, String.class);
                this.mSetCustomized = KeyboardShortcutInfo.class.getMethod("setCustomized", Boolean.TYPE);
            } catch (Exception e) {
                Slog.e("MiuiCustomizeShortCutUtils", e.toString());
            }
        }

        public MiuiKeyboardShortcutInfo invokeObject(KeyboardShortcutInfo rawInfo) {
            MiuiKeyboardShortcutInfo resultInfo = null;
            try {
                resultInfo = new MiuiKeyboardShortcutInfo(((Long) this.mGetShortcutKeyCode.invoke(rawInfo, new Object[0])).longValue(), ((Boolean) this.mIsEnable.invoke(rawInfo, new Object[0])).booleanValue(), ((Integer) this.mGetType.invoke(rawInfo, new Object[0])).intValue(), ((Boolean) this.mIsCustomized.invoke(rawInfo, new Object[0])).booleanValue());
                resultInfo.setAppInfo((String) this.mGetPackageName.invoke(rawInfo, new Object[0]), (String) this.mGetClassName.invoke(rawInfo, new Object[0]));
                resultInfo.setHistoryKeyCode(((Long) this.mGetHistoryKeyCode.invoke(rawInfo, new Object[0])).longValue());
                return resultInfo;
            } catch (Exception e) {
                Slog.i("MiuiCustomizeShortCutUtils", e.toString());
                return resultInfo;
            }
        }

        public KeyboardShortcutInfo reflectObject(MiuiKeyboardShortcutInfo rawInfo) {
            KeyboardShortcutInfo resultInfo = transformKeycode(rawInfo);
            try {
                this.mSetShortcutKeyCode.invoke(resultInfo, Long.valueOf(rawInfo.mShortcutKeyCode));
                this.mSetEnable.invoke(resultInfo, Boolean.valueOf(rawInfo.mEnable));
                this.mSetType.invoke(resultInfo, Integer.valueOf(rawInfo.mType));
                if (rawInfo.getType() == 0) {
                    this.mSetAppInfo.invoke(resultInfo, rawInfo.mPackageName, rawInfo.mClassName);
                }
                this.mSetCustomized.invoke(resultInfo, Boolean.valueOf(rawInfo.mCustomized));
            } catch (Exception e) {
                Slog.e("MiuiCustomizeShortCutUtils", e.toString());
            }
            return resultInfo;
        }

        private KeyboardShortcutInfo transformKeycode(MiuiKeyboardShortcutInfo rawInfo) {
            String label;
            int keyCode = (int) (rawInfo.getShortcutKeyCode() & 65535);
            int metaStaus = ((rawInfo.getShortcutKeyCode() >> 32) & 65536) == 65536 ? 0 | 65536 : 0;
            if (((rawInfo.getShortcutKeyCode() >> 32) & 1) == 1) {
                metaStaus |= 1;
            }
            if (((rawInfo.getShortcutKeyCode() >> 32) & 2) == 2) {
                metaStaus |= 2;
            }
            if (((rawInfo.getShortcutKeyCode() >> 32) & 4096) == 4096) {
                metaStaus |= 4096;
            }
            if (rawInfo.getType() == 0) {
                label = getAppName(rawInfo.getPackageName());
            } else {
                label = getSystemLabel(rawInfo);
            }
            return new KeyboardShortcutInfo(label, keyCode, metaStaus);
        }

        private String getAppName(String packageName) {
            PackageManager packageManager = MiuiCustomizeShortCutUtils.this.mContext.getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 128);
                if (applicationInfo == null) {
                    return "";
                }
                return (String) packageManager.getApplicationLabel(applicationInfo);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private String getSystemLabel(MiuiKeyboardShortcutInfo rawInfo) {
            switch (rawInfo.getType()) {
                case 1:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196311);
                case 2:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196312);
                case 3:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196313);
                case 4:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196307);
                case 5:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196316);
                case 6:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196318);
                case 7:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196317);
                case 8:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196308);
                case 9:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196310);
                case 10:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196309);
                case 11:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196314);
                case 12:
                    return MiuiCustomizeShortCutUtils.this.mContext.getResources().getString(286196315);
                default:
                    return null;
            }
        }
    }
}
