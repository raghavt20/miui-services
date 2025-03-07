package com.android.server.display.animation;

import com.android.server.display.animation.DynamicAnimation;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public final class FlingAnimation extends DynamicAnimation<FlingAnimation> {
    private final DragForce mFlingForce;

    public FlingAnimation(FloatValueHolder floatValueHolder) {
        super(floatValueHolder);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public <K> FlingAnimation(K object, FloatPropertyCompat<K> property) {
        super(object, property);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public FlingAnimation setFriction(float friction) {
        if (friction <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            throw new IllegalArgumentException("Friction must be positive");
        }
        this.mFlingForce.setFrictionScalar(friction);
        return this;
    }

    public float getFriction() {
        return this.mFlingForce.getFrictionScalar();
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    public FlingAnimation setMinValue(float minValue) {
        super.setMinValue(minValue);
        return this;
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    public FlingAnimation setMaxValue(float maxValue) {
        super.setMaxValue(maxValue);
        return this;
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    public FlingAnimation setStartVelocity(float startVelocity) {
        super.setStartVelocity(startVelocity);
        return this;
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    boolean updateValueAndVelocity(long deltaT) {
        DynamicAnimation.MassState state = this.mFlingForce.updateValueAndVelocity(this.mValue, this.mVelocity, deltaT);
        this.mValue = state.mValue;
        this.mVelocity = state.mVelocity;
        if (this.mValue < this.mMinValue) {
            this.mValue = this.mMinValue;
            return true;
        }
        if (this.mValue <= this.mMaxValue) {
            return isAtEquilibrium(this.mValue, this.mVelocity);
        }
        this.mValue = this.mMaxValue;
        return true;
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    float getAcceleration(float value, float velocity) {
        return this.mFlingForce.getAcceleration(value, velocity);
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    boolean isAtEquilibrium(float value, float velocity) {
        return value >= this.mMaxValue || value <= this.mMinValue || this.mFlingForce.isAtEquilibrium(value, velocity);
    }

    @Override // com.android.server.display.animation.DynamicAnimation
    void setValueThreshold(float threshold) {
        this.mFlingForce.setValueThreshold(threshold);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DragForce implements Force {
        private static final float DEFAULT_FRICTION = -4.2f;
        private static final float VELOCITY_THRESHOLD_MULTIPLIER = 62.5f;
        private float mFriction = DEFAULT_FRICTION;
        private final DynamicAnimation.MassState mMassState = new DynamicAnimation.MassState();
        private float mVelocityThreshold;

        DragForce() {
        }

        void setFrictionScalar(float frictionScalar) {
            this.mFriction = DEFAULT_FRICTION * frictionScalar;
        }

        float getFrictionScalar() {
            return this.mFriction / DEFAULT_FRICTION;
        }

        DynamicAnimation.MassState updateValueAndVelocity(float value, float velocity, long deltaT) {
            this.mMassState.mVelocity = (float) (velocity * Math.exp((((float) deltaT) / 1000.0f) * this.mFriction));
            DynamicAnimation.MassState massState = this.mMassState;
            float f = this.mFriction;
            massState.mValue = (float) ((value - (velocity / f)) + ((velocity / f) * Math.exp((f * ((float) deltaT)) / 1000.0f)));
            if (isAtEquilibrium(this.mMassState.mValue, this.mMassState.mVelocity)) {
                this.mMassState.mVelocity = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            }
            return this.mMassState;
        }

        @Override // com.android.server.display.animation.Force
        public float getAcceleration(float position, float velocity) {
            return this.mFriction * velocity;
        }

        @Override // com.android.server.display.animation.Force
        public boolean isAtEquilibrium(float value, float velocity) {
            return Math.abs(velocity) < this.mVelocityThreshold;
        }

        void setValueThreshold(float threshold) {
            this.mVelocityThreshold = VELOCITY_THRESHOLD_MULTIPLIER * threshold;
        }
    }
}
