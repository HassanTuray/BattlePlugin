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

    // ---- how it ended ----

    private EndReason endReason;
    private boolean ended = false;

    /** Why it ended, or null if it has not (or nothing has said yet). */
    public EndReason endReason() {
        return endReason;
    }

    /** Records why it is ending. The first reason given wins. */
    public void setEndReason(EndReason reason) {
        if (endReason == null) {
            endReason = reason;
        }
    }

    /**
     * The service calls this once when the shikigami is over, whatever the cause. Runs onEnded exactly once.
     * A reaction that throws is logged by the caller's catch, never allowed to stop the rest of the cleanup.
     */
    public final void notifyEnded(EndReason fallback) {
        if (ended) {
            return;
        }
        ended = true;
        setEndReason(fallback);
        onEnded(endReason);
    }

    /** Override to react to the ending (not called twice). Default: nothing. */
    protected void onEnded(EndReason reason) {
    }

}
