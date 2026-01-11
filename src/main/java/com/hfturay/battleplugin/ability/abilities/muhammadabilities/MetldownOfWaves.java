package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MetldownOfWaves extends ProjectileAbility {
    public MetldownOfWaves(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Meltdown of Waves");
        this.setCoolDown(8);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.speed = 0.6;
        this.hitbox = 3;
        this.maxTicks = 100;
        this.effect = new MeltdownOfWavesParticleEffect();
        this.callback = new MeltdownOfWavesCallback();
        this.virtualProjectile = new VirtualProjectile ( 
            speed, 
            maxTicks, 
            owner, 
            hitbox, 
            callback,
            effect
        );
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        virtualProjectile.setParticleEffectPeriod(2);
        virtualProjectile.setStopOnHitPlayer(false);
        virtualProjectile.setStopOnHitBlock(false);
        virtualProjectile.setHitBlocks(false);

        launch();
        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
