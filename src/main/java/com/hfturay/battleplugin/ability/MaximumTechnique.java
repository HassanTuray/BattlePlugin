package com.hfturay.battleplugin.ability;

import java.util.UUID;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The shared rules of a maximum technique: it lives on the domain item (the amethyst shard), needs "chaos-cost" chaos
 * to use (50 by default), and spends that much when it goes off, leaving the rest of the bar. Unlike a domain, it does
 * not empty the bar. A subclass does its technique in cast() and must list "chaos-cost" in @RequiresSettings.
 */
public abstract class MaximumTechnique extends Ability {

    protected MaximumTechnique(BattlePlayer owner) {
        super(owner);
    }

    /** The technique itself. Only called once the player has enough chaos. FAILED spends nothing. */
    protected abstract Activation cast();

    /** A subclass with rules of its own overrides this and returns super.blockedReason() when it has nothing to say. */
    @Override
    protected String blockedReason() {
        double cost = setting("chaos-cost");
        double have = BattlePlugin.getInstance().getChaosService().get(owner.getPlayer().getUniqueId());
        if (have < cost) {
            return "Not enough chaos (" + (int) have + "/" + (int) cost + ")";
        }
        return null;
    }

    @Override
    protected final Activation onActivate() {
        Activation result = cast();
        if (result != Activation.FAILED) {
            UUID id = owner.getPlayer().getUniqueId();
            BattlePlugin.getInstance().getChaosService().spend(id, setting("chaos-cost"));
        }
        return result;
    }
}
