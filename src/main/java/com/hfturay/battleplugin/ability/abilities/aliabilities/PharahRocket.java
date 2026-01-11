package com.hfturay.battleplugin.ability.abilities.aliabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PharahRocket extends ProjectileAbility {
    public PharahRocket(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Pharah Rocket");
        this.setCoolDown(9);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.speed = 1.4;
        this.maxTicks = 100;
        this.hitbox = 1.2;
        this.callback = new PharahRocketCallback();
        this.effect = new PharahRocketParticleEffect();
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
