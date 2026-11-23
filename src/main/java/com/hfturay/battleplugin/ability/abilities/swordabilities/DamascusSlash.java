package com.hfturay.battleplugin.ability.abilities.swordabilities;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;

import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * Damascus Slash: a fast, straight-flying vertical slash, drawn as an upright white crescent (SwordVisuals.DAMASCUS_SLASH).
 * Whoever it hits takes `damage` and is launched straight up `height` blocks, with no push in any other direction.
 * Every number comes from abilities.yml (DamascusSlash section), read each time it is used.
 */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "damage", "height" })
public class DamascusSlash extends ProjectileAbility {

    public DamascusSlash(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Damascus Slash");
        this.particles = SwordParticles.DAMASCUS_SLASH_TRAIL;
    }

    @Override
    protected Activation onActivate() {
        VirtualProjectile virtualProjectile = new VirtualProjectile(
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new DamascusSlashCallback(setting("damage"), setting("height")),
            particles
        );

        virtualProjectile.setVisual(SwordVisuals.DAMASCUS_SLASH);    // removed the moment the projectile ends

        this.virtualProjectile = virtualProjectile;
        launch();

        owner.getPlayer().getWorld().playSound(owner.getPlayer().getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            SoundCategory.PLAYERS, 1.2f, 1.3f);
        return Activation.USED;
    }
}
