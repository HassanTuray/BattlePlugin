package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.hfturay.battleplugin.BattlePlugin;

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

    /**
     * Shows the body to this player, or hides it from them: the mob (its hitbox, name and sounds included) and anything
     * drawn on top of it. Calling it again with the same answer changes nothing, so it is cheap to repeat.
     */
    default void setVisibleTo(Player player, boolean visible) {
        Plugin plugin = BattlePlugin.getInstance();
        if (visible) {
            player.showEntity(plugin, mob());
        } else {
            player.hideEntity(plugin, mob());
        }
    }

    /** True if the body draws its own name tag, so the mob's vanilla one should be hidden. */
    default boolean providesNametag() {
        return false;
    }
}
