package vendor.qti.sla.service.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class StatusCode {
    public static final int FAILURE = -1;
    public static final int SUCCESS = 0;

    public static final String toString(int o) {
        if (o == -1) {
            return "FAILURE";
        }
        if (o == 0) {
            return "SUCCESS";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & (-1)) == -1) {
            list.add("FAILURE");
            flipped = 0 | (-1);
        }
        list.add("SUCCESS");
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
