package com.hfturay.battleplugin.ability.abilities.domainabilities;

import org.bukkit.Material;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.domainservice.DomainService;
import com.hfturay.battleplugin.domainservice.neverendingnight.NeverendingNightEffects;

/**
 * Domain: Neverending Night. A blue pillar of flame bursts from the ground as a dome of darkness closes around the
 * caster and their enemies; the world turns to night (and back to day when it ends); the pillar hardens into an obsidian tower; creatures of
 * darkness (endermen, phantoms, a warden) hunt the caster's enemies, who are slowed and blinded; and the caster
 * gains Titan's Wrath. Every number is in abilities.yml (NeverendingNight section).
 */
@RequiresSettings({ "cooldown", "radius", "thickness", "form-ticks", "duration-ticks", "chaos-cost", "tower-height",
    "endermen-count", "phantom-count", "warden-count", "mobs-target-caster", "slow-amplifier", "flame-height" })
public class NeverendingNight extends DomainAbility {

    public NeverendingNight(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Domain: Neverending Night");
    }

    @Override
    protected DomainService.Spec buildSpec() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        NeverendingNightEffects.Settings settings = new NeverendingNightEffects.Settings(
            settingInt("flame-height"), settingInt("tower-height"), settingInt("endermen-count"), settingInt("phantom-count"), settingInt("warden-count"),
            setting("mobs-target-caster") > 0, settingInt("slow-amplifier"));

        NeverendingNightEffects effects = new NeverendingNightEffects(plugin.getNeverendingNightService(), settings);

        // charcoal is not a block, so the coal block stands in for it
        return new DomainService.Spec("Neverending Night", setting("radius"), setting("thickness"),
            settingInt("form-ticks"), settingInt("duration-ticks"), Material.COAL_BLOCK,
            (int) tuning().number(getId(), "night-vision-amplifier", 0), effects::scene, effects);
    }
}
