package com.android.server.audio.pad;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class FaceInfo {
    private static final int AON_SCALE_X = SystemProperties.getInt("ro.vendor.audio.aon.xscale", 0);
    private static final int AON_SCALE_Y = SystemProperties.getInt("ro.vendor.audio.aon.yscale", 0);
    private static final String PREFIX_FACE_RECTS = "fvsam_voip_tx=FocusRegion@";
    private static final String PREFIX_VIEW_REGION = "fvsam_voip_tx=ViewRegion@";
    private static final int RESCALED_VIEW_HEIGHT = 2448;
    private static final int RESCALED_VIEW_WIDTH = 3264;
    public static final int SRC_AON = 1;
    public static final int SRC_CAMERA = 2;
    private static final String TAG = "PadAdapter.FaceInfo";
    public String mParamFocusRegion;
    public String mParamViewRegion;
    private int mSource;

    private FaceInfo(int source, String paramViewRegion, String paramFocusRegion) {
        this.mSource = source;
        this.mParamViewRegion = paramViewRegion;
        this.mParamFocusRegion = paramFocusRegion;
    }

    public static FaceInfo build(int src, int orientation, int[] viewRegion, int[] faceRects) {
        int i;
        int center;
        int i2;
        int i3;
        int center2;
        int[] iArr = faceRects;
        if (viewRegion == null || viewRegion.length != 4 || iArr == null) {
            return null;
        }
        int i4 = 1;
        int dataNumPerFace = src == 1 ? 6 : 4;
        int faceNum = iArr.length / dataNumPerFace;
        if (faceNum < 1 || iArr.length % dataNumPerFace != 0) {
            return null;
        }
        if (src == 1) {
            faceNum = 1;
        }
        int scene = orientationToScene(orientation);
        int curX1 = 0;
        int curY1 = 0;
        int curX2 = 0;
        int curY2 = 0;
        int width = viewRegion[2];
        int height = viewRegion[3];
        int[] rectsExtracted = new int[faceNum * 4];
        int faceId = 0;
        while (faceId < faceNum) {
            if (src != i4) {
                if (src == 2) {
                    curX1 = iArr[(faceId * dataNumPerFace) + 0];
                    curY1 = iArr[(faceId * dataNumPerFace) + 1];
                    curX2 = iArr[(faceId * dataNumPerFace) + 2];
                    curY2 = iArr[(faceId * dataNumPerFace) + 3];
                }
            } else {
                curX1 = iArr[(faceId * dataNumPerFace) + 2];
                curY1 = iArr[(faceId * dataNumPerFace) + 3];
                int curX22 = iArr[(faceId * dataNumPerFace) + 2] + iArr[(faceId * dataNumPerFace) + 0];
                int curX23 = faceId * dataNumPerFace;
                curY2 = iArr[curX23 + 3] + iArr[(faceId * dataNumPerFace) + 1];
                curX2 = curX22;
            }
            if (scene == i4 || scene == 2) {
                rectsExtracted[(faceId * 4) + 0] = height - curY2;
                rectsExtracted[(faceId * 4) + i4] = width - curX2;
                rectsExtracted[(faceId * 4) + 2] = height - curY1;
                i = 3;
                rectsExtracted[(faceId * 4) + 3] = width - curX1;
            } else {
                rectsExtracted[(faceId * 4) + 0] = curY1;
                rectsExtracted[(faceId * 4) + i4] = curX1;
                rectsExtracted[(faceId * 4) + 2] = curY2;
                rectsExtracted[(faceId * 4) + 3] = curX2;
                i = 3;
            }
            if (src == 1) {
                if (scene == 1 || scene == i) {
                    int center3 = (rectsExtracted[(faceId * 4) + 1] + rectsExtracted[(faceId * 4) + 3]) / 2;
                    int delta = (((width / 2) - center3) * AON_SCALE_Y) / (width / 2);
                    rectsExtracted[(faceId * 4) + 1] = rectsExtracted[(faceId * 4) + 1] + delta;
                    rectsExtracted[(faceId * 4) + 3] = rectsExtracted[(faceId * 4) + 3] + delta;
                    int i5 = (faceId * 4) + 1;
                    if (rectsExtracted[(faceId * 4) + 1] < 0) {
                        center = 0;
                    } else {
                        int center4 = rectsExtracted[(faceId * 4) + 1];
                        center = center4 > width ? width : rectsExtracted[(faceId * 4) + 1];
                    }
                    rectsExtracted[i5] = center;
                    int i6 = (faceId * 4) + 3;
                    if (rectsExtracted[(faceId * 4) + 3] < 0) {
                        i2 = 0;
                    } else {
                        i2 = rectsExtracted[(faceId * 4) + 3] > width ? width : rectsExtracted[(faceId * 4) + 3];
                    }
                    rectsExtracted[i6] = i2;
                } else if (scene == 2 || scene == 4) {
                    int center5 = (rectsExtracted[(faceId * 4) + 0] + rectsExtracted[(faceId * 4) + 2]) / 2;
                    int delta2 = (((height / 2) - center5) * AON_SCALE_X) / (height / 2);
                    rectsExtracted[(faceId * 4) + 0] = rectsExtracted[(faceId * 4) + 0] + delta2;
                    rectsExtracted[(faceId * 4) + 2] = rectsExtracted[(faceId * 4) + 2] + delta2;
                    int i7 = (faceId * 4) + 0;
                    if (rectsExtracted[(faceId * 4) + 0] < 0) {
                        i3 = 0;
                    } else {
                        i3 = rectsExtracted[(faceId * 4) + 0] > height ? height : rectsExtracted[(faceId * 4) + 0];
                    }
                    rectsExtracted[i7] = i3;
                    int i8 = (faceId * 4) + 2;
                    if (rectsExtracted[(faceId * 4) + 2] < 0) {
                        center2 = 0;
                    } else {
                        int center6 = rectsExtracted[(faceId * 4) + 2];
                        center2 = center6 > height ? height : rectsExtracted[(faceId * 4) + 2];
                    }
                    rectsExtracted[i8] = center2;
                }
                rectsExtracted[(faceId * 4) + 0] = (rectsExtracted[(faceId * 4) + 0] * RESCALED_VIEW_HEIGHT) / height;
                rectsExtracted[(faceId * 4) + 1] = (rectsExtracted[(faceId * 4) + 1] * RESCALED_VIEW_WIDTH) / width;
                rectsExtracted[(faceId * 4) + 2] = (rectsExtracted[(faceId * 4) + 2] * RESCALED_VIEW_HEIGHT) / height;
                rectsExtracted[(faceId * 4) + 3] = (rectsExtracted[(faceId * 4) + 3] * RESCALED_VIEW_WIDTH) / width;
            }
            faceId++;
            iArr = faceRects;
            i4 = 1;
        }
        int[] regionExtracted = new int[4];
        regionExtracted[0] = viewRegion[0];
        regionExtracted[1] = viewRegion[1];
        if (src == 1) {
            regionExtracted[2] = RESCALED_VIEW_HEIGHT;
            regionExtracted[3] = RESCALED_VIEW_WIDTH;
        } else {
            regionExtracted[2] = viewRegion[3];
            regionExtracted[3] = viewRegion[2];
        }
        String paramViewRegion = PREFIX_VIEW_REGION + scene + "," + arrayToString(regionExtracted);
        String paramFocusRegion = PREFIX_FACE_RECTS + faceNum + "," + arrayToString(rectsExtracted);
        return new FaceInfo(src, paramViewRegion, paramFocusRegion);
    }

    private static int orientationToScene(int orientation) {
        switch (orientation) {
            case 0:
                return 2;
            case 1:
                return 1;
            case 2:
                return 4;
            case 3:
                return 3;
            default:
                return -1;
        }
    }

    private static String arrayToString(int[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        int len = arr.length;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(arr[i]);
            if (i < len - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private String sourceToString(int source) {
        switch (source) {
            case 1:
                return "aon";
            case 2:
                return "camera";
            default:
                return "unknown";
        }
    }

    public String toString() {
        return "FaceInfo : mSource : " + sourceToString(this.mSource) + ", mParamViewRegion : " + this.mParamViewRegion + ", mParamFocusRegion : " + this.mParamFocusRegion;
    }
}
