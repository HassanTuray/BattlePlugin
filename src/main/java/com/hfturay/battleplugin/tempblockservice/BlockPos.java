package com.hfturay.battleplugin.tempblockservice;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;

/** One block position in one world. Plain data, so the service logic needs no server to be tested. */
public record BlockPos(UUID world, int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockPos of(Location location) {
        return new BlockPos(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
