package com.android.server.net;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class NetworkManagermentServiceImpl implements NetworkManagementServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NetworkManagermentServiceImpl> {

        /* compiled from: NetworkManagermentServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NetworkManagermentServiceImpl INSTANCE = new NetworkManagermentServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public NetworkManagermentServiceImpl m2026provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public NetworkManagermentServiceImpl m2025provideNewInstance() {
            return new NetworkManagermentServiceImpl();
        }
    }

    public IBinder getMiuiNetworkManager() {
        return MiuiNetworkManager.get();
    }

    public void setPidForPackage(String packageName, int pid, int uid) {
        MiuiNetworkManagementService.getInstance().setPidForPackage(packageName, pid, uid);
    }

    public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) {
        return MiuiNetworkManagementService.getInstance().setMiuiFirewallRule(packageName, uid, rule, type);
    }

    public void showLogin(Context context, Intent intent, String ssid) {
    }

    public void dump(PrintWriter pw) {
        MiuiNetworkManagementService.getInstance().dump(pw);
    }

    public void updateAurogonUidRule(int uid, boolean allow) {
        MiuiNetworkManagementService.getInstance().updateAurogonUidRule(uid, allow);
    }

    public boolean addMiuiFirewallSharedUid(int uid) {
        return MiuiNetworkManagementService.getInstance().addMiuiFirewallSharedUid(uid);
    }

    public boolean setCurrentNetworkState(int state) {
        return MiuiNetworkManagementService.getInstance().setCurrentNetworkState(state);
    }
}
