package com.miui.server.sentinel;

import android.content.Context;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.SystemService;
import com.miui.server.sentinel.MiuiSentinelMemoryManager;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/* loaded from: classes.dex */
public class MiuiSentinelService extends Binder {
    private static final int DEFAULT_BUFFER_SIZE = 4096000;
    public static final String HOST_NAME = "data/localsocket/prochunter_native_socket";
    public static final int MAX_BUFF_SIZE = 9000;
    public static final int PROC_HUNTER_MSG = 10;
    public static final int REPORT_NATIVEHEAP_LEAKTOMQS = 6;
    public static final String SERVICE_NAME = "miui.sentinel.service";
    private static final String TAG = "MiuiSentinelService";
    public static final int TRACK_HEAP_REPORT_MSG = 6;
    private static MiuiSentinelServiceThread miuiSentinelServiceThread;
    private Context mContext = null;
    private static volatile MiuiSentinelService sInstance = null;
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.sys.mss", false);
    private static final HashMap<String, Integer> APP_JAVAHEAP_WHITE_LIST = new HashMap<>();
    private static final HashMap<String, Integer> APP_NATIVEHEAP_WHITE_LIST = new HashMap<>();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nRegisteredBpfEvent();

    private MiuiSentinelService() {
        miuiSentinelServiceThread = new MiuiSentinelServiceThread();
    }

