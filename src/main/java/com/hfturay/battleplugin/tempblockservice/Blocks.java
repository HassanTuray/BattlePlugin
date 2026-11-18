package com.hfturay.battleplugin.tempblockservice;

import org.bukkit.block.data.BlockData;

/** The only way TempBlockService touches the world. The real one is BukkitBlocks; tests use a fake. */
public interface Blocks {

    /**
     * What is at the position now, in the form it should be put back. Returns null if the position cannot be
     * used (its world is not loaded). Blocks with contents (chests, furnaces, signs...) are emptied and reported
     * as air: they are deleted for good, never restored.
     */
    BlockData capture(BlockPos pos);

    /** Puts a block there without triggering block updates. */
    void set(BlockPos pos, BlockData data);
}
