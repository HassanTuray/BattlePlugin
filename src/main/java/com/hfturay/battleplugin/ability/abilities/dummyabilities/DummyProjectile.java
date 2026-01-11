package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.projectileservice.ProjectileLogger;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DummyProjectile extends Ability{
    
    public DummyProjectile(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy Projectile");
        this.setCoolDown(1);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        BattlePlayer player = this.getOwner();
        Location start = player.getPlayer().getEyeLocation();
        Vector direction = start.getDirection();

        ProjectileLogger pm = BattlePlugin.getInstance().getProjectileLogger();
        DummyProjectileParticleEffectCallback effect = new DummyProjectileParticleEffectCallback();

        VirtualProjectile projectile = new VirtualProjectile(
            start,                    
            direction,                  
            0.7,                        
            200,                         
            player,                   
            0.5,
            new DummyProjectileCallback(),
            effect
        );
        
        cooldownLogger.startCooldown(owner, this);
        pm.add(projectile);
        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
}
