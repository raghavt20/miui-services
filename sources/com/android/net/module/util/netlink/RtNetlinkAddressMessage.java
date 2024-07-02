package com.android.net.module.util.netlink;

import android.system.OsConstants;
import com.android.net.module.util.HexDump;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/* loaded from: classes.dex */
public class RtNetlinkAddressMessage extends NetlinkMessage {
    public static final short IFA_ADDRESS = 1;
    public static final short IFA_CACHEINFO = 6;
    public static final short IFA_FLAGS = 8;
    private int mFlags;
    private StructIfacacheInfo mIfacacheInfo;
    private StructIfaddrMsg mIfaddrmsg;
    private InetAddress mIpAddress;

    public RtNetlinkAddressMessage(StructNlMsgHdr header, StructIfaddrMsg ifaddrMsg, InetAddress ipAddress, StructIfacacheInfo structIfacacheInfo, int flags) {
        super(header);
        this.mIfaddrmsg = ifaddrMsg;
        this.mIpAddress = ipAddress;
        this.mIfacacheInfo = structIfacacheInfo;
        this.mFlags = flags;
    }

    private RtNetlinkAddressMessage(StructNlMsgHdr header) {
        this(header, null, null, null, 0);
    }

    public int getFlags() {
        return this.mFlags;
    }

    public StructIfaddrMsg getIfaddrHeader() {
        return this.mIfaddrmsg;
    }

    public InetAddress getIpAddress() {
        return this.mIpAddress;
    }

    public StructIfacacheInfo getIfacacheInfo() {
        return this.mIfacacheInfo;
    }

    public static RtNetlinkAddressMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        Integer value;
        RtNetlinkAddressMessage addrMsg = new RtNetlinkAddressMessage(header);
        StructIfaddrMsg parse = StructIfaddrMsg.parse(byteBuffer);
        addrMsg.mIfaddrmsg = parse;
        if (parse == null) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr == null) {
            return null;
        }
        InetAddress valueAsInetAddress = nlAttr.getValueAsInetAddress();
        addrMsg.mIpAddress = valueAsInetAddress;
        if (valueAsInetAddress == null) {
            return null;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 6, byteBuffer);
        if (nlAttr2 != null) {
            addrMsg.mIfacacheInfo = StructIfacacheInfo.parse(nlAttr2.getValueAsByteBuffer());
        }
        addrMsg.mFlags = addrMsg.mIfaddrmsg.flags;
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType((short) 8, byteBuffer);
        if (nlAttr3 == null || (value = nlAttr3.getValueAsInteger()) == null) {
            return null;
        }
        addrMsg.mFlags = value.intValue();
        return addrMsg;
    }

    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mIfaddrmsg.pack(byteBuffer);
        StructNlAttr address = new StructNlAttr((short) 1, this.mIpAddress);
        address.pack(byteBuffer);
        StructIfacacheInfo structIfacacheInfo = this.mIfacacheInfo;
        if (structIfacacheInfo != null) {
            StructNlAttr cacheInfo = new StructNlAttr((short) 6, structIfacacheInfo.writeToBytes());
            cacheInfo.pack(byteBuffer);
        }
        StructNlAttr flags = new StructNlAttr((short) 8, this.mFlags);
        flags.pack(byteBuffer);
    }

    public static byte[] newRtmNewAddressMessage(int seqNo, InetAddress ip, short prefixlen, int flags, byte scope, int ifIndex, long preferred, long valid) {
        Objects.requireNonNull(ip, "IP address to be set via netlink message cannot be null");
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_type = (short) 20;
        nlmsghdr.nlmsg_flags = (short) 261;
        nlmsghdr.nlmsg_seq = seqNo;
        RtNetlinkAddressMessage msg = new RtNetlinkAddressMessage(nlmsghdr);
        byte family = (byte) (ip instanceof Inet6Address ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        msg.mIfaddrmsg = new StructIfaddrMsg(family, prefixlen, (short) 0, scope, ifIndex);
        msg.mIpAddress = ip;
        msg.mIfacacheInfo = new StructIfacacheInfo(preferred, valid, 0L, 0L);
        msg.mFlags = flags;
        byte[] bytes = new byte[msg.getRequiredSpace()];
        nlmsghdr.nlmsg_len = bytes.length;
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        msg.pack(byteBuffer);
        return bytes;
    }

    private int getRequiredSpace() {
        int spaceRequired = 24 + NetlinkConstants.alignedLengthOf(this.mIpAddress.getAddress().length + 4);
        return spaceRequired + NetlinkConstants.alignedLengthOf(20) + 8;
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        StringBuilder append = new StringBuilder().append("RtNetlinkAddressMessage{ nlmsghdr{").append(this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_ROUTE))).append("}, Ifaddrmsg{").append(this.mIfaddrmsg.toString()).append("}, IP Address{").append(this.mIpAddress.getHostAddress()).append("}, IfacacheInfo{");
        StructIfacacheInfo structIfacacheInfo = this.mIfacacheInfo;
        return append.append(structIfacacheInfo == null ? "" : structIfacacheInfo.toString()).append("}, Address Flags{").append(HexDump.toHexString(this.mFlags)).append("} }").toString();
    }
}
