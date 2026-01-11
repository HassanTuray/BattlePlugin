package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Color;
import org.bukkit.Fluid;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.HitScan;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;

public class LaserRepeater extends ScheduledAbilityTask {
    private BattlePlayer owner;
    private double damage;
    private double range;

    public LaserRepeater(int duration, BattlePlayer owner, double damage, double range) {
        super(duration);
        
        this.owner = owner;
        this.damage = damage;
        this.range = range;
    }

    @Override
    public boolean update() {
        FluidCollisionMode neverCollide = FluidCollisionMode.NEVER;
        Entity target = HitScan.HitScanEntities(owner, range, neverCollide);
        playParticleEffect(target);

        if (target == null) {
            return super.update();
        }

        if (target instanceof Damageable) {
            ((Damageable)target).damage(damage);
        }

        return super.update();
    }

    public void playParticleEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        LineEffect effect = new LineEffect(effectManager); 

        Player player = owner.getPlayer();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize(); 

        RayTraceResult result = player.getWorld().rayTrace(
            eye, 
            direction, 
            range, 
            FluidCollisionMode.NEVER, 
            false, 
            0.4, 
            entity -> !entity.equals(player)
        );

        

        if (result == null) {
            effect.length = range;
            effect.maxLength = range;
        } else {
            Vector hitPosition = result.getHitPosition();

            if (hitPosition != null) {
                effect.setTarget(hitPosition.toLocation(player.getWorld()));
            } 
        }

        

        effect.setLocation(player.getEyeLocation());

        effect.particle = Particle.DUST;
        effect.color = Color.AQUA;
        effect.particleCount = 20;
        effect.particleSize = 0.25f;

        effect.period = 1;
        effect.iterations = 1;
        effect.start();
    }

    @Override
    public void run() {
        
    }
}
