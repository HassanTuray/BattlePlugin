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

public class DummyStunFloat extends Ability {

    public DummyStunFloat(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Stun Float");
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
        Stun floatLock = new Stun(owner.getPlayer(), 100);
        floatLock.lockX = true;
        floatLock.lockZ = true;
        floatLock.grantImmunity = true;

        stunLogger.add(owner, floatLock);

        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
