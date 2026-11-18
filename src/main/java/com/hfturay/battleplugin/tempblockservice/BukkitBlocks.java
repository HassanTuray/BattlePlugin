package com.hfturay.battleplugin.tempblockservice;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

/** Reads and writes real blocks. Anything with a block entity (chest, sign, spawner...) is deleted, contents included. */
public final class BukkitBlocks implements Blocks {

    @Override
    public BlockData capture(BlockPos pos) {
        World world = Bukkit.getWorld(pos.world());
        if (world == null) {
            return null;
        }
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        BlockState state = block.getState();
        if (state instanceof TileState) {
            if (state instanceof Container container) {
                container.getInventory().clear();    // so nothing can drop or be kept: the contents are deleted
            }
            return Material.AIR.createBlockData();
        }
        return block.getBlockData();
    }

    @Override
    public void set(BlockPos pos, BlockData data) {
        World world = Bukkit.getWorld(pos.world());
        if (world == null) {
            throw new IllegalStateException("world " + pos.world() + " is not loaded");
        }
        world.getBlockAt(pos.x(), pos.y(), pos.z()).setBlockData(data, false);
    }
}
