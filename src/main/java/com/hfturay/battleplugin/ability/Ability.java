package com.hfturay.battleplugin.ability;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public abstract class Ability {
    protected String id = "Default ID";
    protected String displayName = "Default Display Name";
    protected long cooldown = 0;
    protected final BattlePlayer owner;
    protected double horizForce;
    protected double vertForce;
    protected double damage = 0;

    public abstract boolean activate(); 
    public abstract void playParticleEffect();

    public Ability(BattlePlayer owner) {
        this.owner = owner;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCoolDown(long cooldown) {
        this.cooldown = cooldown;
    }

    public double getHorizForce() {
        return this.horizForce;
    }

    public void setHorizForce(double horizForce) {
        this.horizForce = horizForce;
    }

    public double getVertForce() {
        return this.vertForce;
    }

    public void setVertForce(double vertForce) {
        this.vertForce = vertForce;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public BattlePlayer getOwner() {
        return this.owner;
    }

    public void cancel() {}

}
