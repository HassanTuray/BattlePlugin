package com.hfturay.battleplugin.ability.abilities.swordabilities;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

/** The Damascus Slash landing: damage, then a launch straight up (no sideways push at all). Hitting a block does nothing. */
public class DamascusSlashCallback implements ProjectileHitCallback {
    // No numbers here: DamascusSlash reads them from abilities.yml each time it is used.
    private final double damage;
    private final double height;

    public DamascusSlashCallback(double damage, double height) {
        this.damage = damage;
        this.height = height;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        if (!result.hasHitEntity()) {
            return;
        }
        Entity victim = result.getHitEntity();
        if (!(victim instanceof Damageable)) {
            return;
        }

        BattlePlugin.getInstance().getDamageService().damage(owner, victim, damage);
        // After the damage, so the hit's own knockback is replaced by the launch.
        Knockback.straightUp(victim, height);

        BattlePlugin.getInstance().getParticleService().burst(owner, SwordParticles.DAMASCUS_SLASH_HIT, victim.getLocation());
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5f, 0.8f);
    }
}
