package com.hfturay.battleplugin.domainservice.neverendingnight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Keeps track of the Neverending Night domains that are running and the creatures they spawned, and makes the
 * creatures behave: they only go after the caster's enemies, never leave the dome, never pick up blocks, burn,
 * or drop anything.
 *
 * A creature is recognised by a tag on it (the caster's id), so this works even after a reload.
 */
public final class NeverendingNightService implements Listener {
    private final NamespacedKey mobKey;
    private final Map<UUID, NeverendingNightEffects> active = new HashMap<>();

    public NeverendingNightService(Plugin plugin) {
        this.mobKey = new NamespacedKey(plugin, "night-creature");
    }

    void register(UUID caster, NeverendingNightEffects effects) {
        active.put(caster, effects);
    }

    void unregister(UUID caster, NeverendingNightEffects effects) {
        active.remove(caster, effects);
    }

    /** The running Neverending Night of this caster, or null. */
    public NeverendingNightEffects of(UUID caster) {
        return active.get(caster);
    }

    void tag(Entity entity, UUID caster) {
        entity.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, caster.toString());
    }

    /** True if this entity is a creature of some Neverending Night (so beams and the like do not hurt it). */
    public boolean isCreature(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(mobKey, PersistentDataType.STRING);
    }

    /** The domain this creature belongs to, or null (not a creature, or its domain is over). */
    private NeverendingNightEffects domainOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        String caster = entity.getPersistentDataContainer().get(mobKey, PersistentDataType.STRING);
        if (caster == null) {
            return null;
        }
        try {
            return active.get(UUID.fromString(caster));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- behaviour of the creatures ----

    /** Creatures only target the caster's enemies. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!isCreature(event.getEntity())) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }
        NeverendingNightEffects domain = domainOf(event.getEntity());
        if (domain == null || !domain.mayTarget(target)) {
            event.setCancelled(true);
        }
    }

    /** Creatures only hurt what they may attack: never the caster, their own kind, or anything the caster owns. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isCreature(event.getDamager()) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        NeverendingNightEffects domain = domainOf(event.getDamager());
        if (domain == null || !domain.mayTarget(victim)) {
            event.setCancelled(true);
        }
    }

    /** Endermen cannot teleport out of the dome. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (!isCreature(event.getEntity())) {
            return;
        }
        NeverendingNightEffects domain = domainOf(event.getEntity());
        if (domain == null || event.getTo() == null || !domain.isInside(event.getTo())) {
            event.setCancelled(true);
        }
    }

    /** Endermen must not pick up the dome's blocks. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (isCreature(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (isCreature(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * The Warden pulses Darkness onto every player near it. The caster is spared (the enemies still get it, and the
     * Darkness this domain gives its enemies on purpose is not from the Warden, so it is not touched).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWardenDarkness(EntityPotionEffectEvent event) {
        if (event.getCause() != EntityPotionEffectEvent.Cause.WARDEN || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (active.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCreature(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
