package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.MeleeAbility;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.stunservice.StunLogger;
import com.hfturay.battleplugin.targetingservice.HitScan;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.EquationEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class GrandSlam extends MeleeAbility {
    private ScheduledAbilityTask cooldownRepeater = null;
    public boolean slamDown = false;
    private int deactivationTicks = 40;
    public double damage = 4;
    private double slam = -1.5;

    public GrandSlam(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Grand Slam");
        this.setCoolDown(7);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.reach = 5;
        this.vertForce = 0.75;
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        if (cooldownRepeater == null) {
            this.cooldownRepeater = new GrandSlamRepeater(deactivationTicks, this);
        }

        if (slamDown) {
            slam();
        } else {
            upwardSlash();
        }

        return true;
    }

    private void upwardSlash() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, reach, collisionMode);

        if (entity == null) {
            return;
        }

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        abilityLogger.schedule(cooldownRepeater);
        
        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }
        
        upwardSlashEffect(entity);

        GrandSlamKnockbackRepeater targetLift = new GrandSlamKnockbackRepeater(1, entity, vertForce);
        GrandSlamKnockbackRepeater castorLift = new GrandSlamKnockbackRepeater(1, owner.getPlayer(), vertForce);

        abilityLogger.schedule(targetLift);
        abilityLogger.schedule(castorLift);

        slamDown = true;
    }

    private void slam() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, reach, collisionMode);

        if (entity == null) {
            slamDown = false;
            return;
        }

        if (entity instanceof Damageable) {
            ((Damageable) entity).damage(damage);
        }

        StunLogger stunLogger = BattlePlugin.getInstance().getStunLogger();
        Stun lockHoriz = new Stun(entity, 100);
        lockHoriz.lockX = true;
        lockHoriz.lockZ = true;

        stunLogger.add(entity, lockHoriz);

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        abilityLogger.schedule(cooldownRepeater);

        GrandSlamKnockbackRepeater targetSlamDown = new GrandSlamKnockbackRepeater(1, entity, slam);
        GrandSlamKnockbackRepeater castorSlamDown = new GrandSlamKnockbackRepeater(1, owner.getPlayer(), slam);
        GrandSlamDownRepeater targetFallTracker = new GrandSlamDownRepeater(100, entity, damage);

        abilityLogger.schedule(targetSlamDown);
        abilityLogger.schedule(castorSlamDown);
        abilityLogger.schedule(targetFallTracker);

        this.cooldownRepeater.cancel();

        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();
        cooldownLogger.startCooldown(owner, this);

        slamDown = false;
    }

    private void upwardSlashEffect(Entity target) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        EquationEffect effect = new EquationEffect(effectManager);

        effect.particle = Particle.DUST;
        effect.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE));
        effect.particleSize = 1f;

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

    public void reset() {
        this.cooldownRepeater = null;
        this.slamDown = false;
    }

    @Override
    public void playParticleEffect() {
    }
}
