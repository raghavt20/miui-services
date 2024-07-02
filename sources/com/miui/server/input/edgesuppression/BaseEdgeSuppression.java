package com.miui.server.input.edgesuppression;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import java.util.ArrayList;
import miui.util.ITouchFeature;

/* loaded from: classes.dex */
public abstract class BaseEdgeSuppression {
    protected static final int ABSOLUTE = 2;
    protected static final int CONDITION = 1;
    private static final int CORNER = 0;
    protected static final int EMPTY_POINT = 0;
    protected static final int RECT_FIRST = 0;
    protected static final int RECT_FOURTH = 3;
    protected static final int RECT_SECOND = 1;
    protected static final int RECT_THIRD = 2;
    protected static final String TAG = "EdgeSuppressionManager";
    protected int mAbsoluteSize;
    protected int mHoldSensorState;
    protected boolean mIsHorizontal;
    protected int mRotation;
    protected int mScreenHeight;
    protected int mScreenWidth;
    protected int mTargetId;
    protected ArrayList<Integer> mSendList = new ArrayList<>();
    protected int mConditionSize = 0;
    protected int mConnerWidth = 0;
    protected int mConnerHeight = 0;

    abstract ArrayList<Integer> getEdgeSuppressionData(int i, int i2, int i3);

    private boolean hasParamChanged(EdgeSuppressionInfo info, int widthOfScreen, int heightOfScreen) {
        return (this.mConditionSize == info.getConditionSize() && this.mConnerWidth == info.getConnerWidth() && this.mConnerHeight == info.getConnerHeight() && this.mScreenWidth == widthOfScreen && this.mConnerHeight == heightOfScreen) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInterNalParam(EdgeSuppressionInfo info, int holdSensorState, int rotation, int targetId, int widthOfScreen, int heightOfScreen) {
        this.mRotation = rotation;
        this.mTargetId = targetId;
        this.mHoldSensorState = holdSensorState;
        if (hasParamChanged(info, widthOfScreen, heightOfScreen)) {
            this.mConditionSize = info.getConditionSize();
            this.mAbsoluteSize = info.getAbsoluteSize();
            this.mConnerWidth = info.getConnerWidth();
            this.mConnerHeight = info.getConnerHeight();
            this.mIsHorizontal = info.isHorizontal();
            this.mScreenWidth = widthOfScreen;
            this.mScreenHeight = heightOfScreen;
            this.mSendList = getEdgeSuppressionData(rotation, widthOfScreen, heightOfScreen);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void syncDataToKernel() {
        Slog.i(TAG, this.mSendList.toString());
        ITouchFeature iTouchFeature = ITouchFeature.getInstance();
        int i = this.mTargetId;
        ArrayList<Integer> arrayList = this.mSendList;
        iTouchFeature.setEdgeMode(i, 15, arrayList, arrayList.size());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x0163, code lost:
    
        return r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.util.ArrayList<com.miui.server.input.edgesuppression.SuppressionRect> getCornerData(int r19, int r20, int r21, int r22, int r23) {
        /*
            Method dump skipped, instructions count: 368
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.input.edgesuppression.BaseEdgeSuppression.getCornerData(int, int, int, int, int):java.util.ArrayList");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRectValue(SuppressionRect suppressionRect, int type, int position, int startX, int startY, int endX, int endY) {
        suppressionRect.setValue(type, position, startX, startY, endX, endY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initInputMethodData(Context context, int size) {
        int verticalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, -1);
        int horizontalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, -1);
        if (verticalSize == -1 && horizontalSize == -1 && EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE && !EdgeSuppressionManager.SHOULD_REMOVE_EDGE_SETTINGS) {
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, size);
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, size);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputMethodSize(Context context, int minInputMethodSize, int maxInputMethodSize, boolean isVertical) {
        int result;
        boolean shouldOverWrite = true;
        String str = EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE;
        if (isVertical) {
            result = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, -1);
        } else {
            result = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, -1);
        }
        if (!EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE || EdgeSuppressionManager.SHOULD_REMOVE_EDGE_SETTINGS) {
            if (result != -1) {
                result = -1;
            } else {
                shouldOverWrite = false;
            }
        } else if (result > maxInputMethodSize) {
            result = maxInputMethodSize;
        } else if (result < minInputMethodSize) {
            result = minInputMethodSize;
        } else {
            shouldOverWrite = false;
        }
        if (shouldOverWrite) {
            ContentResolver contentResolver = context.getContentResolver();
            if (!isVertical) {
                str = EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE;
            }
            Settings.Global.putInt(contentResolver, str, result);
        }
        Slog.i(TAG, "InputMethod ShrinkSize shouldOverWrite = " + shouldOverWrite + " result = " + result + " isVertical = " + isVertical);
    }
}
