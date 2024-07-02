package com.miui.server.xspace;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.android.server.am.ActivityManagerService;

/* loaded from: classes.dex */
class SecondSpaceSwitchingDialog extends BaseUserSwitchingDialog {
    public SecondSpaceSwitchingDialog(ActivityManagerService service, Context context, int userId) {
        super(service, context, 286261275, userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.miui.server.xspace.BaseUserSwitchingDialog, android.app.AlertDialog, android.app.Dialog
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window win = getWindow();
        win.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = -1;
        lp.height = -1;
        lp.layoutInDisplayCutoutMode = 1;
        win.setAttributes(lp);
        win.getDecorView().setSystemUiVisibility(3846);
        View view = LayoutInflater.from(getContext()).inflate(285999172, (ViewGroup) null);
        setContentView(view);
    }
}
