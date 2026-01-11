package com.hfturay.battleplugin.ability;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public abstract class MeleeAbility extends Ability {
    protected double reach = 0;

    public MeleeAbility(BattlePlayer owner) {
        super(owner);
    }

    public double getReach() {
        return this.reach;
    }

    public void setReach(double reach) {
        this.reach = reach;
    }
}
