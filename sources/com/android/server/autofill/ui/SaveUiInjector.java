package com.android.server.autofill.ui;

import android.app.AlertDialog;
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
import android.view.WindowManager;

/* loaded from: classes.dex */
final class SaveUiInjector {
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

    private SaveUiInjector() {
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x015c  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x016f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static android.app.Dialog showDialog(android.content.Context r22, com.android.server.autofill.ui.OverlayControl r23, final android.content.DialogInterface.OnClickListener r24, final android.content.DialogInterface.OnClickListener r25, final android.content.DialogInterface.OnDismissListener r26) {
        /*
            Method dump skipped, instructions count: 383
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.autofill.ui.SaveUiInjector.showDialog(android.content.Context, com.android.server.autofill.ui.OverlayControl, android.content.DialogInterface$OnClickListener, android.content.DialogInterface$OnClickListener, android.content.DialogInterface$OnDismissListener):android.app.Dialog");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$0(DialogInterface.OnClickListener okListener, DialogInterface v, int w) {
        Log.d(TAG, "showDialog  save");
        okListener.onClick(null, 0);
        autoSave();
        mDialog = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$1(DialogInterface.OnClickListener cancelListener, DialogInterface v, int w) {
        Log.d(TAG, "showDialog  cancel");
        cancelListener.onClick(null, 0);
        autoCancel();
        mDialog = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showDialog$2(DialogInterface.OnDismissListener onDismissListener, DialogInterface v) {
        Log.d(TAG, "showDialog  dismiss");
        onDismissListener.onDismiss(null);
        mDialog = null;
    }

    public static void changeBackground(View decor, WindowManager.LayoutParams params) {
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

    public static void autoSave() {
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

    public static void autoCancel() {
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
}
