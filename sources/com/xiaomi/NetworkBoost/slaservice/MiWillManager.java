package com.xiaomi.NetworkBoost.slaservice;

import android.content.Context;
import android.database.ContentObserver;
import android.net.MacAddress;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.system.NetlinkSocketAddress;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import com.xiaomi.NetworkBoost.StatusManager;
import com.xiaomi.NetworkBoost.slaservice.netlinkclient.NetlinkMessage;
import com.xiaomi.NetworkBoost.slaservice.netlinkclient.NetlinkSocket;
import com.xiaomi.NetworkBoost.slaservice.netlinkclient.RtNetlinkNeighborMessage;
import com.xiaomi.NetworkBoost.slaservice.netlinkclient.StructNlMsgHdr;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import libcore.io.IoUtils;
import miui.android.services.internal.hidl.manager.V1_0.IServiceManager;
import miui.android.services.internal.hidl.manager.V1_0.IServiceNotification;
import vendor.xiaomi.hidl.miwill.V1_0.IMiwillService;

/* loaded from: classes.dex */
public class MiWillManager {
    private static final String ACTION_ENABLE_MIWILL = "miui.wifi.ENABLE_MIWILL";
    private static final long COOKIE_UPBOUND = 100;
    private static final boolean DBG = true;
    private static final boolean DEBUG_COMMAND = true;
    private static final String INTERFACE_NAME = "miw_oem0";
    private static final int MIWILL_DISABLED = 0;
    private static final int MIWILL_ENABLED = 1;
    private static final int RECHECK_MAX_TIMES = 2;
    private static final String REDUDANCY_MODE = "redudancy";
    public static final int REDUDANCY_MODE_KEY = 1;
    private static final String TAG = "MIWILL-MiWillManager";
    private static final int WAIT_RECHECK_DELAY_MILLIS = 10000;
    private static final String WIFI_MIWILL_ENABLE = "wifi_miwill_enable";
    private static final String WPS_DEVICE_XIAOMI = "xiaomi";
    private static final HashMap<Integer, String> mModeMap;
    private static volatile MiWillManager sInstance;
    private Context mContext;
    private static final byte[] XIAOMI_OUI = {-116, -66, -66};
    private static final byte[] MIWILL_TYPE = {MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH, CommunicationUtil.COMMAND_AUTH_52};
    private ContentObserver mSettingsObserver = null;
    private WifiManager mWifiManager = null;
    private Handler mHandler = null;
    private HandlerThread mThread = null;
    private IMiwillService mMiWillHal = null;
    private long mCookie = 0;
    private boolean isDualWifiReady = false;
    private int mModeKey = 1;
    private IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() { // from class: com.xiaomi.NetworkBoost.slaservice.MiWillManager.1
        public void serviceDied(long cookie) {
            Log.e(MiWillManager.TAG, "HAL service died cookie = " + cookie + " mCookie = " + MiWillManager.this.mCookie);
            if (MiWillManager.this.mCookie == cookie) {
                MiWillManager.this.mMiWillHal = null;
            }
        }
    };
    private final Notification mNotification = new Notification();

