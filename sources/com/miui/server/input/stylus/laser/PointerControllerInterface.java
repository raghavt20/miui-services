package com.miui.server.input.stylus.laser;

import android.hardware.display.DisplayViewport;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/* loaded from: classes.dex */
public interface PointerControllerInterface {
    public static final int TRANSITION_GRADUAL = 1;
    public static final int TRANSITION_IMMEDIATE = 0;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface TransitionType {
    }

    boolean canShowPointer();

    void fade(int i);

    void getPosition(float[] fArr);

    void move(float f, float f2);

    void resetPosition();

    void setDisplayId(int i);

    void setDisplayViewPort(List<DisplayViewport> list);

    void setPosition(float f, float f2);

    void unfade(int i);
}
