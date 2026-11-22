package com.hfturay.battleplugin.shikigamiservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.world.EntitiesLoadEvent;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.Targets;

/**
 * Keeps track of summoned mobs (shikigami) and makes sure none are left behind.
 *
 *  - Ticks every shikigami each tick. One that throws is logged and removed, and the others carry on.
 *  - A shikigami ends when its own tick says so, when it is killed (owner died or left), or when the mob
 *    is already gone (killed by someone, unloaded). Nothing stays in the list after that.
 *  - They drop no loot or experience.
 *  - On shutdown every tracked mob is removed. Mobs that carry an owner tag but are not tracked (left over from
 *    a crash, a restart or a reload) are removed when the plugin starts and whenever their chunk loads.
 */
public final class ShikigamiService implements Listener {
    private final List<Shikigami> active = new ArrayList<>();
    private final Logger logger;
    private Plugin plugin;
    private BukkitTask task;

    public ShikigamiService(Logger logger) {
        this.logger = logger;
    }

    public void start(Plugin plugin) {
        this.plugin = plugin;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);

        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            removed += removeOrphans(world.getEntities());
        }
        if (removed > 0) {
            logger.info("Removed " + removed + " shikigami left over from before the plugin started");
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Shikigami shikigami : new ArrayList<>(active)) {
            end(shikigami, false, EndReason.SHUTDOWN);
        }
        active.clear();
    }

    // ---- who may summon, and how many ----

    /** A rule that can stop a summon (the Chaos energy bar will be one). Return a message, or null to allow. */
    @FunctionalInterface
    public interface SummonGate {
        String blockedReason(BattlePlayer owner, String shikigamiName);
    }

    private final List<SummonGate> gates = new ArrayList<>();

    public void addSummonGate(SummonGate gate) {
        gates.add(gate);
    }

    /** How many live shikigami this player has out. */
    public int countOwnedBy(UUID owner) {
        int count = 0;
        for (Shikigami shikigami : active) {
            if (shikigami.getOwner().getPlayer().getUniqueId().equals(owner) && shikigami.getMob().isValid()) {
                count++;
            }
        }
        return count;
    }

    /** Why this player cannot summon right now (message for them), or null if they can. */
    public String summonBlockedReason(BattlePlayer owner, String shikigamiName, int maxActive) {
        if (countOwnedBy(owner.getPlayer().getUniqueId()) >= maxActive) {
            return maxActive == 1 ? "Your shikigami is already out" : "You already have " + maxActive + " shikigami out";
        }
        for (SummonGate gate : gates) {
            String reason = gate.blockedReason(owner, shikigamiName);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    // ---- who hit whom (for targeting) ----

    private record Mark(UUID other, int tick) { }

    private final Map<UUID, Mark> lastHit = new HashMap<>();
    private final Map<UUID, Mark> lastAttackedBy = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        int now = Bukkit.getCurrentTick();
        Entity damager = shooterOf(event.getDamager());
        if (damager instanceof Player attacker) {
            lastHit.put(attacker.getUniqueId(), new Mark(event.getEntity().getUniqueId(), now));
        }
        if (event.getEntity() instanceof Player victim) {
            lastAttackedBy.put(victim.getUniqueId(), new Mark(damager.getUniqueId(), now));
        }
    }

    /** An arrow or other projectile counts as whoever shot it. */
    private static Entity shooterOf(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return damager;
    }

    private static LivingEntity resolve(Mark mark, int maxAgeTicks) {
        if (mark == null || Bukkit.getCurrentTick() - mark.tick() > maxAgeTicks) {
            return null;
        }
        return Bukkit.getEntity(mark.other()) instanceof LivingEntity living ? living : null;
    }

    /** Whoever this player hit most recently (within maxAgeTicks), or null. */
    public LivingEntity lastHitBy(UUID player, int maxAgeTicks) {
        return resolve(lastHit.get(player), maxAgeTicks);
    }

    /** Whoever hurt this player most recently (within maxAgeTicks), or null. */
    public LivingEntity lastAttackerOf(UUID player, int maxAgeTicks) {
        return resolve(lastAttackedBy.get(player), maxAgeTicks);
    }

    /**
     * This player's latest fight (within maxAgeTicks): whoever they hit or whoever hurt them, whichever happened last.
     * If the newer one is gone, the older one is used. Null if neither is around.
     */
    public LivingEntity lastFoeOf(UUID player, int maxAgeTicks) {
        Mark hit = lastHit.get(player);
        Mark hurt = lastAttackedBy.get(player);
        boolean hitIsNewer = hurt == null || (hit != null && hit.tick() >= hurt.tick());
        LivingEntity newer = resolve(hitIsNewer ? hit : hurt, maxAgeTicks);
        return newer != null ? newer : resolve(hitIsNewer ? hurt : hit, maxAgeTicks);
    }

    public void add(Shikigami shikigami) {
        if (shikigami != null && !active.contains(shikigami)) {
            active.add(shikigami);
        }
    }

    public int activeCount() {
        return active.size();
    }

    public void tick() {
        for (Shikigami shikigami : new ArrayList<>(active)) {
            if (!active.contains(shikigami)) {
                continue;
            }

            if (!shikigami.getMob().isValid()) {
                active.remove(shikigami);       // already dead or unloaded
                react(shikigami, shikigami.getMob().isDead() ? EndReason.KILLED : EndReason.DISMISSED);
                continue;
            }
            if (shikigami.isDead()) {
                active.remove(shikigami);
                end(shikigami, true, EndReason.DISMISSED);
                continue;
            }

            boolean finished;
            try {
                finished = shikigami.tick();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Shikigami " + shikigami.getDisplayName() + " failed and was removed", e);
                finished = true;
            }
            if (finished) {
                active.remove(shikigami);
                end(shikigami, true, EndReason.TIMEOUT);   // a tick that just says "done" means its time ran out
            }
        }
    }

    /** Ends everything this player summoned (they left). */
    public void killOwnedBy(UUID owner) {
        for (Shikigami shikigami : new ArrayList<>(active)) {
            if (shikigami.getOwner().getPlayer().getUniqueId().equals(owner)) {
                shikigami.kill();
                active.remove(shikigami);
                end(shikigami, true, EndReason.OWNER_LEFT);
            }
        }
        lastHit.remove(owner);
        lastAttackedBy.remove(owner);
    }

    /** Runs the shikigami's end reaction once. A reaction that fails is logged, never allowed to break cleanup. */
    private void react(Shikigami shikigami, EndReason reason) {
        try {
            shikigami.notifyEnded(reason);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Shikigami " + shikigami.getDisplayName() + " failed while ending", e);
        }
    }

    /** @param animate true plays a normal death; false removes the mob silently (shutdown) */
    private void end(Shikigami shikigami, boolean animate, EndReason reason) {
        react(shikigami, reason);
        Mob mob = shikigami.getMob();
        try {
            if (mob.isValid()) {
                if (animate) {
                    mob.setHealth(0);
                } else {
                    mob.remove();
                }
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not remove shikigami " + shikigami.getDisplayName(), e);
        }
    }

    // ---- leftovers ----

    private boolean isTracked(Entity entity) {
        for (Shikigami shikigami : active) {
            if (shikigami.getMob().getUniqueId().equals(entity.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /** True if this is a summoned mob that nobody is tracking. */
    private boolean isOrphan(Entity entity) {
        return Targets.shikigamiOwnerOf(entity) != null && !isTracked(entity);
    }

    /** Removes shikigami that are not tracked. Returns how many. */
    public int removeOrphans(Collection<? extends Entity> entities) {
        int removed = 0;
        for (Entity entity : entities) {
            try {
                if (entity.isValid() && isOrphan(entity)) {
                    entity.remove();
                    removed++;
                }
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Could not check an entity for leftover shikigami", e);
            }
        }
        return removed;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        List<Entity> orphans = new ArrayList<>();
        for (Entity entity : event.getEntities()) {
            if (isOrphan(entity)) {
                orphans.add(entity);
            }
        }
        if (orphans.isEmpty() || plugin == null) {
            return;
        }
        // Not inside the load event itself: the chunk is still being set up.
        plugin.getServer().getScheduler().runTask(plugin, () -> removeOrphans(orphans));
    }

    // ---- no loot ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (stripLoot(event.getEntity(), event.getDrops())) {
            event.setDroppedExp(0);
        }

        // Killed while still tracked (an enemy got it): its end reaction runs now, with the reason KILLED.
        // A shikigami the service itself ended already reacted, and notifyEnded only ever runs once.
        for (Shikigami shikigami : new ArrayList<>(active)) {
            if (shikigami.getMob().getUniqueId().equals(event.getEntity().getUniqueId())) {
                active.remove(shikigami);
                react(shikigami, EndReason.KILLED);
            }
        }
    }

    /** Clears the drops of a shikigami. Returns true if it was one. */
    public boolean stripLoot(Entity entity, List<ItemStack> drops) {
        if (Targets.shikigamiOwnerOf(entity) == null) {
            return false;
        }
        drops.clear();
        return true;
    }
}
