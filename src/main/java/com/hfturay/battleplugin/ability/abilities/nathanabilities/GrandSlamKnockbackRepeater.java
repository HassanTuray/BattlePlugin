package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;

public class GrandSlamKnockbackRepeater extends ScheduledAbilityTask {
    private Entity target;
    private double force;
    

    public GrandSlamKnockbackRepeater(int duration, Entity target, double force) {
        super(duration);
        
        this.target = target;
        this.force = force;
    }

    @Override
    public void run() {
        if (force > 0) {
            BattlePlugin.getInstance().getLogger().info("Lifting up");
        } else {
            BattlePlugin.getInstance().getLogger().info("Slamming down");
        }

        target.setVelocity(new Vector(0, force,0));
    }
    
}
