package com.android.server.display.animation;

import com.android.server.display.animation.DynamicAnimation;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public final class SpringForce implements Force {
    public static final float DAMPING_RATIO_HIGH_BOUNCY = 0.2f;
    public static final float DAMPING_RATIO_LOW_BOUNCY = 0.75f;
    public static final float DAMPING_RATIO_MEDIUM_BOUNCY = 0.5f;
    public static final float DAMPING_RATIO_NO_BOUNCY = 1.0f;
    public static final float STIFFNESS_HIGH = 10000.0f;
    public static final float STIFFNESS_LOW = 200.0f;
    public static final float STIFFNESS_MEDIUM = 1500.0f;
    public static final float STIFFNESS_VERY_LOW = 50.0f;
    private static final double UNSET = Double.MAX_VALUE;
    private static final double VELOCITY_THRESHOLD_MULTIPLIER = 62.5d;
    private double mDampedFreq;
    double mDampingRatio;
    private double mFinalPosition;
    private double mGammaMinus;
    private double mGammaPlus;
    private boolean mInitialized;
    private final DynamicAnimation.MassState mMassState;
    double mNaturalFreq;
    private double mValueThreshold;
    private double mVelocityThreshold;

    public SpringForce() {
        this.mNaturalFreq = Math.sqrt(1500.0d);
        this.mDampingRatio = 0.5d;
        this.mInitialized = false;
        this.mFinalPosition = UNSET;
        this.mMassState = new DynamicAnimation.MassState();
    }

    public SpringForce(float finalPosition) {
        this.mNaturalFreq = Math.sqrt(1500.0d);
        this.mDampingRatio = 0.5d;
        this.mInitialized = false;
        this.mFinalPosition = UNSET;
        this.mMassState = new DynamicAnimation.MassState();
        this.mFinalPosition = finalPosition;
    }

    public SpringForce setStiffness(float stiffness) {
        if (stiffness <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            throw new IllegalArgumentException("Spring stiffness constant must be positive.");
        }
        this.mNaturalFreq = Math.sqrt(stiffness);
        this.mInitialized = false;
        return this;
    }

    public float getStiffness() {
        double d = this.mNaturalFreq;
        return (float) (d * d);
    }

    public SpringForce setDampingRatio(float dampingRatio) {
        if (dampingRatio < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            throw new IllegalArgumentException("Damping ratio must be non-negative");
        }
        this.mDampingRatio = dampingRatio;
        this.mInitialized = false;
        return this;
    }

    public float getDampingRatio() {
        return (float) this.mDampingRatio;
    }

    public SpringForce setFinalPosition(float finalPosition) {
        this.mFinalPosition = finalPosition;
        return this;
    }

    public float getFinalPosition() {
        return (float) this.mFinalPosition;
    }

    @Override // com.android.server.display.animation.Force
    public float getAcceleration(float lastDisplacement, float lastVelocity) {
        float lastDisplacement2 = lastDisplacement - getFinalPosition();
        double d = this.mNaturalFreq;
        double k = d * d;
        double c = d * 2.0d * this.mDampingRatio;
        return (float) (((-k) * lastDisplacement2) - (lastVelocity * c));
    }

    @Override // com.android.server.display.animation.Force
    public boolean isAtEquilibrium(float value, float velocity) {
        if (Math.abs(velocity) < this.mVelocityThreshold && Math.abs(value - getFinalPosition()) < this.mValueThreshold) {
            return true;
        }
        return false;
    }

    private void init() {
        if (this.mInitialized) {
            return;
        }
        if (this.mFinalPosition == UNSET) {
            throw new IllegalStateException("Error: Final position of the spring must be set before the animation starts");
        }
        double d = this.mDampingRatio;
        if (d > 1.0d) {
            double d2 = this.mNaturalFreq;
            this.mGammaPlus = ((-d) * d2) + (d2 * Math.sqrt((d * d) - 1.0d));
            double d3 = this.mDampingRatio;
            double d4 = this.mNaturalFreq;
            this.mGammaMinus = ((-d3) * d4) - (d4 * Math.sqrt((d3 * d3) - 1.0d));
        } else if (d >= 0.0d && d < 1.0d) {
            this.mDampedFreq = this.mNaturalFreq * Math.sqrt(1.0d - (d * d));
        }
        this.mInitialized = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DynamicAnimation.MassState updateValues(double lastDisplacement, double lastVelocity, long timeElapsed) {
        double displacement;
        double cosCoeff;
        init();
        double deltaT = timeElapsed / 1000.0d;
        double lastDisplacement2 = lastDisplacement - this.mFinalPosition;
        double displacement2 = this.mDampingRatio;
        if (displacement2 > 1.0d) {
            double d = this.mGammaMinus;
            double d2 = this.mGammaPlus;
            double coeffA = lastDisplacement2 - (((d * lastDisplacement2) - lastVelocity) / (d - d2));
            double coeffB = ((d * lastDisplacement2) - lastVelocity) / (d - d2);
            displacement = (Math.pow(2.718281828459045d, d * deltaT) * coeffA) + (Math.pow(2.718281828459045d, this.mGammaPlus * deltaT) * coeffB);
            double d3 = this.mGammaMinus;
            double pow = coeffA * d3 * Math.pow(2.718281828459045d, d3 * deltaT);
            double d4 = this.mGammaPlus;
            double currentVelocity = pow + (coeffB * d4 * Math.pow(2.718281828459045d, d4 * deltaT));
            cosCoeff = currentVelocity;
        } else if (displacement2 == 1.0d) {
            double d5 = this.mNaturalFreq;
            double coeffB2 = lastVelocity + (d5 * lastDisplacement2);
            double displacement3 = Math.pow(2.718281828459045d, (-d5) * deltaT) * (lastDisplacement2 + (coeffB2 * deltaT));
            double pow2 = (lastDisplacement2 + (coeffB2 * deltaT)) * Math.pow(2.718281828459045d, (-this.mNaturalFreq) * deltaT);
            double d6 = this.mNaturalFreq;
            double currentVelocity2 = (pow2 * (-d6)) + (Math.pow(2.718281828459045d, (-d6) * deltaT) * coeffB2);
            displacement = displacement3;
            cosCoeff = currentVelocity2;
        } else {
            double d7 = 1.0d / this.mDampedFreq;
            double d8 = this.mNaturalFreq;
            double sinCoeff = d7 * ((displacement2 * d8 * lastDisplacement2) + lastVelocity);
            double displacement4 = Math.pow(2.718281828459045d, (-displacement2) * d8 * deltaT) * ((Math.cos(this.mDampedFreq * deltaT) * lastDisplacement2) + (Math.sin(this.mDampedFreq * deltaT) * sinCoeff));
            double d9 = this.mNaturalFreq;
            double lastDisplacement3 = this.mDampingRatio;
            double d10 = (-d9) * displacement4 * lastDisplacement3;
            double pow3 = Math.pow(2.718281828459045d, (-lastDisplacement3) * d9 * deltaT);
            double d11 = this.mDampedFreq;
            double sin = (-d11) * lastDisplacement2 * Math.sin(d11 * deltaT);
            double d12 = this.mDampedFreq;
            double cos = d10 + (pow3 * (sin + (d12 * sinCoeff * Math.cos(d12 * deltaT))));
            displacement = displacement4;
            cosCoeff = cos;
        }
        this.mMassState.mValue = (float) (this.mFinalPosition + displacement);
        this.mMassState.mVelocity = (float) cosCoeff;
        return this.mMassState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setValueThreshold(double threshold) {
        double abs = Math.abs(threshold);
        this.mValueThreshold = abs;
        this.mVelocityThreshold = abs * VELOCITY_THRESHOLD_MULTIPLIER;
    }
}
