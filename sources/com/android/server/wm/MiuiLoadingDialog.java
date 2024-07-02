package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.WindowManager;
import miuix.appcompat.app.ProgressDialog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class MiuiLoadingDialog extends ProgressDialog {
    private static final String TAG = "MiuiLoadingDialog";
    private Handler mHandler;
    private BroadcastReceiver mReceiver;

    public MiuiLoadingDialog(Context context) {
        this(context, 1712390150);
    }

    public MiuiLoadingDialog(Context context, int theme) {
        super(context, theme);
        this.mHandler = new Handler();
        setCancelable(false);
        setProgressStyle(0);
        getWindow().setType(2003);
        getWindow().setFlags(131072, 131072);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.gravity = MiuiMultiWindowUtils.isPadScreen(context) ? 17 : 80;
        getWindow().setAttributes(attrs);
        String message = context.getResources().getString(286196363);
        setMessage(message);
    }

    public void onStart() {
        super.onStart();
        if (this.mReceiver == null) {
            this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.MiuiLoadingDialog.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                        MiuiLoadingDialog.this.closeDialog();
                    }
                }
            };
            getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), null, this.mHandler, 2);
        }
    }

    protected void onStop() {
        super.onStop();
        if (this.mReceiver != null) {
            try {
                getContext().unregisterReceiver(this.mReceiver);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "unregisterReceiver threw exception: " + e.getMessage());
            }
            this.mReceiver = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeDialog() {
        dismiss();
    }
}
