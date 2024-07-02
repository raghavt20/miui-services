package com.android.server.app;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class GmsLimitLogger {
    private final LinkedList<Event> mEvents = new LinkedList<>();
    private final int mMemSize;

    public GmsLimitLogger(int size) {
        this.mMemSize = size;
    }

    public synchronized void log(Event evt) {
        if (this.mEvents.size() >= this.mMemSize) {
            this.mEvents.removeFirst();
        }
        this.mEvents.add(evt);
    }

    public synchronized void dump(PrintWriter pw) {
        Iterator<Event> it = this.mEvents.iterator();
        while (it.hasNext()) {
            Event evt = it.next();
            pw.println(evt.toString());
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Event {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final long mTimestamp = System.currentTimeMillis();

        public abstract String eventToString();

        Event() {
        }

        public String toString() {
            return sFormat.format(new Date(this.mTimestamp)) + " " + eventToString();
        }
    }

    /* loaded from: classes.dex */
    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String msg) {
            this.mMsg = msg;
        }

        @Override // com.android.server.app.GmsLimitLogger.Event
        public String eventToString() {
            return this.mMsg;
        }
    }
}
