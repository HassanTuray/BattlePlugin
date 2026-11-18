package com.hfturay.battleplugin.abilityschedulerservice;

import java.util.UUID;

import com.hfturay.battleplugin.ability.Ability;

/**
 * Something an ability wants to happen later or over several ticks.
 *
 * Hooks, in the order they can fire:
 *   onTick()    every tick while running (also on the tick it finishes)
 *   onFinish()  the timer ran out, or finishNow() was called. Normal completion.
 *   onCancel()  stopped early (owner left or died, ability removed, plugin disabled, cancel() called).
 *               Do cleanup here: end visuals, undo temporary buffs. Never runs together with onFinish().
 *
 * cancel() stops the task WITHOUT finishing it. Use finishNow() to end it early and still run onFinish().
 * A task runs at most once and cannot be scheduled again afterwards.
 */
public abstract class ScheduledAbilityTask {
    public enum State { NEW, QUEUED, RUNNING, FINISHED, CANCELLED }

    protected int ticksRemaining;
    protected final Ability ability;
    private State state = State.NEW;

    public ScheduledAbilityTask(int duration, Ability ability) {
        this.ticksRemaining = duration;
        this.ability = ability;
    }

    // ---- override these ----

    protected void onTick() { }

    protected void onFinish() { }

    protected void onCancel() { }

    /** Whether the owner dying stops this task. Placed things (mines, gas clouds) return false. */
    public boolean cancelOnOwnerDeath() {
        return true;
    }

    // ---- control ----

    public final boolean isActive() {
        return state == State.QUEUED || state == State.RUNNING;
    }

    public final State getState() {
        return state;
    }

    public final int getTicksRemaining() {
        return ticksRemaining;
    }

    public final void setTicksRemaining(int ticks) {
        this.ticksRemaining = ticks;
    }

    public final Ability getAbility() {
        return ability;
    }

    /** The player this task belongs to, or null if it has none. */
    public UUID ownerId() {
        if (ability == null || ability.getOwner() == null || ability.getOwner().getPlayer() == null) {
            return null;
        }
        return ability.getOwner().getPlayer().getUniqueId();
    }

    /** Stops the task without finishing it. Safe to call any number of times. */
    public final void cancel() {
        if (!isActive()) {
            return;
        }
        state = State.CANCELLED;
        onCancel();
    }

    /** Ends the task right now and runs onFinish(). No effect if it is no longer active. */
    public final void finishNow() {
        if (!isActive()) {
            return;
        }
        state = State.FINISHED;
        onFinish();
    }

    // ---- used by AbilityScheduler ----

    final void markQueued() {
        state = State.QUEUED;
    }

    /** One tick. Returns true when the task is over and should be dropped. */
    final boolean tick() {
        if (!isActive()) {
            return true;
        }
        state = State.RUNNING;

        onTick();
        if (!isActive()) {
            return true;    // onTick cancelled or finished it
        }

        ticksRemaining--;
        if (ticksRemaining <= 0) {
            finishNow();
        }
        return !isActive();
    }
}
