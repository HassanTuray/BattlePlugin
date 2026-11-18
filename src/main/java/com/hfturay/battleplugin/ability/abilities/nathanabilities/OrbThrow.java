package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * An orb that flies through players. After `homing-distance` blocks it turns around and homes in on the caster
 * (`turn-strength`), hitting for `homing-damage-multiplier` times as much; it ends when it reaches them
 * (`catch-radius`). Every number comes from abilities.yml (OrbThrow section). The damage includes any buff from The Ascended.
 */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "damage", "homing-distance", "turn-strength",
    "homing-damage-multiplier", "catch-radius" })
public class OrbThrow extends ProjectileAbility {

    public OrbThrow(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Orb Throw");
    }

    @Override
    protected Activation onActivate() {
        this.speed = setting("speed");
        this.hitbox = setting("hitbox");
        this.maxTicks = settingInt("max-ticks");
        this.particles = OrbThrowParticles.SPEC;

        OrbThrowCallback orbCallback = new OrbThrowCallback(setting("damage") + getDamageBonus());
        this.callback = orbCallback;

        VirtualProjectile virtualProjectile = new VirtualProjectile(
            speed,
            maxTicks,
            owner,
            hitbox,
            callback,
            particles
        );

        this.virtualProjectile = virtualProjectile;

        virtualProjectile.setStopOnHitPlayer(false);
        launch();

        OrbThrowRepeater repeater = new OrbThrowRepeater(maxTicks, virtualProjectile, owner.getPlayer().getEyeLocation(), this,
            orbCallback, setting("homing-distance"), setting("turn-strength"),
            setting("homing-damage-multiplier"), setting("catch-radius"));
        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        abilityScheduler.schedule(repeater);
        return Activation.USED;
    }
}
