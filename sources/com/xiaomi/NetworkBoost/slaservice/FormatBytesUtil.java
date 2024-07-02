package com.xiaomi.NetworkBoost.slaservice;

/* loaded from: classes.dex */
public class FormatBytesUtil {
    public static final long GB = 1073741824;
    public static final long KB = 1024;
    public static final long MB = 1048576;
    public static String GBString = "GB";
    public static String MBString = "MB";
    public static String KBString = "KB";
    public static String BString = "B";

    public static String formatBytes(long bytes, int numOfPoint) {
        double f;
        String uint;
        if (bytes >= GB) {
            f = (bytes * 1.0d) / 1.073741824E9d;
            uint = GBString;
        } else if (bytes >= MB) {
            f = (bytes * 1.0d) / 1048576.0d;
            uint = MBString;
        } else if (bytes >= KB) {
            f = (bytes * 1.0d) / 1024.0d;
            uint = KBString;
        } else {
            double f2 = bytes;
            f = f2 * 1.0d;
            uint = BString;
        }
        return textFormat(f, uint, numOfPoint);
    }

    public static String formatBytes(long bytes) {
        double f;
        String uint;
        int numOfPoint = 1;
        if (bytes >= GB) {
            f = (bytes * 1.0d) / 1.073741824E9d;
            uint = GBString;
            numOfPoint = 2;
        } else if (bytes >= MB) {
            f = (bytes * 1.0d) / 1048576.0d;
            uint = MBString;
        } else if (bytes >= KB) {
            f = (bytes * 1.0d) / 1024.0d;
            uint = KBString;
        } else {
            double f2 = bytes;
            f = f2 * 1.0d;
            uint = BString;
        }
        return textFormat(f, uint, numOfPoint);
    }

    public static String formatBytesByMB(long bytes) {
        String uint = MBString;
        double f = (bytes * 1.0d) / 1048576.0d;
        return textFormat(f, uint, 0);
    }

    public static String[] formatBytesSplited(long bytes) {
        double f;
        String[] result = new String[2];
        int point = 2;
        if (bytes >= GB) {
            f = (bytes * 1.0d) / 1.073741824E9d;
            result[1] = GBString;
        } else if (bytes >= MB) {
            f = (bytes * 1.0d) / 1048576.0d;
            result[1] = MBString;
        } else if (bytes >= KB) {
            f = (bytes * 1.0d) / 1024.0d;
            result[1] = KBString;
        } else {
            double f2 = bytes;
            f = f2 * 1.0d;
            result[1] = BString;
            point = 0;
        }
        result[0] = textFormat(f, null, point);
        return result;
    }

    public static String formatUniteUnit(long bytes, long maxUnit) {
        double f = (bytes * 1.0d) / maxUnit;
        return textFormat(f, null, 1);
    }

    public static long formatMaxBytes(long bytes) {
        if (bytes >= GB) {
            return GB;
        }
        if (bytes > MB) {
            return MB;
        }
        if (bytes > KB) {
            return KB;
        }
        return 1L;
    }

    public static String formatBytesNoUint(long bytes) {
        double f;
        if (bytes >= GB) {
            f = (bytes * 1.0d) / 1.073741824E9d;
        } else if (bytes > MB) {
            f = (bytes * 1.0d) / 1048576.0d;
        } else if (bytes > KB) {
            f = (bytes * 1.0d) / 1024.0d;
        } else {
            double f2 = bytes;
            f = f2 * 1.0d;
        }
        return textFormat(f, null, 1);
    }

    public static String formatBytesWithUintLong(long bytes) {
        return String.valueOf(bytes / MB) + MBString;
    }

    private static String textFormat(double f, String uint, int numOfPoint) {
        String text;
        if (f > 999.5d || BString.equals(uint)) {
            text = String.format("%d", Integer.valueOf((int) f));
        } else if (f > 99.5d) {
            text = String.format("%.01f", Double.valueOf(f));
        } else {
            StringBuilder format = new StringBuilder(16);
            format.append("%.0");
            format.append(numOfPoint);
            format.append('f');
            text = String.format(format.toString(), Double.valueOf(f));
        }
        if (uint != null) {
            return text + uint;
        }
        return text;
    }

    public static long getBytesByUnit(float value, String unit) {
        String unit2 = unit.toLowerCase();
        if (unit2.contains("k")) {
            return 1024.0f * value;
        }
        if (unit2.contains("m")) {
            return 1048576.0f * value;
        }
        if (unit2.contains("g")) {
            return 1.07374182E9f * value;
        }
        return value;
    }
}
