package com.hfturay.battleplugin.ability.abilities.waterabilities;

import java.util.List;

import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Sends a floating water block after every entity within range. Each one that connects is popped into the air and
 * held in a solid cube of still water for a few seconds: no damage, no targeting, no abilities.
 * Every number comes from abilities.yml (WaterPrison section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "range", "speed", "hitbox", "max-ticks", "pop-ticks", "pop-lift", "cube-size", "duration-ticks", "trail-ticks" })
public class WaterPrison extends Ability {

    public WaterPrison(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Water Prison");
    }

    @Override
    protected Activation onActivate() {
        List<Entity> targets = AOE.getTargets(owner, owner.getPlayer().getLocation(), setting("range"));
        if (targets.isEmpty()) {
            owner.getPlayer().sendActionBar(Component.text("No targets in range", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
            return Activation.FAILED;
        }

        WaterPrisonTask.Settings settings = new WaterPrisonTask.Settings(
            setting("speed"),
            setting("hitbox"),
            settingInt("max-ticks"),
            Math.max(1, settingInt("cube-size")),
            settingInt("pop-ticks"),
            settingInt("duration-ticks"),
            setting("pop-lift"),
            Math.max(1, settingInt("trail-ticks")));

        for (Entity target : targets) {
            BattlePlugin.getInstance().getAbilityScheduler().schedule(new WaterPrisonTask(owner, this, target, settings));
        }
        return Activation.USED;
    }
}
