package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;
import com.hfturay.battleplugin.targetingservice.HitScan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyAOE extends Ability{
    public DummyAOE(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Dummy AOE");
        this.setCoolDown(1);

        
    }

    @Override
    protected Activation onActivate() {
        Location center = owner.getPlayer().getLocation();
        List<Entity> result = AOE.getTargets(owner, center, 10);

        if (result == null) {

            return Activation.FAILED;
        } else {
            
            return Activation.USED;
        }
    }

}

