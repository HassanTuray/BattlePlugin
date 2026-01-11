package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;

public class ThreeHitsKnockbackRepeater extends ScheduledAbilityTask {
    private Entity target;
    private double force;

    public ThreeHitsKnockbackRepeater(int duration, Entity target, double force) {
        super(duration);
        
        this.target = target;
        this.force = force;
    }

    @Override
    public void run() {
        Vector velocity = target.getVelocity();
        target.setVelocity(velocity.add(new Vector(0, force,0)));
    }
}
