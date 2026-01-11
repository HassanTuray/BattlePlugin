package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.falldamageservice.FallDamageLogger;

import com.hfturay.battleplugin.stunservice.StunLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.FlameEffect;

public class GrandSlamDownRepeater extends ScheduledAbilityTask {
    private Entity target;
    private double damage;

    public GrandSlamDownRepeater(int duration, Entity target, Double damage) {
        super(duration);
        this.target = target;
        this.damage = damage;
    }

    @Override
    public boolean update() {
        StunLogger stunLogger = BattlePlugin.getInstance().getStunLogger();
        if (!stunLogger.isStunned(target)) {
            BattlePlugin.getInstance().getLogger().info("Target is no longer stunned, canceling");
            this.cancel();
            return super.update();
        }

        if (!(target instanceof Damageable)) {
            BattlePlugin.getInstance().getLogger().info("Target is not a damageable, canceling");
            this.cancel();
            return super.update();
        }

        FallDamageLogger fallDamageLogger = BattlePlugin.getInstance().getFallDamageLogger();

        if (fallDamageLogger.isGrounded(target) || 
            target.isInWater() || 
            target.isInLava() || 
            target.isInPowderedSnow()) {
            
            ((Damageable) target).damage(damage);
            stunLogger.removeStun(target);
            slamEffect(target);
            
            BattlePlugin.getInstance().getLogger().info("Target is grounded, canceling");
            this.cancel();
            return true;
        }

        return super.update();
    }

    @Override
    public void run() {
        BattlePlugin.getInstance().getLogger().info("Target did not hit ground, unstunning");
        StunLogger stunLogger = BattlePlugin.getInstance().getStunLogger();

        if (stunLogger.isStunned(target)) {
            stunLogger.removeStun(target);
        }
    }

    private void slamEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        FlameEffect effect = new FlameEffect(effectManager);

        effect.setLocation(target.getLocation());
        
        effect.particle = Particle.DUST;
        effect.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE));
        effect.particleSize = 1.5f;
        effect.particles = 60;

        effect.period = 1;
        effect.iterations = 2;
        
        effect.start();
    }
}
