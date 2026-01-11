package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.MotionFunction;
import com.hfturay.battleplugin.projectileservice.ProjectileLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Brick extends ProjectileAbility {

    public Brick(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Brick");
        this.setCoolDown(1);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.speed = 2;
        this.hitbox = 1;
        this.maxTicks = 100;
        this.motionFunction = (v, t) -> v.setY(v.getY() - 0.01);
        this.effect = new BrickParticleEffect();
        this.callback = new BrickCallback();
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
