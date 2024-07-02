package com.android.server.am;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/* loaded from: classes.dex */
public class ProcessProphetModel {
    private static final String BT_MODEL_NAME = "bt.model";
    private static final int BT_OF_TRACK = 20;
    private static final String BT_PROBTAB_NAME = "bt.prob";
    private static final long BT_PROTECTION_TIME = 120000;
    private static final long CP_PROTECTION_TIME = 120000;
    private static final int DAY_OF_TRACK = 7;
    private static final String LU_MODEL_NAME = "launchusage.model";
    private static final String LU_PROBTAB_NAME = "launchusage.prob";
    private static final String PSS_MODEL_NAME = "pss.model";
    private static final String SAVE_PATH = "/data/system/procprophet/";
    private static final long SLICE_IN_MILLIS = 3600000;
    private static final int SLICE_OF_DAY = 24;
    private static final String TAG = "ProcessProphetModel";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.procprophet.debug", false);
    private static double LU_PROB_THRESHOLD = SystemProperties.getInt("persist.sys.procprophet.lu_threshold", 60) / 100.0d;
    private static double BT_PROB_THRESHOLD = SystemProperties.getInt("persist.sys.procprophet.bt_threshold", 70) / 100.0d;
    private static double CP_PROB_THRESHOLD = SystemProperties.getInt("persist.sys.procprophet.cp_threshold", 70) / 100.0d;
    private LaunchUsageModel mLUModel = null;
    private LaunchUsageProbTab mLUProbTab = null;
    private BlueToothUsageModel mBTUsageModel = null;
    private UseInfoMapList mBTProbTab = null;
    private long mLastBlueToothTime = 0;
    private CopyPatternModel mCopyPatternModel = new CopyPatternModel();
    private long mLastCopyedTime = 0;
    private String mLastMatchedPkg = "";
    public HashSet<String> mWhiteListEmptyProc = new HashSet<>();
    public HashSet<String> mBlackListLaunch = new HashSet<>();
    public HashSet<String> mBlackListBTAudio = new HashSet<>();
    private HashMap<String, Long> mEmptyProcPss = new HashMap<>();

