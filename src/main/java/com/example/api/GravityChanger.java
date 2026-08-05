package com.example.api;

// PORT NOTE: copied verbatim from the Zemlya project. See Gravity.java.
public interface GravityChanger {
    Gravity getGravityDirection();

    void setGravity(Gravity gravity);

    void onGravityChanged();

    Gravity getPrevGravityDirection();

    int getGravityTransitionTimer();

    void setGravityTransitionTimer(int ticks);

    float getGravityTransitionProgress(float tickDelta);

    boolean hasGravityImmunity();

    default boolean isInfected() {
        return false;
    }

    default int getInfectionTimer() {
        return 0;
    }

    default Gravity getInfectedGravity() {
        return Gravity.DOWN;
    }

    default Gravity getBaseGravity() {
        return Gravity.DOWN;
    }

    default void infect(Gravity gravity, int durationTicks) {}

    default void clearInfection() {}

    default void setGravityInstant(Gravity gravity) {
        setGravity(gravity);
    }

    default float getGravityAnomalyStrength() { return 0.0f; }

    default void setGravityAnomalyStrength(float strength) {}

    default float getPrevGravityAnomalyStrength() { return 0.0f; }
}
