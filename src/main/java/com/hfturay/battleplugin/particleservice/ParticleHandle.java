package com.hfturay.battleplugin.particleservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Returned by every ParticleService verb. Lets an ability cancel what it started, or chain something after it.
 * The service owns the lifecycle; holding on to a handle is optional.
 */
public final class ParticleHandle {

    private final UUID owner;
    private final ParticleService service;
    final Anchor anchor;
    final Anchor target;
    final int duration;

    ParticleInstance instance;
    int age = 0;
    private boolean active = true;
    private List<Runnable> whenDone;

    ParticleHandle(ParticleService service, UUID owner, Anchor anchor, Anchor target, int duration) {
        this.service = service;
        this.owner = owner;
        this.anchor = anchor;
        this.target = target;
        this.duration = duration;
    }

    public UUID getOwner() {
        return owner;
    }

    private boolean survivesOwnerDeath = false;

    /**
     * Marks a visual that belongs to something which keeps existing after its owner dies (a placed mine or cloud).
     * It is skipped when the owner's particles are cancelled on death. Quitting still cancels it.
     */
    public ParticleHandle survivesOwnerDeath() {
        this.survivesOwnerDeath = true;
        return this;
    }

    boolean isSurvivesOwnerDeath() {
        return survivesOwnerDeath;
    }

    public boolean isActive() {
        return active;
    }

    /** Ends the play now. whenDone callbacks do NOT run. Safe to call repeatedly. */
    public void cancel() {
        end(false);
    }

    /**
     * Runs the callback when the play finishes by itself (duration elapsed, projectile ended, entity gone).
     * Does not run if the play is cancelled. If the play already ended naturally the callback runs immediately.
     * Use this to chain: burst -> aura, impact -> lingering zone.
     */
    public ParticleHandle whenDone(Runnable callback) {
        if (!active) {
            if (endedNaturally) {
                service.runSafely(callback);
            }
            return this;
        }
        if (whenDone == null) {
            whenDone = new ArrayList<>(1);
        }
        whenDone.add(callback);
        return this;
    }

    private boolean endedNaturally = false;

    boolean shouldEnd() {
        return (duration > 0 && age >= duration) || instance.isFinished();
    }

    void end(boolean natural) {
        if (!active) {
            return;
        }
        active = false;
        endedNaturally = natural;

        try {
            if (instance != null) {
                instance.stop();
            }
        } catch (RuntimeException e) {
            service.report(this, e);
        }

        if (natural && whenDone != null) {
            List<Runnable> callbacks = whenDone;
            whenDone = null;
            for (Runnable callback : callbacks) {
                service.runSafely(callback);
            }
        }
    }
}
