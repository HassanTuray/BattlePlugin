package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.targetingservice.AOE;

public class AnthraxPackageRepeater extends ScheduledAbilityTask {
    private final Location location;
    private final BattlePlayer owner;
    private final AnthraxPackage.Cloud cloud;
    private final ParticleHandle zone;

    // No numbers here: they come from abilities.yml through the Cloud the ability built when it was thrown.
    public AnthraxPackageRepeater(AnthraxPackage.Cloud cloud, BattlePlayer owner, Location location, Ability ability) {
        super(cloud.durationTicks(), ability);

        this.cloud = cloud;
        this.location = location;
        this.owner = owner;

        // One effect for the whole cloud, instead of new ones every tick.
        this.zone = BattlePlugin.getInstance().getParticleService()
            .zone(owner, MuhammadParticles.ANTHRAX_ZONE, location, cloud.durationTicks())
            .survivesOwnerDeath();      // the cloud keeps going after its owner dies, so it stays visible
    }

    // A placed cloud keeps going even if its owner dies.
    @Override
    public boolean cancelOnOwnerDeath() {
        return false;
    }

    @Override
    protected void onCancel() {
        zone.cancel();
    }

    @Override
    protected void onTick() {
        // Once every interval-ticks. (This used to be inverted, so it ran on every tick EXCEPT those.)
        if (super.getTicksRemaining() % cloud.intervalTicks() == 0) {
            List<Entity> targets = AOE.getTargets(owner, location, cloud.radius(), cloud.xBound(), cloud.yBound(), cloud.zBound());
            PotionEffect anthraxSlow = new PotionEffect(PotionEffectType.SLOWNESS, cloud.effectTicks(), cloud.slowAmplifier());
            PotionEffect anthraxPoison = new PotionEffect(PotionEffectType.POISON, cloud.effectTicks(), cloud.poisonAmplifier());
            PotionEffect anthraxWither = new PotionEffect(PotionEffectType.WITHER, cloud.effectTicks(), cloud.witherAmplifier());

            for (Entity entity : targets) {
                if (entity instanceof Damageable) {
                    BattlePlugin.getInstance().getDamageService().damage(owner, entity, cloud.damage()); // need to look into making custom damage events
                }

                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).addPotionEffect(anthraxSlow);
                    ((LivingEntity) entity).addPotionEffect(anthraxPoison);
                    ((LivingEntity) entity).addPotionEffect(anthraxWither);
                }
            }
        }
    }
}
