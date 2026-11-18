package com.hfturay.battleplugin.particleservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;

/**
 * The one place particles are played from.
 *
 * - One tick loop drives every effect (no per-effect Bukkit tasks, no new effect every tick).
 * - Every play is tracked per owner, so it can be cancelled in one call (quit, death, reset).
 * - A failing effect is ended and logged; it can never stop the others from ticking.
 * - A global cap and a density multiplier keep a busy fight from flooding the server.
 *
 * Verbs, matched to how abilities use particles:
 *   burst   one-shot or short animation at a point
 *   follow  stays on an entity, or on a projectile until it ends
 *   zone    stays at a fixed point for N ticks
 *   beam    between two anchors
 *   delay   run something after N ticks (owner-cancellable)
 * Chain with ParticleHandle.whenDone(...).
 */
public final class ParticleService {

    /** Play until the spec itself says it is finished (one frame for most direct specs). */
    public static final int UNTIL_DONE = 0;
    /** Play until cancelled or the anchor goes invalid (entity gone, projectile ended). */
    public static final int FOREVER = -1;

    /** Safety net: nothing is allowed to run longer than this, even FOREVER on a valid anchor. */
    private static final int MAX_AGE_TICKS = 20 * 60 * 5;

    private final Plugin plugin;
    private final EffectManager effectManager;

    private final List<ParticleHandle> active = new ArrayList<>();
    private final List<ParticleHandle> pending = new ArrayList<>();
    private final Set<String> reported = new HashSet<>();

    private BukkitTask task;
    private double density = 1.0;
    private int maxActive = 300;
    private long lastCapWarning = 0;

    public ParticleService(Plugin plugin, EffectManager effectManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
    }

    // ---- lifecycle ----

    public void start() {
        if (task != null) {
            return;
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Ends everything and stops the loop. Call from onDisable before disposing EffectLib. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ParticleHandle handle : active) {
            handle.end(false);
        }
        for (ParticleHandle handle : pending) {
            handle.end(false);
        }
        active.clear();
        pending.clear();
    }

    // ---- settings ----

    /** 1.0 = full detail, lower thins out particle counts. Clamped to 0.1..1. */
    public void setDensity(double density) {
        this.density = Math.max(0.1, Math.min(1.0, density));
    }

    public double getDensity() {
        return density;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = Math.max(1, maxActive);
    }

    public int getActiveCount() {
        return active.size() + pending.size();
    }

    // ---- verbs ----

    /** One-shot (or short animation) at a point. */
    public ParticleHandle burst(BattlePlayer owner, ParticleSpec spec, Location at) {
        return play(owner, spec, Anchor.at(at), null, UNTIL_DONE);
    }

    /** Stays on an entity for the given ticks (FOREVER = until the entity is gone). */
    public ParticleHandle follow(BattlePlayer owner, ParticleSpec spec, Entity entity, int ticks) {
        return play(owner, spec, Anchor.of(entity), null, ticks);
    }

    /** Stays on a projectile until it hits something or expires. */
    public ParticleHandle follow(BattlePlayer owner, ParticleSpec spec, VirtualProjectile projectile) {
        return play(owner, spec, Anchor.of(projectile), null, FOREVER);
    }

    /** Stays at a fixed point for the given ticks. */
    public ParticleHandle zone(BattlePlayer owner, ParticleSpec spec, Location at, int ticks) {
        return play(owner, spec, Anchor.at(at), null, ticks);
    }

    /** Between two anchors. Pass moving anchors (Anchor.of(supplier)) for a beam that tracks. */
    public ParticleHandle beam(BattlePlayer owner, ParticleSpec spec, Anchor from, Anchor to, int ticks) {
        return play(owner, spec, from, to, ticks);
    }

    /** Runs code after N ticks. Cancelled with the owner's other effects. N = 0 runs it immediately. */
    public ParticleHandle delay(BattlePlayer owner, int ticks, Runnable action) {
        ParticleSpec timer = context -> new ParticleInstance() {
            private boolean fired = false;

            @Override
            public void tick(int age) {
                if (!fired && age >= ticks) {
                    fired = true;
                    action.run();
                }
            }

            @Override
            public boolean isFinished() {
                return fired;
            }

            @Override
            public void stop() {
            }
        };
        return start(owner, timer, Anchor.none(), null, UNTIL_DONE, false);
    }

