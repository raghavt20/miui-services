package com.android.server.audio;

import android.media.AudioSystem;
import android.util.Log;
import android.view.IDisplayFoldListener;
import android.view.WindowManagerGlobal;

/* loaded from: classes.dex */
public class FoldHelper {
    private static final String TAG = "AudioService.FoldHelper";
    private static AudioFoldListener sFoldListener;

    private FoldHelper() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void init() {
        sFoldListener = new AudioFoldListener();
        enable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enable() {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDisplayFoldListener(sFoldListener);
        } catch (Exception e) {
            Log.e(TAG, "registerDisplayFoldListener error " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void disable() {
        if (sFoldListener != null) {
            try {
                WindowManagerGlobal.getWindowManagerService().unregisterDisplayFoldListener(sFoldListener);
            } catch (Exception e) {
                Log.e(TAG, "unregisterDisplayFoldListener error " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AudioFoldListener extends IDisplayFoldListener.Stub {
        AudioFoldListener() {
        }

        public void onDisplayFoldChanged(int displayId, boolean folded) {
            Log.v(FoldHelper.TAG, "publishing device fold=" + folded);
            AudioSystem.setParameters("fold=" + folded);
        }
    }
}
