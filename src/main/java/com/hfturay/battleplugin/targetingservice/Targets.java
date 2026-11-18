package com.hfturay.battleplugin.targetingservice;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.Keys;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The single answer to "can this ability's attacker hit this entity?". Hit scans, area attacks and projectiles
 * all use it, so they can no longer disagree (before, hit scans could hit armor stands and any living thing,
 * while area attacks and projectiles only hit players and mobs).
 *
 * Targetable: a player or mob that is alive and loaded, that is not the attacker, not a spectator, and not a
 * shikigami summoned by the attacker, and not untargetable (see ImmunityService.grantUntargetable). Because every
 * ability asks here, an untargetable entity gets none of an ability's effects, not just no damage.
 */
public final class Targets {
    private Targets() { }

    /** Who summoned this entity as a shikigami, or null if it is not one. Replaceable for tests. */
    @FunctionalInterface
    public interface ShikigamiOwnerLookup {
        UUID ownerOf(Entity entity);
    }

    private static ShikigamiOwnerLookup lookup = PdcLookup::ownerOf;

    /** Whether an entity is currently untargetable. Replaceable for tests. */
    @FunctionalInterface
    public interface UntargetableLookup {
        boolean isUntargetable(Entity entity);
    }

    private static UntargetableLookup untargetable = Targets::fromImmunityService;

    public static void setUntargetableLookup(UntargetableLookup replacement) {
        untargetable = replacement == null ? Targets::fromImmunityService : replacement;
    }

    private static boolean fromImmunityService(Entity entity) {
        BattlePlugin plugin = BattlePlugin.getInstance();
        return plugin != null && plugin.getImmunityService() != null
            && plugin.getImmunityService().isUntargetable(entity.getUniqueId());
    }

    /**
     * True if the entity is untargetable right now. For effects that were aimed earlier and land later (delayed
     * knockback and the like): check this again at the moment they land.
     */
    public static boolean isUntargetable(Entity entity) {
        return entity != null && untargetable.isUntargetable(entity);
    }

    public static void setShikigamiOwnerLookup(ShikigamiOwnerLookup replacement) {
        lookup = replacement == null ? PdcLookup::ownerOf : replacement;
    }

    /** Who summoned this entity as a shikigami, or null if it is not one. */
    public static UUID shikigamiOwnerOf(Entity entity) {
        return lookup.ownerOf(entity);
    }

    public static boolean isTargetable(BattlePlayer attacker, Entity entity) {
        return isTargetable(attacker.getPlayer().getUniqueId(), entity);
    }

    public static boolean isTargetable(UUID attackerId, Entity entity) {
        if (entity == null || !entity.isValid()) {
            return false;
        }
        if (!(entity instanceof Player) && !(entity instanceof Mob)) {
            return false;
        }
        if (entity.getUniqueId().equals(attackerId)) {
            return false;
        }
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (untargetable.isUntargetable(entity)) {
            return false;
        }
        return !attackerId.equals(lookup.ownerOf(entity));
    }

    /** Reads the shikigami owner from the entity. In its own class so the plugin key is only built when needed. */
    private static final class PdcLookup {
        static UUID ownerOf(Entity entity) {
            PersistentDataContainer data = entity.getPersistentDataContainer();
            String owner = data.get(Keys.shikigamiOwner, PersistentDataType.STRING);
            if (owner == null) {
                return null;
            }
            try {
                return UUID.fromString(owner);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
