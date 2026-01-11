package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class OrbThrow extends ProjectileAbility {
    public OrbThrowCallback orbThrowCallback = new OrbThrowCallback();

    public OrbThrow(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Orb Throw");
        this.setCoolDown(7);
        
        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.speed = 1;
        this.hitbox = 0.5;
        this.maxTicks = 300;
        this.callback = new OrbThrowCallback();
        this.effect = new OrbThrowParticleEffect();
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        VirtualProjectile virtualProjectile = new VirtualProjectile (
            speed, 
            maxTicks, 
            owner, 
            hitbox, 
            callback,
            effect
        );

        this.virtualProjectile = virtualProjectile;
   
        virtualProjectile.setParticleEffectPeriod(2);
        virtualProjectile.setStopOnHitPlayer(false);
        launch();

        OrbThrowRepeater repeater = new OrbThrowRepeater(maxTicks, virtualProjectile, owner.getPlayer().getEyeLocation());
        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        abilityLogger.schedule(repeater);
        cooldownLogger.startCooldown(owner, this);

        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
}
