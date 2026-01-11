package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.Mob;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public abstract class Shikigami {
    private final Mob shikigami;
    private final BattlePlayer owner;
    private boolean dead = false;
    private String name;

    public abstract boolean tick();
    public abstract void configureShikigami(Mob shikigami, BattlePlayer owner);

    public Shikigami(Mob shikigami, BattlePlayer owner) {
        this.shikigami = shikigami;
        this.owner = owner;
    }

    public String getDisplayName() {
        return name;
    }

    public void setDisplayName(String name) {
        this.name = name;
    }

    public BattlePlayer getOwner() {
        return this.owner;
    }

    public void kill() {
        this.dead = true;
    }

    public boolean isDead() {
        return this.dead;
    }

    public Mob getMob() {
        return this.shikigami;
    }

}
