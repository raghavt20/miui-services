package com.android.server.wm;

/* loaded from: classes.dex */
public class RecommendDataEntry {
    private static final String FREEFORM_TASKID = "freeFormTaskId";
    private static final String MIUI_RECOMMEND_CONTENT = "miuiRecommendContent";
    private static final String RECOMMEND_SCENE = "recommendScene";
    public static final int RECOMMEND_SCENE_TYPE_GAME_UPDATING = 2;
    public static final int RECOMMEND_SCENE_TYPE_SPLIT_SCREEN = 3;
    public static final int RECOMMEND_SCENE_TYPE_TAXI_WAITING = 1;
    private static final String RECOMMEND_TRANSACTION_TYPE = "recommendTransactionType";
    public static final int RECOMMEND_TYPE_FREE_FORM = 1;
    public static final int RECOMMEND_TYPE_FREE_FORM_REMOVE = 3;
    public static final int RECOMMEND_TYPE_SPLIT_SCREEN = 2;
    private static final String SENDER_PACKAGENAME = "senderPackageName";
    private static final String TAG = "RecommendDataEntry";
    private String freeformPackageName;
    private int freeformTaskId;
    private int freeformUserId;
    private boolean inFreeFormRecommend;
    private boolean inSplitScreenRecommend;
    private String primaryPackageName;
    private int primaryTaskId;
    private int primaryUserId;
    private int recommendSceneType;
    private int recommendTransactionType;
    private String secondaryPackageName;
    private int secondaryTaskId;
    private int secondaryUserId;

    public int getTransactionType() {
        return this.recommendTransactionType;
    }

    public void setTransactionType(int transactionType) {
        this.recommendTransactionType = transactionType;
    }

    public int getRecommendSceneType() {
        return this.recommendSceneType;
    }

    public void setRecommendSceneType(int recommendSceneType) {
        this.recommendSceneType = recommendSceneType;
    }

    public void setFreeFormTaskId(int freeformTaskId) {
        this.freeformTaskId = freeformTaskId;
    }

    public int getFreeFormTaskId() {
        return this.freeformTaskId;
    }

    public void setPrimaryTaskId(int primaryTaskId) {
        this.primaryTaskId = primaryTaskId;
    }

    public int getPrimaryTaskId() {
        return this.primaryTaskId;
    }

    public void setSecondaryTaskId(int secondaryTaskId) {
        this.secondaryTaskId = secondaryTaskId;
    }

    public int getSecondaryTaskId() {
        return this.secondaryTaskId;
    }

    public String getFreeformPackageName() {
        return this.freeformPackageName;
    }

    public void setFreeformPackageName(String packageName) {
        this.freeformPackageName = packageName;
    }

    public String getPrimaryPackageName() {
        return this.primaryPackageName;
    }

    public void setPrimaryPackageName(String packageName) {
        this.primaryPackageName = packageName;
    }

    public String getSecondaryPackageName() {
        return this.secondaryPackageName;
    }

    public void setSecondaryPackageName(String packageName) {
        this.secondaryPackageName = packageName;
    }

    public boolean getSplitScreenRecommendState() {
        return this.inSplitScreenRecommend;
    }

    public void setSplitScreenRecommendState(boolean inSplitScreenRecommend) {
        this.inSplitScreenRecommend = inSplitScreenRecommend;
    }

    public boolean getFreeFormRecommendState() {
        return this.inFreeFormRecommend;
    }

    public void setFreeFormRecommendState(boolean inFreeFormRecommend) {
        this.inFreeFormRecommend = inFreeFormRecommend;
    }

    public void setFreeformUserId(int userId) {
        this.freeformUserId = userId;
    }

    public int getFreeformUserId() {
        return this.freeformUserId;
    }

    public void setPrimaryUserId(int userId) {
        this.primaryUserId = userId;
    }

    public int getPrimaryUserId() {
        return this.primaryUserId;
    }

    public void setSecondaryUserId(int userId) {
        this.secondaryUserId = userId;
    }

    public int getSecondaryUserId() {
        return this.secondaryUserId;
    }
}
