package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyProjectile extends ProjectileAbility {
    
    public DummyProjectile(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy Projectile");
        this.setCoolDown(1);

        
        this.speed = 0.7;
        this.maxTicks = 200;
        this.hitbox = 0.5;
        this.callback = new DummyProjectileCallback();
        this.particles = null; // no visuals
    }

    @Override
    protected Activation onActivate() {
        VirtualProjectile virtualProjectile = new VirtualProjectile (                                    
            speed,                        
            maxTicks,                         
            owner,                   
            hitbox,
            callback,
            particles
        );

        this.virtualProjectile = virtualProjectile;
        launch();
        return Activation.USED;
    }

}
