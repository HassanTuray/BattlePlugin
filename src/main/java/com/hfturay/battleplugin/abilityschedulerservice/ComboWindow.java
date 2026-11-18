package com.hfturay.battleplugin.abilityschedulerservice;

import java.util.function.Supplier;

import com.hfturay.battleplugin.ability.Ability;

/**
 * The "do the next hit within N ticks or the combo ends" pattern, for multi-stage abilities.
 *
 * Keeps the stage number and one expiry timer. hit() moves to a stage and restarts the timer.
 * If the timer runs out, the stage goes back to 0 and onExpire runs (usually: start the cooldown).
 * reset() ends the combo yourself without calling onExpire.
 */
public final class ComboWindow {
    private final Ability ability;
    private final java.util.function.IntSupplier windowTicks;
    private final Runnable onExpire;
    private final Supplier<AbilityScheduler> scheduler;

    private int stage = 0;
    private Expiry timer;

    public ComboWindow(Ability ability, int windowTicks, Runnable onExpire, Supplier<AbilityScheduler> scheduler) {
        this(ability, () -> windowTicks, onExpire, scheduler);
    }

    /** The window length is asked each time the timer starts or restarts, so a config change applies at once. */
    public ComboWindow(Ability ability, java.util.function.IntSupplier windowTicks, Runnable onExpire, Supplier<AbilityScheduler> scheduler) {
        this.ability = ability;
        this.windowTicks = windowTicks;
        this.onExpire = onExpire;
        this.scheduler = scheduler;
    }

    public int stage() {
        return stage;
    }

    public boolean isOpen() {
        return timer != null && timer.isActive();
    }

    /** Go to this stage and restart the timer (starting it if it is not running). */
    public void hit(int nextStage) {
        stage = nextStage;
        if (isOpen()) {
            timer.setTicksRemaining(windowTicks.getAsInt());
        } else {
            timer = new Expiry(windowTicks.getAsInt(), ability);
            scheduler.get().schedule(timer);
        }
    }

    /** Change the stage but leave the timer alone. */
    public void setStage(int stage) {
        this.stage = stage;
    }

    /** End the combo now. onExpire does NOT run. */
    public void reset() {
        stage = 0;
        if (timer != null) {
            Expiry old = timer;
            timer = null;
            old.cancel();
        }
    }

    private final class Expiry extends ScheduledAbilityTask {
        Expiry(int duration, Ability ability) {
            super(duration, ability);
        }

        @Override
        protected void onFinish() {
            stage = 0;
            timer = null;
            onExpire.run();
        }

        @Override
        protected void onCancel() {
            // Owner left, died, or the ability was removed. The combo is simply over.
            stage = 0;
            if (timer == this) {
                timer = null;
            }
        }
    }
}
