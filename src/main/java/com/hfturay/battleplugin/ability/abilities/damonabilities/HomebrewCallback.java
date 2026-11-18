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
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.EffectManager;

public class HomebrewCallback implements ProjectileHitCallback {
    private final int duration;
    private final int amplifier;
    private final double homebrewRadius;

    /** All three come from abilities.yml (Homebrew section), read when the potion is thrown. */
    public HomebrewCallback(int duration, int amplifier, double radius) {
        this.duration = duration;
        this.amplifier = amplifier;
        this.homebrewRadius = radius;
    }

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

        brewImpactEffect(owner, location);
    }

    private void brewImpactEffect(BattlePlayer owner, Location location) {
        BattlePlugin.getInstance().getParticleService().burst(owner, HomebrewParticles.IMPACT, location);
    }
}
