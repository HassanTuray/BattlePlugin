package com.hfturay.battleplugin.projectileservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Flies every VirtualProjectile once per server tick.
 *
 * - Launching from inside a hit callback (a projectile that spawns another) is safe: the new one starts flying next tick.
 * - A projectile that throws is ended and logged. It never stops the others.
 * - Projectiles can be ended by owner, or by any rule.
 */
public final class ProjectileService {
    private final Logger logger;
    private final List<VirtualProjectile> flying = new ArrayList<>();
    private final List<VirtualProjectile> launching = new ArrayList<>();
    private BukkitTask loop;

    public ProjectileService(Logger logger) {
        this.logger = logger;
    }

    public void start(Plugin plugin) {
        loop = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Ends every projectile (no hit callbacks) and stops the loop. */
    public void shutdown() {
        if (loop != null) {
            loop.cancel();
            loop = null;
        }
        cancelWhere(projectile -> true);
    }

    /** Starts a projectile flying (and its visuals). It first moves on the next tick. */
    public void add(VirtualProjectile projectile) {
        projectile.onLaunch();
        launching.add(projectile);
    }

    /** Advances every projectile by one tick. Public so it can be driven without a server. */
    public void tick() {
        flying.addAll(launching);
        launching.clear();

        for (int i = 0; i < flying.size(); i++) {
            VirtualProjectile projectile = flying.get(i);
            boolean over;
            try {
                over = projectile.tick();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "A projectile failed and was ended", e);
                projectile.cancel();
                over = true;
            }
            if (over) {
                flying.remove(i--);
            }
        }
    }

    /** The player left: end everything they fired. */
    public int cancelAll(UUID owner) {
        return cancelWhere(projectile -> owner.equals(projectile.getOwner().getPlayer().getUniqueId()));
    }

    public int cancelWhere(Predicate<VirtualProjectile> rule) {
        int count = 0;
        List<VirtualProjectile> all = new ArrayList<>(flying);
        all.addAll(launching);

        for (VirtualProjectile projectile : all) {
            if (!projectile.isFinished() && rule.test(projectile)) {
                projectile.cancel();
                count++;
            }
        }
        flying.removeIf(VirtualProjectile::isFinished);
        launching.removeIf(VirtualProjectile::isFinished);
        return count;
    }

    public int activeCount() {
        return (int) (flying.stream().filter(p -> !p.isFinished()).count()
            + launching.stream().filter(p -> !p.isFinished()).count());
    }
}
