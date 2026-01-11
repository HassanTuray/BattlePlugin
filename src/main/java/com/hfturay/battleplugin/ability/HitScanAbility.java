package com.hfturay.battleplugin.ability;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public abstract class HitScanAbility extends Ability {
    protected double range = 0;

    public HitScanAbility(BattlePlayer owner) {
        super(owner);
    }

    public double getRange() {
        return this.range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
