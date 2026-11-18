package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;
import com.hfturay.battleplugin.combatservice.Knockback;

/**
 * Runs every tick for everything the wave is touching.
 *
 * The push is not a fixed speed. Each tick it looks at where the target is along the wave's line and chooses the
 * speed that brings it to a spot just ahead of the wave's middle: the wave's own speed, plus a correction that
 * grows with how far the target is from that spot. So a target that is falling behind is pushed faster than the
 * wave, and one that has got ahead is slowed so the wave catches up. It stays at the front for as long as it is
 * touching the wave, however fast the wave moves.
 *
 * Everything the wave touches takes damage and is silenced; the numbers all come from abilities.yml.
 */
public class MeltdownOfWavesCallback implements ProjectileHitCallback {

    /**
     * @param damage        damage dealt every tick a target is touching the wave
     * @param lift          upward push, just enough to keep targets off the ground
     * @param silenceTicks  how long a player is silenced after the last tick the wave touched them
     * @param frontOffset   how far ahead of the wave's middle a target is held, in blocks
     * @param backReach     a target further behind the middle than this has been passed and is let go (still hurt)
     * @param pullGain      how hard the push corrects a target's position (speed per block of error)
     * @param maxPushSpeed  the fastest a target is ever pushed, in blocks per tick
     */
    public record Settings(double damage, double lift, int silenceTicks, double frontOffset,
                           double backReach, double pullGain, double maxPushSpeed) { }

    private final Settings settings;

    public MeltdownOfWavesCallback(Settings settings) {
        this.settings = settings;
    }

    /**
     * The velocity to give a target this tick, or null if the wave has passed it and lets it go.
     *
     * @param waveDirection horizontal unit vector the wave travels along
     * @param waveSpeed     how fast the wave moves along that line, in blocks per tick
     * @param along         how far the target is ahead of the wave's middle along that line (negative = behind)
     */
    public static Vector pushVelocity(Vector waveDirection, double waveSpeed, double along, Settings s) {
        if (along < -s.backReach()) {
            return null;
        }
        double speed = waveSpeed + s.pullGain() * (s.frontOffset() - along);
        speed = Math.max(0, Math.min(s.maxPushSpeed(), speed));
        return new Vector(waveDirection.getX() * speed, s.lift(), waveDirection.getZ() * speed);
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Entity target = result.getHitEntity();
        if (target == null) {
            return;
        }

        VirtualProjectile projectile = result.getVirtualProjectile();
        Location wave = projectile.getLocation();

        // The wave can only push along the ground. Fired straight up or down it has no direction to push in.
        Vector direction = projectile.getDirection().setY(0);
        if (direction.lengthSquared() > 1.0E-9) {
            direction.normalize();

            Vector velocity = projectile.getVelocity();
            double waveSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());

            Location at = target.getLocation();
            double along = (at.getX() - wave.getX()) * direction.getX() + (at.getZ() - wave.getZ()) * direction.getZ();

            Vector push = pushVelocity(direction, waveSpeed, along, settings);
            if (push != null) {
                Knockback.set(target, push);
            }
        }

        if (target instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, target, settings.damage());
        }

        if (target instanceof Player player) {
            BattlePlugin.getInstance().getSilenceService().silence(player.getUniqueId(), settings.silenceTicks());
        }
    }
}
