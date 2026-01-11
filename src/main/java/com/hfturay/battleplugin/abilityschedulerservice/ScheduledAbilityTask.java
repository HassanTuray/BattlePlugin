package com.hfturay.battleplugin.abilityschedulerservice;

import com.hfturay.battleplugin.BattlePlugin;

public abstract class ScheduledAbilityTask {
    private int ticksRemaining;

    public ScheduledAbilityTask (int duration) {
        this.ticksRemaining = duration;
    }

    public boolean update() {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            run();
            return true;
        }
        return false;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void setTicksRemaining(int ticks) {
        this.ticksRemaining = ticks;
    }

    public void cancel() {
        ticksRemaining = 0;
    }

    public abstract void run();
    
}
