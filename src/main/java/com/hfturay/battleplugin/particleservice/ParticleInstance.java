package com.hfturay.battleplugin.particleservice;

/** One running copy of a spec. Created by ParticleSpec.create, driven by the ParticleService tick loop. */
public interface ParticleInstance {

    /** Called once per server tick. age is 0 on the tick the effect starts. */
    void tick(int age);

    /** True when the instance has nothing left to show (only consulted when the play has no fixed duration). */
    default boolean isFinished() {
        return false;
    }

    /** Release anything held (cancel EffectLib effects). Always called exactly once when the play ends. */
    void stop();
}