    public ProcessProphetModel() {
        long startUpTime = SystemClock.uptimeMillis();
        init();
        Slog.d(TAG, "Model init consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
    }

    private void init() {
        try {
            this.mLUModel = (LaunchUsageModel) loadModelFromDisk(LU_MODEL_NAME);
        } catch (Exception e) {
            if (DEBUG) {
                Slog.e(TAG, "Failed to load LU model: " + e);
            }
        }
        if (this.mLUModel == null) {
            Slog.w(TAG, "Failed to load LU model, return a new Model.");
            this.mLUModel = new LaunchUsageModel();
        }
        try {
            this.mBTUsageModel = (BlueToothUsageModel) loadModelFromDisk(BT_MODEL_NAME);
        } catch (Exception e2) {
            if (DEBUG) {
                Slog.e(TAG, "Failed to load BT model: " + e2);
            }
        }
        if (this.mBTUsageModel == null) {
            Slog.w(TAG, "Failed to load BT model, return a new Model.");
            this.mBTUsageModel = new BlueToothUsageModel();
        }
        try {
            this.mLUProbTab = (LaunchUsageProbTab) loadModelFromDisk(LU_PROBTAB_NAME);
        } catch (Exception e3) {
            this.mLUProbTab = null;
            if (DEBUG) {
                Slog.e(TAG, "Failed to load LU probtab: " + e3);
            }
        }
        try {
            this.mBTProbTab = (UseInfoMapList) loadModelFromDisk(BT_PROBTAB_NAME);
        } catch (Exception e4) {
            this.mBTProbTab = null;
            if (DEBUG) {
                Slog.e(TAG, "Failed to load BT probtab: " + e4);
            }
        }
        try {
            this.mEmptyProcPss = (HashMap) loadModelFromDisk(PSS_MODEL_NAME);
        } catch (Exception e5) {
            if (DEBUG) {
                Slog.e(TAG, "Failed to load PSS model: " + e5);
            }
        }
        if (this.mEmptyProcPss == null) {
            Slog.w(TAG, "Failed to load PSS model, return a new Model.");
            this.mEmptyProcPss = new HashMap<>();
        }
    }

    public void updateModelThreshold(String[] newThresList) {
        LU_PROB_THRESHOLD = Double.parseDouble(newThresList[0]) / 100.0d;
        BT_PROB_THRESHOLD = Double.parseDouble(newThresList[1]) / 100.0d;
        CP_PROB_THRESHOLD = Double.parseDouble(newThresList[2]) / 100.0d;
        if (DEBUG) {
            Slog.i(TAG, "PROB_THRESHOLD change to " + newThresList[0] + " " + newThresList[1] + " " + newThresList[2] + " complete.");
        }
    }

    public long[] updateModelSizeTrack() {
        return new long[]{this.mLUModel.getAllDays(), this.mLUModel.getAllLUUpdateTimes(), this.mBTUsageModel.getBTConnectedTimes(), this.mBTUsageModel.getAllBTUpdateTimes(), this.mCopyPatternModel.getAllCopyTimes(), this.mCopyPatternModel.getCopyLaunchedTimes()};
    }

    public ArrayList<Double> uploadModelPredProb() {
        return this.mLUModel.getTopModelPredict();
    }

    public void updateLaunchEvent(String pkgName) {
        if (pkgName == null || pkgName.equals("")) {
            return;
        }
        this.mLUModel.update(pkgName, System.currentTimeMillis(), this.mLUProbTab);
        if (System.currentTimeMillis() - this.mLastCopyedTime < 120000 && pkgName.equals(this.mLastMatchedPkg)) {
            if (DEBUG) {
                Slog.i(TAG, "matchLaunch: " + this.mLastMatchedPkg);
            }
            this.mCopyPatternModel.updateMatchLaunchEvent(this.mLastMatchedPkg);
            this.mLastMatchedPkg = "";
        }
    }

    public void notifyBTConnected() {
        this.mLastBlueToothTime = System.currentTimeMillis();
        this.mBTUsageModel.notifyBTConnected();
    }

    public void updateBTAudioEvent(String pkgName) {
        this.mBTUsageModel.update(pkgName);
    }

    public String updateCopyEvent(CharSequence text, String curTopProcName) {
        this.mLastCopyedTime = System.currentTimeMillis();
        String match = this.mCopyPatternModel.match(text);
        this.mLastMatchedPkg = match;
        double prob = this.mCopyPatternModel.getProb(match);
        Slog.i(TAG, "match: " + this.mLastMatchedPkg + ", prob: " + prob + ", curTop: " + curTopProcName);
        String str = this.mLastMatchedPkg;
        if (str != null && !str.equals(curTopProcName)) {
            this.mCopyPatternModel.updateMatchEvent(this.mLastMatchedPkg);
            if (prob > CP_PROB_THRESHOLD) {
                return this.mLastMatchedPkg;
            }
            return null;
        }
        return null;
    }

    public void conclude() {
        this.mLUProbTab = this.mLUModel.conclude();
        saveModelToDisk(this.mLUModel, LU_MODEL_NAME);
        saveModelToDisk(this.mLUProbTab, LU_PROBTAB_NAME);
        this.mBTProbTab = this.mBTUsageModel.conclude();
        saveModelToDisk(this.mBTUsageModel, BT_MODEL_NAME);
        saveModelToDisk(this.mBTProbTab, BT_PROBTAB_NAME);
        saveModelToDisk(this.mEmptyProcPss, PSS_MODEL_NAME);
    }

    public Long getPssInRecord(String procName) {
        return this.mEmptyProcPss.get(procName);
    }

    public void updatePssInRecord(String procName, Long pss) {
        Long rawPss = this.mEmptyProcPss.get(procName);
        if (rawPss != null) {
            this.mEmptyProcPss.put(procName, Long.valueOf((rawPss.longValue() + pss.longValue()) / 2));
        } else {
            this.mEmptyProcPss.put(procName, pss);
        }
    }

    public void dump() {
        DecimalFormat doubleDF = new DecimalFormat("#.####");
        Slog.i(TAG, "Probability threshold of LU, BT and CP: [" + doubleDF.format(LU_PROB_THRESHOLD) + ", " + doubleDF.format(BT_PROB_THRESHOLD) + ", " + doubleDF.format(CP_PROB_THRESHOLD) + "]");
        this.mLUModel.dump();
        this.mBTUsageModel.dump();
        LaunchUsageProbTab launchUsageProbTab = this.mLUProbTab;
        if (launchUsageProbTab != null) {
            launchUsageProbTab.dump();
        }
        if (this.mBTProbTab != null) {
            Slog.i(TAG, "Dumping Bluetooth Prob Tab.");
            Slog.i(TAG, "" + this.mBTProbTab);
        }
        this.mCopyPatternModel.dump();
        Slog.i(TAG, "Dumping EmptyProcPss:");
        for (String procName : this.mEmptyProcPss.keySet()) {
            Slog.i(TAG, "\t" + procName + " Pss=" + (this.mEmptyProcPss.get(procName).longValue() / FormatBytesUtil.KB) + "MB");
        }
    }

    public ArrayList<PkgValuePair> getWeightedTab(HashMap<String, ProcessRecord> aliveEmptyProcs) {
        ArrayList<PkgValuePair> topLUProbs;
        Double weightBT;
        Double weightCP;
        String str;
        ArrayList<PkgValuePair> topLUProbs2;
        long currentTimeStamp = System.currentTimeMillis();
        Double weightLU = Double.valueOf(1.0d);
        LaunchUsageProbTab launchUsageProbTab = this.mLUProbTab;
        if (launchUsageProbTab == null) {
            topLUProbs = null;
        } else {
            UseInfoMapList curProbTab = launchUsageProbTab.getCurProbTab();
            if (curProbTab == null) {
                topLUProbs = null;
            } else {
                if (isWeekend(Calendar.getInstance())) {
                    topLUProbs2 = curProbTab.getProbsGreaterThan(LU_PROB_THRESHOLD / 2.0d);
                    if (DEBUG) {
                        Slog.i(TAG, "topLUProbs' threshold is " + (LU_PROB_THRESHOLD / 2.0d));
                    }
                } else {
                    topLUProbs2 = curProbTab.getProbsGreaterThan(LU_PROB_THRESHOLD);
                    if (DEBUG) {
                        Slog.i(TAG, "topLUProbs' threshold is " + LU_PROB_THRESHOLD);
                    }
                }
                topLUProbs = topLUProbs2;
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "topLUProbs: " + topLUProbs);
        }
        Double weightBT2 = Double.valueOf(0.0d);
        ArrayList<PkgValuePair> topBTProbs = null;
        if (this.mBTProbTab != null && currentTimeStamp - this.mLastBlueToothTime <= 120000) {
            Double weightBT3 = Double.valueOf(2.0d);
            topBTProbs = this.mBTProbTab.getProbsGreaterThan(BT_PROB_THRESHOLD);
            weightBT = weightBT3;
        } else {
            weightBT = weightBT2;
        }
        if (DEBUG) {
            Slog.d(TAG, "topBTProbs: " + topBTProbs);
        }
        HashSet<String> allowToStartProcs = new HashSet<>();
        if (topLUProbs != null) {
            Iterator<PkgValuePair> it = topLUProbs.iterator();
            while (it.hasNext()) {
                PkgValuePair p = it.next();
                allowToStartProcs.add(p.pkgName);
            }
        }
        if (topBTProbs != null) {
            Iterator<PkgValuePair> it2 = topBTProbs.iterator();
            while (it2.hasNext()) {
                PkgValuePair p2 = it2.next();
                allowToStartProcs.add(p2.pkgName);
            }
        }
        Double weightCP2 = Double.valueOf(0.0d);
        if (currentTimeStamp - this.mLastCopyedTime <= 120000 && (str = this.mLastMatchedPkg) != null && !str.equals("")) {
            Double weightCP3 = Double.valueOf(2.0d);
            allowToStartProcs.add(this.mLastMatchedPkg);
            weightCP = weightCP3;
        } else {
            weightCP = weightCP2;
        }
        synchronized (aliveEmptyProcs) {
            Iterator<String> it3 = aliveEmptyProcs.keySet().iterator();
            while (it3.hasNext()) {
                allowToStartProcs.add(it3.next());
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "current weight: weightBT=" + weightBT + ", weightCP=" + weightCP);
        }
        UseInfoMapList probMapList = new UseInfoMapList();
        Iterator<String> it4 = allowToStartProcs.iterator();
        while (it4.hasNext()) {
            String procName = it4.next();
            double score = probFusion(procName, weightLU, weightBT, weightCP);
            probMapList.update(procName, score);
        }
        ArrayList<PkgValuePair> res = probMapList.updateList();
        Slog.d(TAG, "getWeightedTab consumed " + (System.currentTimeMillis() - currentTimeStamp) + "ms," + probMapList);
        return res;
    }

    public ArrayList<PkgValuePair> sortEmptyProcs(HashMap<String, ProcessRecord> aliveEmptyProcs) {
        long currentTimeStamp = System.currentTimeMillis();
        Double weightLU = Double.valueOf(1.0d);
        Double weightBT = Double.valueOf(0.0d);
        Double weightCP = Double.valueOf(0.0d);
        if (currentTimeStamp - this.mLastBlueToothTime <= 120000) {
            weightBT = Double.valueOf(2.0d);
        }
        if (currentTimeStamp - this.mLastCopyedTime <= 120000) {
            weightCP = Double.valueOf(2.0d);
        }
        if (DEBUG) {
            Slog.d(TAG, "current weight: weightBT=" + weightBT + ", weightCP=" + weightCP);
        }
        UseInfoMapList probMapList = new UseInfoMapList();
        synchronized (aliveEmptyProcs) {
            for (ProcessRecord app : aliveEmptyProcs.values()) {
                String procName = app.processName;
                double score = probFusion(procName, weightLU, weightBT, weightCP);
                probMapList.update(procName, score);
            }
        }
        ArrayList<PkgValuePair> res = probMapList.updateList();
        Slog.d(TAG, "sortEmptyProcs consumed " + (System.currentTimeMillis() - currentTimeStamp) + "ms," + probMapList);
        return res;
    }

    private double probFusion(String procName, Double weightLU, Double weightBT, Double weightCP) {
        Double valueOf = Double.valueOf(0.0d);
        if (procName != null && !procName.equals("")) {
            double probLU = 0.0d;
            double probBT = 0.0d;
            double probCP = 0.0d;
            if (this.mLUProbTab != null && weightLU.compareTo(valueOf) != 0) {
                PkgValuePair p = this.mLUProbTab.getProb(procName);
                probLU = p.value * weightLU.doubleValue();
            }
            if (this.mBTProbTab != null && weightBT.compareTo(valueOf) != 0) {
                probBT = this.mBTProbTab.getProb(procName) * weightBT.doubleValue();
            }
            if (this.mCopyPatternModel != null && weightCP.compareTo(valueOf) != 0 && procName.equals(this.mLastMatchedPkg)) {
                probCP = this.mCopyPatternModel.getProb(procName) * weightCP.doubleValue();
            }
            DecimalFormat doubleDF = new DecimalFormat("#.####");
            if (DEBUG) {
                Slog.d(TAG, "\tprob fusion: " + procName + "[" + doubleDF.format(probLU) + ", " + doubleDF.format(probBT) + ", " + doubleDF.format(probCP) + "]");
            }
            return probLU + probBT + probCP;
        }
        return 0.0d;
    }

    private void saveModelToDisk(Serializable model, String modelName) {
        StringBuilder sb;
        File dir = new File(SAVE_PATH);
        if (!dir.exists() && !dir.mkdirs()) {
            Slog.w(TAG, "mk dir fail.");
            return;
        }
        FileOutputStream fileOut = null;
        ObjectOutputStream objOut = null;
        try {
            try {
                fileOut = new FileOutputStream(new File(SAVE_PATH + modelName));
                objOut = new ObjectOutputStream(fileOut);
                objOut.writeObject(model);
                if (DEBUG) {
                    Slog.i(TAG, "Save " + modelName + " done.");
                }
                try {
                    objOut.close();
                } catch (IOException e) {
                    Slog.e(TAG, "error when closing oos.  " + e);
                }
                try {
                    fileOut.close();
                } catch (IOException e2) {
                    e = e2;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("error when closing fos.  ").append(e).toString());
                }
            } catch (Exception e3) {
                if (DEBUG) {
                    Slog.e(TAG, "Failed to save " + modelName, e3);
                }
                if (objOut != null) {
                    try {
                        objOut.close();
                    } catch (IOException e4) {
                        Slog.e(TAG, "error when closing oos.  " + e4);
                    }
                }
                if (fileOut != null) {
                    try {
                        fileOut.close();
                    } catch (IOException e5) {
                        e = e5;
                        sb = new StringBuilder();
                        Slog.e(TAG, sb.append("error when closing fos.  ").append(e).toString());
                    }
                }
            }
        } finally {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0103  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0127  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Object loadModelFromDisk(java.lang.String r12) {
        /*
            Method dump skipped, instructions count: 373
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ProcessProphetModel.loadModelFromDisk(java.lang.String):java.lang.Object");
    }

    public void reset() {
        this.mLUModel = new LaunchUsageModel();
        this.mBTUsageModel = new BlueToothUsageModel();
        this.mLUProbTab = null;
        this.mBTProbTab = null;
        this.mCopyPatternModel = new CopyPatternModel();
    }

    public void testCP(String targetProcName) {
        if (targetProcName == null || targetProcName.equals("")) {
            Slog.d(TAG, "target proc name error!");
            return;
        }
        Slog.d(TAG, "enter CP test mode, target proc name: " + targetProcName);
        reset();
        for (int i = 0; i < 4; i++) {
            this.mCopyPatternModel.updateMatchEvent(targetProcName);
            this.mCopyPatternModel.updateMatchLaunchEvent(targetProcName);
        }
        this.mCopyPatternModel.updateMatchEvent(targetProcName);
        conclude();
    }

    public void testMTBF() {
        LU_PROB_THRESHOLD = 0.0d;
        BT_PROB_THRESHOLD = 0.0d;
        CP_PROB_THRESHOLD = 0.0d;
    }

    /* loaded from: classes.dex */
    public static final class PkgValuePair implements Serializable, Comparable<PkgValuePair> {
        String pkgName;
        double value;

        PkgValuePair(String pkgName, double value) {
            this.pkgName = null;
            this.value = 0.0d;
            this.pkgName = pkgName;
            this.value = value;
        }

        @Override // java.lang.Comparable
        public int compareTo(PkgValuePair p) {
            double d = this.value;
            double d2 = p.value;
            if (d > d2) {
                return -1;
            }
            if (d < d2) {
                return 1;
            }
            return 0;
        }

        public String toString() {
            return "[" + this.pkgName + "," + new DecimalFormat("#.####").format(this.value) + "]";
        }
    }

    /* loaded from: classes.dex */
    public static final class UseInfoMapList implements Serializable {
        HashMap<String, PkgValuePair> myMap = new HashMap<>();
        ArrayList<PkgValuePair> myList = new ArrayList<>();
        private boolean needUpdateList = false;
        private DecimalFormat doubleDF = new DecimalFormat("#.####");
        public double total = 0.0d;

        public void update(String pkgName, double useTime) {
            PkgValuePair p = this.myMap.get(pkgName);
            if (p == null) {
                p = new PkgValuePair(pkgName, useTime);
            } else {
                p.value += useTime;
            }
            this.myMap.put(pkgName, p);
            this.total += useTime;
            this.needUpdateList = true;
        }

        public void update(PkgValuePair pkgValuePair) {
            update(pkgValuePair.pkgName, pkgValuePair.value);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ArrayList<PkgValuePair> updateList() {
            if (!this.needUpdateList) {
                return this.myList;
            }
            this.myList.clear();
            for (String pkg : this.myMap.keySet()) {
                this.myList.add(this.myMap.get(pkg));
            }
            Collections.sort(this.myList);
            this.needUpdateList = false;
            return this.myList;
        }

        public int getMapSize() {
            return this.myMap.size();
        }

        public double get(String pkgName) {
            PkgValuePair p = this.myMap.get(pkgName);
            if (p != null) {
                return p.value;
            }
            return 0.0d;
        }

        public double getProb(String pkgName) {
            Double freq = Double.valueOf(get(pkgName));
            if (freq.compareTo(Double.valueOf(0.0d)) == 0) {
                return 0.0d;
            }
            return get(pkgName) / this.total;
        }

        public PkgValuePair getTopProb() {
            return getTopProb(1);
        }

        public PkgValuePair getTopProb(int top) {
            updateList();
            String topPkgName = null;
            double topProb = 0.0d;
            try {
                PkgValuePair target = this.myList.get(top - 1);
                topPkgName = target.pkgName;
                topProb = target.value / this.total;
            } catch (Exception e) {
            }
            return new PkgValuePair(topPkgName, topProb);
        }

        public ArrayList<PkgValuePair> getProbsGreaterThan(double threshold) {
            updateList();
            ArrayList<PkgValuePair> topProbs = new ArrayList<>();
            Iterator<PkgValuePair> it = this.myList.iterator();
            while (it.hasNext()) {
                PkgValuePair p = it.next();
                Double prob = Double.valueOf(p.value / this.total);
                if (prob.compareTo(Double.valueOf(threshold)) < 0) {
                    break;
                }
                topProbs.add(new PkgValuePair(p.pkgName, prob.doubleValue()));
            }
            return topProbs;
        }

        public void clear() {
            this.myMap.clear();
            this.myList.clear();
        }

        public String toString() {
            updateList();
            String ret = " Total=" + this.doubleDF.format(this.total);
            for (int i = 0; i < this.myList.size(); i++) {
                if (i >= 3) {
                    return ret + "......";
                }
                ret = (ret + " Top" + (i + 1) + ":") + this.myList.get(i);
            }
            return ret;
        }

        public String toProbString() {
            updateList();
            String ret = " Total=" + this.doubleDF.format(this.total);
            for (int i = 0; i < this.myList.size(); i++) {
                if (i >= 3) {
                    return ret + "......";
                }
                ret = ((ret + " Top" + (i + 1) + ":") + "[" + this.myList.get(i).pkgName + ",") + this.doubleDF.format(this.myList.get(i).value / this.total) + "]";
            }
            return ret;
        }

        public void dump() {
            Slog.i(ProcessProphetModel.TAG, "" + toProbString());
        }

        public long getUseInfoMapTimes() {
            double uiMapTimes = 0.0d;
            for (Map.Entry<String, PkgValuePair> mmEntry : this.myMap.entrySet()) {
                uiMapTimes += mmEntry.getValue().value;
            }
            return (long) uiMapTimes;
        }

        public int getPkgPos(String pkgName) {
            updateList();
            return this.myList.indexOf(this.myMap.get(pkgName));
        }
    }

    /* loaded from: classes.dex */
    public static final class LaunchInfoDaily implements Serializable {
        public long zeroTimeStamp;
        public long launchUpdateTimes = 0;
        public boolean isWeekend = false;
        HashMap<String, Integer>[] launchUsageInThisDay = new HashMap[24];

        LaunchInfoDaily(long zeroTimeStamp) {
            this.zeroTimeStamp = 0L;
            this.zeroTimeStamp = zeroTimeStamp;
        }

        public void update(String packageName, long launchTimeStamp) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(launchTimeStamp);
            int hourOfDay = calendar.get(11);
            add(packageName, hourOfDay, 1);
        }

        private void add(String packageName, int hourOfDay, int launchFreq) {
            HashMap<String, Integer>[] hashMapArr = this.launchUsageInThisDay;
            if (hashMapArr[hourOfDay] == null) {
                hashMapArr[hourOfDay] = new HashMap<>();
            }
            HashMap<String, Integer> temp = this.launchUsageInThisDay[hourOfDay];
            if (temp.containsKey(packageName)) {
                temp.put(packageName, Integer.valueOf(temp.get(packageName).intValue() + launchFreq));
            } else {
                temp.put(packageName, Integer.valueOf(launchFreq));
            }
            this.launchUpdateTimes += launchFreq;
        }

        public void combine(LaunchInfoDaily another) {
            for (int hourOfDay = 0; hourOfDay < 24; hourOfDay++) {
                HashMap<String, Integer> usageInHour = another.launchUsageInThisDay[hourOfDay];
                if (usageInHour != null && usageInHour.size() > 0) {
                    for (String pkg : usageInHour.keySet()) {
                        add(pkg, hourOfDay, usageInHour.get(pkg).intValue());
                    }
                }
            }
        }

        public void dump() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(this.zeroTimeStamp);
            Slog.i(ProcessProphetModel.TAG, "Launch Usage OneDay of " + calendar.getTime());
            for (int hourOfDay = 0; hourOfDay < 24; hourOfDay++) {
                HashMap<String, Integer> temp = this.launchUsageInThisDay[hourOfDay];
                if (temp != null) {
                    String dumpRes = "\thourOfDay=" + hourOfDay + " size=" + temp.keySet().size() + " data:";
                    Iterator<String> it = temp.keySet().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        String pkg = it.next();
                        dumpRes = dumpRes + " [" + pkg + "," + temp.get(pkg) + "]";
                        if (dumpRes.length() > 200) {
                            dumpRes = dumpRes + "......";
                            break;
                        }
                    }
                    Slog.i(ProcessProphetModel.TAG, dumpRes);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isWeekend(Calendar mycalendar) {
        int dayOfWeek = mycalendar.get(7) - 1;
        if (dayOfWeek == 0 || dayOfWeek == 6) {
            if (DEBUG) {
                Slog.i(TAG, "Today is weekend.");
            }
            return true;
        }
        if (DEBUG) {
            Slog.i(TAG, "Today is workday.");
            return false;
        }
        return false;
    }

    /* loaded from: classes.dex */
    public static final class LaunchUsageModel implements Serializable {
        LaunchInfoDaily launchUsageToday;
        private Queue<LaunchInfoDaily> dataList = new LinkedList();
        double[] modelPredictData = new double[6];
        private long mLastLaunchTimeStamp = 0;

        public void update(String packageName, long launchTimeStamp, LaunchUsageProbTab mLUProbTab) {
            if (launchTimeStamp < this.mLastLaunchTimeStamp) {
                return;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(launchTimeStamp);
            if (ProcessProphetModel.DEBUG) {
                Slog.d(ProcessProphetModel.TAG, "update launch " + packageName + ", time=" + calendar.getTime());
            }
            updateModelPredict(packageName, calendar.getTime(), mLUProbTab);
            calendar.set(11, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            if (this.launchUsageToday == null || calendar.getTimeInMillis() > this.mLastLaunchTimeStamp) {
                if (ProcessProphetModel.DEBUG) {
                    Slog.i(ProcessProphetModel.TAG, "update a new day");
                }
                if (this.dataList.size() >= 8) {
                    this.dataList.remove();
                }
                this.launchUsageToday = new LaunchInfoDaily(calendar.getTimeInMillis());
                if (ProcessProphetModel.isWeekend(calendar)) {
                    this.launchUsageToday.isWeekend = true;
                    if (ProcessProphetModel.DEBUG) {
                        Slog.i(ProcessProphetModel.TAG, "Today is the weekend.");
                    }
                }
                this.dataList.add(this.launchUsageToday);
            }
            this.launchUsageToday.update(packageName, launchTimeStamp);
            this.mLastLaunchTimeStamp = launchTimeStamp;
        }

        public void dump() {
            Slog.i(ProcessProphetModel.TAG, "dumping Launch Usage Model");
            for (LaunchInfoDaily l : this.dataList) {
                l.dump();
            }
        }

        public LaunchUsageProbTab conclude() {
            long concludeTimeStamp = System.currentTimeMillis();
            LaunchInfoDaily daySum = new LaunchInfoDaily(0L);
            LaunchInfoDaily weekendSum = new LaunchInfoDaily(0L);
            for (LaunchInfoDaily usage : this.dataList) {
                if (usage.isWeekend) {
                    weekendSum.combine(usage);
                } else {
                    daySum.combine(usage);
                }
            }
            LaunchUsageProbTab probTabResult = new LaunchUsageProbTab(daySum, weekendSum);
            Slog.i(ProcessProphetModel.TAG, "conclude consumed " + (System.currentTimeMillis() - concludeTimeStamp) + "ms");
            return probTabResult;
        }

        public long getAllDays() {
            return this.dataList.size();
        }

        public long getAllLUUpdateTimes() {
            long allLaunchUpdateTimes = 0;
            for (LaunchInfoDaily lid : this.dataList) {
                allLaunchUpdateTimes += lid.launchUpdateTimes;
            }
            return allLaunchUpdateTimes;
        }

        private void updateModelPredict(String packageName, Date launchTime, LaunchUsageProbTab mLUProbTab) {
            if (mLUProbTab != null) {
                int pos = mLUProbTab.getCurProbTab().getPkgPos(packageName);
                if (pos == -1 || pos > 4) {
                    double[] dArr = this.modelPredictData;
                    dArr[5] = dArr[5] + 1.0d;
                } else if (pos >= 0 && pos <= 4) {
                    double[] dArr2 = this.modelPredictData;
                    dArr2[pos] = dArr2[pos] + 1.0d;
                }
                if (ProcessProphetModel.DEBUG) {
                    Slog.d(ProcessProphetModel.TAG, "update Model Predict " + packageName + ", time=" + launchTime);
                }
            }
        }

        public ArrayList<Double> getTopModelPredict() {
            ArrayList<Double> uploadModPredList = new ArrayList<>();
            int i = 0;
            while (true) {
                double[] dArr = this.modelPredictData;
                if (i < dArr.length) {
                    uploadModPredList.add(Double.valueOf(dArr[i]));
                    i++;
                } else {
                    Arrays.fill(dArr, 0.0d);
                    return uploadModPredList;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class LaunchUsageProbTab implements Serializable {
        UseInfoMapList[] probTabs = new UseInfoMapList[24];
        UseInfoMapList weekendTab;

        LaunchUsageProbTab(LaunchInfoDaily oneDay, LaunchInfoDaily weekendDay) {
            HashMap<String, Integer> usageInNextHour;
            this.weekendTab = null;
            UseInfoMapList weekendUseInfo = new UseInfoMapList();
            for (int hourOfDay = 0; hourOfDay < 24; hourOfDay++) {
                UseInfoMapList useInfoMapList = new UseInfoMapList();
                mergeUseInfo(weekendDay, weekendUseInfo, hourOfDay);
                mergeUseInfo(oneDay, useInfoMapList, hourOfDay);
                if (hourOfDay + 1 >= 24) {
                    usageInNextHour = oneDay.launchUsageInThisDay[0];
                } else {
                    usageInNextHour = oneDay.launchUsageInThisDay[hourOfDay + 1];
                }
                if (usageInNextHour != null && usageInNextHour.size() > 0) {
                    for (String pkg : usageInNextHour.keySet()) {
                        useInfoMapList.update(pkg, usageInNextHour.get(pkg).intValue());
                    }
                }
                this.probTabs[hourOfDay] = useInfoMapList;
            }
            this.weekendTab = weekendUseInfo;
        }

        private void mergeUseInfo(LaunchInfoDaily launchInfoDaily, UseInfoMapList uInfoMapList, int hour) {
            HashMap<String, Integer> usageInThisHour = launchInfoDaily.launchUsageInThisDay[hour];
            if (usageInThisHour != null && usageInThisHour.size() > 0) {
                for (String pkg : usageInThisHour.keySet()) {
                    uInfoMapList.update(pkg, usageInThisHour.get(pkg).intValue());
                }
            }
        }

        public PkgValuePair getProb(String pkgName) {
            UseInfoMapList useInfoMapList;
            Calendar calendar = Calendar.getInstance();
            int clock = calendar.get(11);
            if (ProcessProphetModel.isWeekend(calendar)) {
                useInfoMapList = this.weekendTab;
            } else {
                useInfoMapList = this.probTabs[clock];
            }
            if (useInfoMapList != null) {
                return new PkgValuePair(pkgName, useInfoMapList.getProb(pkgName));
            }
            return new PkgValuePair(pkgName, 0.0d);
        }

        public PkgValuePair getTopProb() {
            return getTopProb(1);
        }

        public PkgValuePair getTopProb(int i) {
            Calendar calendar = Calendar.getInstance();
            int clock = calendar.get(11);
            PkgValuePair top = getTopProb(clock, i, ProcessProphetModel.isWeekend(calendar));
            return top;
        }

        public PkgValuePair getTopProb(int hourOfDay, int top, boolean isWeekend) {
            UseInfoMapList useInfoMapList;
            if (isWeekend) {
                useInfoMapList = this.weekendTab;
            } else {
                useInfoMapList = this.probTabs[hourOfDay];
            }
            if (useInfoMapList == null) {
                return null;
            }
            return useInfoMapList.getTopProb(top);
        }

        public UseInfoMapList getCurProbTab() {
            Calendar calendar = Calendar.getInstance();
            if (ProcessProphetModel.isWeekend(calendar)) {
                return this.weekendTab;
            }
            int clock = calendar.get(11);
            return this.probTabs[clock];
        }

        public void dump() {
            Slog.i(ProcessProphetModel.TAG, "Dumping Launch Usage Prob Tab.");
            for (int hourOfDay = 0; hourOfDay < 24; hourOfDay++) {
                UseInfoMapList useInfoMapList = this.probTabs[hourOfDay];
                if (useInfoMapList != null) {
                    String dumpRes = "\thourOfDay=" + hourOfDay;
                    Slog.i(ProcessProphetModel.TAG, dumpRes + useInfoMapList.toProbString());
                }
            }
            if (this.weekendTab != null) {
                String dumpRes2 = "\tweekendOfDay:" + this.weekendTab.toProbString();
                Slog.i(ProcessProphetModel.TAG, dumpRes2);
            }
        }

        public void dumpNow() {
            Slog.i(ProcessProphetModel.TAG, "Dumping Launch Usage Prob Tab for now.");
            Calendar calendar = Calendar.getInstance();
            int clock = calendar.get(11);
            UseInfoMapList useInfoMapList = this.probTabs[clock];
            if (useInfoMapList != null) {
                String dumpRes = "\tclock=" + clock;
                Slog.i(ProcessProphetModel.TAG, dumpRes + useInfoMapList.toProbString());
            }
            if (this.weekendTab != null) {
                String dumpRes2 = "\tweekendOfDay:" + this.weekendTab.toProbString();
                Slog.i(ProcessProphetModel.TAG, dumpRes2);
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class BlueToothUsageModel implements Serializable {
        UseInfoMapList curUseInfoMapList;
        private Queue<UseInfoMapList> dataList = new LinkedList();

        BlueToothUsageModel() {
            UseInfoMapList useInfoMapList = new UseInfoMapList();
            this.curUseInfoMapList = useInfoMapList;
            this.dataList.add(useInfoMapList);
        }

        public void update(String packageName) {
            this.curUseInfoMapList.update(packageName, 1.0d);
        }

        public void update(String packageName, int useTime) {
            this.curUseInfoMapList.update(packageName, useTime);
        }

        public void notifyBTConnected() {
            if (this.curUseInfoMapList.getMapSize() > 0) {
                if (this.dataList.size() >= 20) {
                    this.dataList.remove();
                }
                UseInfoMapList useInfoMapList = new UseInfoMapList();
                this.curUseInfoMapList = useInfoMapList;
                this.dataList.add(useInfoMapList);
            }
        }

        public UseInfoMapList conclude() {
            UseInfoMapList mBTProbTab = new UseInfoMapList();
            for (UseInfoMapList u : this.dataList) {
                for (String pkgName : u.myMap.keySet()) {
                    mBTProbTab.update(u.myMap.get(pkgName));
                }
            }
            mBTProbTab.updateList();
            return mBTProbTab;
        }

        public void dump() {
            Slog.i(ProcessProphetModel.TAG, "dumping BlueTooth Usage Model");
            for (UseInfoMapList u : this.dataList) {
                Slog.i(ProcessProphetModel.TAG, "\t" + u);
            }
        }

        public long getBTConnectedTimes() {
            return this.dataList.size();
        }

        public long getAllBTUpdateTimes() {
            long allBTUpdateTimes = 0;
            for (UseInfoMapList uiml : this.dataList) {
                allBTUpdateTimes += uiml.getUseInfoMapTimes();
            }
            return allBTUpdateTimes;
        }
    }

    /* loaded from: classes.dex */
    public static final class CopyPatternModel implements Serializable {
        private HashMap<String, Double> matchCountMap = new HashMap<>();
        private HashMap<String, Double> launchCountMap = new HashMap<>();
        private long mAllCopyTimes = 0;

        public String match(CharSequence text) {
            String copiedString = ((Object) text) + "";
            if (copiedString.contains("tb.cn")) {
                this.mAllCopyTimes++;
                return "com.taobao.taobao";
            }
            if (copiedString.contains("douyin.com")) {
                this.mAllCopyTimes++;
                return "com.ss.android.ugc.aweme";
            }
            if (copiedString.contains("kuaishou.com")) {
                this.mAllCopyTimes++;
                return "com.smile.gifmaker";
            }
            return null;
        }

        public void updateMatchEvent(String pkgName) {
            if (this.matchCountMap.containsKey(pkgName)) {
                HashMap<String, Double> hashMap = this.matchCountMap;
                hashMap.put(pkgName, Double.valueOf(hashMap.get(pkgName).doubleValue() + 1.0d));
            } else {
                this.matchCountMap.put(pkgName, Double.valueOf(1.0d));
            }
        }

        public void updateMatchLaunchEvent(String pkgName) {
            if (this.launchCountMap.containsKey(pkgName)) {
                HashMap<String, Double> hashMap = this.launchCountMap;
                hashMap.put(pkgName, Double.valueOf(hashMap.get(pkgName).doubleValue() + 1.0d));
            } else {
                this.launchCountMap.put(pkgName, Double.valueOf(1.0d));
            }
        }

        public double getProb(String pkgName) {
            if (pkgName == null || !this.matchCountMap.containsKey(pkgName) || !this.launchCountMap.containsKey(pkgName)) {
                return 0.0d;
            }
            return this.launchCountMap.get(pkgName).doubleValue() / this.matchCountMap.get(pkgName).doubleValue();
        }

        public void dump() {
            Slog.i(ProcessProphetModel.TAG, "dumping Copy Usage Model");
            String ret = "\t";
            for (String pkgName : this.matchCountMap.keySet()) {
                ret = (ret + " [" + pkgName + ",") + new DecimalFormat("#.####").format(getProb(pkgName)) + "]";
            }
            Slog.i(ProcessProphetModel.TAG, ret);
        }

        public long getAllCopyTimes() {
            return this.mAllCopyTimes;
        }

        public long getCopyLaunchedTimes() {
            double allCopyLaunchTimes = 0.0d;
            for (Map.Entry<String, Double> lcmEntry : this.launchCountMap.entrySet()) {
                allCopyLaunchTimes += lcmEntry.getValue().doubleValue();
            }
            return (long) allCopyLaunchTimes;
        }
    }
}
