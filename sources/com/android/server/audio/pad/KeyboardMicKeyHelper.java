package com.android.server.audio.pad;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import miuix.appcompat.app.AlertDialog;

/* loaded from: classes.dex */
public class KeyboardMicKeyHelper {
    private static final int MSG_MIC_MUTE_CHANGED = 1;
    private static final int MSG_MIC_SRC_RECORD_UPDATE = 2;
    private static final int STATE_PENDING_MUTED = 1;
    private static final int STATE_PENDING_NONE = 0;
    private static final int STATE_PENDING_UNMUTED = 2;
    private static final String TAG = "PadAdapter.KeyboardMicKeyHelper";
    private AudioManager mAudioManager;
    private Context mContext;
    private AlertDialog mDialog;
    private Handler mHandler;
    private int mStatePending;
    private Toast mToast;
    private volatile boolean mToastShowing;
    private Object mToastStateLock = new Object();
    private Object mDialogStateLock = new Object();

    public KeyboardMicKeyHelper(Context context) {
        Log.d(TAG, "KeyboardMicKeyHelper Construct ...");
        this.mContext = context;
        this.mHandler = new H(context.getMainLooper());
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
    }

    public void handleMicrophoneMuteChanged(boolean muted) {
        Log.d(TAG, "handleMicrophoneMuteChanged(), muted : " + muted);
        this.mHandler.obtainMessage(1, Boolean.valueOf(muted)).sendToTarget();
    }

    public void handleRecordEventUpdate(int audioSource) {
        Log.d(TAG, "handleRecordEventUpdate(), audio source : " + audioSource);
        if (isMicrophoneAudioSource(audioSource)) {
            this.mHandler.obtainMessage(2).sendToTarget();
        }
    }

    private void initToast() {
        Toast toast = new Toast(this.mContext);
        this.mToast = toast;
        toast.setDuration(1);
        this.mToast.addCallback(new Toast.Callback() { // from class: com.android.server.audio.pad.KeyboardMicKeyHelper.1
            @Override // android.widget.Toast.Callback
            public void onToastShown() {
                synchronized (KeyboardMicKeyHelper.this.mToastStateLock) {
                    Log.d(KeyboardMicKeyHelper.TAG, "onToastShown() ...");
                    KeyboardMicKeyHelper.this.mToastShowing = true;
                }
            }

            @Override // android.widget.Toast.Callback
            public void onToastHidden() {
                synchronized (KeyboardMicKeyHelper.this.mToastStateLock) {
                    Log.d(KeyboardMicKeyHelper.TAG, "onToastHidden() ...");
                    KeyboardMicKeyHelper.this.mToastShowing = false;
                    if (KeyboardMicKeyHelper.this.mStatePending != 0) {
                        Log.d(KeyboardMicKeyHelper.TAG, "show toast pending ...");
                        Toast toast2 = KeyboardMicKeyHelper.this.mToast;
                        KeyboardMicKeyHelper keyboardMicKeyHelper = KeyboardMicKeyHelper.this;
                        boolean z = true;
                        if (keyboardMicKeyHelper.mStatePending != 1) {
                            z = false;
                        }
                        toast2.setText(keyboardMicKeyHelper.getToastString(z));
                        KeyboardMicKeyHelper.this.mStatePending = 0;
                        KeyboardMicKeyHelper.this.mToast.show();
                    }
                }
            }
        });
    }

    private AlertDialog createDialog() {
        ContextImpl systemUiContext = ActivityThread.currentActivityThread().getSystemUiContext();
        int themeId = systemUiContext.getResources().getIdentifier("AlertDialog.Theme.DayNight", "style", InputMethodManagerServiceImpl.MIUIXPACKAGE);
        AlertDialog dialog = new AlertDialog.Builder(systemUiContext, themeId).setTitle(getString(286196173)).setMessage(getString(286196172)).setPositiveButton(getString(286196171), new DialogInterface.OnClickListener() { // from class: com.android.server.audio.pad.KeyboardMicKeyHelper.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int which) {
                KeyboardMicKeyHelper.this.mAudioManager.setMicrophoneMute(false);
            }
        }).setNegativeButton(getString(286196170), new DialogInterface.OnClickListener() { // from class: com.android.server.audio.pad.KeyboardMicKeyHelper.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int which) {
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.android.server.audio.pad.KeyboardMicKeyHelper.4
            @Override // android.content.DialogInterface.OnShowListener
            public void onShow(DialogInterface dialog2) {
                Log.d(KeyboardMicKeyHelper.TAG, "dialog onShow()");
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.server.audio.pad.KeyboardMicKeyHelper.5
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialog2) {
                Log.d(KeyboardMicKeyHelper.TAG, "dialog onDismiss()");
                synchronized (KeyboardMicKeyHelper.this.mDialogStateLock) {
                    KeyboardMicKeyHelper.this.mDialog = null;
                }
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setType(2003);
        return dialog;
    }

    void showMicMuteChanged(boolean muted) {
        Log.d(TAG, "showMicMuteChanged muted : " + muted);
        if (this.mToast == null) {
            initToast();
        }
        synchronized (this.mToastStateLock) {
            if (this.mToastShowing) {
                this.mStatePending = muted ? 1 : 2;
                this.mToast.cancel();
            } else {
                this.mStatePending = 0;
                this.mToast.setText(getToastString(muted));
                this.mToast.show();
            }
        }
        if (!muted) {
            dismissEnableMicDialogIfNeed();
        }
    }

    void showEnableMicDialog() {
        Log.d(TAG, "showEnableMicDialog() ...");
        synchronized (this.mDialogStateLock) {
            AlertDialog alertDialog = this.mDialog;
            if (alertDialog == null || !alertDialog.isShowing()) {
                AlertDialog createDialog = createDialog();
                this.mDialog = createDialog;
                createDialog.show();
            }
        }
    }

    private void dismissEnableMicDialogIfNeed() {
        Log.d(TAG, "dismissEnableMicDialogIfNeed() ...");
        synchronized (this.mDialogStateLock) {
            AlertDialog alertDialog = this.mDialog;
            if (alertDialog != null && alertDialog.isShowing()) {
                this.mDialog.dismiss();
            }
        }
    }

    private boolean isMicrophoneAudioSource(int source) {
        switch (source) {
            case 0:
            case 1:
            case 5:
            case 6:
            case 7:
            case 9:
            case 10:
                return true;
            case 2:
            case 3:
            case 4:
            case 8:
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getToastString(boolean muted) {
        return muted ? getString(286196677) : getString(286196678);
    }

    private String getString(int id) {
        return this.mContext.getResources().getString(id);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    KeyboardMicKeyHelper.this.showMicMuteChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 2:
                    if (KeyboardMicKeyHelper.this.mAudioManager.isMicrophoneMute()) {
                        KeyboardMicKeyHelper.this.showEnableMicDialog();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
