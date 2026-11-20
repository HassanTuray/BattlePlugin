package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.stunservice.Stun;

/**
 * What the ball does when it lands. On a creature: a little damage, a small knockback along the ball's flight, then (once they have
 * slid a few ticks) their movement is held still for stunTicks, a white flash fills their screen, and the ball starts
 * rolling back to the caster. On a wall or the ground: just a puff.
 */
final class JogaBonitoCallback implements ProjectileHitCallback {

    /** Ticks the victim is left free to slide back before their movement is locked, so the knockback is seen. */
    private static final int SLIDE_TICKS = 4;

    private final JogaBonito ability;
    private final double damage;
    private final double knockback;
    private final double lift;
    private final int stunTicks;

    JogaBonitoCallback(JogaBonito ability, double damage, double knockback, double lift, int stunTicks) {
        this.ability = ability;
        this.damage = damage;
        this.knockback = knockback;
        this.lift = lift;
        this.stunTicks = stunTicks;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location at = result.getHitLocation();
        at.getWorld().spawnParticle(Particle.CLOUD, at, 12, 0.2, 0.2, 0.2, 0.05);

        Entity entity = result.getHitEntity();
        if (!(entity instanceof LivingEntity victim)) {
            at.getWorld().playSound(at, Sound.BLOCK_WOOL_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return;
        }

        BattlePlugin.getInstance().getDamageService().damage(owner, victim, damage);

        // a small shove the way the ball was travelling, with a little pop
        Vector shove = result.getVirtualProjectile().getVelocity().clone().setY(0);
        if (shove.lengthSquared() < 1.0E-9) {
            shove = victim.getLocation().toVector().subtract(at.toVector()).setY(0);
        }
        if (shove.lengthSquared() > 1.0E-9) {
            Knockback.set(victim, shove.normalize().multiply(knockback).setY(lift));
        }

        // then hold them still
        if (stunTicks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isValid()) {
                        return;
                    }
                    Stun hold = new Stun(victim, stunTicks);
                    hold.lockX = true;
                    hold.lockZ = true;
                    BattlePlugin.getInstance().getStunService().add(victim, hold);
                }
            }.runTaskLater(BattlePlugin.getInstance(), SLIDE_TICKS);
        }

        flash(victim);
        ability.onBallHit(at);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * The white flash: the same particle a firework star makes. Spawned at the victim's eyes it fills their whole screen
     * with white for a moment, no resource pack needed. Spawned twice a couple of ticks apart so it reads as one flash.
     */
    private static void flash(LivingEntity victim) {
        victim.getWorld().spawnParticle(Particle.FLASH, victim.getEyeLocation(), 2);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) {
                    victim.getWorld().spawnParticle(Particle.FLASH, victim.getEyeLocation(), 1);
                }
            }
        }.runTaskLater(BattlePlugin.getInstance(), 2L);
    }
}
