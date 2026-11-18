package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.stunservice.StunService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyStunLock extends Ability {

    public DummyStunLock(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Stun Lock");
        this.setCoolDown(1);

    }

    @Override
    protected Activation onActivate() {
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        Stun lock = new Stun(owner.getPlayer(), 100);
        lock.lockX = true;
        lock.lockY = true;
        lock.lockZ = true;
        lock.grantImmunity = true;

        stunLogger.add(owner, lock);

        return Activation.USED;
    }

    
}
