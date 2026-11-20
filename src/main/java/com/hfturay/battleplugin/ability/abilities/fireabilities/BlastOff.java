package com.hfturay.battleplugin.ability.abilities.fireabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The caster blasts off in the elytra flying pose and is pushed forward every tick along wherever they look, so the
 * mouse steers. When they land a small explosion hurts and shoves back everyone near. Every number comes from abilities.yml (BlastOff
 * section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "duration-ticks", "speed", "launch-lift", "damage", "radius", "knockback", "lift" })
public class BlastOff extends Ability {

    public BlastOff(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Blast Off");
    }

    @Override
    protected Activation onActivate() {
        BlastOffTask.Settings settings = new BlastOffTask.Settings(
            settingInt("duration-ticks"),
            setting("speed"),
            setting("launch-lift"),
            setting("damage"),
            setting("radius"),
            setting("knockback"),
            setting("lift"));

        BattlePlugin.getInstance().getAbilityScheduler().schedule(new BlastOffTask(owner, this, settings));
        return Activation.USED;
    }
}
