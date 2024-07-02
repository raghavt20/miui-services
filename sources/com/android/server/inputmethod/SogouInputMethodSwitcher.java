package com.android.server.inputmethod;

import android.content.ComponentName;
import android.text.TextUtils;
import android.util.Slog;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class SogouInputMethodSwitcher extends BaseInputMethodSwitcher {
    public static final boolean DEBUG = true;
    private static final String PKG_SOGOU = "com.sohu.inputmethod.sogou.xiaomi";
    public static final String TAG = "SogouInputMethodSwitcher";

    public void onSystemRunningLocked() {
    }

    public void onSwitchUserLocked(int newUserId) {
    }

    public boolean mayChangeInputMethodLocked(EditorInfo attribute) {
        if (!DeviceUtils.isFlipDevice() || isPasswdInputType(attribute.inputType)) {
            return false;
        }
        if (this.mService.getSelectedMethodIdLocked() == null) {
            Slog.w(TAG, "input_service has no current_method_id");
            return false;
        }
        if (attribute != null) {
            InputMethodInfo curMethodInfo = (InputMethodInfo) this.mService.mMethodMap.get(this.mService.getSelectedMethodIdLocked());
            if (curMethodInfo == null) {
                Slog.w(TAG, "fail to find current_method_info in the map");
                return false;
            }
            if (InputMethodManagerServiceImpl.getInstance().mBindingController == null) {
                Slog.w(TAG, "IMMS_IMPL has not init");
                return false;
            }
            String sogouMethodId = getSogouMethodIdLocked();
            if (TextUtils.isEmpty(sogouMethodId)) {
                Slog.w(TAG, "fail to find sogou_input_method in input_method_list");
                return false;
            }
            boolean editorInDefaultImeApp = isEditorInDefaultImeApp(attribute);
            if (!editorInDefaultImeApp) {
                boolean isSogouInputNow = isSogouMethodLocked(this.mService.getSelectedMethodIdLocked());
                boolean isFlipTinyScreen = DeviceUtils.isFlipTinyScreen(this.mService.mContext);
                boolean switchToSogouInput = !isSogouInputNow && isFlipTinyScreen;
                boolean switchFromSogouInput = isSogouInputNow && !isFlipTinyScreen;
                if (switchToSogouInput) {
                    InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(sogouMethodId);
                    this.mService.clearClientSessionsLocked();
                    this.mService.unbindCurrentClientLocked(2);
                    InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                    this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                    return true;
                }
                if (!switchFromSogouInput) {
                    return false;
                }
                String selectedInputMethod = this.mService.mSettings.getSelectedInputMethod();
                if (TextUtils.isEmpty(selectedInputMethod)) {
                    Slog.w(TAG, "something is weired, maybe the input method app are uninstalled");
                    InputMethodInfo imi = InputMethodInfoUtils.getMostApplicableDefaultIME(this.mService.mSettings.getEnabledInputMethodListLocked());
                    if (imi == null || TextUtils.equals(imi.getPackageName(), PKG_SOGOU)) {
                        Slog.w(TAG, "fail to find a most applicable default ime");
                        List<InputMethodInfo> imiList = this.mService.mSettings.getEnabledInputMethodListLocked();
                        if (imiList == null || imiList.size() == 0) {
                            Slog.w(TAG, "there is no enabled method list");
                            return false;
                        }
                        Iterator<InputMethodInfo> it = imiList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            InputMethodInfo inputMethodInfo = it.next();
                            if (!TextUtils.equals(inputMethodInfo.getPackageName(), PKG_SOGOU)) {
                                selectedInputMethod = inputMethodInfo.getId();
                                break;
                            }
                        }
                    }
                }
                if (TextUtils.isEmpty(selectedInputMethod)) {
                    Slog.w(TAG, "finally, we still fail to find default input method");
                    return false;
                }
                if (TextUtils.equals(this.mService.getSelectedMethodIdLocked(), selectedInputMethod)) {
                    Slog.w(TAG, "It looks like there is sogou_input_method in the system");
                    return false;
                }
                InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(selectedInputMethod);
                this.mService.clearClientSessionsLocked();
                this.mService.unbindCurrentClientLocked(2);
                InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                return true;
            }
            Slog.w(TAG, "editor in default ime app");
            return false;
        }
        Slog.w(TAG, "editor_info is null, we cannot judge");
        return false;
    }

    public boolean shouldHideImeSwitcherLocked() {
        if (DeviceUtils.isFlipTinyScreen(this.mService.mContext)) {
            return isSogouMethodLocked(this.mService.getSelectedMethodIdLocked()) || (InputMethodManagerServiceImpl.getInstance().mBindingController != null && InputMethodManagerServiceImpl.getInstance().mBindingController.getCurMethod() == null);
        }
        return false;
    }

    public void removeMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        if (!DeviceUtils.isFlipTinyScreen(this.mService.mContext) || imList == null || imList.size() == 0) {
            return;
        }
        Iterator<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> iterator = imList.iterator();
        while (iterator.hasNext()) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem = iterator.next();
            InputMethodInfo imi = imeSubtypeListItem.mImi;
            if (TextUtils.equals(imi.getPackageName(), PKG_SOGOU)) {
                iterator.remove();
            }
        }
    }

    public List<InputMethodInfo> filterMethodLocked(List<InputMethodInfo> methodInfos) {
        return methodInfos;
    }

    private String getSogouMethodIdLocked() {
        for (Map.Entry<String, InputMethodInfo> entry : this.mService.mMethodMap.entrySet()) {
            if (isSogouMethodLocked(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isSogouMethodLocked(String methodId) {
        InputMethodInfo imi = (InputMethodInfo) this.mService.mMethodMap.get(methodId);
        return imi != null && TextUtils.equals(imi.getPackageName(), PKG_SOGOU);
    }

    private boolean isEditorInDefaultImeApp(EditorInfo editor) {
        ComponentName cn;
        String pkg = editor.packageName;
        String defaultIme = this.mService.mSettings.getSelectedInputMethod();
        return (TextUtils.isEmpty(defaultIme) || (cn = ComponentName.unflattenFromString(defaultIme)) == null || !TextUtils.equals(pkg, cn.getPackageName())) ? false : true;
    }
}
