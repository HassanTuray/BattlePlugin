package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.ability.abilities.domainabilities.DomainAbility;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cutsceneservice.Scenes;
import com.hfturay.battleplugin.domainservice.DomainService;

/**
 * A plain test domain expansion, to try the dome and the cutscene: a dome of coal blocks closes around the caster,
 * everyone inside gets night vision (and everyone but the caster is silenced), a cutscene plays, and the domain lasts
 * for its duration. Numbers are in abilities.yml (DummyDomain section).
 */
@RequiresSettings({ "cooldown", "radius", "thickness", "form-ticks", "duration-ticks", "chaos-cost" })
public class DummyDomain extends DomainAbility {

    public DummyDomain(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy Domain");
    }

    @Override
    protected DomainService.Spec buildSpec() {
        return new DomainService.Spec("Dummy Domain", setting("radius"), setting("thickness"),
            settingInt("form-ticks"), settingInt("duration-ticks"), Material.COAL_BLOCK,
            (int) tuning().number(getId(), "night-vision-amplifier", 0), Scenes::test, null);
    }
}
