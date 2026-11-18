package com.hfturay.battleplugin.ability.abilities.damonabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Multiplies the Lazer's damage for a while. Needs the player to have the Lazer.
 * Every number comes from abilities.yml (SwitchOnALazer section).
 */
@RequiresSettings({ "cooldown", "duration-ticks", "damage-multiplier" })
public class SwitchOnALazer extends Ability {

    public SwitchOnALazer(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Switch on a lazer");
    }

    @Override
    protected Activation onActivate() {
        // Looked up by type, not by slot position: it only works if the player actually has the Lazer
        Lazer lazer = owner.getAbility(Lazer.class);

        if (lazer == null) {
            this.owner.getPlayer().sendActionBar(Component.text("You need the Lazer to use this", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            return Activation.FAILED;
        }

        double multiplier = setting("damage-multiplier");
        int duration = settingInt("duration-ticks");

        lazer.setDamageScale(lazer.getDamageScale() * multiplier);

        playParticleEffect(duration);

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        abilityScheduler.schedule(new LazerBuffRepeater(duration, multiplier, lazer));

        return Activation.USED;
    }

    private void playParticleEffect(int duration) {
        BattlePlugin.getInstance().getParticleService().follow(owner, SwitchOnALazerParticles.SPARKS, owner.getPlayer(), duration);
    }
}
