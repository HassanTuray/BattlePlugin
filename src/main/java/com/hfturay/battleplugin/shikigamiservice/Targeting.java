package com.hfturay.battleplugin.shikigamiservice;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.Targets;

/** The ready-made ways a shikigami can choose its target. Combine them with orElse. */
public final class Targeting {
    private Targeting() { }

    /**
     * Something a shikigami may attack: a player or another player's shikigami that the owner is allowed to hit.
     * Ordinary animals and monsters are left alone.
     */
    public static boolean isEnemy(BattlePlayer owner, Entity entity) {
        if (!Targets.isTargetable(owner, entity)) {
            return false;
        }
        return entity instanceof Player || Targets.shikigamiOwnerOf(entity) != null;
    }

    private static List<LivingEntity> candidates(BrainShikigami self, double range) {
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : self.mob().getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity living && isEnemy(self.getOwner(), living)) {
                found.add(living);
            }
        }
        return found;
    }

    /** The closest enemy to the shikigami within its target range. */
    public static TargetingStrategy nearest() {
        return self -> {
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : candidates(self, self.spec().targetRange())) {
                double distance = candidate.getLocation().distanceSquared(self.mob().getLocation());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            return best;
        };
    }

    /** The enemy (in range of the shikigami) who is farthest from its owner. */
    public static TargetingStrategy farthestFromOwner() {
        return self -> {
            LivingEntity best = null;
            double bestDistance = -1;
            org.bukkit.Location ownerLocation = self.getOwner().getPlayer().getLocation();
            for (LivingEntity candidate : candidates(self, self.spec().targetRange())) {
                if (!candidate.getWorld().equals(ownerLocation.getWorld())) {
                    continue;
                }
                double distance = candidate.getLocation().distanceSquared(ownerLocation);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            return best;
        };
    }

    /** The enemy with the least health. */
    public static TargetingStrategy lowestHealth() {
        return self -> {
            LivingEntity best = null;
            double bestHealth = Double.MAX_VALUE;
            for (LivingEntity candidate : candidates(self, self.spec().targetRange())) {
                if (candidate.getHealth() < bestHealth) {
                    bestHealth = candidate.getHealth();
                    best = candidate;
                }
            }
            return best;
        };
    }

    /** Whoever the owner hit most recently (within maxAgeTicks), if still around and in reach. */
    public static TargetingStrategy ownersLastTarget(int maxAgeTicks) {
        return self -> checked(self, BattlePlugin.getInstance().getShikigamiService()
            .lastHitBy(self.getOwner().getPlayer().getUniqueId(), maxAgeTicks));
    }

    /** Whoever hurt the owner most recently (within maxAgeTicks). */
    public static TargetingStrategy ownersAttacker(int maxAgeTicks) {
        return self -> checked(self, BattlePlugin.getInstance().getShikigamiService()
            .lastAttackerOf(self.getOwner().getPlayer().getUniqueId(), maxAgeTicks));
    }

    /**
     * The owner's latest fight (within maxAgeTicks): whoever they last hit or whoever last hurt them, whichever
     * happened more recently. Null when the owner is not fighting anyone.
     */
    public static TargetingStrategy ownersFight(int maxAgeTicks) {
        return self -> checked(self, BattlePlugin.getInstance().getShikigamiService()
            .lastFoeOf(self.getOwner().getPlayer().getUniqueId(), maxAgeTicks));
    }

    /**
     * Only keep a remembered target if it can be hit and is within twice the target range. Unlike searching,
     * this accepts any mob (a cow the owner hit counts), because the owner chose it.
     */
    private static LivingEntity checked(BrainShikigami self, LivingEntity entity) {
        if (entity == null || !Targets.isTargetable(self.getOwner(), entity)
            || !entity.getWorld().equals(self.mob().getWorld())) {
            return null;
        }
        double range = self.spec().targetRange() * 2;
        return entity.getLocation().distanceSquared(self.mob().getLocation()) <= range * range ? entity : null;
    }
}
