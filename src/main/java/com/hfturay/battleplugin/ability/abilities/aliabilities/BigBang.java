package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.AOEAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BigBang extends AOEAbility {

    public BigBang(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Personal Space");
        this.setCoolDown(7);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.radius = 0;
        this.vertForce = 0.6;
        this.horizForce = 1.2;
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

        playParticleEffect();
        Location center = owner.getPlayer().getLocation();
        
        List<Entity> targets = AOE.getTargets(owner, center, radius);
        Vector knockback;

        for (Entity target : targets) {
            if (target instanceof Damageable) {
                ((Damageable) target).damage(damage);
            }

            knockback = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(horizForce);
            knockback.setY(vertForce);
            
            target.setVelocity(knockback);
        }

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        SphereEffect  burst = new SphereEffect(effectManager);
        
         burst.setEntity(owner.getPlayer());
        
         burst.particle = Particle.DUST;
         burst.color = Color.WHITE;
         burst.particles = 100;
         burst.particleIncrease = 40;
         burst.particleSize = 1.5f;

         burst.period = 1;
         burst.iterations = 10;
        
         burst.radius = 1;
         burst.radiusIncrease = 0.4;
         burst.start();
    }
}
