package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.Color;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.HitScanAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (Laser section). damage is dealt every tick the beam is on someone. */
@RequiresSettings({ "cooldown", "duration-ticks", "range", "damage" })
public class Laser extends HitScanAbility {

    public Laser(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Laser");
    }

    @Override
    protected Activation onActivate() {
        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        LaserRepeater laserDamage = new LaserRepeater(settingInt("duration-ticks"), this, owner, setting("damage"), setting("range"));
        abilityScheduler.schedule(laserDamage);

        return Activation.USED;
    }
    
    
}
