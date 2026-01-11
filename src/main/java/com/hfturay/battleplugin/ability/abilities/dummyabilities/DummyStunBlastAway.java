package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.stunservice.StunLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DummyStunBlastAway extends Ability {

    public DummyStunBlastAway(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Stun Blast Away");
        this.setCoolDown(1);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        StunLogger stunLogger = BattlePlugin.getInstance().getStunLogger();
        Stun blastAway = new Stun(owner.getPlayer(), 100);
        blastAway.lockX = true;
        blastAway.lockY = true;
        blastAway.lockZ = true;
        blastAway.grantImmunity = true;
        blastAway.forcedVelocity = new Vector(1.5, 1.0, 1.5);

        stunLogger.add(owner, blastAway);

        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
