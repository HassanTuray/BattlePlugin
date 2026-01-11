package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class RevolutionarySpirit extends Ability {

    public RevolutionarySpirit(BattlePlayer owner) {
        super(owner);
    
        this.setDisplayName("Revolutionary Spirit");
        this.setCoolDown(60);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
    }

    @Override
    public boolean activate() {
        if (owner.getPlayer().getHealth() > 16) {
            this.owner.getPlayer().sendActionBar(Component.text("Your resolve is not great enough", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            return false;
        }

        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        PotionEffect revStrength = new PotionEffect(PotionEffectType.STRENGTH, 300, 2);
        PotionEffect revRes = new PotionEffect(PotionEffectType.RESISTANCE, 300, 2);
        PotionEffect revRegen = new PotionEffect(PotionEffectType.REGENERATION, 300, 1);    

        Player player = owner.getPlayer();
        player.addPotionEffect(revStrength);
        player.addPotionEffect(revRes);
        player.addPotionEffect(revRegen);

        playParticleEffect();

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        SphereEffect revBurst = new SphereEffect(effectManager);
        
        revBurst.setEntity(owner.getPlayer());
        
        revBurst.particle = Particle.DUST;
        revBurst.color = Color.RED;
        revBurst.particles = 50;
        revBurst.particleIncrease = 20;

        revBurst.period = 1;
        revBurst.iterations = 16;
        
        revBurst.radius = 1;
        revBurst.radiusIncrease = 0.2;
        revBurst.start();

        revBurst.callback = () -> {
            SphereEffect revAura = new SphereEffect(effectManager);
            revAura.setEntity(owner.getPlayer());

            revAura.particle = Particle.DUST;
            revAura.color = Color.RED;
            revAura.particles = 50;
            
            revAura.period = 1;
            revAura.iterations = 300;
            
            revAura.radius = 4;
            revAura.start();

        };
    }
}
