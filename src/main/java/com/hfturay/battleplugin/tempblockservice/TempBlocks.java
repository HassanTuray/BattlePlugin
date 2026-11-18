package com.hfturay.battleplugin.tempblockservice;

import java.util.List;

import org.bukkit.block.data.BlockData;

/**
 * A set of temporary blocks placed together (one water cube). Call remove() to undo them early; otherwise they
 * undo themselves when the duration ends.
 */
public final class TempBlocks {
    private final TempBlockService service;
    final BlockData data;
    final int expiresAt;      // server tick, or -1 for "until removed"
    List<BlockPos> positions;
    boolean active = true;

    TempBlocks(TempBlockService service, BlockData data, int expiresAt) {
        this.service = service;
        this.data = data;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    /** How many blocks this set really placed. */
    public int size() {
        return positions == null ? 0 : positions.size();
    }

    /** Puts everything back now. Safe to call twice. */
    public void remove() {
        service.release(this);
    }
}
