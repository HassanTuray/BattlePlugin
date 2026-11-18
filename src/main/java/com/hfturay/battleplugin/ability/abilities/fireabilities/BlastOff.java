package com.hfturay.battleplugin.ability.abilities.fireabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The caster blasts off in the elytra flying pose and is pushed forward every tick along wherever they look, so the
 * mouse steers. Anyone close to them while they fly is burned. Every number comes from abilities.yml (BlastOff
 * section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "duration-ticks", "speed", "launch-lift", "damage", "radius", "interval-ticks" })
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
            Math.max(1, settingInt("interval-ticks")));

        BattlePlugin.getInstance().getAbilityScheduler().schedule(new BlastOffTask(owner, this, settings));
        return Activation.USED;
    }
}
