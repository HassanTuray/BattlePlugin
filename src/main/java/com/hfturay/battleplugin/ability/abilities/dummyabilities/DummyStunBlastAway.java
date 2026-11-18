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

public class DummyStunBlastAway extends Ability {

    public DummyStunBlastAway(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Stun Blast Away");
        this.setCoolDown(1);

    }

    @Override
    protected Activation onActivate() {
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        Stun blastAway = new Stun(owner.getPlayer(), 100);
        blastAway.lockX = true;
        blastAway.lockY = true;
        blastAway.lockZ = true;
        blastAway.grantImmunity = true;
        blastAway.forcedVelocity = new Vector(1.5, 1.0, 1.5);

        stunLogger.add(owner, blastAway);

        return Activation.USED;
    }

    
}
