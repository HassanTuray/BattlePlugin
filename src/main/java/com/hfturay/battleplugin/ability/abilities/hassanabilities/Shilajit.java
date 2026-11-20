package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Shilajit: heals the caster at once and gives them Resistance for a while. As long as the resistance lasts, two rings of
 * coal blocks (charcoal stand-ins) orbit them. Every number comes from abilities.yml (Shilajit section).
 */
@RequiresSettings({ "cooldown", "heal", "duration-ticks", "resistance-amplifier", "radius", "count" })
public class Shilajit extends Ability {

    public Shilajit(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Shilajit");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected Activation onActivate() {
        Player player = owner.getPlayer();
        int duration = settingInt("duration-ticks");

        // the healing, up to full health
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + setting("heal")));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, settingInt("resistance-amplifier")));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 0.8f);

        BattlePlugin.getInstance().getAbilityScheduler().schedule(
            new ShilajitTask(this, owner, duration, setting("radius"), Math.max(1, settingInt("count"))));
        return Activation.USED;
    }
}
