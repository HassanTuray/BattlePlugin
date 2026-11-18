package com.hfturay.battleplugin.visualservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The one place entity-based visuals (blocks and items shown with Display entities) are created and removed.
 * Particles stay in the ParticleService; this is for the few moves that need something solid.
 *
 * - One tick loop moves every visual. A visual spawns on its first tick, so it appears where the projectile
 *   actually is rather than inside the owner's head at launch.
 * - A visual ends the tick its anchor ends, and can also be cancelled by hand (the projectile does this the
 *   moment it hits, so nothing lingers for even a tick).
 * - Every entity is tagged and not saved with the world, so a crash cannot leave one behind. Tagged entities that
 *   turn up anyway (loaded from disk) are removed.
 * - A global cap and a hard maximum age keep a mistake from filling the world with entities.
 * - A failing visual is ended and logged; it never stops the others.
 */
public final class VisualService implements Listener {

    /** Nothing lives longer than this, whatever its anchor says. */
    public static final int MAX_AGE_TICKS = 20 * 30;

    private final Logger logger;
    private final Consumer<Entity> tagger;
    private final Predicate<Entity> isTagged;
    private final Plugin plugin;

    private final List<VisualHandle> active = new ArrayList<>();
    private final Set<String> reported = new HashSet<>();
    private BukkitTask task;
    private int maxActive = 40;
    private long lastCapWarning = 0;

    public VisualService(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        NamespacedKey key = new NamespacedKey(plugin, "visual");
        this.tagger = entity -> entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        this.isTagged = entity -> entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** For tests: no server needed. */
    public VisualService(Logger logger, Consumer<Entity> tagger, Predicate<Entity> isTagged) {
        this.plugin = null;
        this.logger = logger;
        this.tagger = tagger;
        this.isTagged = isTagged;
    }

    // ---- lifecycle ----

    public void start() {
        if (task != null || plugin == null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);

        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            removed += removeTagged(world.getEntities());
        }
        if (removed > 0) {
            logger.info("Removed " + removed + " visual entities left over from before the plugin started");
        }
    }

    /** Removes every visual and stops the loop. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (VisualHandle handle : new ArrayList<>(active)) {
            handle.cancel();
        }
        active.clear();
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = Math.max(1, maxActive);
    }

    public int activeCount() {
        return active.size();
    }

    // ---- starting and ending ----

    /** Shows the spec on the anchor until the anchor ends or the handle is cancelled. */
    public VisualHandle follow(BattlePlayer owner, VisualSpec spec, VisualAnchor anchor) {
        UUID ownerId = owner == null ? null : owner.getPlayer().getUniqueId();
        VisualHandle handle = new VisualHandle(ownerId, spec, anchor);

        if (spec == null || anchor == null) {
            handle.cancel();
            return handle;
        }
        if (active.size() >= maxActive) {
            warnCapReached();
            handle.cancel();
            return handle;
        }

        active.add(handle);
        return handle;
    }

    /** Removes everything this player started. */
    public void cancelAll(UUID owner) {
        for (VisualHandle handle : new ArrayList<>(active)) {
            if (owner.equals(handle.getOwner())) {
                handle.cancel();
            }
        }
        active.removeIf(handle -> !handle.isActive());
    }

    // ---- the loop ----

    /** Public so it can be driven without a server. */
    public void tick() {
        for (VisualHandle handle : new ArrayList<>(active)) {
            if (!handle.isActive()) {
                active.remove(handle);
                continue;
            }

            try {
                if (!handle.anchor.isValid() || handle.age > MAX_AGE_TICKS) {
                    handle.cancel();
                    active.remove(handle);
                    continue;
                }

                Location at = handle.anchor.location();
                Vector direction = handle.anchor.direction();
                double length = handle.anchor.length();

                if (handle.instance == null) {
                    handle.instance = handle.spec.create(new VisualContext(at.clone(), direction.clone(), tagger, length));
                } else if (!handle.instance.isValid()) {
                    handle.cancel();
                    active.remove(handle);
                    continue;
                } else {
                    handle.instance.update(at, direction, length);
                }
                handle.age++;
            } catch (RuntimeException e) {
                report(e);
                handle.cancel();
                active.remove(handle);
            }
        }
    }

    // ---- leftovers ----

    /** Removes tagged entities. Returns how many. Used on start, and on chunks loaded from disk. */
    public int removeTagged(Collection<? extends Entity> entities) {
        int removed = 0;
        for (Entity entity : entities) {
            try {
                if (entity.isValid() && isTagged.test(entity)) {
                    entity.remove();
                    removed++;
                }
            } catch (RuntimeException e) {
                report(e);
            }
        }
        return removed;
    }

    /** Entities a visual spawned are never saved, so a tagged one arriving from disk is always a leftover. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        List<Entity> leftovers = new ArrayList<>();
        for (Entity entity : event.getEntities()) {
            if (isTagged.test(entity)) {
                leftovers.add(entity);
            }
        }
        if (leftovers.isEmpty() || plugin == null) {
            return;
        }
        // Not inside the load event itself: the chunk is still being set up.
        plugin.getServer().getScheduler().runTask(plugin, () -> removeTagged(leftovers));
    }

    // ---- reporting ----

    private void report(RuntimeException e) {
        String key = e.getClass().getName() + ":" + (e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "");
        if (reported.add(key)) {
            logger.log(Level.WARNING, "A visual failed and was ended (further repeats are hidden)", e);
        }
    }

    private void warnCapReached() {
        long now = System.currentTimeMillis();
        if (now - lastCapWarning > 5000) {
            lastCapWarning = now;
            logger.warning("Visual cap of " + maxActive + " reached; new visuals are being skipped.");
        }
    }
}
