package com.hfturay.battleplugin.stunservice;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.statservice.DamageLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ParticleEffect;

public class Stun {
    public int duration;
    public Entity target;
    public boolean grantImmunity = false;

    public boolean lockX = false;
    public boolean lockY = false;
    public boolean lockZ = false;

    public Vector forcedVelocity = null;

    public Stun(Entity target, int duration) {
        this.target = target;
        this.duration = duration;
    } 

    public boolean apply() {
        Vector velocity = target.getVelocity();

        if (duration <= 0) {
            return false;
        }

        duration -= 1;

        stunEffect();
        DamageLogger damageLogger = BattlePlugin.getInstance().getDamageLogger();
        if (grantImmunity && !damageLogger.isImmune(target)) {
            damageLogger.grantImmunity(target, duration);
        }

        if (forcedVelocity != null) {
            target.setVelocity(forcedVelocity);
            return false;
        }

        Vector newVelocity = velocity.clone();

        if (lockX) {
            newVelocity.setX(0);
        }

        if (lockY) {
            newVelocity.setY(0);
        }

        if (lockZ) {
            newVelocity.setZ(0);
        }

        target.setVelocity(newVelocity);
        return false;
    } 

    public void cancel() {
        this.duration = 0;

        DamageLogger damageLogger = BattlePlugin.getInstance().getDamageLogger();
        if (grantImmunity && damageLogger.isImmune(target)) {
            damageLogger.removeImmunity(target);
        }
    }

    private void stunEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        ParticleEffect effect = new ParticleEffect(effectManager);

        effect.setLocation(target.getLocation().clone().add(0, 1, 0));

        effect.particle = Particle.CRIT;
        effect.particleCount = 5;

        effect.period = 1;
        effect.iterations = 1;

        effect.start();
    }
}