package com.hfturay.battleplugin.ability.abilities.waterabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Fires a few blasts of water in quick succession. Each keeps flying away from the caster while steering toward
 * wherever the caster is looking. Every number comes from abilities.yml (WaterBarrage section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "damage", "speed", "hitbox", "max-ticks", "interval-ticks", "count", "trail-ticks", "turn-rate" })
public class WaterBarrage extends Ability {

    public WaterBarrage(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Water Barrage");
    }

    @Override
    protected Activation onActivate() {
        WaterBarrageTask.Settings settings = new WaterBarrageTask.Settings(
            setting("damage"),
            setting("speed"),
            setting("hitbox"),
            settingInt("max-ticks"),
            Math.max(1, settingInt("interval-ticks")),
            Math.max(1, settingInt("count")),
            Math.max(1, settingInt("trail-ticks")),
            Math.max(0, Math.min(1, setting("turn-rate"))));

        BattlePlugin.getInstance().getAbilityScheduler().schedule(new WaterBarrageTask(owner, this, settings));
        return Activation.USED;
    }
}
