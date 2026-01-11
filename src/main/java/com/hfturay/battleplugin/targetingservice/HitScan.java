package com.hfturay.battleplugin.targetingservice;

import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class HitScan {
    public static boolean IGNORE_PASSABLE_BLOCKS = true; 
    public static double PERFECT_RAY = 0.15;
    
    public static Entity HitScanPlayer(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Player player = battlePlayer.getPlayer();
        Location location = player.getEyeLocation();
        Predicate<Entity> playersOnlyFilter = entity -> !entity.equals(player) && entity instanceof Player;

        RayTraceResult result = BattlePlugin.getInstance().getWorld().rayTrace(location, location.getDirection(), distance, collisionMode, IGNORE_PASSABLE_BLOCKS, PERFECT_RAY, playersOnlyFilter);

        if (result == null) {
            return null;
        }

        if (result.getHitEntity() == null) {
            return null;
        } 

        return result.getHitEntity(); 
    }

    public static Block HitScanBlock(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Player player = battlePlayer.getPlayer();
        Location location = player.getEyeLocation();
        Predicate<Entity> ignoreCaster = entity -> !entity.equals(player);

        RayTraceResult result = BattlePlugin.getInstance().getWorld().rayTrace(location, location.getDirection(), distance, collisionMode, IGNORE_PASSABLE_BLOCKS, PERFECT_RAY, ignoreCaster);

        if (result == null) {
            return null;
        }

        if (result.getHitBlock() == null) {
            return null;
        } 

        return result.getHitBlock();
    }

    public static Entity HitScanEntities(BattlePlayer battlePlayer, double distance, FluidCollisionMode collisionMode) {
        Player player = battlePlayer.getPlayer();
        Location location = player.getEyeLocation();
        Predicate<Entity> filter = entity -> !entity.equals(player) && entity instanceof LivingEntity;
        
        RayTraceResult result = BattlePlugin.getInstance().getWorld().rayTrace(location, location.getDirection(), distance, collisionMode, IGNORE_PASSABLE_BLOCKS, PERFECT_RAY, filter);

        if (result == null) {
            battlePlayer.getPlayer().sendActionBar(Component.text("No Targets Found", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            return null;
        }
        
        if (result.getHitEntity() == null) {
            battlePlayer.getPlayer().sendActionBar(Component.text("No Targets Found", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            return null;
        }
        
        return result.getHitEntity();
    }
}

