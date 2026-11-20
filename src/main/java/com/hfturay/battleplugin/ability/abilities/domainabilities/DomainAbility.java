package com.hfturay.battleplugin.ability.abilities.domainabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.domainservice.DomainService;

/**
 * The shared rules of a domain expansion ability: it needs "chaos-cost" chaos (100 for a real domain; 0 skips the
 * requirement while testing), only one domain per player at a time, and casting empties the player's chaos bar.
 * A subclass says what its domain is like by building a Spec, and must list "chaos-cost" in @RequiresSettings.
 */
public abstract class DomainAbility extends Ability {

    protected DomainAbility(BattlePlayer owner) {
        super(owner);
    }

    /** Builds this cast's domain from the numbers in abilities.yml (read now, so a reload applies). */
    protected abstract DomainService.Spec buildSpec();

    @Override
    protected String blockedReason() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        if (plugin.getDomainService().isActive(owner.getPlayer().getUniqueId())) {
            return "Your domain is already up";
        }
        double cost = setting("chaos-cost");
        double have = plugin.getChaosService().get(owner.getPlayer().getUniqueId());
        if (have < cost) {
            return "Not enough chaos (" + (int) have + "/" + (int) cost + ")";
        }
        return null;
    }

    @Override
    protected Activation onActivate() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        if (!plugin.getDomainService().cast(owner.getPlayer(), buildSpec())) {
            return Activation.FAILED;
        }
        plugin.getChaosService().set(owner.getPlayer().getUniqueId(), 0);     // casting a domain empties the bar
        return Activation.USED;
    }
}
