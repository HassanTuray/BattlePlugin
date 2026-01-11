package com.hfturay.battleplugin.ability.abilities.damonabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Homebrew extends ProjectileAbility {
    public Homebrew(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Homebrew");
        this.setCoolDown(13);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.speed = 0.7;
        this.maxTicks = 200;
        this.hitbox = 0.5;
        this.motionFunction = (v, t) -> v.setY(v.getY() - 0.03);
        this.effect = new HomebrewParticleEffect();
        this.callback = new HomebrewCallback();
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
        virtualProjectile.setMotionFunction(motionFunction);
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

