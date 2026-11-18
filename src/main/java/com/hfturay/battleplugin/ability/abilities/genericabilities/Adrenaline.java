package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/** Speed for a long while and a short burst of regeneration. Every number comes from abilities.yml (Adrenaline section). */
@RequiresSettings({ "cooldown", "duration-ticks", "amplifier", "regeneration-ticks", "regeneration-amplifier" })
public class Adrenaline extends Ability {

    public Adrenaline(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Adrenaline");
    }

    @Override
    protected Activation onActivate() {
        PotionEffect potionEffect = new PotionEffect(
            PotionEffectType.SPEED,
            settingInt("duration-ticks"),
            settingInt("amplifier")
        );

        PotionEffect healingEffect = new PotionEffect(
            PotionEffectType.REGENERATION,
            settingInt("regeneration-ticks"),
            settingInt("regeneration-amplifier")
        );

        owner.getPlayer().addPotionEffect(potionEffect);
        owner.getPlayer().addPotionEffect(healingEffect);

        return Activation.USED;
    }
}
