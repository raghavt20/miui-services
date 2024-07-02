package com.android.server.input;

import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class InputShellCommandStubImpl implements InputShellCommandStub {
    private static final String INVALID_ARGUMENTS = "Error: Invalid arguments for command: ";
    private InputShellCommand mInputShellCommand;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InputShellCommandStubImpl> {

        /* compiled from: InputShellCommandStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InputShellCommandStubImpl INSTANCE = new InputShellCommandStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public InputShellCommandStubImpl m1399provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public InputShellCommandStubImpl m1398provideNewInstance() {
            return new InputShellCommandStubImpl();
        }
    }

    public void init(InputShellCommand inputShellCommand) {
        this.mInputShellCommand = inputShellCommand;
    }

    public boolean onCommand(String cmd) {
        if (!"miui".equals(cmd)) {
            return false;
        }
        String arg = getNextArgRequired();
        try {
            if ("swipe".equals(arg)) {
                sendSwipe();
                return true;
            }
            handleDefaultCommands(arg);
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException(INVALID_ARGUMENTS + arg);
        }
    }

    private void sendSwipe() {
        int duration;
        int everyDelayTime;
        int x1 = Integer.parseInt(getNextArgRequired());
        int y1 = Integer.parseInt(getNextArgRequired());
        int x2 = Integer.parseInt(getNextArgRequired());
        int y2 = Integer.parseInt(getNextArgRequired());
        String durationArg = getNextArg();
        int duration2 = durationArg != null ? Integer.parseInt(durationArg) : -1;
        if (duration2 >= 0) {
            duration = duration2;
        } else {
            duration = 300;
        }
        MiuiInputManagerInternal miuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        String everyDelayTimeArg = getNextArg();
        if (everyDelayTimeArg == null) {
            miuiInputManagerInternal.swipe(x1, y1, x2, y2, duration);
            return;
        }
        int everyDelayTime2 = Integer.parseInt(everyDelayTimeArg);
        if (everyDelayTime2 >= 0) {
            everyDelayTime = everyDelayTime2;
        } else {
            everyDelayTime = 5;
        }
        miuiInputManagerInternal.swipe(x1, y1, x2, y2, duration, everyDelayTime);
    }

    private void handleDefaultCommands(String arg) {
        this.mInputShellCommand.handleDefaultCommands(arg);
    }

    private String getNextArg() {
        return this.mInputShellCommand.getNextArg();
    }

    private String getNextArgRequired() {
        return this.mInputShellCommand.getNextArgRequired();
    }
}
