package com.hfturay.battleplugin.ability.abilities.dummyabilities;

import org.bukkit.Fluid;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.HitScan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

public class DummyHitScan extends Ability {

    public DummyHitScan(BattlePlayer owner) {
        super(owner);
        this.setDisplayName("Dummy Hit Scan");
        this.setCoolDown(10);

        
    }

    @Override
    protected Activation onActivate() {
        Entity result = HitScan.HitScanEntities(this.owner, 10, FluidCollisionMode.NEVER);

        if (result == null) {
            return Activation.FAILED;
        } else {
            
            return Activation.USED;
        }
    }

}
