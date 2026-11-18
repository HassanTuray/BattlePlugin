package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.particleservice.ParticleService;
import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (RevolutionarySpirit section). Usable only at or below health-threshold. */
@RequiresSettings({ "cooldown", "health-threshold", "duration-ticks", "strength-amplifier", "resistance-amplifier", "regeneration-amplifier" })
public class RevolutionarySpirit extends Ability {

    public RevolutionarySpirit(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Revolutionary Spirit");
    }

    @Override
    protected String blockedReason() {
        return owner.getPlayer().getHealth() > setting("health-threshold") ? "Your resolve is not great enough" : null;
    }

    @Override
    protected Activation onActivate() {
        int duration = settingInt("duration-ticks");
        PotionEffect revStrength = new PotionEffect(PotionEffectType.STRENGTH, duration, settingInt("strength-amplifier"));
        PotionEffect revRes = new PotionEffect(PotionEffectType.RESISTANCE, duration, settingInt("resistance-amplifier"));
        PotionEffect revRegen = new PotionEffect(PotionEffectType.REGENERATION, duration, settingInt("regeneration-amplifier"));

        Player player = owner.getPlayer();
        player.addPotionEffect(revStrength);
        player.addPotionEffect(revRes);
        player.addPotionEffect(revRegen);

        playParticleEffect();

        return Activation.USED;
    }

    @Override
    public void playParticleEffect() {
        ParticleService particles = BattlePlugin.getInstance().getParticleService();
        Player player = owner.getPlayer();

        // burst first; when it finishes by itself, settle into the aura for the length of the buff
        particles.follow(owner, MuhammadParticles.REVOLUTIONARY_BURST, player, ParticleService.UNTIL_DONE)
            .whenDone(() -> particles.follow(owner, MuhammadParticles.REVOLUTIONARY_AURA, player, settingInt("duration-ticks")));
    }
}
