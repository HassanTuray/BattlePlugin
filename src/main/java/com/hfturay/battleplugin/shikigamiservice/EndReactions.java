package com.hfturay.battleplugin.shikigamiservice;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.BattlePlugin;

/** The ready-made end reactions. */
public final class EndReactions {
    private EndReactions() { }

    public static EndReaction none() {
        return (self, reason) -> { };
    }

    /** Only react when the shikigami ends for one of these reasons. */
    public static EndReaction only(EndReaction reaction, EndReason first, EndReason... rest) {
        Set<EndReason> reasons = EnumSet.of(first, rest);
        return (self, reason) -> {
            if (reasons.contains(reason)) {
                reaction.onEnd(self, reason);
            }
        };
    }

    /** Hurts the owner (hp = half hearts). Never during shutdown, and only if the owner is online and alive. */
    public static EndReaction damageOwner(double hp) {
        return (self, reason) -> {
            Player owner = self.getOwner().getPlayer();
            if (owner.isOnline() && !owner.isDead()) {
                BattlePlugin.getInstance().getDamageService().damage((com.hfturay.battleplugin.battleplayer.BattlePlayer) null, owner, hp);
            }
        };
    }

    /** Heals the owner (hp = half hearts), up to their maximum. */
    public static EndReaction healOwner(double hp) {
        return (self, reason) -> {
            Player owner = self.getOwner().getPlayer();
            if (!owner.isOnline() || owner.isDead()) {
                return;
            }
            AttributeInstance max = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double limit = max == null ? 20 : max.getValue();
            owner.setHealth(Math.min(limit, owner.getHealth() + hp));
        };
    }
}