    public static MiuiSentinelService getInstance() {
        if (sInstance == null) {
            synchronized (MiuiSentinelService.class) {
                if (sInstance == null) {
                    sInstance = new MiuiSentinelService();
                }
            }
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MiuiSentinelService initContext(Context context) {
        this.mContext = context;
        initWhilteList(context);
        return getInstance();
    }

    /* loaded from: classes.dex */
    private class MiuiSentinelServiceThread extends Thread {
        private MiuiSentinelServiceThread() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            try {
                LocalSocket localSocket = MiuiSentinelService.this.createSystemServerSocketForProchunter();
                if (localSocket != null) {
                    Slog.e(MiuiSentinelService.TAG, "local socekt fd is:" + localSocket.getFileDescriptor());
                    while (true) {
                        MiuiSentinelService.this.recvMessage(localSocket.getFileDescriptor());
                    }
                }
            } catch (Exception e) {
                Slog.e(MiuiSentinelService.TAG, "prochunter_native_sockets connection catch Exception");
                e.printStackTrace();
            }
        }
    }

    private void handlerTrigger(SocketPacket socketPacket) {
        int type = MiuiSentinelEvent.getEventType(socketPacket.getEvent_type());
        switch (type) {
            case 2:
                Slog.d(TAG, "begin judgmentNativeHeapLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentNativeHeapLeakException(socketPacket);
                return;
            case 3:
                Slog.d(TAG, "begin judgmentJavaHeapLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentJavaHeapLeakException(socketPacket);
                return;
            case 4:
                Slog.d(TAG, "begin judgment ThreadAmountLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentThreadAmountLeakException(socketPacket);
                return;
            case 5:
            case 6:
            case 8:
            default:
                Slog.e(TAG, "receive invalid event");
                return;
            case 7:
                Slog.d(TAG, "begin judgmentRssLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentRssLeakException(socketPacket);
                return;
            case 9:
                Slog.d(TAG, "begin judgment FdAmountLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentFdAmountLeakException(socketPacket);
                return;
        }
    }

    private void handlerTrackMessage(TrackPacket trackPacket) {
        try {
            MiuiSentinelMemoryManager.getInstance().outPutTrackLog(trackPacket);
        } catch (IOException e) {
            Slog.e(TAG, "Track stack output to file failed", new Throwable());
        }
        ConcurrentHashMap<String, NativeHeapUsageInfo> tracklist = MiuiSentinelMemoryManager.getInstance().getTrackList();
        StringBuilder key = new StringBuilder();
        key.append(trackPacket.getProcess_name()).append("#").append(trackPacket.getPid());
        NativeHeapUsageInfo nativeHeapUsageInfo = tracklist.get(key.toString());
        if (nativeHeapUsageInfo != null) {
            nativeHeapUsageInfo.setStackTrace(trackPacket.getData());
            tracklist.remove(key);
            MiuiSentinelMemoryManager.getInstance().sendMessage(nativeHeapUsageInfo, 6);
        }
    }

    private void initWhilteList(Context context) {
        Resources r = context.getResources();
        String[] javaheaps = r.getStringArray(285409301);
        String[] nativeheaps = r.getStringArray(285409302);
        if (javaheaps == null || javaheaps.length == 0 || nativeheaps == null || nativeheaps.length == 0) {
            Slog.e(TAG, "initwhileList is failed");
        }
        for (String javaheap : javaheaps) {
            String[] split = javaheap.split(",");
            Integer threshold = Integer.valueOf(Integer.parseInt(split[1]));
            APP_JAVAHEAP_WHITE_LIST.put(split[0], threshold);
        }
        for (String nativeheap : nativeheaps) {
            String[] split2 = nativeheap.split(",");
            Integer threshold2 = Integer.valueOf(Integer.parseInt(split2[1]));
            APP_NATIVEHEAP_WHITE_LIST.put(split2[0], threshold2);
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiSentinelService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = MiuiSentinelService.getInstance().initContext(context);
        }

        public void onStart() {
            if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("persist.sys.debug.enable_sentinel_memory_monitor", false)) {
                MiuiSentinelService.miuiSentinelServiceThread.start();
                MiuiSentinelService.nRegisteredBpfEvent();
                publishBinderService(MiuiSentinelService.SERVICE_NAME, this.mService);
            }
        }
    }

    public static HashMap<String, Integer> getAppNativeheapWhiteList() {
        return APP_NATIVEHEAP_WHITE_LIST;
    }

    public static HashMap<String, Integer> getAppJavaheapWhiteList() {
        return APP_JAVAHEAP_WHITE_LIST;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LocalSocket createSystemServerSocketForProchunter() {
        LocalSocket serverSocket = null;
        try {
            serverSocket = new LocalSocket(1);
            serverSocket.bind(new LocalSocketAddress(HOST_NAME, LocalSocketAddress.Namespace.ABSTRACT));
            serverSocket.setSendBufferSize(10000);
            serverSocket.setReceiveBufferSize(10000);
            Slog.e(TAG, "prochunter socket create success");
            return serverSocket;
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e2) {
                }
                return null;
            }
            return serverSocket;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recvMessage(FileDescriptor fd) {
        if (DEBUG) {
            Slog.e(TAG, "begin recv message");
        }
        byte[] splitbuffer = new byte[8];
        try {
            FileInputStream fi = new FileInputStream(fd);
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fi, DEFAULT_BUFFER_SIZE);
                try {
                    bufferedInputStream.read(splitbuffer, 0, 4);
                    int type = SocketPacket.readInt(splitbuffer);
                    if (type > 10) {
                        SocketPacket socketPacket = parseSocketPacket(bufferedInputStream);
                        if (MiuiSentinelMemoryManager.getInstance().filterMessages(socketPacket)) {
                            handlerTrigger(socketPacket);
                        }
                    } else if (type == 6) {
                        TrackPacket trackPacket = parseTrackPacket(bufferedInputStream);
                        handlerTrackMessage(trackPacket);
                    }
                    bufferedInputStream.close();
                    fi.close();
                } finally {
                }
            } catch (Throwable th) {
                try {
                    fi.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private SocketPacket parseSocketPacket(BufferedInputStream bufferedInputStream) {
        SocketPacket socketPacket = new SocketPacket();
        StringBuilder sb = new StringBuilder();
        byte[] databuff = new byte[MAX_BUFF_SIZE];
        byte[] splitbuffer = new byte[128];
        try {
            bufferedInputStream.read(splitbuffer, 0, 4);
            socketPacket.setPid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 8);
            socketPacket.setGrowsize(SocketPacket.readLong(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 30);
            socketPacket.setEvent_type(SocketPacket.readString(splitbuffer, 0, 30));
            bufferedInputStream.read(splitbuffer, 0, 128);
            socketPacket.setProcess_name(SocketPacket.readString(splitbuffer, 0, 128));
            while (bufferedInputStream.available() > 0) {
                int readsize = bufferedInputStream.read(databuff);
                if (readsize == 9000) {
                    sb.append(SocketPacket.readString(databuff, 0, MAX_BUFF_SIZE));
                } else {
                    sb.append(SocketPacket.readString(databuff, 0, readsize));
                }
            }
            socketPacket.setData(sb.toString());
            if (DEBUG) {
                Slog.e(TAG, "SocketPacket data = " + socketPacket.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return socketPacket;
    }

    private TrackPacket parseTrackPacket(BufferedInputStream bufferedInputStream) {
        TrackPacket trackPacket = new TrackPacket();
        StringBuilder sb = new StringBuilder();
        byte[] splitbuffer = new byte[64];
        try {
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setReport_id(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setReport_argsz(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 64);
            trackPacket.setProcess_name(SocketPacket.readString(splitbuffer, 0, 64));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setPid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setTid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 12);
            trackPacket.setTimestamp(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 8);
            trackPacket.setReport_sz(SocketPacket.readInt(splitbuffer));
            Slog.e(TAG, "track-heap report_sz:" + trackPacket.getReport_sz());
            int chunkSize = trackPacket.getReport_sz();
            byte[] databuff = new byte[MAX_BUFF_SIZE];
            int totalRead = 0;
            while (totalRead < chunkSize) {
                int readsize = bufferedInputStream.read(databuff, 0, Math.min(chunkSize - totalRead, databuff.length));
                if (readsize == -1) {
                    break;
                }
                if (readsize == 9000) {
                    sb.append(SocketPacket.readString(databuff, 0, MAX_BUFF_SIZE));
                } else {
                    sb.append(SocketPacket.readString(databuff, 0, readsize));
                }
                totalRead += readsize;
            }
            trackPacket.setData(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return trackPacket;
    }

    private void initSystemServerTrack() {
        int systemPid = Process.myPid();
        MiuiSentinelMemoryManager.getInstance().handlerTriggerTrack(systemPid, MiuiSentinelMemoryManager.Action.START_TRACK);
    }
}
