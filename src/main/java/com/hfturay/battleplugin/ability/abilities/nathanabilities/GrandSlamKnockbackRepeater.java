package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.targetingservice.Targets;

public class GrandSlamKnockbackRepeater extends ScheduledAbilityTask {
    private Entity target;
    private double force;
    

    public GrandSlamKnockbackRepeater(int duration, Entity target, double force, Ability ability) {
        super(duration, ability);
        
        this.target = target;
        this.force = force;
    }

    @Override
    protected void onFinish() {
        if (Targets.isUntargetable(target)) {
            return;    // became untargetable after being hit: the delayed launch must not land
        }
        Knockback.set(target, new Vector(0, force, 0));
    }
    
}
