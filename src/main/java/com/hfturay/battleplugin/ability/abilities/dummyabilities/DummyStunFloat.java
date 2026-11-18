package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.stunservice.StunService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyStunFloat extends Ability {

    public DummyStunFloat(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Stun Float");
        this.setCoolDown(1);

    }

    @Override
    protected Activation onActivate() {
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        Stun floatLock = new Stun(owner.getPlayer(), 100);
        floatLock.lockX = true;
        floatLock.lockZ = true;
        floatLock.grantImmunity = true;

        stunLogger.add(owner, floatLock);

        return Activation.USED;
    }

    
}
