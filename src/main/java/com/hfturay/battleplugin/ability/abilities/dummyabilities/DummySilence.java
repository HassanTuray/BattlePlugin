package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummySilence extends Ability {

    public DummySilence(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Dummy Silence");
        this.setCoolDown(3);
    }

    @Override
    protected Activation onActivate() {
        BattlePlugin.getInstance().getSilenceService().silence(owner.getPlayer().getUniqueId(), 5 * 20);

        return Activation.USED;
    }

    
}
