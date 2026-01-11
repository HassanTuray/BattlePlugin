package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.AOEAbility;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.HitScan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class LandMine extends AOEAbility {
    private int explosionDelay = 100;

    public LandMine(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Landmine");
        this.setCoolDown(8);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.radius = 5;
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }
        
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Block block = HitScan.HitScanBlock(owner, radius, collisionMode);

        if (block == null) {
            return false;
        }

        Location location = block.getLocation().clone();

        LandMineRepeater repeater = new LandMineRepeater(owner, location, explosionDelay);
        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();

        abilityLogger.schedule(repeater);

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
    
}
