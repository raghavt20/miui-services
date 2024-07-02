package com.android.server.location.gnss.hal;

import android.os.SystemClock;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class GnssScoringModelImpl implements GnssScoringModelStub {
    private static final long IGNORE_RUNNING_TIME = 300000;
    private static final int NO_FIELD_SCORE = 4;
    private static final long NO_FIELD_TIME = 19000;
    private static final String TAG = "GnssScoringModel";
    private static final long WEEK_FIELD_TIME = 61000;
    private Gpo5Client mGpo5Client;
    private final GpoUtil mGpoUtil = GpoUtil.getInstance();
    private long mStartTime = 0;
    private long mLastLocationTime = 0;
    private boolean mFeatureSwitch = false;
    private boolean mModelRunning = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssScoringModelImpl> {

        /* compiled from: GnssScoringModelImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssScoringModelImpl INSTANCE = new GnssScoringModelImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssScoringModelImpl m1835provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssScoringModelImpl m1834provideNewInstance() {
            return new GnssScoringModelImpl();
        }
    }

    public void init(boolean state) {
        if (this.mFeatureSwitch == state) {
            return;
        }
        this.mFeatureSwitch = state;
        this.mGpoUtil.logi(TAG, "set GnssScoringModel running: " + state, true);
        this.mGpo5Client = Gpo5Client.getInstance();
    }

    public void startScoringModel(boolean on) {
        if (this.mFeatureSwitch) {
            this.mGpoUtil.logv(TAG, "start Scoring Model ? " + on);
            if (!this.mModelRunning && on) {
                this.mStartTime = SystemClock.elapsedRealtime();
            }
            this.mModelRunning = on && (this.mLastLocationTime == 0 || SystemClock.elapsedRealtime() - this.mLastLocationTime > IGNORE_RUNNING_TIME);
        }
    }

    public void updateFixTime(long time) {
        if (this.mFeatureSwitch) {
            this.mGpoUtil.logv(TAG, "update gnss fix time.");
            this.mModelRunning = false;
            this.mLastLocationTime = time;
        }
    }

    public void reportSvStatus2Score(float[] basebandCn0DbHzs) {
        if (!this.mFeatureSwitch || !this.mModelRunning) {
            return;
        }
        int sum = 0;
        int length = basebandCn0DbHzs.length;
        for (int i = 0; i < length; i++) {
            float cn0 = basebandCn0DbHzs[i];
            sum += cn0 > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? 1 : 0;
        }
        switchEngineStateWithScore(sum * 2, SystemClock.elapsedRealtime() - this.mStartTime);
    }

    private void switchEngineStateWithScore(int score, long time) {
        if (time >= setLimitTime(score) && this.mGpoUtil.getEngineStatus() == 2) {
            if (this.mGpoUtil.getCurUserBehav() == 4) {
                this.mGpoUtil.logv(TAG, "Re-run Scoring Model.");
                startScoringModel(true);
            } else {
                this.mGpoUtil.doStopEngineByInstance();
                this.mGpoUtil.logi(TAG, "Gnss Score: " + score + ", using time: " + (time / 1000), true);
            }
        }
    }

    private long setLimitTime(int score) {
        long limitTime;
        this.mGpoUtil.logv(TAG, "The score is : " + score);
        if (score <= 4) {
            limitTime = NO_FIELD_TIME;
        } else {
            limitTime = WEEK_FIELD_TIME;
        }
        this.mGpoUtil.logv(TAG, "The Limit time is :  " + limitTime);
        return limitTime;
    }
}
