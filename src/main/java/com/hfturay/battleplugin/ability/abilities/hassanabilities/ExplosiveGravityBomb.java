package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.MaximumTechnique;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Explosive Gravity Bomb, a maximum technique (on the amethyst shard, costs chaos-cost chaos): a black hole appears
 * above the caster and drags in everything around it while it shrinks. When it is small enough it explodes, hurting
 * what is close and blasting away everything in range.
 * Every number comes from abilities.yml (ExplosiveGravityBomb section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "chaos-cost", "height", "duration-ticks", "start-radius", "end-radius", "pull-radius",
    "pull-force", "radius", "blast-radius", "damage", "knockback" })
public class ExplosiveGravityBomb extends MaximumTechnique {

    public ExplosiveGravityBomb(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Explosive Gravity Bomb");
    }

    @Override
    protected Activation cast() {
        GravityBombTask.Settings settings = new GravityBombTask.Settings(
            settingInt("duration-ticks"), setting("start-radius"), setting("end-radius"),
            setting("pull-radius"), setting("pull-force"),
            setting("radius"), setting("blast-radius"), setting("damage"), setting("knockback"));

        Location ball = owner.getPlayer().getLocation().add(0, setting("height"), 0);
        BattlePlugin.getInstance().getAbilityScheduler().schedule(new GravityBombTask(this, owner, ball, settings));
        return Activation.USED;
    }
}
