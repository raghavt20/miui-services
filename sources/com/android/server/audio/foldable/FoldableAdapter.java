package com.android.server.audio.foldable;

import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.audio.foldable.FoldStateHelper;

/* loaded from: classes.dex */
public class FoldableAdapter implements FoldStateHelper.AngleChangedListener {
    private static final int CONFIG_PROP;
    public static final boolean ENABLE;
    private static final int FLAG_FOLDABLE_ADAPTER_ENABLE = 1;
    private static final int FLAG_FOLD_STATE_HELP_ENABLE = 2;
    private static final String TAG = "AudioService.FoldableAdapter";
    private final boolean FOLD_STATE_HELP_ENABLE;
    private int mFoldAngle;
    private FoldStateHelper mFoldStateHelper;

    static {
        int i = SystemProperties.getInt("ro.vendor.audio.foldable.adapter", 0);
        CONFIG_PROP = i;
        ENABLE = (i & 1) != 0;
    }

    public FoldableAdapter(Context context, Looper looper) {
        boolean z = (CONFIG_PROP & 2) != 0;
        this.FOLD_STATE_HELP_ENABLE = z;
        Log.d(TAG, "FoldableAdapter Construct ...");
        if (z) {
            FoldStateHelper foldStateHelper = new FoldStateHelper(context, looper);
            this.mFoldStateHelper = foldStateHelper;
            foldStateHelper.setAngleChangedListener(this);
        }
    }

    @Override // com.android.server.audio.foldable.FoldStateHelper.AngleChangedListener
    public void onAngleChanged(int foldAngle) {
        this.mFoldAngle = foldAngle;
    }

    public void onUpdateAudioMode(int mode) {
        if (this.FOLD_STATE_HELP_ENABLE) {
            this.mFoldStateHelper.onUpdateAudioMode(mode);
        }
    }

    public void onUpdateMediaState(boolean mediaActive) {
        if (this.FOLD_STATE_HELP_ENABLE) {
            this.mFoldStateHelper.onUpdateMediaState(mediaActive);
        }
    }

    public void onAudioServerDied() {
        if (this.FOLD_STATE_HELP_ENABLE) {
            this.mFoldStateHelper.onAudioServerDied();
        }
    }
}
