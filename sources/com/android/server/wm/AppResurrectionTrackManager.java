package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.miui.analytics.ITrackBinder;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AppResurrectionTrackManager {
    private static final String APP_ID = "31000401629";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String PACKAGE_NAME = "android";
    private static final String SERVICE_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    private static final String TAG = "AppResurrectionTrackManager";
    private boolean DEBUG = false;
    private final ServiceConnection mConnection = new ServiceConnection() { // from class: com.android.server.wm.AppResurrectionTrackManager.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppResurrectionTrackManager.this.mITrackBinder = ITrackBinder.Stub.asInterface(service);
            Slog.d(AppResurrectionTrackManager.TAG, "onServiceConnected: " + AppResurrectionTrackManager.this.mITrackBinder);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            AppResurrectionTrackManager.this.mITrackBinder = null;
            AppResurrectionTrackManager.this.mIsBind = false;
            Slog.d(AppResurrectionTrackManager.TAG, "onServiceDisconnected");
        }
    };
    private final Context mContext;
    private ITrackBinder mITrackBinder;
    private boolean mIsBind;

    public AppResurrectionTrackManager(Context context) {
        this.mContext = context;
    }

    public void bindTrackBinder() {
        if (this.mITrackBinder == null) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.miui.analytics", SERVICE_NAME);
                this.mIsBind = this.mContext.bindServiceAsUser(intent, this.mConnection, 1, UserHandle.CURRENT);
            } catch (Exception e) {
                e.printStackTrace();
                Slog.e(TAG, "Bind Service Exception");
            }
        }
    }

    public void unbindTrackBinder() {
        if (this.mIsBind) {
            try {
                this.mContext.unbindService(this.mConnection);
            } catch (Exception e) {
                e.printStackTrace();
                Slog.e(TAG, "Unbind Service Exception");
            }
        }
    }

    public void sendTrack(String pkg, long launchTime, String killReason) {
        sendTrack(pkg, "", launchTime, killReason);
    }

    public void sendTrack(String pkg, String pkgVersion, long launchTime, String killReason) {
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "event_appres_launch_time");
            jsonData.put("pkg_name", pkg);
            jsonData.put("pkg_version", pkgVersion);
            jsonData.put("kill_reason", killReason);
            jsonData.put("launch_time_num", launchTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        trackEvent(jsonData);
    }

    private void trackEvent(JSONObject jsonData) {
        ITrackBinder iTrackBinder = this.mITrackBinder;
        if (iTrackBinder == null) {
            Slog.d(TAG, "TrackService Not Bound,Please Try Again");
            bindTrackBinder();
        } else {
            if (jsonData == null) {
                Slog.w(TAG, "JSONObject is null");
                return;
            }
            try {
                iTrackBinder.trackEvent(APP_ID, PACKAGE_NAME, jsonData.toString(), 3);
                if (this.DEBUG) {
                    Slog.i(TAG, "Send to TrackService Success:" + jsonData.toString());
                } else {
                    Slog.v(TAG, "Binder to Success");
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "trackEvent: " + e.getMessage());
            }
        }
    }
}
