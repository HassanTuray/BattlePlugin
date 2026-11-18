package com.hfturay.battleplugin.targetingservice;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Instant "what am I looking at" checks, in the world the player is actually in. */
public class HitScan {
    public static boolean IGNORE_PASSABLE_BLOCKS = true;
    public static double PERFECT_RAY = 0.15;

    /** The player being looked at, or null. */
    public static Entity HitScanPlayer(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        RayTraceResult result = trace(battlePlayer, distance, collisionMode);
        if (result == null || !(result.getHitEntity() instanceof Player)) {
            return null;
        }
        return result.getHitEntity();
    }

    /**
     * The block being looked at, or null. Entities in the way do not block the check.
     * (It used to be a combined entity-and-block ray, so a mob in front hid the block behind it.)
     */
    public static Block HitScanBlock(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Player player = battlePlayer.getPlayer();
        Location eye = player.getEyeLocation();
        RayTraceResult result = eye.getWorld().rayTraceBlocks(eye, eye.getDirection(), distance, collisionMode, IGNORE_PASSABLE_BLOCKS);
        if (result == null) {
            return null;
        }
        return result.getHitBlock();
    }

    /** The target being looked at, or null. Tells the player when there is none. Use for button-press abilities. */
    public static Entity HitScanEntities(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Entity target = HitScanEntitiesQuietly(battlePlayer, distance, collisionMode);
        if (target == null) {
            battlePlayer.getPlayer().sendActionBar(Component.text("No Targets Found", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        }
        return target;
    }

    /** Same, without the message. Use for things that check every tick, like the Laser. */
    public static Entity HitScanEntitiesQuietly(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        RayTraceResult result = trace(battlePlayer, distance, collisionMode);
        if (result == null) {
            return null;
        }
        return result.getHitEntity();
    }

    private static RayTraceResult trace(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Player player = battlePlayer.getPlayer();
        Location eye = player.getEyeLocation();
        return eye.getWorld().rayTrace(eye, eye.getDirection(), distance, collisionMode,
            IGNORE_PASSABLE_BLOCKS, PERFECT_RAY, entity -> Targets.isTargetable(battlePlayer, entity));
    }
}
