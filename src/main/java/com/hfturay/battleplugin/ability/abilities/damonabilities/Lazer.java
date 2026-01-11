package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.HitScanAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.HitScan;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.VortexEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Lazer extends HitScanAbility {
    private Location targetLocation = null;

    public Lazer(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Lazer");
        this.setCoolDown(8);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.range = 10;
        this.damage = 5;
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
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity target = HitScan.HitScanEntities(player, range, collisionMode);

        if (target == null) {
            return false;
        }

        if (target instanceof LivingEntity) {
            targetLocation = target.getLocation().clone().add(0, 0.5, 0);
            ((Damageable) target).damage(damage);
            playParticleEffect();
        }

        cooldownLogger.startCooldown(player, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        LineEffect lazerLine = new LineEffect(effectManager);
        
        Location eye = owner.getPlayer().getEyeLocation();
        Location start = eye.clone().add(eye.getDirection().multiply(0.3));

        lazerLine.setLocation(start);
        lazerLine.particle = Particle.DUST;
        lazerLine.color = Color.ORANGE;
        lazerLine.particleSize = 0.5f;
        lazerLine.particles = 100;
        lazerLine.period = 20;
        lazerLine.setTarget(targetLocation); 
        lazerLine.length = 10;
        lazerLine.maxLength = 10;

        lazerLine.start();

        VortexEffect lazerSpiral = new VortexEffect(effectManager);
        double distance = start.distance(targetLocation);

        lazerSpiral.particle = Particle.DUST;
        lazerSpiral.color = Color.PURPLE;

        lazerSpiral.radiusGrow = 0;
        lazerSpiral.grow = 0.25f;
        lazerSpiral.radials = Math.PI/2;
        lazerSpiral.radius = 0.35f;

        lazerSpiral.circles = 10;
        lazerSpiral.helixes = 5;

        lazerSpiral.period = 1;
        lazerSpiral.iterations = (int) Math.ceil(distance / (lazerSpiral.grow * lazerSpiral.circles));
        BattlePlugin.getInstance().getLogger().info(Integer.toString(lazerSpiral.iterations));
        lazerSpiral.particleCount = 50;
        lazerSpiral.particleSize = 0.75f;
        lazerSpiral.setLocation(start);
        
        lazerSpiral.start();
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }
}
