package com.hfturay.battleplugin.particleservice;

import de.slikey.effectlib.EffectManager;

/**
 * Everything a spec needs when it starts playing.
 *
 * @param anchor   where the effect lives
 * @param target   second end point for beams (null for everything else)
 * @param duration ticks to play, or ParticleService.UNTIL_DONE / FOREVER
 * @param density  global particle density multiplier, 0..1
 * @param effects  EffectLib manager, for specs that need it
 */
public record PlayContext(Anchor anchor, Anchor target, int duration, double density, EffectManager effects) {

    /** How many iterations an EffectLib effect with the given period needs to cover the requested duration. */
    public int iterationsFor(int period, int untilDoneIterations) {
        if (duration > 0) {
            return Math.max(1, (int) Math.ceil(duration / (double) Math.max(1, period)));
        }
        if (duration == ParticleService.FOREVER) {
            return -1;
        }
        return untilDoneIterations;
    }
}
