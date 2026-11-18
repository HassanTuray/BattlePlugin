package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.ImmunityService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyImmunity extends Ability {

    public DummyImmunity(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Dummy Immunity");
        this.setCoolDown(1);

    }

    @Override
    protected Activation onActivate() {
        ImmunityService immunity = BattlePlugin.getInstance().getImmunityService();
        immunity.grant(owner.getPlayer(), 100);

        return Activation.USED;
    }

    
}
