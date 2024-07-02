package com.android.net.module.util.netlink;

import android.net.util.SocketUtils;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.Range;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class InetDiagMessage extends NetlinkMessage {
    private static final int[] FAMILY = {OsConstants.AF_INET6, OsConstants.AF_INET};
    public static final String TAG = "InetDiagMessage";
    private static final int TIMEOUT_MS = 500;
    public StructInetDiagMsg inetDiagMsg;

    public static byte[] inetDiagReqV2(int protocol, InetSocketAddress local, InetSocketAddress remote, int family, short flags) {
        return inetDiagReqV2(protocol, local, remote, family, flags, 0, 0, -1);
    }

    public static byte[] inetDiagReqV2(int protocol, InetSocketAddress local, InetSocketAddress remote, int family, short flags, int pad, int idiagExt, int state) throws IllegalArgumentException {
        if ((local == null) != (remote == null)) {
            throw new IllegalArgumentException("Local and remote must be both null or both non-null");
        }
        StructInetDiagSockId id = (local == null || remote == null) ? null : new StructInetDiagSockId(local, remote);
        return inetDiagReqV2(protocol, id, family, (short) 20, flags, pad, idiagExt, state);
    }

    public static byte[] inetDiagReqV2(int protocol, StructInetDiagSockId id, int family, short type, short flags, int pad, int idiagExt, int state) {
        byte[] bytes = new byte[72];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        StructNlMsgHdr nlMsgHdr = new StructNlMsgHdr();
        nlMsgHdr.nlmsg_len = bytes.length;
        nlMsgHdr.nlmsg_type = type;
        nlMsgHdr.nlmsg_flags = flags;
        nlMsgHdr.pack(byteBuffer);
        StructInetDiagReqV2 inetDiagReqV2 = new StructInetDiagReqV2(protocol, id, family, pad, idiagExt, state);
        inetDiagReqV2.pack(byteBuffer);
        return bytes;
    }

    public InetDiagMessage(StructNlMsgHdr header) {
        super(header);
        this.inetDiagMsg = new StructInetDiagMsg();
    }

    public static InetDiagMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        InetDiagMessage msg = new InetDiagMessage(header);
        StructInetDiagMsg parse = StructInetDiagMsg.parse(byteBuffer);
        msg.inetDiagMsg = parse;
        if (parse == null) {
            return null;
        }
        return msg;
    }

    private static void closeSocketQuietly(FileDescriptor fd) {
        try {
            SocketUtils.closeSocket(fd);
        } catch (IOException e) {
        }
    }

    private static int lookupUidByFamily(int protocol, InetSocketAddress local, InetSocketAddress remote, int family, short flags, FileDescriptor fd) throws ErrnoException, InterruptedIOException {
        byte[] msg = inetDiagReqV2(protocol, local, remote, family, flags);
        NetlinkUtils.sendMessage(fd, msg, 0, msg.length, 500L);
        ByteBuffer response = NetlinkUtils.recvMessage(fd, 8192, 500L);
        NetlinkMessage nlMsg = NetlinkMessage.parse(response, OsConstants.NETLINK_INET_DIAG);
        if (nlMsg == null) {
            return -1;
        }
        StructNlMsgHdr hdr = nlMsg.getHeader();
        if (hdr.nlmsg_type == 3 || !(nlMsg instanceof InetDiagMessage)) {
            return -1;
        }
        return ((InetDiagMessage) nlMsg).inetDiagMsg.idiag_uid;
    }

    private static int lookupUid(int protocol, InetSocketAddress local, InetSocketAddress remote, FileDescriptor fd) throws ErrnoException, InterruptedIOException {
        int uid;
        for (int family : FAMILY) {
            if (protocol == OsConstants.IPPROTO_UDP) {
                uid = lookupUidByFamily(protocol, remote, local, family, (short) 1, fd);
            } else {
                uid = lookupUidByFamily(protocol, local, remote, family, (short) 1, fd);
            }
            if (uid != -1) {
                return uid;
            }
        }
        if (protocol == OsConstants.IPPROTO_UDP) {
            try {
                InetSocketAddress wildcard = new InetSocketAddress(Inet6Address.getByName("::"), 0);
                int uid2 = lookupUidByFamily(protocol, local, wildcard, OsConstants.AF_INET6, (short) 769, fd);
                if (uid2 != -1) {
                    return uid2;
                }
                InetSocketAddress wildcard2 = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), 0);
                int uid3 = lookupUidByFamily(protocol, local, wildcard2, OsConstants.AF_INET, (short) 769, fd);
                if (uid3 != -1) {
                    return uid3;
                }
            } catch (UnknownHostException e) {
                Log.e("InetDiagMessage", e.toString());
            }
        }
        return -1;
    }

    public static int getConnectionOwnerUid(int protocol, InetSocketAddress local, InetSocketAddress remote) {
        int uid = -1;
        FileDescriptor fd = null;
        try {
            try {
                fd = NetlinkUtils.netlinkSocketForProto(OsConstants.NETLINK_INET_DIAG);
                NetlinkUtils.connectSocketToNetlink(fd);
                uid = lookupUid(protocol, local, remote, fd);
            } catch (ErrnoException | InterruptedIOException | IllegalArgumentException | SocketException e) {
                Log.e("InetDiagMessage", e.toString());
            }
            return uid;
        } finally {
            closeSocketQuietly(fd);
        }
    }

    public static byte[] buildInetDiagReqForAliveTcpSockets(int family) {
        return inetDiagReqV2(OsConstants.IPPROTO_TCP, (InetSocketAddress) null, (InetSocketAddress) null, family, (short) 769, 0, 2, 14);
    }

    private static void sendNetlinkDestroyRequest(FileDescriptor fd, int proto, InetDiagMessage diagMsg) throws InterruptedIOException, ErrnoException {
        byte[] destroyMsg = inetDiagReqV2(proto, diagMsg.inetDiagMsg.id, (int) diagMsg.inetDiagMsg.idiag_family, (short) 21, (short) 5, 0, 0, 1 << diagMsg.inetDiagMsg.idiag_state);
        NetlinkUtils.sendMessage(fd, destroyMsg, 0, destroyMsg.length, 300L);
        NetlinkUtils.receiveNetlinkAck(fd);
    }

    private static void sendNetlinkDumpRequest(FileDescriptor fd, int proto, int states, int family) throws InterruptedIOException, ErrnoException {
        byte[] dumpMsg = inetDiagReqV2(proto, (StructInetDiagSockId) null, family, (short) 20, (short) 769, 0, 0, states);
        NetlinkUtils.sendMessage(fd, dumpMsg, 0, dumpMsg.length, 300L);
    }

    private static int processNetlinkDumpAndDestroySockets(FileDescriptor dumpFd, FileDescriptor destroyFd, int proto, Predicate<InetDiagMessage> filter) throws InterruptedIOException, ErrnoException {
        int destroyedSockets = 0;
        while (true) {
            ByteBuffer buf = NetlinkUtils.recvMessage(dumpFd, 8192, 300L);
            while (true) {
                if (buf.remaining() > 0) {
                    int position = buf.position();
                    NetlinkMessage nlMsg = NetlinkMessage.parse(buf, OsConstants.NETLINK_INET_DIAG);
                    if (nlMsg == null) {
                        buf.position(position);
                        Log.e("InetDiagMessage", "Failed to parse netlink message: " + NetlinkConstants.hexify(buf));
                        break;
                    }
                    if (nlMsg.getHeader().nlmsg_type == 3) {
                        return destroyedSockets;
                    }
                    if (!(nlMsg instanceof InetDiagMessage)) {
                        Log.wtf("InetDiagMessage", "Received unexpected netlink message: " + nlMsg);
                    } else {
                        InetDiagMessage diagMsg = (InetDiagMessage) nlMsg;
                        if (filter.test(diagMsg)) {
                            try {
                                sendNetlinkDestroyRequest(destroyFd, proto, diagMsg);
                                destroyedSockets++;
                            } catch (ErrnoException | InterruptedIOException e) {
                                if (!(e instanceof ErrnoException) || ((ErrnoException) e).errno != OsConstants.ENOENT) {
                                    Log.e("InetDiagMessage", "Failed to destroy socket: diagMsg=" + diagMsg + ", " + e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isAdbSocket(InetDiagMessage msg) {
        return msg.inetDiagMsg.idiag_uid == 2000;
    }

    public static boolean containsUid(InetDiagMessage msg, Set<Range<Integer>> ranges) {
        for (Range<Integer> range : ranges) {
            if (range.contains((Range<Integer>) Integer.valueOf(msg.inetDiagMsg.idiag_uid))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLoopbackAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) {
            return true;
        }
        if (!(addr instanceof Inet6Address)) {
            return false;
        }
        byte[] addrBytes = addr.getAddress();
        for (int i = 0; i < 10; i++) {
            if (addrBytes[i] != 0) {
                return false;
            }
        }
        int i2 = addrBytes[10];
        return i2 == -1 && addrBytes[11] == -1 && addrBytes[12] == Byte.MAX_VALUE;
    }

    public static boolean isLoopback(InetDiagMessage msg) {
        InetAddress srcAddr = msg.inetDiagMsg.id.locSocketAddress.getAddress();
        InetAddress dstAddr = msg.inetDiagMsg.id.remSocketAddress.getAddress();
        return isLoopbackAddress(srcAddr) || isLoopbackAddress(dstAddr) || srcAddr.equals(dstAddr);
    }

    private static void destroySockets(int proto, int states, Predicate<InetDiagMessage> filter) throws ErrnoException, SocketException, InterruptedIOException {
        FileDescriptor dumpFd = null;
        FileDescriptor destroyFd = null;
        try {
            dumpFd = NetlinkUtils.createNetLinkInetDiagSocket();
            destroyFd = NetlinkUtils.createNetLinkInetDiagSocket();
            NetlinkUtils.connectSocketToNetlink(dumpFd);
            NetlinkUtils.connectSocketToNetlink(destroyFd);
            Iterator it = List.of(Integer.valueOf(OsConstants.AF_INET), Integer.valueOf(OsConstants.AF_INET6)).iterator();
            while (it.hasNext()) {
                int family = ((Integer) it.next()).intValue();
                try {
                    sendNetlinkDumpRequest(dumpFd, proto, states, family);
                    int destroyedSockets = processNetlinkDumpAndDestroySockets(dumpFd, destroyFd, proto, filter);
                    Log.d("InetDiagMessage", "Destroyed " + destroyedSockets + " sockets, proto=" + NetlinkConstants.stringForProtocol(proto) + ", family=" + NetlinkConstants.stringForAddressFamily(family) + ", states=" + states);
                } catch (ErrnoException | InterruptedIOException e) {
                    Log.e("InetDiagMessage", "Failed to send netlink dump request: " + e);
                }
            }
        } finally {
            closeSocketQuietly(dumpFd);
            closeSocketQuietly(destroyFd);
        }
    }

    public static void destroyLiveTcpSockets(final Set<Range<Integer>> ranges, final Set<Integer> exemptUids) throws SocketException, InterruptedIOException, ErrnoException {
        long startTimeMs = SystemClock.elapsedRealtime();
        destroySockets(OsConstants.IPPROTO_TCP, 14, new Predicate() { // from class: com.android.net.module.util.netlink.InetDiagMessage$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return InetDiagMessage.lambda$destroyLiveTcpSockets$0(exemptUids, ranges, (InetDiagMessage) obj);
            }
        });
        long durationMs = SystemClock.elapsedRealtime() - startTimeMs;
        Log.d("InetDiagMessage", "Destroyed live tcp sockets for uids=" + ranges + " exemptUids=" + exemptUids + " in " + durationMs + "ms");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$destroyLiveTcpSockets$0(Set exemptUids, Set ranges, InetDiagMessage diagMsg) {
        return (exemptUids.contains(Integer.valueOf(diagMsg.inetDiagMsg.idiag_uid)) || !containsUid(diagMsg, ranges) || isLoopback(diagMsg) || isAdbSocket(diagMsg)) ? false : true;
    }

    public static void destroyLiveTcpSocketsByOwnerUids(final Set<Integer> ownerUids) throws SocketException, InterruptedIOException, ErrnoException {
        long startTimeMs = SystemClock.elapsedRealtime();
        destroySockets(OsConstants.IPPROTO_TCP, 14, new Predicate() { // from class: com.android.net.module.util.netlink.InetDiagMessage$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return InetDiagMessage.lambda$destroyLiveTcpSocketsByOwnerUids$1(ownerUids, (InetDiagMessage) obj);
            }
        });
        long durationMs = SystemClock.elapsedRealtime() - startTimeMs;
        Log.d("InetDiagMessage", "Destroyed live tcp sockets for uids=" + ownerUids + " in " + durationMs + "ms");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$destroyLiveTcpSocketsByOwnerUids$1(Set ownerUids, InetDiagMessage diagMsg) {
        return (!ownerUids.contains(Integer.valueOf(diagMsg.inetDiagMsg.idiag_uid)) || isLoopback(diagMsg) || isAdbSocket(diagMsg)) ? false : true;
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        StringBuilder append = new StringBuilder().append("InetDiagMessage{ nlmsghdr{").append(this.mHeader == null ? "" : this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_INET_DIAG))).append("}, inet_diag_msg{");
        StructInetDiagMsg structInetDiagMsg = this.inetDiagMsg;
        return append.append(structInetDiagMsg != null ? structInetDiagMsg.toString() : "").append("} }").toString();
    }
}
