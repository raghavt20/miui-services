package com.xiaomi.NetworkBoost.slaservice.netlinkclient;

import android.system.OsConstants;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.nio.ByteBuffer;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;

/* loaded from: classes.dex */
public class NetlinkMessage {
    private static final String TAG = "NetlinkMessage";
    protected StructNlMsgHdr mHeader;

    public static NetlinkMessage parse(ByteBuffer byteBuffer, int nlFamily) {
        if (byteBuffer != null) {
            byteBuffer.position();
        }
        StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(byteBuffer);
        if (nlmsghdr == null) {
            return null;
        }
        int payloadLength = NetlinkConstants.alignedLengthOf(nlmsghdr.nlmsg_len) - 16;
        if (payloadLength < 0 || payloadLength > byteBuffer.remaining()) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        }
        if (nlmsghdr.nlmsg_type <= 15) {
            return parseCtlMessage(nlmsghdr, byteBuffer, payloadLength);
        }
        if (nlFamily == OsConstants.NETLINK_ROUTE) {
            return parseRtMessage(nlmsghdr, byteBuffer);
        }
        if (nlFamily == OsConstants.NETLINK_INET_DIAG) {
            return parseInetDiagMessage(nlmsghdr, byteBuffer);
        }
        if (nlFamily != OsConstants.NETLINK_NETFILTER) {
            return null;
        }
        return parseNfMessage(nlmsghdr, byteBuffer);
    }

    public NetlinkMessage(StructNlMsgHdr nlmsghdr) {
        this.mHeader = nlmsghdr;
    }

    public StructNlMsgHdr getHeader() {
        return this.mHeader;
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("NetlinkMessage{");
        StructNlMsgHdr structNlMsgHdr = this.mHeader;
        return append.append(structNlMsgHdr == null ? "" : structNlMsgHdr.toString()).append("}").toString();
    }

    private static NetlinkMessage parseCtlMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer, int payloadLength) {
        switch (nlmsghdr.nlmsg_type) {
            case 2:
                return NetlinkErrorMessage.parse(nlmsghdr, byteBuffer);
            default:
                byteBuffer.position(byteBuffer.position() + payloadLength);
                return new NetlinkMessage(nlmsghdr);
        }
    }

    private static NetlinkMessage parseRtMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 28:
            case IResultValue.MISYS_ESPIPE /* 29 */:
            case 30:
                return RtNetlinkNeighborMessage.parse(nlmsghdr, byteBuffer);
            case CommunicationUtil.SEND_COMMAND_BYTE_LONG /* 68 */:
                return NduseroptMessage.parse(nlmsghdr, byteBuffer);
            default:
                return null;
        }
    }

    private static NetlinkMessage parseInetDiagMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 20:
                return InetDiagMessage.parse(nlmsghdr, byteBuffer);
            default:
                return null;
        }
    }

    private static NetlinkMessage parseNfMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 256:
            case 258:
                return ConntrackMessage.parse(nlmsghdr, byteBuffer);
            case 257:
            default:
                return null;
        }
    }
}
