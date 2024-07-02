package com.android.server.autofill.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import miuix.appcompat.app.AlertDialog;

@MiuiStubHead(manifestName = "com.android.server.autofill.ui.SaveUiStub$$")
/* loaded from: classes.dex */
public class SaveUiStubImpl extends SaveUiStub {
    private static final String AUTOFILL_ACTIVITY = "com.miui.contentcatcher.autofill.activitys.AutofillSettingActivity";
    private static final String AUTOFILL_PACKAGE = "com.miui.contentcatcher";
    private static final String AUTO_CANCEL = "auto_cancel";
    private static final String AUTO_SAVE = "auto_save";
    private static int MIUI_VERSION_CODE = SystemProperties.getInt("ro.miui.ui.version.code", 13);
    private static final String NEVER_SHOW_SAVE_UI = "never_show_save_ui";
    private static final String SAVEUI_ACTION = "intent.action.saveui";
    private static final String SERVICE_SP_NAME = "multi_process";
    private static final String TAG = "AutofillSaveUi";
    private static AlertDialog mDialog;
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SaveUiStubImpl> {

        /* compiled from: SaveUiStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SaveUiStubImpl INSTANCE = new SaveUiStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SaveUiStubImpl m856provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SaveUiStubImpl m855provideNewInstance() {
            return new SaveUiStubImpl();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x016e  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0182  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.app.Dialog showDialog(android.content.Context r24, com.android.server.autofill.ui.OverlayControl r25, final android.content.DialogInterface.OnClickListener r26, final android.content.DialogInterface.OnClickListener r27, final android.content.DialogInterface.OnDismissListener r28) {
        /*
            Method dump skipped, instructions count: 402
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.autofill.ui.SaveUiStubImpl.showDialog(android.content.Context, com.android.server.autofill.ui.OverlayControl, android.content.DialogInterface$OnClickListener, android.content.DialogInterface$OnClickListener, android.content.DialogInterface$OnDismissListener):android.app.Dialog");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$0(DialogInterface.OnClickListener okListener, DialogInterface v, int w) {
        Log.d(TAG, "showDialog  save");
        okListener.onClick(null, 0);
        setDialog(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$1(DialogInterface.OnClickListener cancelListener, DialogInterface v, int w) {
        Log.d(TAG, "showDialog  cancel");
        cancelListener.onClick(null, 0);
        setDialog(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$2(DialogInterface.OnDismissListener onDismissListener, DialogInterface v) {
        Log.d(TAG, "showDialog  dismiss");
        onDismissListener.onDismiss(null);
        setDialog(null);
    }

    public void changeBackground(View decor, WindowManager.LayoutParams params) {
        if (decor == null || params == null) {
            return;
        }
        String autofillService = Settings.Secure.getStringForUser(decor.getContext().getContentResolver(), "autofill_service", UserHandle.myUserId());
        if (!TextUtils.isEmpty(autofillService)) {
            String packageName = autofillService.split("/")[0];
            if (TextUtils.equals(packageName, AUTOFILL_PACKAGE)) {
                decor.setBackgroundResource(285737826);
                if (MIUI_VERSION_CODE > 10) {
                    params.x -= 40;
                    params.y -= 80;
                    params.width += 100;
                    params.height += 160;
                    return;
                }
                params.x -= 60;
                params.y -= 60;
                params.width += 120;
                params.height += 120;
            }
        }
    }

    public void autoSave() {
        if (mDialog != null) {
            Log.d(TAG, "autoSave  checked=true");
            try {
                Context serviceContext = mDialog.getContext().createPackageContext(AUTOFILL_PACKAGE, 3);
                SharedPreferences sharedPreferences = serviceContext.getSharedPreferences(SERVICE_SP_NAME, 4);
                sharedPreferences.edit().putBoolean(NEVER_SHOW_SAVE_UI, true).apply();
                sharedPreferences.edit().putBoolean(AUTO_SAVE, true).apply();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void autoCancel() {
        if (mDialog != null) {
            Log.d(TAG, "autoCancel  checked=true");
            try {
                Context serviceContext = mDialog.getContext().createPackageContext(AUTOFILL_PACKAGE, 3);
                SharedPreferences sharedPreferences = serviceContext.getSharedPreferences(SERVICE_SP_NAME, 4);
                sharedPreferences.edit().putBoolean(NEVER_SHOW_SAVE_UI, true).apply();
                sharedPreferences.edit().putBoolean(AUTO_CANCEL, true).apply();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void setViewPadding(Dialog dialog) {
        Window window = dialog.getWindow();
        int id = this.mContext.getResources().getIdentifier("customPanel", "id", InputMethodManagerServiceImpl.MIUIXPACKAGE);
        ViewGroup customPanel = (ViewGroup) window.findViewById(id);
        if (customPanel != null) {
            customPanel.setPadding(80, 0, 80, 0);
        }
    }

    public static synchronized void setDialog(AlertDialog dialog) {
        synchronized (SaveUiStubImpl.class) {
            mDialog = dialog;
        }
    }
}
