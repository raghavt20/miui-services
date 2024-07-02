package com.android.net.module.util.netlink;

import android.system.OsConstants;
import android.util.Log;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class StructInetDiagSockId {
    private static final long INET_DIAG_NOCOOKIE = -1;
    public static final int STRUCT_SIZE = 48;
    public final long cookie;
    public final int ifIndex;
    public final InetSocketAddress locSocketAddress;
    public final InetSocketAddress remSocketAddress;
    private static final String TAG = StructInetDiagSockId.class.getSimpleName();
    private static final byte[] IPV4_PADDING = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public StructInetDiagSockId(InetSocketAddress loc, InetSocketAddress rem) {
        this(loc, rem, 0, -1L);
    }

    public StructInetDiagSockId(InetSocketAddress loc, InetSocketAddress rem, int ifIndex, long cookie) {
        this.locSocketAddress = loc;
        this.remSocketAddress = rem;
        this.ifIndex = ifIndex;
        this.cookie = cookie;
    }

    public static StructInetDiagSockId parse(ByteBuffer byteBuffer, short family) {
        InetAddress dstAddr;
        InetAddress srcAddr;
        if (byteBuffer.remaining() < 48) {
            return null;
        }
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int srcPort = Short.toUnsignedInt(byteBuffer.getShort());
        int dstPort = Short.toUnsignedInt(byteBuffer.getShort());
        if (family == OsConstants.AF_INET) {
            byte[] srcAddrByte = new byte[4];
            byte[] dstAddrByte = new byte[4];
            byteBuffer.get(srcAddrByte);
            byteBuffer.position(byteBuffer.position() + 12);
            byteBuffer.get(dstAddrByte);
            byteBuffer.position(byteBuffer.position() + 12);
            try {
                srcAddr = Inet4Address.getByAddress(srcAddrByte);
                dstAddr = Inet4Address.getByAddress(dstAddrByte);
            } catch (UnknownHostException e) {
                Log.wtf(TAG, "Failed to parse address: " + e);
                return null;
            }
        } else if (family == OsConstants.AF_INET6) {
            byte[] srcAddrByte2 = new byte[16];
            byte[] dstAddrByte2 = new byte[16];
            byteBuffer.get(srcAddrByte2);
            byteBuffer.get(dstAddrByte2);
            try {
                InetAddress srcAddr2 = Inet6Address.getByAddress((String) null, srcAddrByte2, -1);
                dstAddr = Inet6Address.getByAddress((String) null, dstAddrByte2, -1);
                srcAddr = srcAddr2;
            } catch (UnknownHostException e2) {
                Log.wtf(TAG, "Failed to parse address: " + e2);
                return null;
            }
        } else {
            Log.wtf(TAG, "Invalid address family: " + ((int) family));
            return null;
        }
        InetSocketAddress srcSocketAddr = new InetSocketAddress(srcAddr, srcPort);
        InetSocketAddress dstSocketAddr = new InetSocketAddress(dstAddr, dstPort);
        byteBuffer.order(ByteOrder.nativeOrder());
        int ifIndex = byteBuffer.getInt();
        long cookie = byteBuffer.getLong();
        return new StructInetDiagSockId(srcSocketAddr, dstSocketAddr, ifIndex, cookie);
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) this.locSocketAddress.getPort());
        byteBuffer.putShort((short) this.remSocketAddress.getPort());
        byteBuffer.put(this.locSocketAddress.getAddress().getAddress());
        if (this.locSocketAddress.getAddress() instanceof Inet4Address) {
            byteBuffer.put(IPV4_PADDING);
        }
        byteBuffer.put(this.remSocketAddress.getAddress().getAddress());
        if (this.remSocketAddress.getAddress() instanceof Inet4Address) {
            byteBuffer.put(IPV4_PADDING);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.putInt(this.ifIndex);
        byteBuffer.putLong(this.cookie);
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("StructInetDiagSockId{ idiag_sport{").append(this.locSocketAddress.getPort()).append("}, idiag_dport{").append(this.remSocketAddress.getPort()).append("}, idiag_src{").append(this.locSocketAddress.getAddress().getHostAddress()).append("}, idiag_dst{").append(this.remSocketAddress.getAddress().getHostAddress()).append("}, idiag_if{").append(this.ifIndex).append("}, idiag_cookie{");
        long j = this.cookie;
        return append.append(j == -1 ? "INET_DIAG_NOCOOKIE" : Long.valueOf(j)).append("}}").toString();
    }
}
