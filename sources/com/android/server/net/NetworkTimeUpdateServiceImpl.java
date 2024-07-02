package com.android.server.net;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Network;
import android.provider.Settings;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import miui.os.Build;

/* loaded from: classes.dex */
public class NetworkTimeUpdateServiceImpl implements NetworkTimeUpdateServiceStub {
    private static String CN_NTP_SERVER = "pool.ntp.org";
    private static final boolean DBG = true;
    private static final String TAG = "NetworkTimeUpdateService";
    private Context mContext;
    private String mDefaultNtpServer;
    private ArrayList<String> mNtpServers = new ArrayList<>();
    private NtpTrustedTime mTime;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NetworkTimeUpdateServiceImpl> {

        /* compiled from: NetworkTimeUpdateServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NetworkTimeUpdateServiceImpl INSTANCE = new NetworkTimeUpdateServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public NetworkTimeUpdateServiceImpl m2044provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public NetworkTimeUpdateServiceImpl m2043provideNewInstance() {
            return new NetworkTimeUpdateServiceImpl();
        }
    }

    public void initNtpServers(Context context, NtpTrustedTime trustedTime) {
        this.mTime = trustedTime;
        initDefaultNtpServer(context);
        this.mNtpServers.add(this.mDefaultNtpServer);
        String[] globalNtpServers = context.getResources().getStringArray(285409409);
        String[] chinaNtpServers = context.getResources().getStringArray(285409306);
        for (String ntpServer : globalNtpServers) {
            this.mNtpServers.add(ntpServer);
        }
        if (!Build.IS_GLOBAL_BUILD) {
            for (String ntpServer2 : chinaNtpServers) {
                this.mNtpServers.add(ntpServer2);
            }
        }
        Log.d(TAG, "the servers are " + this.mNtpServers);
    }

    public boolean switchNtpServer(int tryCounter, NtpTrustedTime trustedTime, Network network) {
        if (!refreshNtpServer(tryCounter, network)) {
            return this.mTime.forceRefresh(network);
        }
        return true;
    }

    private void initDefaultNtpServer(Context context) {
        if (context == null) {
            return;
        }
        this.mContext = context;
        ContentResolver resolver = context.getContentResolver();
        this.mContext.getResources();
        String secureServer = Settings.Global.getString(resolver, "ntp_server");
        if (Build.IS_GLOBAL_BUILD) {
            this.mDefaultNtpServer = secureServer != null ? secureServer : "time.android.com";
        } else {
            this.mDefaultNtpServer = CN_NTP_SERVER;
        }
    }

    private void setNtpServer(String ntpServer) {
        Context context = this.mContext;
        if (context != null) {
            Settings.Global.putString(context.getContentResolver(), "ntp_server", ntpServer);
        }
    }

    private boolean refreshNtpServer(int tryCounter, Network network) {
        int index = tryCounter % this.mNtpServers.size();
        String ntpServer = this.mNtpServers.get(index);
        Log.d(TAG, "tryCounter = " + tryCounter + ",ntpServers = " + ntpServer);
        setNtpServer(ntpServer);
        boolean result = this.mTime.forceRefresh(network);
        setNtpServer(this.mDefaultNtpServer);
        return result;
    }
}
