package com.hfturay.battleplugin.ability.abilities.damonabilities;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.customeffects.ExpandingCircleEffect;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.EffectManager;

public class HomebrewCallback implements ProjectileHitCallback {
    private int duration = 60;
    private int amplifier = 2; 
    private int homebrewRadius = 5;

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location location = result.getHitLocation();
        List<Entity> targets = AOE.getTargets(owner, location, homebrewRadius);

        PotionEffect homebrewPoison = new PotionEffect(
            PotionEffectType.POISON,
            duration,
            amplifier
        );

        PotionEffect homebrewWither = new PotionEffect(
            PotionEffectType.WITHER,
            duration,
            amplifier
        );

        PotionEffect homebrewWeakness = new PotionEffect(
            PotionEffectType.WEAKNESS,
            duration,
            amplifier
        );

        PotionEffect homebrewSlow = new PotionEffect(
            PotionEffectType.SLOWNESS,
            duration,
            amplifier
        );

        PotionEffect homebrewNausea = new PotionEffect(
            PotionEffectType.NAUSEA,
            duration,
            amplifier
        );

        PotionEffect homebrewLevitating = new PotionEffect(
            PotionEffectType.LEVITATION,
            duration,
            amplifier
        );

        PotionEffect homebrewBlindness = new PotionEffect(
            PotionEffectType.BLINDNESS,
            duration,
            amplifier
        );

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntity)entity).addPotionEffect(homebrewNausea);
                ((LivingEntity)entity).addPotionEffect(homebrewPoison);
                ((LivingEntity)entity).addPotionEffect(homebrewWither);
                ((LivingEntity)entity).addPotionEffect(homebrewWeakness);
                ((LivingEntity)entity).addPotionEffect(homebrewSlow);
                ((LivingEntity)entity).addPotionEffect(homebrewLevitating);
                ((LivingEntity)entity).addPotionEffect(homebrewBlindness);
            }
        }

        brewImpactEffect(location);
    }

    private void brewImpactEffect(Location location) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        ExpandingCircleEffect brewImpact = new ExpandingCircleEffect(effectManager);
        
        // it is safe to do this because the location is a clone and pitch and yaw are not used in getting the targets of the ability
        // also the effect is called after the ability is activated so nothing else in the ability is using the location
        location.setPitch(0);
        location.setYaw(0);

        brewImpact.setLocation(location.add(0, 0.65, 0));
        brewImpact.particle = Particle.DUST;
        brewImpact.color = Color.GREEN;
        brewImpact.particleCount = 10;
        brewImpact.radius = 0.1f;
        brewImpact.iterations = 10;
        brewImpact.period = 1;
        brewImpact.enableRotation = false;
        brewImpact.growthPerTick = 0.35;
        brewImpact.maxRadius = 5;
        brewImpact.wholeCircle = true;
        brewImpact.linger = false;

        brewImpact.start();
    }
}
