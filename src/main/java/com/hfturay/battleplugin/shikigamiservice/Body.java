package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.Mob;

/**
 * What a shikigami looks like and how it is shown. The brain never cares which kind it is: it moves the mob
 * (the physical body and hitbox) and says "play attack". A vanilla mob ignores animations it does not have;
 * a custom model (later) maps the names to its own animations.
 */
public interface Body {
    /** The mob that is the hitbox. Always exists, whatever it looks like. */
    Mob mob();

    /** Names used so far: "attack". More can be added without touching existing bodies. */
    void play(String animation);

    /** Take the visuals away (the mob itself is removed by the service). */
    void remove();

    /**
     * Draws the body this many times its normal size (1 = normal). The mob's own size (its hitbox) is separate:
     * set its scale attribute too. A vanilla mob is drawn from that attribute, so it does nothing here.
     */
    default void scale(double factor) {
    }

    /** True if the body draws its own name tag, so the mob's vanilla one should be hidden. */
    default boolean providesNametag() {
        return false;
    }
}
