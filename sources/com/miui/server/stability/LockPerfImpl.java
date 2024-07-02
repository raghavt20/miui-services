package com.miui.server.stability;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.RingBuffer;
import com.android.server.LockPerfStub;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/* loaded from: classes.dex */
public class LockPerfImpl implements LockPerfStub {
    private static final int DEFAULT_LOCK_PERF_THRESHOLD_SINGLE = 100;
    private static final int DEFAULT_RING_BUFFER_SIZE = 100;
    private static final int MEANINGFUL_TRACE_START = 4;
    private static final String TAG = LockPerfImpl.class.getSimpleName();
    private static final String LOCK_PERF_THRESHOLD_PROP = "persist.debug.lockperf.threshold";
    private static final int LOCK_PERF_THRESHOLD_SINGLE = SystemProperties.getInt(LOCK_PERF_THRESHOLD_PROP, 100);
    private static final String LOCK_PERF_SELF_DEBUG_PROP = "persist.debug.lockperf.self";
    private static final boolean DEBUG = SystemProperties.getBoolean(LOCK_PERF_SELF_DEBUG_PROP, true);
    private static final ThreadLocal<ThreadLocalData> sThreadLocalData = ThreadLocal.withInitial(new Supplier() { // from class: com.miui.server.stability.LockPerfImpl$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return LockPerfImpl.$r8$lambda$ClCO5txLu8hRiftiu7srcjS1yXQ();
        }
    });
    private static final ThreadSafeRingBuffer<SlowLockedMethodEvent> sEventRingBuffer = new ThreadSafeRingBuffer<>(SlowLockedMethodEvent.class, 100);

    public static /* synthetic */ ThreadLocalData $r8$lambda$ClCO5txLu8hRiftiu7srcjS1yXQ() {
        return new ThreadLocalData();
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LockPerfImpl> {

        /* compiled from: LockPerfImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LockPerfImpl INSTANCE = new LockPerfImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LockPerfImpl m3452provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LockPerfImpl m3451provideNewInstance() {
            return new LockPerfImpl();
        }
    }

    public void onPerfStart(String fullMethod) {
        ThreadLocalData tls = sThreadLocalData.get();
        if (DEBUG) {
            tls.debugEventBuffer.append(fullMethod);
        }
        if (tls.unwindingStack != null) {
            Slog.i(TAG, "Record long locked event point 2");
            tls.recordEvent();
        }
        tls.perfStack.push(new PerfInfo(fullMethod, SystemClock.elapsedRealtime()));
    }

    public void onPerfEnd(String fullMethod) {
        ThreadLocalData tls = sThreadLocalData.get();
        boolean z = DEBUG;
        if (z) {
            tls.debugEventBuffer.append("-" + fullMethod);
        }
        PerfStack<PerfInfo> perfStack = tls.perfStack;
        PerfInfo perfInfo = perfStack.topElement();
        if (z && !perfInfo.matchMethod(fullMethod)) {
            Slog.e(TAG, "Not matched perf start and end! Expected: " + perfInfo.getMethodString() + ". Actual: " + fullMethod);
            tls.outputRecentEvents();
            throw new RuntimeException("Lock perf error! Please check log.");
        }
        long duration = SystemClock.elapsedRealtime() - perfInfo.start;
        perfInfo.setDuration(duration);
        if (duration > LOCK_PERF_THRESHOLD_SINGLE) {
            Slog.w(TAG, "Single long locked method. " + duration + " ms. method: " + fullMethod);
            if (tls.unwindingStack == null) {
                tls.setUnwindingStack(trimStackTrace(Thread.currentThread().getStackTrace()));
                perfStack.clearBuffer();
            }
        }
        perfStack.pop();
        if (tls.unwindingStack != null && perfStack.isEmpty()) {
            Slog.i(TAG, "Record long locked event point 1");
            tls.recordEvent();
        }
    }

    public void perf(long start, String fullMethod) {
        long duration = System.currentTimeMillis() - start;
        if (duration > LOCK_PERF_THRESHOLD_SINGLE) {
            Slog.w(TAG, "Simple long locked method: " + fullMethod + ", Duration: " + duration);
        }
    }

    public void dumpRecentEvents(File tracesFile, boolean append, int recentSeconds, int maxEventNum) {
        FileWriter fw;
        Throwable th;
        try {
        } catch (IOException e) {
            e = e;
        }
        try {
            fw = new FileWriter(tracesFile, append);
        } catch (IOException e2) {
            e = e2;
            Slog.w(TAG, "Dumping slow locked events failed: ", e);
        }
        try {
            try {
                SlowLockedMethodEvent[] events = sEventRingBuffer.toArray();
                if (events.length == 0) {
                    fw.append((CharSequence) "No events in buffer. Is the rom debuggable?");
                    fw.close();
                    return;
                }
                ArrayList<SlowLockedMethodEvent> eventsToDump = new ArrayList<>();
                try {
                    LocalDateTime eventStartTime = LocalDateTime.now().minusSeconds(recentSeconds);
                    for (int i = events.length - 1; i >= 0; i--) {
                        if (events[i].datetime.isBefore(eventStartTime) || eventsToDump.size() >= maxEventNum) {
                            break;
                        }
                        try {
                            eventsToDump.add(events[i]);
                        } catch (Throwable th2) {
                            th = th2;
                            th = th;
                            try {
                                fw.close();
                                throw th;
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                                throw th;
                            }
                        }
                    }
                    if (eventsToDump.isEmpty()) {
                        fw.append((CharSequence) "No events to dump in last ").append((CharSequence) String.valueOf(recentSeconds)).append((CharSequence) " seconds.");
                        fw.close();
                        return;
                    }
                    fw.append((CharSequence) "--- Slow locked method ---\n");
                    for (int i2 = 0; i2 < eventsToDump.size(); i2++) {
                        SlowLockedMethodEvent event = eventsToDump.get(i2);
                        fw.append((CharSequence) String.valueOf(i2 + 1)).append((CharSequence) ") ");
                        fw.append((CharSequence) event.formattedDatetime()).append(' ');
                        fw.append((CharSequence) "tid=").append((CharSequence) String.valueOf(event.tid)).append(' ');
                        fw.append((CharSequence) "tname=").append((CharSequence) event.tname).append((CharSequence) "\n");
                        for (String line : event.getStackTraceLines()) {
                            fw.append((CharSequence) "\t ").append((CharSequence) line).append('\n');
                        }
                    }
                    fw.close();
                } catch (Throwable th4) {
                    th = th4;
                    th = th;
                    fw.close();
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (IOException e3) {
            e = e3;
            Slog.w(TAG, "Dumping slow locked events failed: ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String[] parseFullMethod(String fullMethod) {
        int pos = fullMethod.indexOf(40);
        String classAndName = fullMethod.substring(0, pos);
        String[] rst = {classAndName.substring(0, pos), classAndName.substring(pos + 1), fullMethod.substring(pos)};
        int pos2 = classAndName.lastIndexOf(46);
        return rst;
    }

    private static StackTraceElement[] trimStackTrace(StackTraceElement[] rawTrace) {
        ArrayList<StackTraceElement> elements = new ArrayList<>();
        for (int i = 4; i < rawTrace.length; i++) {
            elements.add(rawTrace[i]);
        }
        return (StackTraceElement[]) elements.toArray(new StackTraceElement[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PerfInfo {
        private String className;
        private long duration = -1;
        private final String fullMethod;
        private String methodDesc;
        private String methodName;
        private final long start;

        PerfInfo(String fullMethod, long start) {
            this.fullMethod = fullMethod;
            this.start = start;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setDuration(long duration) {
            this.duration = duration;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean matchStackTraceElement(StackTraceElement element) {
            return getClassName().equals(element.getClassName()) && getMethodName().equals(element.getMethodName());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean matchMethod(String fullMethod) {
            return this.fullMethod.equals(fullMethod);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getMethodString() {
            return this.fullMethod;
        }

        private String getClassName() {
            if (this.className == null) {
                initMethodDetail();
            }
            return this.className;
        }

        private String getMethodName() {
            if (this.methodName == null) {
                initMethodDetail();
            }
            return this.methodName;
        }

        private String getMethodDesc() {
            if (this.methodDesc == null) {
                initMethodDetail();
            }
            return this.methodDesc;
        }

        private void initMethodDetail() {
            String[] strs = LockPerfImpl.parseFullMethod(this.fullMethod);
            this.className = strs[0];
            this.methodName = strs[1];
            this.methodDesc = strs[2];
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getDurationString() {
            return this.duration >= 0 ? "duration=" + this.duration + " ms" : "un-finished";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SlowLockedMethodEvent {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uu-MM-dd HH:mm:ss.SSS");
        private final LocalDateTime datetime;
        private final List<PerfInfo> perfStackSnapshot;
        private final String[] stackTraceLines;
        private final long tid;
        private final String tname;
        private final StackTraceElement[] unwindingStack;

        private SlowLockedMethodEvent(PerfStack<PerfInfo> perfStack, StackTraceElement[] unwindingStack, long tid, String tname, LocalDateTime datetime) {
            this.perfStackSnapshot = new ArrayList();
            this.tid = tid;
            this.tname = tname;
            this.unwindingStack = unwindingStack;
            for (int i = perfStack.size() - 1; i >= 0; i--) {
                this.perfStackSnapshot.add(perfStack.get(i));
            }
            this.datetime = datetime;
            this.stackTraceLines = initStackTraceLines();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String formattedDatetime() {
            return this.datetime.format(DATE_TIME_FORMATTER);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String[] getStackTraceLines() {
            return this.stackTraceLines;
        }

        private String[] initStackTraceLines() {
            StackTraceElement[] stackTrace = this.unwindingStack;
            String[] lines = new String[stackTrace.length];
            int currPerfIdx = 0;
            PerfInfo perfInfo = this.perfStackSnapshot.get(0);
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement stackElement = stackTrace[i];
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("#%02d ", Integer.valueOf(i)));
                sb.append(stackElement.toString());
                if (currPerfIdx < this.perfStackSnapshot.size() && perfInfo.matchStackTraceElement(stackElement)) {
                    sb.append(" ").append(perfInfo.getDurationString());
                    currPerfIdx++;
                    if (currPerfIdx < this.perfStackSnapshot.size()) {
                        PerfInfo perfInfo2 = this.perfStackSnapshot.get(currPerfIdx);
                        perfInfo = perfInfo2;
                    }
                }
                lines[i] = sb.toString();
            }
            return lines;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void printToLogcat() {
            Slog.w(LockPerfImpl.TAG, "Long locked method event. Thread: " + this.tname + ". Full stack trace: ");
            for (String line : getStackTraceLines()) {
                Slog.w(LockPerfImpl.TAG, "\t " + line);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ThreadSafeRingBuffer<T> {
        private final RingBuffer<T> mBuffer;
        private final int mCapacity;

        ThreadSafeRingBuffer(Class<T> clazz, int capacity) {
            this.mCapacity = capacity;
            this.mBuffer = new RingBuffer<>(clazz, capacity);
        }

        synchronized void append(T t) {
            this.mBuffer.append(t);
        }

        synchronized T[] toArray() {
            return (T[]) this.mBuffer.toArray();
        }

        int capacity() {
            return this.mCapacity;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ThreadLocalData {
        private final RingBuffer<String> debugEventBuffer;
        private final PerfStack<PerfInfo> perfStack;
        private StackTraceElement[] unwindingStack;

        private ThreadLocalData() {
            this.perfStack = new PerfStack<>();
            this.debugEventBuffer = new RingBuffer<>(String.class, 20);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setUnwindingStack(StackTraceElement[] stack) {
            this.unwindingStack = stack;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordEvent() {
            Thread currentThread = Thread.currentThread();
            SlowLockedMethodEvent event = new SlowLockedMethodEvent(this.perfStack, this.unwindingStack, currentThread.getId(), currentThread.getName(), LocalDateTime.now());
            setUnwindingStack(null);
            this.perfStack.clearBuffer();
            event.printToLogcat();
            LockPerfImpl.sEventRingBuffer.append(event);
        }

        public void outputRecentEvents() {
            String[] events = (String[]) this.debugEventBuffer.toArray();
            for (int i = 0; i < events.length; i++) {
                Slog.e(LockPerfImpl.TAG, "Event[" + i + "]: " + events[i]);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PerfStack<E> extends ArrayList<E> {
        private int currTop;

        private PerfStack() {
            this.currTop = 0;
        }

        public boolean push(E item) {
            if (hasBufferedElement()) {
                clearBuffer();
            }
            this.currTop++;
            return add(item);
        }

        public E pop() {
            int i = this.currTop - 1;
            this.currTop = i;
            return get(i);
        }

        public E topElement() {
            return get(this.currTop - 1);
        }

        public int top() {
            return this.currTop;
        }

        @Override // java.util.ArrayList, java.util.AbstractCollection, java.util.Collection, java.util.List
        public boolean isEmpty() {
            return this.currTop == 0;
        }

        private boolean hasBufferedElement() {
            return size() > 0 && size() > this.currTop;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clearBuffer() {
            removeRange(this.currTop, size());
        }
    }
}
