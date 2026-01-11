package com.hfturay.battleplugin.targetingservice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.Keys;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class AOE {
    public static List<Entity> getTargets(BattlePlayer battlePlayer, Location center, double radius) {
        
        World world = BattlePlugin.getInstance().getWorld();
        List<Entity> targets = new ArrayList<>();
        Predicate<Entity> playersAndMobsFilter =  entity -> (entity instanceof Player) || (entity instanceof Mob);

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity.equals(battlePlayer.getPlayer())) {
                continue;
            }

            if (entity.getLocation().distanceSquared(center) > radius * radius) {
                continue;
            }

            if (!playersAndMobsFilter.test(entity)) {
                continue;
            }
            PersistentDataContainer entityPDC = entity.getPersistentDataContainer();
            BattlePlugin.getInstance().getLogger().info(entityPDC.get(Keys.shikigamiOwner, PersistentDataType.STRING));
            
            if (entityPDC.has(Keys.shikigamiOwner, PersistentDataType.STRING) && 
                entityPDC.get(Keys.shikigamiOwner, PersistentDataType.STRING).equals(battlePlayer.getPlayer().getUniqueId().toString())) {
                continue;
            }

            targets.add(entity);
        }

        return targets;
    }

    public static List<Entity> getTargets(BattlePlayer battlePlayer, Location center, double radius, double x, double y, double z) {
        
        World world = BattlePlugin.getInstance().getWorld();
        List<Entity> targets = new ArrayList<>();
        Predicate<Entity> playersAndMobsFilter =  entity -> (entity instanceof Player) || (entity instanceof Mob);

        for (Entity entity : world.getNearbyEntities(center, x, y, z)) {
            if (entity.equals(battlePlayer.getPlayer())) {
                continue;
            }

            if (entity.getLocation().distanceSquared(center) > radius * radius) {
                continue;
            }

            if (!playersAndMobsFilter.test(entity)) {
                continue;
            }

            PersistentDataContainer entityPDC = entity.getPersistentDataContainer();
            if (entityPDC.has(Keys.shikigamiOwner, PersistentDataType.STRING) && 
                entityPDC.get(Keys.shikigamiOwner, PersistentDataType.STRING).equals(battlePlayer.getPlayer().getUniqueId().toString())) {
                continue;
            }
            targets.add(entity);
        }
        return targets;
    }
}