    /** General form. The verbs above are shortcuts for this. */
    public ParticleHandle play(BattlePlayer owner, ParticleSpec spec, Anchor anchor, Anchor target, int duration) {
        return start(owner, spec, anchor, target, duration, true);
    }

    /** Ends every effect and timer this player started. */
    public void cancelAll(BattlePlayer owner) {
        if (owner != null) {
            cancelAll(owner.getPlayer().getUniqueId());
        }
    }

    public void cancelAll(UUID owner) {
        cancelOwned(owner, false);
    }

    /**
     * Ends the owner's effects because they died. Visuals marked survivesOwnerDeath() (placed mines and clouds
     * that keep working without their owner) are left playing.
     */
    public void cancelOnDeath(UUID owner) {
        cancelOwned(owner, true);
    }

    private void cancelOwned(UUID owner, boolean deathOnly) {
        for (ParticleHandle handle : active) {
            if (owner.equals(handle.getOwner()) && !(deathOnly && handle.isSurvivesOwnerDeath())) {
                handle.end(false);
            }
        }
        for (ParticleHandle handle : pending) {
            if (owner.equals(handle.getOwner()) && !(deathOnly && handle.isSurvivesOwnerDeath())) {
                handle.end(false);
            }
        }
    }

    // ---- internals ----

    private ParticleHandle start(BattlePlayer owner, ParticleSpec spec, Anchor anchor, Anchor target,
                                 int duration, boolean countsTowardsCap) {
        UUID ownerId = owner == null ? null : owner.getPlayer().getUniqueId();
        ParticleHandle handle = new ParticleHandle(this, ownerId, anchor, target, duration);

        if (spec == null || !anchor.isValid() || (target != null && !target.isValid())) {
            handle.end(false);
            return handle;
        }

        if (countsTowardsCap && getActiveCount() >= maxActive) {
            warnCapReached();
            handle.end(false);
            return handle;
        }

        try {
            handle.instance = spec.create(new PlayContext(anchor, target, duration, density, effectManager));
            handle.instance.tick(0);
            handle.age = 1;
        } catch (RuntimeException e) {
            report(handle, e);
            handle.end(false);
            return handle;
        }

        if (handle.shouldEnd()) {
            handle.end(true);
            return handle;
        }

        // Never add to `active` here: this may be running inside tickAll (a whenDone callback starting
        // the next effect), and modifying the list mid-iteration would throw.
        pending.add(handle);
        return handle;
    }

    private void tickAll() {
        if (!pending.isEmpty()) {
            active.addAll(pending);
            pending.clear();
        }

        Iterator<ParticleHandle> it = active.iterator();
        while (it.hasNext()) {
            ParticleHandle handle = it.next();

            if (!handle.isActive()) {
                it.remove();
                continue;
            }

            try {
                if (!handle.anchor.isValid() || (handle.target != null && !handle.target.isValid())
                        || handle.age > MAX_AGE_TICKS) {
                    handle.end(true);
                    it.remove();
                    continue;
                }

                handle.instance.tick(handle.age++);

                if (handle.shouldEnd()) {
                    handle.end(true);
                    it.remove();
                }
            } catch (RuntimeException e) {
                report(handle, e);
                handle.end(false);
                it.remove();
            }
        }
    }

    void runSafely(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Particle callback failed: " + e);
        }
    }

    void report(ParticleHandle handle, RuntimeException e) {
        String key = e.getClass().getName() + ":" + (e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "");
        if (reported.add(key)) {
            plugin.getLogger().warning("Particle effect failed and was ended (further repeats are hidden): " + e);
            e.printStackTrace();
        }
    }

    private void warnCapReached() {
        long now = System.currentTimeMillis();
        if (now - lastCapWarning > 5000) {
            lastCapWarning = now;
            plugin.getLogger().warning("Particle cap of " + maxActive + " reached; new effects are being skipped.");
        }
    }
}
