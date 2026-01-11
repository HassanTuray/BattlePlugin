package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.MeleeAbility;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.HitScan;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.EquationEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ThreeHits extends MeleeAbility {
    private ScheduledAbilityTask repeater = null;
    private int attack = 0;
    private int deactivationTicks = 140;
    private double slam = -1.5;

    public ThreeHits(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Three-Hits");
        this.setCoolDown(6);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
        
        this.reach = 5;
        this.damage = 2.5;
        this.vertForce = 0.35;
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        if (repeater == null) {
            this.repeater = new ThreeHitsRepeater(deactivationTicks, this);
            AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
            abilityLogger.schedule(repeater);
        }

        if (attack == 0) {
            firstSwing();
        } else if (attack == 1) {
            secondSwing();
        } else {
            thirdSwing();
        }
        
        return true;
    }

    public void firstSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, reach, collisionMode);

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }

        firstSwingEffect(entity);
        repeater.setTicksRemaining(deactivationTicks);
        attack += 1;
    }

    public void secondSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, reach, collisionMode);

        if (entity == null) {
            return;
        }


        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        ThreeHitsKnockbackRepeater knockbackRepeater = new ThreeHitsKnockbackRepeater(1, entity, vertForce);
        abilityLogger.schedule(knockbackRepeater);

        secondSwingEffect(entity);
        repeater.setTicksRemaining(deactivationTicks);
        attack += 1;
    }

    public void thirdSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, reach, collisionMode);

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        ThreeHitsKnockbackRepeater knockbackRepeater = new ThreeHitsKnockbackRepeater(1, entity, slam);
        abilityLogger.schedule(knockbackRepeater);

        thirdSwingEffect(entity);
        repeater.setTicksRemaining(deactivationTicks);
        attack = 0;
    }

    public void resetRepeater() {
        this.repeater = null;
    }

    public void resetAttack() {
        this.attack = 0;
    }

    @Override
    public void playParticleEffect() {
        
    }

    private void firstSwingEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        EquationEffect effect = new EquationEffect(effectManager);

        effect.particle = Particle.DUST;
        effect.color = Color.PURPLE;
        effect.particleSize = 0.75f;

        effect.xEquation = "2 * sin(t * 3.14 / 180)";
        effect.yEquation = "0";
        effect.zEquation = "2 * cos(t * 3.14 / 180)";

        effect.particles = 36;
        effect.iterations = 5;
        effect.period = 1;

        effect.orient = true;
        effect.orientPitch = true; 

        Location ownerLocation = owner.getPlayer().getLocation();
        Location targetLocation = target.getLocation();

        Location midpoint = ownerLocation.clone().add(targetLocation).multiply(0.5).add(0, 1.0, 0); 

        effect.setLocation(midpoint);
        effect.maxSteps = 360;

        effect.start();
    }

    private void secondSwingEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();

        EquationEffect effect = new EquationEffect(effectManager);

        effect.particle = Particle.DUST;
        effect.color = Color.RED;
        effect.particleSize = 0.75f;

        effect.xEquation = "2 * sin(t * 3.14 / 180)";
        effect.yEquation = "-2 * cos(t * 3.14 / 180)";
        effect.zEquation = "0";

        effect.particles = 36;
        effect.iterations = 5;
        effect.period = 1;

        effect.orient = true;
        effect.orientPitch = true; 

        Location ownerLocation = owner.getPlayer().getLocation();
        Location targetLocation = target.getLocation();
        Location midpoint = ownerLocation.clone().add(targetLocation).multiply(0.5).add(0, 1.0, 0); 

        effect.setLocation(midpoint);
        effect.maxSteps = 360;

        effect.start();
    }

    private void thirdSwingEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();

        EquationEffect effect = new EquationEffect(effectManager);

        effect.particle = Particle.DUST;
        effect.color = Color.BLACK;
        effect.particleSize = 0.75f;

        effect.xEquation = "2 * sin(t * 3.14 / 180)";
        effect.yEquation = "2 * cos(t * 3.14 / 180)";
        effect.zEquation = "0";  

        effect.particles = 36;
        effect.iterations = 5;
        effect.period = 1;

        effect.orient = true;
        effect.orientPitch = true; 

        Location ownerLocation = owner.getPlayer().getLocation();
        Location targetLocation = target.getLocation();
        Location midpoint = ownerLocation.clone().add(targetLocation).multiply(0.5).add(0, 1.0, 0); 

        effect.setLocation(midpoint);
        effect.maxSteps = 360;

        effect.start();
    }
}
