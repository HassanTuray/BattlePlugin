package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.Mob;

/** A shikigami that is just a normal Minecraft mob. */
public final class VanillaBody implements Body {
    private final Mob mob;

    public VanillaBody(Mob mob) {
        this.mob = mob;
    }

    @Override
    public Mob mob() {
        return mob;
    }

    @Override
    public void play(String animation) {
        if ("attack".equals(animation)) {
            mob.swingMainHand();
        }
    }

    @Override
    public void remove() {
        // nothing extra: the mob is the whole body
    }
}
