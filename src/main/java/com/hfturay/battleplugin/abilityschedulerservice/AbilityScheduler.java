package com.hfturay.battleplugin.abilityschedulerservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.hfturay.battleplugin.ability.Ability;

/**
 * Runs ScheduledAbilityTasks once per server tick.
 *
 * - Scheduling while a tick is running is safe: the task joins on the next tick.
 * - A task that throws is cancelled and logged. It never stops the others.
 * - Tasks can be cancelled by owner, by ability, or by any rule.
 */
public final class AbilityScheduler {
    private final Logger logger;
    private final List<ScheduledAbilityTask> running = new ArrayList<>();
    private final List<ScheduledAbilityTask> queued = new ArrayList<>();
    private BukkitTask loop;

    public AbilityScheduler(Logger logger) {
        this.logger = logger;
    }

    public void start(Plugin plugin) {
        loop = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Cancels everything (running their cleanup) and stops the loop. */
    public void shutdown() {
        if (loop != null) {
            loop.cancel();
            loop = null;
        }
        cancelWhere(task -> true);
    }

    /** Queues a task. Returns false (and does nothing) if it was already scheduled or is over. */
    public boolean schedule(ScheduledAbilityTask task) {
        if (task.getState() != ScheduledAbilityTask.State.NEW) {
            return false;
        }
        task.markQueued();
        queued.add(task);
        return true;
    }

    /** Advances every task by one tick. Public so it can be driven without a server. */
    public void tick() {
        running.addAll(queued);
        queued.clear();

        for (int i = 0; i < running.size(); i++) {
            ScheduledAbilityTask task = running.get(i);
            boolean over;
            try {
                over = task.tick();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Ability task " + task.getClass().getSimpleName() + " failed and was cancelled", e);
                safeCancel(task);
                over = true;
            }
            if (over) {
                running.remove(task);
                i--;
            }
        }
    }

    // ---- cancelling ----

    /** Cancels tasks matching the rule. Returns how many were cancelled. */
    public int cancelWhere(Predicate<ScheduledAbilityTask> rule) {
        int count = 0;
        // Cleanup code may schedule or cancel more tasks, so work on a copy.
        List<ScheduledAbilityTask> all = new ArrayList<>(running);
        all.addAll(queued);

        for (ScheduledAbilityTask task : all) {
            if (task.isActive() && rule.test(task)) {
                safeCancel(task);
                count++;
            }
        }
        running.removeIf(task -> !task.isActive());
        queued.removeIf(task -> !task.isActive());
        return count;
    }

    /** The player left: cancel everything they own. */
    public int cancelAll(UUID owner) {
        return cancelWhere(task -> owner.equals(task.ownerId()));
    }

    /** The player died: cancel their tasks, except placed things that outlive them. */
    public int cancelOnDeath(UUID owner) {
        return cancelWhere(task -> task.cancelOnOwnerDeath() && owner.equals(task.ownerId()));
    }

    public int cancel(Ability ability) {
        return cancelWhere(task -> task.getAbility() == ability);
    }

    public int activeCount() {
        return (int) (running.stream().filter(ScheduledAbilityTask::isActive).count()
            + queued.stream().filter(ScheduledAbilityTask::isActive).count());
    }

    private void safeCancel(ScheduledAbilityTask task) {
        try {
            task.cancel();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Cleanup of " + task.getClass().getSimpleName() + " failed", e);
        }
    }
}
