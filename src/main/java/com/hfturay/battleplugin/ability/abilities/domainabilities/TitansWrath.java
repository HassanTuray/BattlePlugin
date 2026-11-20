package com.hfturay.battleplugin.ability.abilities.domainabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.domainservice.neverendingnight.NeverendingNightEffects;
import com.hfturay.battleplugin.domainservice.neverendingnight.TitansWrathTask;

/**
 * Titan's Wrath: inside their Neverending Night the caster calls up a giant eye on the obsidian tower, which fires a
 * long beam that follows their cursor. Given to the caster for as long as the domain lasts (it is temporary, so it
 * is never saved with their abilities). Numbers are in abilities.yml (TitansWrath section): damage is health
 * points (4 = 2 hearts) dealt every interval-ticks for duration-ticks.
 */
@RequiresSettings({ "cooldown", "damage", "interval-ticks", "duration-ticks", "charge-ticks", "range", "width" })
public class TitansWrath extends Ability {

    public TitansWrath(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Titan's Wrath");
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    protected String blockedReason() {
        if (BattlePlugin.getInstance().getNeverendingNightService().of(owner.getPlayer().getUniqueId()) == null) {
            return "Titan's Wrath only works inside your Neverending Night";
        }
        return null;
    }

    @Override
    protected Activation onActivate() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        NeverendingNightEffects night = plugin.getNeverendingNightService().of(owner.getPlayer().getUniqueId());
        if (night == null || night.eyeCenter() == null) {
            return Activation.FAILED;
        }

        TitansWrathTask.Params params = new TitansWrathTask.Params(settingInt("charge-ticks"), settingInt("duration-ticks"),
            settingInt("interval-ticks"), setting("damage"), setting("range"), setting("width"));
        Ability self = this;
        plugin.getAbilityScheduler().schedule(new TitansWrathTask(self, owner, night, plugin.getNeverendingNightService(), params));
        return Activation.USED;
    }
}
