package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.AOE;
import com.hfturay.battleplugin.targetingservice.HitScan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DummyAOE extends Ability{
    public DummyAOE(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy AOE");
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

        Location center = owner.getPlayer().getLocation();
        List<Entity> result = AOE.getTargets(owner, center, 10);

        if (result == null) {

            return false;
        } else {
            cooldownLogger.startCooldown(owner, this);
            
            return true;
        }
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
}

