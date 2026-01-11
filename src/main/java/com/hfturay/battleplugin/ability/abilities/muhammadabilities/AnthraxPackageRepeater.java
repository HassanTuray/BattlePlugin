package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;

public class AnthraxPackageRepeater extends ScheduledAbilityTask {
    private Location location;
    private BattlePlayer owner;

    private double xBound = 4;
    private double yBound = 1;
    private double zBound = 4;
    private double radius = 4;
    private double damage = 2;

    private int anthraxDuration = 100;
    private int anthraxSlowAmp = 2;
    private int anthraxPoisonAmp = 1;
    private int anthraxWitherAmp = 1;

    private int interval = 1;

    public AnthraxPackageRepeater(int duration, BattlePlayer owner, Location location, int interval) {
        super(duration);
        
        this.location = location;
        this.owner = owner;
        this.interval = interval;
    }

    @Override
    public boolean update() {
        anthraxImpactEffect(location);

        if(super.getTicksRemaining() % interval != 0) {
            List<Entity> targets = AOE.getTargets(owner, location, radius, xBound, yBound, zBound);
            PotionEffect anthraxSlow = new PotionEffect(PotionEffectType.SLOWNESS, anthraxDuration, anthraxSlowAmp);
            PotionEffect anthraxPoison = new PotionEffect(PotionEffectType.POISON, anthraxDuration, anthraxPoisonAmp);
            PotionEffect anthraxWither = new PotionEffect(PotionEffectType.WITHER, anthraxDuration, anthraxWitherAmp);

            for (Entity entity : targets) {
                if (entity instanceof Damageable) {
                    ((Damageable) entity).damage(damage); // need to look into making custom damage events
                }
                
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).addPotionEffect(anthraxSlow);
                    ((LivingEntity) entity).addPotionEffect(anthraxPoison);
                    ((LivingEntity) entity).addPotionEffect(anthraxWither);
                }
            }
        }
        return super.update(); // check if this will ever terminate
    }

    @Override
    public void run() {
        
    }

    private void anthraxImpactEffect(Location location) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();

        CylinderEffect dragonsBreathBlack = new CylinderEffect(effectManager);
        CylinderEffect dragonsBreathGreen = new CylinderEffect(effectManager);

        // dragon breath black
        dragonsBreathBlack.setLocation(location.clone().add(0, 0.65, 0));
        dragonsBreathBlack.particle = Particle.DUST;
        dragonsBreathBlack.color = Color.BLACK;
        dragonsBreathBlack.particles = 25;
        dragonsBreathBlack.particleSize = 1.5f;

        dragonsBreathBlack.enableRotation = false;
        dragonsBreathBlack.height = 0.05f;
        dragonsBreathBlack.radius = 4;

        dragonsBreathBlack.period = 1;
        dragonsBreathBlack.iterations = 1;
        
        // dragon breath green
        dragonsBreathGreen.setLocation(location.clone().add(0, 0.65, 0));
        dragonsBreathGreen.particle = Particle.DUST;
        dragonsBreathGreen.color = Color.LIME;
        dragonsBreathGreen.particles = 25;
        dragonsBreathGreen.particleSize = 1.5f;

        dragonsBreathGreen.enableRotation = false;
        dragonsBreathGreen.height = 0.05f;
        dragonsBreathGreen.radius = 4;

        dragonsBreathGreen.period = 1;
        dragonsBreathGreen.iterations = 1;

        dragonsBreathBlack.start();
        dragonsBreathGreen.start();
    }
    
}
