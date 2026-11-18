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
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

/** Every number comes from abilities.yml (Teleport section). */
@RequiresSettings({ "cooldown", "range" })
public class Teleport extends Ability {

    public Teleport(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Teleport");
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();

        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            setting("range"),
            FluidCollisionMode.NEVER
        );

        if (result == null) {
            return Activation.FAILED;
        }

        Block block = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();
        Location dest;

        if (block == null || face == null) {
            return Activation.FAILED;
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
        

        return Activation.USED;
    }

    
}
