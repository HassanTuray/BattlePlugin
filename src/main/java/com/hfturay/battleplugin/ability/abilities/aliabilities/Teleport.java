package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Teleport extends Ability {
    private double range = 30;

    public Teleport(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Teleport");
        this.setCoolDown(12);

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

        Player player = owner.getPlayer();

        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            range,
            FluidCollisionMode.NEVER
        );

        if (result == null) {
            return false;
        }

        Block block = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();
        Location dest;

        if (block == null || face == null) {
            return false;
        }

        if (block.getRelative(BlockFace.UP).isEmpty()) {
            dest = block.getLocation().clone().add(0, 1,0);
            dest.setYaw(player.getYaw());
            dest.setPitch(player.getPitch());
            
            player.teleport(dest);
        } else {
            Vector faceDirection = face.getDirection();
            dest = block.getLocation().clone();
            dest.add(faceDirection.multiply(1));
            dest.setYaw(player.getYaw());
            dest.setPitch(player.getPitch());

            player.teleport(dest);
        }
        

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
