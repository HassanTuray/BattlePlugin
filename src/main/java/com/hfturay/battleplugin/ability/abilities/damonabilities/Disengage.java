package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Particle;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.falldamageservice.FallDamageLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CloudEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Disengage extends Ability {
    private double horizontalForce = 3;
    private double verticalBoost = 0.5;
    private double upwardCurveScaling = 0.6;

    public Disengage(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Disengage");
        this.setCoolDown(7);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
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

        Vector direction = owner.getPlayer().getEyeLocation().getDirection().multiply(-1).normalize();
        Vector launch = direction.multiply(horizontalForce);

        double pitch = owner.getPlayer().getEyeLocation().getPitch();
        double pitchFactor = (90 - Math.abs(pitch)) / 90.0;

        double additionalLift = verticalBoost + (pitchFactor * upwardCurveScaling);
        launch.setY(additionalLift);

        owner.getPlayer().setVelocity(launch);
        owner.getPlayer().setFallDistance(0);

        FallDamageLogger fallDamageLogger = BattlePlugin.getInstance().getFallDamageLogger();
        fallDamageLogger.grantFallImmunity(owner.getPlayer());

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        CloudEffect effect = new CloudEffect(effectManager);

        effect.setLocation(owner.getPlayer().getLocation().clone());
        effect.yOffset = -0.25;
        effect.particleCount = 20;  
        effect.cloudParticle = Particle.SMOKE;
        effect.mainParticle = Particle.CLOUD;
        effect.cloudSize = 0.75f;
        effect.particleRadius = 0.3f;
        effect.speed = 0.1f; 
        effect.iterations = 1; 

        effect.start();
    }
}