    static {
        HashMap<Integer, String> hashMap = new HashMap<>();
        mModeMap = hashMap;
        hashMap.put(1, REDUDANCY_MODE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Notification extends IServiceNotification.Stub {
        private Notification() {
        }

        @Override // miui.android.services.internal.hidl.manager.V1_0.IServiceNotification
        public final void onRegistration(String interfaceName, String instanceName, boolean preexisting) {
            Log.i(MiWillManager.TAG, "onRegistration interfaceName = " + interfaceName + " instanceName = " + instanceName + " preexisting = " + preexisting);
            if (IMiwillService.kInterfaceName.equals(interfaceName) && NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI.equals(instanceName) && MiWillManager.this.mHandler != null) {
                Log.w(MiWillManager.TAG, "prepare to get new hal service.");
                MiWillManager.this.mHandler.sendEmptyMessage(101);
                MiWillManager.this.mHandler.sendEmptyMessage(100);
            }
        }
    }

    public static MiWillManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MiWillManager.class) {
                if (sInstance == null) {
                    sInstance = new MiWillManager(context);
                    try {
                        sInstance.onCreate();
                    } catch (Exception e) {
                        Log.e(TAG, "getInstance onCreate catch:", e);
                    }
                }
            }
        }
        return sInstance;
    }

    public static void destroyInstance() {
        if (sInstance != null) {
            synchronized (MiWillManager.class) {
                if (sInstance != null) {
                    try {
                        sInstance.onDestroy();
                    } catch (Exception e) {
                        Log.e(TAG, "destroyInstance onDestroy catch:", e);
                    }
                    sInstance = null;
                }
            }
        }
    }

    private MiWillManager(Context context) {
        this.mContext = null;
        this.mContext = context.getApplicationContext();
    }

    public void onCreate() {
        Log.i(TAG, "onCreate");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        HandlerThread handlerThread = new HandlerThread("MiWillWorkHandler");
        this.mThread = handlerThread;
        handlerThread.start();
        this.mHandler = new MiWillHandler(this.mThread.getLooper());
        try {
            IServiceManager.getService().registerForNotifications(IMiwillService.kInterfaceName, NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, this.mNotification);
        } catch (Exception e) {
            Log.e(TAG, "onCreate catch", e);
        }
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        Log.i(TAG, "disable miwill because miwill manager destroying.");
        disableMiwill(false, 0);
        HandlerThread handlerThread = this.mThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            this.mThread = null;
        }
        this.mHandler = null;
        deinitMiWillHal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initMiWillHal() {
        if (this.mMiWillHal != null) {
            Log.w(TAG, "initMiWillHal mMiWillHal has already been inited.");
            return;
        }
        try {
            IMiwillService service = IMiwillService.getService(false);
            this.mMiWillHal = service;
            if (service == null) {
                Log.e(TAG, "MiWill hal get failed");
            } else {
                Log.d(TAG, "MiWill hal get success");
                long j = (this.mCookie + 1) % COOKIE_UPBOUND;
                this.mCookie = j;
                this.mMiWillHal.linkToDeath(this.mDeathRecipient, j);
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate catch", e);
            deinitMiWillHal();
        }
    }

    private void deinitMiWillHal() {
        IMiwillService iMiwillService = this.mMiWillHal;
        if (iMiwillService != null) {
            try {
                iMiwillService.unlinkToDeath(this.mDeathRecipient);
            } catch (Exception e) {
            }
            this.mMiWillHal = null;
        }
    }

    private void registerSwitchObserver() {
        if (this.mSettingsObserver != null) {
            Log.e(TAG, "registerSwitchObserver observer already registered! check your code.");
        } else {
            this.mSettingsObserver = new ContentObserver(new Handler()) { // from class: com.xiaomi.NetworkBoost.slaservice.MiWillManager.2
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    Log.d(MiWillManager.TAG, "onChange selfChange = " + selfChange + " uri = " + uri);
                    if (MiWillManager.this.mHandler != null) {
                        MiWillManager.this.mHandler.sendEmptyMessage(100);
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(WIFI_MIWILL_ENABLE), false, this.mSettingsObserver);
        }
    }

    private void unregisterSwitchObserver() {
        if (this.mSettingsObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            this.mSettingsObserver = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateStatus() {
        IMiwillService iMiwillService = this.mMiWillHal;
        if (iMiwillService != null) {
            try {
                iMiwillService.startMiwild();
                return;
            } catch (Exception e) {
                Log.w(TAG, "updateStatus catch:" + e);
                return;
            }
        }
        Log.e(TAG, "updateStatus mMiWillHal is null");
    }

    private boolean isMiWillRouter(ScanResult scanResult) {
        byte[] bArr;
        if (scanResult == null) {
            Log.e(TAG, "isMiWillRouter: null scan result");
            return false;
        }
        ScanResult.InformationElement[] ies = (ScanResult.InformationElement[]) scanResult.getInformationElements().toArray();
        if (ies != null && ies.length > 0) {
            for (int i = 0; i < ies.length; i++) {
                if (ies[i].getId() == 221) {
                    ByteBuffer byteBuffers = ies[i].getBytes();
                    byte[] value = new byte[byteBuffers.limit()];
                    if (byteBuffers.limit() != XIAOMI_OUI.length + MIWILL_TYPE.length) {
                        continue;
                    } else {
                        try {
                            byteBuffers.get(value, 0, byteBuffers.limit());
                            int j = 0;
                            int k = 0;
                            while (true) {
                                bArr = XIAOMI_OUI;
                                if (k >= bArr.length || bArr[k] != value[j]) {
                                    break;
                                }
                                j++;
                                k++;
                            }
                            int k2 = bArr.length;
                            if (j == k2) {
                                int k3 = 0;
                                while (true) {
                                    byte[] bArr2 = MIWILL_TYPE;
                                    if (k3 >= bArr2.length || bArr2[k3] != value[j]) {
                                        break;
                                    }
                                    j++;
                                    k3++;
                                }
                                int k4 = byteBuffers.limit();
                                if (j == k4) {
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "isMiWillRouter catch:", e);
                        }
                    }
                }
            }
        }
        return false;
    }

    public void setDualWifiReady(boolean ready) {
        this.isDualWifiReady = ready;
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(102);
            this.mHandler.removeMessages(103);
            Handler handler2 = this.mHandler;
            handler2.sendMessage(handler2.obtainMessage(102, Boolean.valueOf(ready)));
        }
    }

    private void disableMiwill(boolean ready, int retry_cnt) {
        Handler handler;
        try {
            this.mMiWillHal.setMiwillParameter("set miw_oem0 0");
            if (ready && retry_cnt > 0 && (handler = this.mHandler) != null) {
                handler.sendMessageDelayed(handler.obtainMessage(103, Integer.valueOf(retry_cnt)), 10000L);
            }
        } catch (Exception e1) {
            Log.e(TAG, "disableMiwill catch e = " + e1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Incorrect condition in loop: B:46:0x0065 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setMiwillStatus(boolean r20, int r21) {
        /*
            Method dump skipped, instructions count: 548
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.NetworkBoost.slaservice.MiWillManager.setMiwillStatus(boolean, int):void");
    }

    private boolean checkMiWillIp() {
        String netInterface;
        try {
            Enumeration<NetworkInterface> net = NetworkInterface.getNetworkInterfaces();
            while (net.hasMoreElements()) {
                NetworkInterface networkInterface = net.nextElement();
                Enumeration<InetAddress> add = networkInterface.getInetAddresses();
                while (add.hasMoreElements()) {
                    InetAddress a = add.nextElement();
                    if (!a.isLoopbackAddress() && !a.getHostAddress().contains(":") && a.getHostAddress().contains("192.168.") && (netInterface = networkInterface.getDisplayName()) != null && netInterface.indexOf(INTERFACE_NAME) != -1 && !StatusManager.isTetheredIfaces(netInterface)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "checkMiWillIp Exception:" + e);
            return false;
        }
    }

    public boolean setMiWillGameStart(String uid) {
        if (!checkMiWillIp()) {
            Log.w(TAG, "setMiWillGameStart miwill ip not found.");
            return false;
        }
        IMiwillService iMiwillService = this.mMiWillHal;
        if (iMiwillService != null) {
            try {
                iMiwillService.setMiwillAppUidList(uid);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "setMiWillGameStart catch:" + e);
                return false;
            }
        }
        Log.e(TAG, "setMiWillGameStart miwill hal service lost");
        return false;
    }

    public boolean setMiWillGameStop(String uid) {
        IMiwillService iMiwillService = this.mMiWillHal;
        if (iMiwillService != null) {
            try {
                iMiwillService.setMiwillAppUidList("0");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "setMiWillGameStop catch:" + e);
                return true;
            }
        }
        Log.e(TAG, "setMiWillGameStop miwill hal service lost");
        return false;
    }

    public static boolean is24GHz(ScanResult scanResult) {
        return is24GHz(scanResult.frequency);
    }

    public static boolean is5GHz(ScanResult scanResult) {
        return is5GHz(scanResult.frequency);
    }

    public static boolean is24GHz(int freq) {
        return freq > 2400 && freq < 2500;
    }

    public static boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiWillHandler extends Handler {
        public static final int GET_MIWILL_HAL = 101;
        public static final int RECHECK_MIWILL_STATUS = 103;
        public static final int SET_MIWILL_STATUS = 102;
        public static final int UPDATE_STATUS = 100;

        public MiWillHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Log.i(MiWillManager.TAG, "handle: " + msg.what);
            switch (msg.what) {
                case 100:
                    MiWillManager.this.updateStatus();
                    return;
                case 101:
                    MiWillManager.this.initMiWillHal();
                    return;
                case 102:
                    MiWillManager.this.setMiwillStatus(((Boolean) msg.obj).booleanValue(), 2);
                    return;
                case 103:
                    int retry_cnt = ((Integer) msg.obj).intValue();
                    MiWillManager.this.setMiwillStatus(true, retry_cnt - 1);
                    return;
                default:
                    Log.e(MiWillManager.TAG, "MiWillHandler handle unexpected code:" + msg.what);
                    return;
            }
        }
    }

    private static MacAddress getMacAddress(byte[] linkLayerAddress) {
        if (linkLayerAddress != null) {
            try {
                return MacAddress.fromBytes(linkLayerAddress);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to parse link-layer address: " + linkLayerAddress);
                return null;
            }
        }
        return null;
    }

    private MacAddress checkGateway(String wifi_gateway, String slave_wifi_gateway) {
        int doneMessageCount;
        if (!wifi_gateway.equals(slave_wifi_gateway)) {
            return null;
        }
        FileDescriptor fd = null;
        try {
            try {
                fd = NetlinkSocket.forProto(OsConstants.NETLINK_ROUTE);
                NetlinkSocket.connectToKernel(fd);
                NetlinkSocketAddress localAddr = Os.getsockname(fd);
                byte[] req = RtNetlinkNeighborMessage.newGetNeighborsRequest(1);
                int len = NetlinkSocket.sendMessage(fd, req, 0, req.length, 500L);
                if (req.length == len) {
                    Log.i(TAG, "req.length == req.length");
                }
                int doneMessageCount2 = 0;
                MacAddress wifi_gateway_mac = null;
                MacAddress slave_wifi_gateway_mac = null;
                while (doneMessageCount2 == 0) {
                    int len2 = len;
                    ByteBuffer response = NetlinkSocket.recvMessage(fd, 8192, 500L);
                    doneMessageCount2 = doneMessageCount2;
                    while (response.remaining() > 0) {
                        NetlinkMessage nlMsg = NetlinkMessage.parse(response, OsConstants.NETLINK_ROUTE);
                        StructNlMsgHdr hdr = nlMsg.getHeader();
                        NetlinkSocketAddress localAddr2 = localAddr;
                        ByteBuffer response2 = response;
                        if (hdr.nlmsg_type == 3) {
                            doneMessageCount2++;
                            localAddr = localAddr2;
                            response = response2;
                        } else {
                            if (28 == hdr.nlmsg_type) {
                                Log.i(TAG, "NetlinkConstants.RTM_NEWNEIGH == hdr.nlmsg_type" + nlMsg.toString());
                                RtNetlinkNeighborMessage neighMsg = (RtNetlinkNeighborMessage) nlMsg;
                                Log.i(TAG, "macaddress " + getMacAddress(neighMsg.getLinkLayerAddress()));
                                String ipLiteral = neighMsg.getDestination() == null ? "" : neighMsg.getDestination().getHostAddress();
                                doneMessageCount = doneMessageCount2;
                                Log.i(TAG, "ip address " + ipLiteral);
                                if (ipLiteral.equals(wifi_gateway)) {
                                    if (wifi_gateway_mac == null) {
                                        wifi_gateway_mac = getMacAddress(neighMsg.getLinkLayerAddress());
                                    } else if (slave_wifi_gateway_mac == null) {
                                        slave_wifi_gateway_mac = getMacAddress(neighMsg.getLinkLayerAddress());
                                    }
                                }
                            } else {
                                doneMessageCount = doneMessageCount2;
                            }
                            localAddr = localAddr2;
                            response = response2;
                            doneMessageCount2 = doneMessageCount;
                        }
                    }
                    len = len2;
                }
                Log.i(TAG, "wifi_gateway_mac:" + wifi_gateway_mac + " slave_wifi_gateway_mac:" + slave_wifi_gateway_mac);
                if (wifi_gateway_mac != null) {
                    if (wifi_gateway_mac.equals(slave_wifi_gateway_mac)) {
                        IoUtils.closeQuietly(fd);
                        return wifi_gateway_mac;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "");
            }
            IoUtils.closeQuietly(fd);
            Log.e(TAG, "wifi_gateway:" + wifi_gateway + " slave_wifi_gateway:" + slave_wifi_gateway);
            return null;
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
            throw th;
        }
    }
}
