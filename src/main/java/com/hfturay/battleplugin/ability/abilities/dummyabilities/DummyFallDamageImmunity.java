package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.FallImmunityService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyFallDamageImmunity extends Ability {
    public DummyFallDamageImmunity(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy Fall Damage Immunity");
        this.setCoolDown(1);

        
    }

    @Override
    protected Activation onActivate() {
        FallImmunityService fallDamageService = BattlePlugin.getInstance().getFallImmunityService();

        fallDamageService.grantFallImmunity(owner.getPlayer());
        return Activation.USED;
    }

    
}
