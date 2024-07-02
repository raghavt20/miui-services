package com.miui.server.smartpower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.view.Display;
import com.miui.app.smartpower.SmartPowerSettings;
import com.miui.server.smartpower.AppPowerResource;
import java.util.ArrayList;
import java.util.Arrays;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AppDisplayResource extends AppPowerResource implements DisplayManager.DisplayListener {
    private DisplayManager mDms;
    private Handler mHandler;
    private final BroadcastReceiver mWifiP2pReceiver;
    private final ArrayMap<Integer, DisplayRecord> mDisplayRecordMap = new ArrayMap<>();
    private ArraySet<String> mCastingWhitelists = new ArraySet<>();
    private boolean mWifiP2pConnected = false;

    public AppDisplayResource(Context context, Looper looper) {
        this.mDms = null;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.smartpower.AppDisplayResource.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                NetworkInfo networkInfo;
                String action = intent.getAction();
                if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE") && (networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo")) != null) {
                    if (AppPowerResource.DEBUG) {
                        Slog.d(AppPowerResourceManager.TAG, "Received WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=" + networkInfo);
                    }
                    boolean connected = networkInfo.isConnected();
                    if (connected != AppDisplayResource.this.mWifiP2pConnected) {
                        AppDisplayResource.this.mWifiP2pConnected = connected;
                        synchronized (AppDisplayResource.this.mDisplayRecordMap) {
                            for (DisplayRecord record : AppDisplayResource.this.mDisplayRecordMap.values()) {
                                record.updateCurrentStatus();
                            }
                        }
                    }
                }
            }
        };
        this.mWifiP2pReceiver = broadcastReceiver;
        this.mType = 7;
        this.mHandler = new Handler(looper);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        this.mDms = displayManager;
        displayManager.registerDisplayListener(this, new Handler(looper));
        String[] packges = context.getResources().getStringArray(285409484);
        this.mCastingWhitelists.addAll(Arrays.asList(packges));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void init() {
        initializeCurrentDisplays();
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void registerCallback(AppPowerResource.IAppPowerResourceCallback callback, final int uid) {
        super.registerCallback(callback, uid);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.AppDisplayResource.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AppDisplayResource.this.mDisplayRecordMap) {
                    DisplayRecord record = (DisplayRecord) AppDisplayResource.this.mDisplayRecordMap.get(Integer.valueOf(uid));
                    if (record != null && record.isActive()) {
                        AppDisplayResource.this.reportResourceStatus(uid, true, record.mBehavier);
                    }
                }
            }
        });
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList getActiveUids() {
        ArrayList arrayList;
        synchronized (this.mDisplayRecordMap) {
            arrayList = new ArrayList(this.mDisplayRecordMap.keySet());
        }
        return arrayList;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mDisplayRecordMap) {
            DisplayRecord record = this.mDisplayRecordMap.get(Integer.valueOf(uid));
            return record != null && record.isActive();
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        return isAppResourceActive(uid);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int pid) {
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int pid) {
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayAdded(int displayId) {
        Display display = this.mDms.getDisplay(displayId);
        onDisplayAdded(display);
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayRemoved(int displayId) {
        Display display = this.mDms.getDisplay(displayId);
        if (DEBUG) {
            Slog.d(AppPowerResourceManager.TAG, "Display removed " + display);
        }
        synchronized (this.mDisplayRecordMap) {
            for (DisplayRecord record : this.mDisplayRecordMap.values()) {
                if (record.onDisplayRemoved(displayId)) {
                    if (!record.isActive()) {
                        this.mDisplayRecordMap.remove(Integer.valueOf(record.mUid));
                    }
                    return;
                }
            }
        }
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayChanged(int displayId) {
    }

    private void initializeCurrentDisplays() {
        Display[] displays = this.mDms.getDisplays();
        for (Display display : displays) {
            onDisplayAdded(display);
        }
    }

    private void onDisplayAdded(Display display) {
        if (DEBUG) {
            Slog.w(AppPowerResourceManager.TAG, "Display added " + display);
        }
        if (display != null) {
            synchronized (this.mDisplayRecordMap) {
                int uid = display.getOwnerUid();
                if (uid == 0) {
                    uid = 1000;
                }
                DisplayRecord displayRecord = this.mDisplayRecordMap.get(Integer.valueOf(uid));
                if (displayRecord == null) {
                    displayRecord = new DisplayRecord(uid);
                    this.mDisplayRecordMap.put(Integer.valueOf(uid), displayRecord);
                }
                displayRecord.onDisplayAdded(display.getDisplayId(), display);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DisplayRecord {
        private boolean mActive;
        private int mBehavier;
        private final ArrayMap<Integer, Display> mDisplays = new ArrayMap<>();
        private final int mUid;

        DisplayRecord(int uid) {
            this.mUid = uid;
        }

        boolean isActive() {
            return this.mActive;
        }

        public void onDisplayAdded(int displayId, Display display) {
            synchronized (this.mDisplays) {
                Display displayRecord = this.mDisplays.get(Integer.valueOf(displayId));
                if (displayRecord == null) {
                    this.mDisplays.put(Integer.valueOf(displayId), display);
                }
            }
            updateCurrentStatus();
        }

        public boolean onDisplayRemoved(int displayId) {
            synchronized (this.mDisplays) {
                if (this.mDisplays.remove(Integer.valueOf(displayId)) == null) {
                    return false;
                }
                updateCurrentStatus();
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateCurrentStatus() {
            boolean active;
            int behavier = 0;
            synchronized (this.mDisplays) {
                active = this.mDisplays.size() > 0;
                for (Display display : this.mDisplays.values()) {
                    switch (display.getType()) {
                        case 2:
                            behavier |= 512;
                            break;
                        case 3:
                            behavier |= 256;
                            break;
                        case 5:
                            if (AppDisplayResource.this.mWifiP2pConnected && AppDisplayResource.this.mCastingWhitelists.contains(display.getOwnerPackageName())) {
                                behavier |= 256;
                                break;
                            }
                            break;
                    }
                }
            }
            if (active != this.mActive || behavier != this.mBehavier) {
                if (AppPowerResource.DEBUG) {
                    Slog.d(AppPowerResourceManager.TAG, this.mUid + " display active " + this.mActive + " " + this.mBehavier);
                }
                EventLog.writeEvent(SmartPowerSettings.EVENT_TAGS, "display u:" + this.mUid + " s:" + active);
                AppDisplayResource.this.reportResourceStatus(this.mUid, active, behavier);
                this.mActive = active;
                this.mBehavier = behavier;
            }
        }
    }
}
