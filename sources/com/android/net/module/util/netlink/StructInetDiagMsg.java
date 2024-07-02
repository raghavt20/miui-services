package com.android.net.module.util.netlink;

import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class StructInetDiagMsg {
    public static final int STRUCT_SIZE = 72;
    public StructInetDiagSockId id;
    public long idiag_expires;
    public short idiag_family;
    public long idiag_inode;
    public short idiag_retrans;
    public long idiag_rqueue;
    public short idiag_state;
    public short idiag_timer;
    public int idiag_uid;
    public long idiag_wqueue;

    private static short unsignedByte(byte b) {
        return (short) (b & 255);
    }

    public static StructInetDiagMsg parse(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 72) {
            return null;
        }
        StructInetDiagMsg struct = new StructInetDiagMsg();
        struct.idiag_family = unsignedByte(byteBuffer.get());
        struct.idiag_state = unsignedByte(byteBuffer.get());
        struct.idiag_timer = unsignedByte(byteBuffer.get());
        struct.idiag_retrans = unsignedByte(byteBuffer.get());
        StructInetDiagSockId parse = StructInetDiagSockId.parse(byteBuffer, struct.idiag_family);
        struct.id = parse;
        if (parse == null) {
            return null;
        }
        struct.idiag_expires = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_rqueue = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_wqueue = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_uid = byteBuffer.getInt();
        struct.idiag_inode = Integer.toUnsignedLong(byteBuffer.getInt());
        return struct;
    }

    public String toString() {
        return "StructInetDiagMsg{ idiag_family{" + ((int) this.idiag_family) + "}, idiag_state{" + ((int) this.idiag_state) + "}, idiag_timer{" + ((int) this.idiag_timer) + "}, idiag_retrans{" + ((int) this.idiag_retrans) + "}, id{" + this.id + "}, idiag_expires{" + this.idiag_expires + "}, idiag_rqueue{" + this.idiag_rqueue + "}, idiag_wqueue{" + this.idiag_wqueue + "}, idiag_uid{" + this.idiag_uid + "}, idiag_inode{" + this.idiag_inode + "}, }";
    }
}
