package com.hfturay.battleplugin.ability.abilities.damonabilities;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;

public class LandMineRepeater extends ScheduledAbilityTask{
    private Location location;  
    private double radius = 5;
    private BattlePlayer owner;

    public LandMineRepeater(BattlePlayer owner, Location location, int duration) {
        super(duration);

        this.owner = owner;
        this.location = location;
    }

    @Override
    public boolean update() {
        List<Entity> targets = AOE.getTargets(owner, location, radius);
        Vector knockback = null;

        playParticleEffect();

        if (targets.isEmpty()) {
            return super.update();
        }

        BattlePlugin.getInstance().getWorld().spawnParticle(Particle.EXPLOSION, location.clone(), 1);
        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                ((Damageable) entity).damage(4);

                knockback = entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.2);
                entity.setVelocity(knockback);
            }
        }

        this.cancel();
        return super.update();
    }

    @Override
    public void run() {

    }

    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        
        CylinderEffect landMine = new CylinderEffect(effectManager);

        landMine.particle = Particle.DUST;
        landMine.color = Color.GRAY;
        landMine.solid = true;
        
        landMine.particles = 20;
        landMine.setLocation(location.clone().add(0, 1, 0));

        landMine.period = 1;
        landMine.iterations = 1;

        landMine.height = 0.15f;
        landMine.radius = 0.5f;
        landMine.enableRotation = false;

        landMine.start();
        
        CylinderEffect button = new CylinderEffect(effectManager);

        button.particle = Particle.DUST;
        button.color = Color.RED;
        button.solid = true;
        
        button.particles = 5;
        button.particleSize = 0.5f;
        button.setLocation(location.clone().add(0, 1.15, 0));

        button.period = 1;
        button.iterations = 1;

        button.height = 0.15f;
        button.radius = 0.2f;
        button.enableRotation = false;

        button.start();
    }

    // public void landMineExplodeParticleEffect() {
    //     EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();

    //     ExplodeEffect explosion = new ExplodeEffect(effectManager);

    //     explosion.setLocation(location);
    //     explosion.amount = 10;
    //     explosion.asynchronous = false;

    //     explosion.start();

    // }
    
}
