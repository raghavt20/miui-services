package com.android.net.module.util.netlink;

import android.net.util.SocketUtils;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class NetlinkUtils {
    public static final int DEFAULT_RECV_BUFSIZE = 8192;
    public static final int INET_DIAG_INFO = 2;
    public static final int INET_DIAG_MARK = 15;
    public static final int INIT_MARK_VALUE = 0;
    public static final long IO_TIMEOUT_MS = 300;
    public static final int NULL_MASK = 0;
    public static final int SOCKET_RECV_BUFSIZE = 65536;
    private static final String TAG = "NetlinkUtils";
    public static final int TCP_ALIVE_STATE_FILTER = 14;
    private static final int TCP_ESTABLISHED = 1;
    private static final int TCP_SYN_RECV = 3;
    private static final int TCP_SYN_SENT = 2;
    public static final int UNKNOWN_MARK = -1;

    public static boolean enoughBytesRemainForValidNlMsg(ByteBuffer bytes) {
        return bytes.remaining() >= 16;
    }

    private static NetlinkErrorMessage parseNetlinkErrorMessage(ByteBuffer bytes) {
        StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(bytes);
        if (nlmsghdr == null || nlmsghdr.nlmsg_type != 2) {
            return null;
        }
        return NetlinkErrorMessage.parse(nlmsghdr, bytes);
    }

    public static void receiveNetlinkAck(FileDescriptor fd) throws InterruptedIOException, ErrnoException {
        String errmsg;
        ByteBuffer bytes = recvMessage(fd, 8192, 300L);
        NetlinkErrorMessage response = parseNetlinkErrorMessage(bytes);
        if (response != null && response.getNlMsgError() != null) {
            int errno = response.getNlMsgError().error;
            if (errno != 0) {
                Log.e(TAG, "receiveNetlinkAck, errmsg=" + response.toString());
                throw new ErrnoException(response.toString(), Math.abs(errno));
            }
            return;
        }
        if (response == null) {
            bytes.position(0);
            errmsg = "raw bytes: " + NetlinkConstants.hexify(bytes);
        } else {
            errmsg = response.toString();
        }
        Log.e(TAG, "receiveNetlinkAck, errmsg=" + errmsg);
        throw new ErrnoException(errmsg, OsConstants.EPROTO);
    }

    public static void sendOneShotKernelMessage(int nlProto, byte[] msg) throws ErrnoException {
        FileDescriptor fd = netlinkSocketForProto(nlProto);
        try {
            try {
                try {
                    connectSocketToNetlink(fd);
                    sendMessage(fd, msg, 0, msg.length, 300L);
                    receiveNetlinkAck(fd);
                    try {
                        SocketUtils.closeSocket(fd);
                    } catch (IOException e) {
                    }
                } catch (InterruptedIOException e2) {
                    Log.e(TAG, "Error in NetlinkSocket.sendOneShotKernelMessage", e2);
                    throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.ETIMEDOUT, e2);
                }
            } catch (SocketException e3) {
                Log.e(TAG, "Error in NetlinkSocket.sendOneShotKernelMessage", e3);
                throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.EIO, e3);
            }
        } catch (Throwable th) {
            try {
                SocketUtils.closeSocket(fd);
            } catch (IOException e4) {
            }
            throw th;
        }
    }

    public static FileDescriptor netlinkSocketForProto(int nlProto) throws ErrnoException {
        FileDescriptor fd = Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM, nlProto);
        Os.setsockoptInt(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 65536);
        return fd;
    }

    public static FileDescriptor createNetLinkInetDiagSocket() throws ErrnoException {
        return Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM | OsConstants.SOCK_CLOEXEC, OsConstants.NETLINK_INET_DIAG);
    }

    public static void connectSocketToNetlink(FileDescriptor fd) throws ErrnoException, SocketException {
        Os.connect(fd, SocketUtils.makeNetlinkSocketAddress(0, 0));
    }

    private static void checkTimeout(long timeoutMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("Negative timeouts not permitted");
        }
    }

    public static ByteBuffer recvMessage(FileDescriptor fd, int bufsize, long timeoutMs) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(timeoutMs);
        Os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(timeoutMs));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufsize);
        int length = Os.read(fd, byteBuffer);
        if (length == bufsize) {
            Log.w(TAG, "maximum read");
        }
        byteBuffer.position(0);
        byteBuffer.limit(length);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer;
    }

    public static int sendMessage(FileDescriptor fd, byte[] bytes, int offset, int count, long timeoutMs) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(timeoutMs);
        Os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(timeoutMs));
        return Os.write(fd, bytes, offset, count);
    }

    private NetlinkUtils() {
    }
}
