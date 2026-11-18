package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.AOEAbility;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.HitScan;

/**
 * Plants a mine on the block the caster looks at (within `range`). It goes off on whoever comes within `radius`.
 * Every number comes from abilities.yml (LandMine section).
 */
@RequiresSettings({ "cooldown", "range", "radius", "duration-ticks", "damage", "knockback" })
public class LandMine extends AOEAbility {

    public LandMine(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Landmine");
    }

    @Override
    protected Activation onActivate() {

        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Block block = HitScan.HitScanBlock(owner, setting("range"), collisionMode);

        if (block == null) {
            return Activation.FAILED;
        }

        Location location = block.getLocation().clone();

        LandMineRepeater repeater = new LandMineRepeater(owner, location, settingInt("duration-ticks"),
            setting("radius"), setting("damage"), setting("knockback"), this);
        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();

        abilityScheduler.schedule(repeater);

        return Activation.USED;
    }
}
