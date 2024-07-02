package com.xiaomi.interconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.SoftApInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.xiaomi.interconnection.IInterconnectionManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class InterconnectionService extends IInterconnectionManager.Stub {
    private static final String ARP_TABLE_PATH = "/proc/net/arp";
    private static final String FEATURE_P2P_160M = "xiaomi.hardware.p2p_160m";
    private static final String FEATURE_P2P_165CHAN = "xiaomi.hardware.p2p_165chan";
    public static final String SERVICE_NAME = "xiaomi.InterconnectionService";
    public static final String TAG = "InterconnectionService";
    private Context mContext;
    private PackageManager mPackageManager;
    private RemoteCallbackList<ISoftApCallback> mSoftApCallbackList;
    private WifiManager mWifiManager;
    private RemoteCallbackList<IWifiP2pCallback> mWifiP2pCallbackList;
    private Handler mHandler = new Handler();
    private String mSoftApIfaceName = "";
    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() { // from class: com.xiaomi.interconnection.InterconnectionService.1
        public void onInfoChanged(List<SoftApInfo> softApInfoList) {
            for (SoftApInfo info : softApInfoList) {
                InterconnectionService.this.mSoftApIfaceName = info.getApInstanceIdentifier();
                InterconnectionService.this.notifySoftApInfoChanged();
            }
        }
    };
    private boolean mIsGo = false;
    private String mDeviceP2pMacAddr = "";
    private String mPeerP2pMacAddr = "";
    private String mDeviceP2pIpAddr = "";
    private String mPeerP2pIpAddr = "";
    private BroadcastReceiver mP2pReceiver = new BroadcastReceiver() { // from class: com.xiaomi.interconnection.InterconnectionService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo;
            if (!"android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(intent.getAction()) || (networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo")) == null || !networkInfo.isConnected()) {
                return;
            }
            WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) intent.getParcelableExtra("p2pGroupInfo");
            WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra("wifiP2pInfo");
            if (wifiP2pGroup == null || wifiP2pInfo == null) {
                return;
            }
            String iface = wifiP2pGroup.getInterface();
            InterconnectionService.this.mDeviceP2pMacAddr = InterconnectionService.getIfaceMacAddr(iface);
            if (wifiP2pGroup.isGroupOwner()) {
                InterconnectionService.this.mIsGo = true;
                Collection<WifiP2pDevice> clients = wifiP2pGroup.getClientList();
                int clientNum = clients.size();
                String macAddr = "";
                for (WifiP2pDevice client : clients) {
                    macAddr = client.deviceAddress;
                }
                InterconnectionService.this.mPeerP2pMacAddr = macAddr;
                InetAddress addr = wifiP2pInfo.groupOwnerAddress;
                InterconnectionService.this.mDeviceP2pIpAddr = addr == null ? "" : addr.getHostAddress();
                InterconnectionService interconnectionService = InterconnectionService.this;
                interconnectionService.mPeerP2pIpAddr = interconnectionService.getIpFromArpTable(interconnectionService.mPeerP2pMacAddr);
                InterconnectionService.this.notifyWifiP2pInfoChanged();
                if (clientNum > 0 && "".equals(InterconnectionService.this.mPeerP2pIpAddr)) {
                    InterconnectionService.this.getPeerIpFromArpTableUntilTimeout();
                    return;
                }
                return;
            }
            InterconnectionService.this.mIsGo = false;
            WifiP2pDevice owner = wifiP2pGroup.getOwner();
            InterconnectionService.this.mPeerP2pMacAddr = owner.deviceAddress;
            InterconnectionService interconnectionService2 = InterconnectionService.this;
            interconnectionService2.mDeviceP2pIpAddr = interconnectionService2.getIfaceIpAddr(iface);
            InetAddress addr2 = wifiP2pInfo.groupOwnerAddress;
            InterconnectionService.this.mPeerP2pIpAddr = addr2 == null ? "" : addr2.getHostAddress();
            InterconnectionService.this.notifyWifiP2pInfoChanged();
            if ("".equals(InterconnectionService.this.mPeerP2pIpAddr)) {
                InterconnectionService.this.getPeerIpFromArpTableUntilTimeout();
            }
        }
    };

    public InterconnectionService(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mPackageManager = this.mContext.getPackageManager();
        this.mWifiManager.registerSoftApCallback(new HandlerExecutor(this.mHandler), this.mSoftApCallback);
        this.mSoftApCallbackList = new RemoteCallbackList<>();
        this.mWifiP2pCallbackList = new RemoteCallbackList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mContext.registerReceiver(this.mP2pReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getIfaceMacAddr(String iface) {
        try {
            NetworkInterface nif = NetworkInterface.getByName(iface);
            if (nif == null) {
                return "";
            }
            byte[] mac = nif.getHardwareAddress();
            String[] sa = new String[mac.length];
            for (int i = 0; i < mac.length; i++) {
                sa[i] = String.format("%02x", Byte.valueOf(mac[i]));
            }
            String macAddr = String.join(":", sa);
            return macAddr;
        } catch (SocketException e) {
            e.printStackTrace();
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getIfaceIpAddr(String iface) {
        try {
            NetworkInterface nif = NetworkInterface.getByName(iface);
            if (nif == null) {
                return "";
            }
            List<InetAddress> ipAddrList = (List) Collections.list(nif.getInetAddresses()).stream().filter(new Predicate() { // from class: com.xiaomi.interconnection.InterconnectionService$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return InterconnectionService.lambda$getIfaceIpAddr$0((InetAddress) obj);
                }
            }).collect(Collectors.toList());
            if (ipAddrList.size() != 1) {
                return "";
            }
            String ipAddr = ipAddrList.get(0).getHostAddress();
            return ipAddr;
        } catch (SocketException e) {
            e.printStackTrace();
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIfaceIpAddr$0(InetAddress ia) {
        return ia instanceof Inet4Address;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getIpFromArpTable(String macAddr) {
        String ipAddr = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(ARP_TABLE_PATH));
            try {
                reader.readLine();
                while (true) {
                    String line = reader.readLine();
                    if (line != null) {
                        String[] tokens = line.split("[ ]+");
                        if (tokens.length >= 6) {
                            String addr = tokens[3];
                            String subAddr = addr.substring(0, addr.length() - 3);
                            if (macAddr.contains(subAddr)) {
                                ipAddr = tokens[0];
                            }
                        }
                    } else {
                        reader.close();
                        return ipAddr;
                    }
                }
            } finally {
            }
        } catch (FileNotFoundException e) {
            return ipAddr;
        } catch (IOException e2) {
            return ipAddr;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getPeerIpFromArpTableUntilTimeout() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final Future future = executor.submit(new Runnable() { // from class: com.xiaomi.interconnection.InterconnectionService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InterconnectionService.this.lambda$getPeerIpFromArpTableUntilTimeout$1();
            }
        });
        Runnable cancelTask = new Runnable() { // from class: com.xiaomi.interconnection.InterconnectionService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                InterconnectionService.lambda$getPeerIpFromArpTableUntilTimeout$2(future);
            }
        };
        executor.schedule(cancelTask, 2000L, TimeUnit.MILLISECONDS);
        executor.shutdown();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getPeerIpFromArpTableUntilTimeout$1() {
        boolean interrupted;
        String ipFromArpTable;
        do {
            interrupted = Thread.currentThread().isInterrupted();
            if (interrupted) {
                break;
            }
            ipFromArpTable = getIpFromArpTable(this.mPeerP2pMacAddr);
            this.mPeerP2pIpAddr = ipFromArpTable;
        } while ("".equals(ipFromArpTable));
        if (!interrupted) {
            notifyWifiP2pInfoChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getPeerIpFromArpTableUntilTimeout$2(Future future) {
        future.cancel(true);
        Log.d(TAG, "task canceled due to timeout");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySoftApInfoChanged() {
        int itemCount = this.mSoftApCallbackList.beginBroadcast();
        for (int i = 0; i < itemCount; i++) {
            try {
                this.mSoftApCallbackList.getBroadcastItem(i).onIfaceInfoChanged(this.mSoftApIfaceName);
            } catch (RemoteException e) {
                Log.e(TAG, "onSoftApInfoChanged: remote exception -- " + e);
            }
        }
        this.mSoftApCallbackList.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyWifiP2pInfoChanged() {
        int itemCount = this.mWifiP2pCallbackList.beginBroadcast();
        for (int i = 0; i < itemCount; i++) {
            try {
                P2pDevicesInfo info = new P2pDevicesInfo(this.mIsGo, this.mDeviceP2pMacAddr, this.mPeerP2pMacAddr, this.mDeviceP2pIpAddr, this.mPeerP2pIpAddr);
                this.mWifiP2pCallbackList.getBroadcastItem(i).onDevicesInfoChanged(info);
            } catch (RemoteException e) {
                Log.e(TAG, "onWifiP2pInfoChanged: remote exception -- " + e);
            }
        }
        this.mWifiP2pCallbackList.finishBroadcast();
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public String getWifiChipModel() {
        boolean isMtk = "mediatek".equals(FeatureParser.getString("vendor"));
        if (isMtk) {
            String wifiChipModel = SystemProperties.get("vendor.connsys.wifi.adie.chipid", "unknown");
            return wifiChipModel;
        }
        String wifiChipModel2 = SystemProperties.get("ro.hardware.wlan.chip", "unknown");
        return wifiChipModel2;
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public boolean supportP2pChannel165() {
        return this.mPackageManager.hasSystemFeature(FEATURE_P2P_165CHAN);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public boolean supportP2p160Mode() {
        return this.mPackageManager.hasSystemFeature(FEATURE_P2P_160M);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public boolean supportHbs() {
        String cloudvalue = Settings.System.getString(this.mContext.getContentResolver(), "cloud_wifi_hbs_support");
        if ("off".equals(cloudvalue)) {
            return false;
        }
        try {
            boolean support = this.mContext.getResources().getBoolean(this.mContext.getResources().getIdentifier("config_wifi_hbs_support", "bool", "android.miui"));
            return support;
        } catch (Exception e) {
            Log.e(TAG, "config for wifi hbs not found");
            return false;
        }
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public int supportDbs() {
        String dbsProp = SystemProperties.get("ro.hardware.wlan.dbs", "unknown");
        if ("unknown".equals(dbsProp)) {
            String chipProp = SystemProperties.get("vendor.connsys.adie.chipid", "unknown");
            if ("unknown".equals(chipProp)) {
                return 0;
            }
            int dbs = ("0x6635".equals(chipProp) || "0x6637".equals(chipProp)) ? 1 : 2;
            return dbs;
        }
        if ("1".equals(dbsProp)) {
            return 1;
        }
        if (!"2".equals(dbsProp)) {
            return 0;
        }
        return 2;
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public void registerSoftApCallback(ISoftApCallback cb) {
        this.mSoftApCallbackList.register(cb);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public void unregisterSoftApCallback(ISoftApCallback cb) {
        this.mSoftApCallbackList.unregister(cb);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public void registerWifiP2pCallback(IWifiP2pCallback cb) {
        this.mWifiP2pCallbackList.register(cb);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public void unregisterWifiP2pCallback(IWifiP2pCallback cb) {
        this.mWifiP2pCallbackList.unregister(cb);
    }

    @Override // com.xiaomi.interconnection.IInterconnectionManager
    public void notifyConcurrentNetworkState(boolean mcc) {
        Log.d(TAG, "mcc: " + mcc);
    }
}
