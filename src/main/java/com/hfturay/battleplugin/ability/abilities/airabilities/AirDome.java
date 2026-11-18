package com.hfturay.battleplugin.ability.abilities.airabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * A swirling dome of wind around the caster. Anyone inside it is pushed away from the caster, and hurt each time the
 * dome touches them. The caster gets a big speed boost. Every number comes from abilities.yml (AirDome section), read
 * each time it is cast.
 */
@RequiresSettings({ "cooldown", "duration-ticks", "radius", "damage", "speed-amplifier", "push-strength", "push-lift",
    "damage-interval-ticks", "count" })
public class AirDome extends Ability {

    public AirDome(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Air Dome");
    }

    @Override
    protected Activation onActivate() {
        AirDomeTask.Settings settings = new AirDomeTask.Settings(
            settingInt("duration-ticks"),
            setting("radius"),
            setting("damage"),
            settingInt("speed-amplifier"),
            setting("push-strength"),
            setting("push-lift"),
            Math.max(1, settingInt("damage-interval-ticks")),
            settingInt("count"));

        BattlePlugin.getInstance().getAbilityScheduler().schedule(new AirDomeTask(owner, this, settings));
        return Activation.USED;
    }
}
