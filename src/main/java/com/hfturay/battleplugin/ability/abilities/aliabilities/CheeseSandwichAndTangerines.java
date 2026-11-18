package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import de.slikey.effectlib.effect.ShieldEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.particleservice.ParticleService;
import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (CheeseSandwichAndTangerines section). */
@RequiresSettings({ "cooldown", "duration-ticks", "amplifier" })
public class CheeseSandwichAndTangerines extends Ability {

    public CheeseSandwichAndTangerines(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Cheese Sandwhich and Tangerines");
    }

    @Override
    protected Activation onActivate() {
        int duration = settingInt("duration-ticks");
        int amp = settingInt("amplifier");

        PotionEffect invis = new PotionEffect(PotionEffectType.INVISIBILITY, duration, amp);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, duration, amp);
        PotionEffect absorb = new PotionEffect(PotionEffectType.ABSORPTION, duration, amp);
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, duration, amp);

        Player player = owner.getPlayer();
        player.addPotionEffects(Arrays.asList(invis, regen, absorb, speed));

        BattlePlugin.getInstance().getParticleService().follow(owner, AliParticles.SHIELD, player, ParticleService.UNTIL_DONE);

        return Activation.USED;
    }

    @Override
    public void playParticleEffect() {

    }
}
