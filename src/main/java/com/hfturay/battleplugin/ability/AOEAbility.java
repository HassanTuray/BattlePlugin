package com.hfturay.battleplugin.ability;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public abstract class AOEAbility extends Ability {
    protected double radius = 0;

    public AOEAbility(BattlePlayer owner) {
        super(owner);
    }

    public double getRadius() {
        return this.radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    } 
}
