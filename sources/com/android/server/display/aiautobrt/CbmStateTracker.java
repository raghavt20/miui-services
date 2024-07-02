package com.android.server.display.aiautobrt;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.display.DisplayDebugConfig;
import com.android.server.display.aiautobrt.CbmStateTracker;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class CbmStateTracker {
    protected static final int CUSTOM_BRT_ADJ_SCENE_IN_BRIGHT_ROOM = 2;
    protected static final int CUSTOM_BRT_ADJ_SCENE_IN_DARK_ROOM = 1;
    protected static final int CUSTOM_BRT_ADJ_SCENE_IN_DEFAULT = 0;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final int MAX_HISTORY_CBM_EVENTS_CAPACITY = 30;
    private static final long MAX_PREDICT_DURATION = 1000;
    private static final int MAX_RECORD_CAPACITY = 200;
    private static final int MINIMUM_AUTO_ADJUST_TIMES = 100;
    private static final int MINIMUM_COMPARED_MODEL_NUM = 2;
    private static final String TAG = "CbmController-Tracker";
    private static boolean sDebug;
    private final Handler mBgHandler;
    private final Context mContext;
    private final CustomBrightnessModeController mCustomController;
    private final Handler mHandler;
    private final Map<Integer, StateRecord> mCbmEvents = new HashMap();
    private final ArrayDeque<ResultRecord> mResultRecords = new ArrayDeque<>(MAX_RECORD_CAPACITY);
    private final ArrayDeque<Map<String, Map<Integer, StateRecord>>> mHistoryCbmEvents = new ArrayDeque<>(30);
    private final Runnable mNoteMaxPredictDurationRunnable = new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda11
        @Override // java.lang.Runnable
        public final void run() {
            CbmStateTracker.this.noteMaxPredictDuration();
        }
    };

    public CbmStateTracker(Context context, Handler bgHandler, Handler handler, CustomBrightnessModeController customController) {
        this.mContext = context;
        this.mBgHandler = bgHandler;
        this.mHandler = handler;
        this.mCustomController = customController;
    }

    private StateRecord getCbmEvent(int state) {
        StateRecord event = this.mCbmEvents.get(Integer.valueOf(state));
        if (event == null) {
            StateRecord event2 = new StateRecord(state);
            this.mCbmEvents.put(Integer.valueOf(state), event2);
            return event2;
        }
        return event;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteAutoAdjustmentTimes(final int cbmState) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteAutoAdjustmentTimes$0(cbmState);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteAutoAdjustmentTimes$0(int cbmState) {
        StateRecord event = getCbmEvent(cbmState);
        if (!event.tracking) {
            return;
        }
        event.autoAdjustTimes += 1.0f;
        if (sDebug) {
            Slog.i(TAG, "noteAutoAdjustmentTimes: cbm state: " + event.type + ", auto adjust times: " + event.autoAdjustTimes);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteManualAdjustmentTimes(int cbmState) {
        noteManualAdjustmentTimes(cbmState, 0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteManualAdjustmentTimes(final int cbmState, final int brtAdjSceneState) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteManualAdjustmentTimes$1(cbmState, brtAdjSceneState);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteManualAdjustmentTimes$1(int cbmState, int brtAdjSceneState) {
        StateRecord event = getCbmEvent(cbmState);
        if (!event.tracking) {
            return;
        }
        if (brtAdjSceneState == 1) {
            event.darkRoomAdjTimes++;
        } else if (brtAdjSceneState == 2) {
            event.brightRoomAdjTimes++;
        } else {
            event.manualAdjustTimes += 1.0f;
        }
        if (sDebug) {
            if (brtAdjSceneState == 0) {
                Slog.i(TAG, "noteManualAdjustmentTimes: cbm state: " + event.type + ", manually adjust times: " + event.manualAdjustTimes);
            } else {
                Slog.i(TAG, "noteManualAdjustmentTimes: brt scene state: " + brtAdjSceneState + ", dark room adjust times: " + event.darkRoomAdjTimes + ", bright room adjust times: " + event.brightRoomAdjTimes);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteStartCbmStateTracking(final int state, final long timeMills) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteStartCbmStateTracking$2(state, timeMills);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteStartCbmStateTracking$2(int state, long timeMills) {
        StateRecord event = getCbmEvent(state);
        if (!event.initialize) {
            event.initialize = true;
            event.startTimeMills = timeMills;
        }
        if (event.tracking) {
            return;
        }
        event.startTimeMills = timeMills;
        event.tracking = true;
        if (sDebug) {
            Slog.i(TAG, "noteStartCbmStateTracking: state: " + event.type + ", usage: " + TimeUtils.formatDuration(event.usageDurations));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteStopCbmStateTracking(final int state, final long timeMills) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteStopCbmStateTracking$3(state, timeMills);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteStopCbmStateTracking$3(int state, long timeMills) {
        StateRecord event = getCbmEvent(state);
        if (!event.initialize) {
            event.initialize = true;
            event.startTimeMills = timeMills;
        }
        if (!event.tracking) {
            return;
        }
        long duration = timeMills - event.startTimeMills;
        event.usageDurations += duration;
        event.tracking = false;
        this.mCustomController.noteBrightnessUsageToAggregate((float) duration, state);
        if (sDebug) {
            Slog.i(TAG, "noteStopCbmStateTracking: state: " + event.type + ", duration: " + (((float) duration) / 1000.0f) + ", usage: " + TimeUtils.formatDuration(event.getUsageDuration()));
        }
    }

    protected void noteStartPredictTracking(final long timeMills) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteStartPredictTracking$4(timeMills);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteStartPredictTracking$4(long timeMills) {
        StateRecord event = getCbmEvent(2);
        if (event.predictTracking) {
            return;
        }
        event.startPredictTimeMills = timeMills;
        event.predictTracking = true;
        this.mBgHandler.removeCallbacks(this.mNoteMaxPredictDurationRunnable);
        this.mBgHandler.postDelayed(this.mNoteMaxPredictDurationRunnable, 1000L);
        if (sDebug) {
            Slog.i(TAG, "noteStartCbmPredictTracking: duration: " + TimeUtils.formatDuration(event.getPredictDurations()));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void noteStopPredictTracking(final long timeMills) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteStopPredictTracking$5(timeMills);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteStopPredictTracking$5(long timeMills) {
        StateRecord event = getCbmEvent(2);
        if (!event.predictTracking) {
            return;
        }
        this.mBgHandler.removeCallbacks(this.mNoteMaxPredictDurationRunnable);
        event.predictDurations += timeMills - event.startPredictTimeMills;
        event.predictTracking = false;
        if (sDebug) {
            Slog.i(TAG, "noteStopPredictTracking: duration: " + TimeUtils.formatDuration(event.getPredictDurations()));
        }
        this.mCustomController.notePredictDurationToAggregate(event.predictDurations);
        event.predictDurations = 0L;
    }

    public void noteIndividualResult(final float lux, final int appId, final float brightness) {
        final long now = System.currentTimeMillis();
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$noteIndividualResult$6(lux, appId, brightness, now);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$noteIndividualResult$6(float lux, int appId, float brightness, long now) {
        ResultRecord resultRecord = new ResultRecord(lux, appId, brightness, now);
        if (this.mResultRecords.size() == MAX_RECORD_CAPACITY) {
            this.mResultRecords.pollLast();
        }
        ResultRecord peekFirst = this.mResultRecords.peekFirst();
        if (peekFirst != null && peekFirst.equals(resultRecord)) {
            return;
        }
        this.mResultRecords.addFirst(resultRecord);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void startCbmStats() {
        int minType = 0;
        float minRatio = -1.0f;
        int minComparedModelNum = 0;
        storeHistoryCbmEvents();
        Map<Integer, StateRecord> tempCbmEvents = new HashMap<>();
        for (Map.Entry<Integer, StateRecord> stateRecord : this.mCbmEvents.entrySet()) {
            StateRecord tempRecord = new StateRecord();
            int type = stateRecord.getKey().intValue();
            StateRecord record = stateRecord.getValue();
            float ratio = record.getManualAdjustRatio();
            if (!Float.isNaN(ratio)) {
                minComparedModelNum++;
                minRatio = minRatio == -1.0f ? ratio : minRatio;
                if (ratio <= minRatio) {
                    minType = type;
                    minRatio = ratio;
                }
            }
            tempRecord.copyFrom(record);
            tempCbmEvents.put(Integer.valueOf(type), tempRecord);
        }
        this.mCbmEvents.clear();
        this.mCbmEvents.putAll(tempCbmEvents);
        if (minComparedModelNum >= 2) {
            final int minimumType = minType;
            final float minimumRatio = minRatio;
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    CbmStateTracker.this.lambda$startCbmStats$7(minimumType, minimumRatio);
                }
            });
            return;
        }
        Slog.w(TAG, "Model switch cannot be satisfied.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startCbmStats$7(int minimumType, float minimumRatio) {
        this.mCustomController.updateModelValid(minimumType, minimumRatio);
    }

    private void storeHistoryCbmEvents() {
        if (!this.mCbmEvents.isEmpty()) {
            Map<String, Map<Integer, StateRecord>> historyStateRecordMap = new HashMap<>();
            if (this.mHistoryCbmEvents.size() == 30) {
                this.mHistoryCbmEvents.pollLast();
            }
            Map<Integer, StateRecord> historyEvents = new HashMap<>(this.mCbmEvents);
            historyStateRecordMap.put(FORMAT.format(new Date(System.currentTimeMillis())), historyEvents);
            this.mHistoryCbmEvents.addFirst(historyStateRecordMap);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void startEvaluateCustomCurve() {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$startEvaluateCustomCurve$8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startEvaluateCustomCurve$8() {
        StateRecord stateRecord = this.mCbmEvents.get(0);
        if (stateRecord != null && stateRecord.satisfyMaxBrtAdjTimes()) {
            Handler handler = this.mHandler;
            final CustomBrightnessModeController customBrightnessModeController = this.mCustomController;
            Objects.requireNonNull(customBrightnessModeController);
            handler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CustomBrightnessModeController.this.customCurveConditionsSatisfied();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void resetBrtAdjSceneCount() {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CbmStateTracker.this.lambda$resetBrtAdjSceneCount$9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$resetBrtAdjSceneCount$9() {
        StateRecord stateRecord = this.mCbmEvents.get(0);
        if (stateRecord != null) {
            stateRecord.resetBrtAdjSceneCount();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isBrightnessAdjustNoted(int cbmState) {
        return getCbmEvent(cbmState).tracking;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void noteMaxPredictDuration() {
        StateRecord event = getCbmEvent(2);
        event.predictTracking = false;
        this.mCustomController.notePredictDurationToAggregate(1000L);
        event.predictDurations = 0L;
    }

    public void dump(final PrintWriter pw) {
        sDebug = DisplayDebugConfig.DEBUG_CBM;
        int size = this.mResultRecords.size();
        pw.println("  Latest " + size + " individual events: ");
        this.mResultRecords.forEach(new Consumer() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda12
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                pw.println("    " + ((CbmStateTracker.ResultRecord) obj));
            }
        });
        pw.println("  Brt adj times stats: ");
        this.mCbmEvents.forEach(new BiConsumer() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda13
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                pw.println(((CbmStateTracker.StateRecord) obj2).toString());
            }
        });
        if (!this.mHistoryCbmEvents.isEmpty()) {
            pw.println("  History brt adj times stats: ");
            Iterator<Map<String, Map<Integer, StateRecord>>> it = this.mHistoryCbmEvents.iterator();
            while (it.hasNext()) {
                Map<String, Map<Integer, StateRecord>> events = it.next();
                events.forEach(new BiConsumer() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda14
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        CbmStateTracker.lambda$dump$13(pw, (String) obj, (Map) obj2);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$13(final PrintWriter pw, String time, Map historyMap) {
        pw.println(time + ":");
        historyMap.forEach(new BiConsumer() { // from class: com.android.server.display.aiautobrt.CbmStateTracker$$ExternalSyntheticLambda9
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                pw.println(((CbmStateTracker.StateRecord) obj2).toString());
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StateRecord {
        private float autoAdjustTimes;
        private int brightRoomAdjTimes;
        private int darkRoomAdjTimes;
        private boolean initialize;
        private float manualAdjustTimes;
        private long predictDurations;
        private boolean predictTracking;
        private long startPredictTimeMills;
        private long startTimeMills;
        protected boolean tracking;
        protected int type;
        private long usageDurations;

        public StateRecord(int type) {
            this.type = type;
            this.startTimeMills = SystemClock.elapsedRealtime();
            this.tracking = true;
        }

        public StateRecord() {
        }

        public void copyFrom(StateRecord record) {
            this.type = record.type;
            this.startTimeMills = record.startTimeMills;
            this.tracking = record.tracking;
            this.startPredictTimeMills = record.startPredictTimeMills;
            this.predictTracking = record.predictTracking;
            this.initialize = record.initialize;
        }

        protected float getManualAdjustTimesPerHour() {
            long j = this.usageDurations;
            if (j == 0) {
                return Float.NaN;
            }
            return this.manualAdjustTimes / ((float) j);
        }

        protected float getManualAdjustRatio() {
            float minimumAdjustTimes = CbmStateTracker.sDebug ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : 100.0f;
            float f = this.autoAdjustTimes;
            if (f <= minimumAdjustTimes) {
                return Float.NaN;
            }
            return this.manualAdjustTimes / f;
        }

        protected long getUsageDuration() {
            return this.usageDurations;
        }

        protected long getPredictDurations() {
            return this.predictDurations;
        }

        protected void reCount() {
            this.manualAdjustTimes = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.autoAdjustTimes = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            this.usageDurations = 0L;
            resetBrtAdjSceneCount();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void resetBrtAdjSceneCount() {
            this.darkRoomAdjTimes = 0;
            this.brightRoomAdjTimes = 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean satisfyMaxBrtAdjTimes() {
            return this.darkRoomAdjTimes >= 5 || this.brightRoomAdjTimes >= 5;
        }

        public String toString() {
            return "{type: " + this.type + ", manual adj times: " + this.manualAdjustTimes + ", auto adj times: " + this.autoAdjustTimes + ", usage duration: " + TimeUtils.formatDuration(this.usageDurations) + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ResultRecord {
        private int appId;
        private float brightness;
        private float lux;
        private long time;

        public ResultRecord(float lux, int appId, float brightness, long time) {
            this.lux = lux;
            this.appId = appId;
            this.brightness = brightness;
            this.time = time;
        }

        public String toString() {
            return CbmStateTracker.FORMAT.format(new Date(this.time)) + ", l: " + this.lux + ", a: " + this.appId + ", b: " + this.brightness;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ResultRecord)) {
                return false;
            }
            ResultRecord record = (ResultRecord) o;
            return this.lux == record.lux && this.appId == record.appId && this.brightness == record.brightness;
        }
    }
}
