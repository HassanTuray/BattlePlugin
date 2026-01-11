package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.Arrays;

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
import de.slikey.effectlib.effect.ShieldEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CheeseSandwichAndTangerines extends Ability {
    private int duration = 80;
    private int amp = 2;

    public CheeseSandwichAndTangerines(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Cheese Sandwhich and Tangerines");
        this.setCoolDown(25);

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

        PotionEffect invis = new PotionEffect(PotionEffectType.INVISIBILITY, duration, amp);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, duration, amp);
        PotionEffect absorb = new PotionEffect(PotionEffectType.ABSORPTION, duration, amp);
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, duration, amp);

        Player player = owner.getPlayer();
        player.addPotionEffects(Arrays.asList(invis, regen, absorb, speed));

        playParticleEffect();

        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        ShieldEffect effect = new ShieldEffect(effectManager);

        effect.setLocation(owner.getPlayer().getLocation());
        effect.particle = Particle.DUST;
        effect.color = Color.BLACK;
        effect.particles = 150;
        effect.particleSize = 3.5f;

        effect.radius = 2.1;

        effect.period = 1;
        effect.iterations = 5;

        effect.sphere = true;

        effect.start();
    }
    
}
