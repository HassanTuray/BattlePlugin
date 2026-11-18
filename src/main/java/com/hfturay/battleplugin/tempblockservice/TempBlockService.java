package com.hfturay.battleplugin.tempblockservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * The one way abilities change the world. Nothing else places or removes blocks for an ability.
 *
 *  - The first time a position is touched its original block is remembered, once. Overlapping effects stack as
 *    layers: the newest shows, and the original comes back only when the last layer ends. A temporary block is
 *    never remembered as an "original".
 *  - Every set ends by itself after its duration, so a block cannot outlive its ability even if the ability dies.
 *  - Everything is also written to a journal, so a crash cannot leave blocks behind: the next start puts them back.
 *  - TempBlockProtection stops players, water, pistons and explosions from altering tracked blocks.
 *
 * Plain Java around a Blocks adapter, so the rules can be tested without a server.
 */
public final class TempBlockService {

    private static final class Cell {
        final BlockData original;
        final List<TempBlocks> layers = new ArrayList<>();

        Cell(BlockData original) {
            this.original = original;
        }

        BlockData shown() {
            return layers.get(layers.size() - 1).data;
        }
    }

    private final Blocks blocks;
    private final TempBlockJournal journal;
    private final IntSupplier clock;
    private final Logger logger;

    private final Map<BlockPos, Cell> cells = new HashMap<>();
    private final List<TempBlocks> groups = new ArrayList<>();
    /** Originals from a previous run whose world was not loaded yet. Kept in the journal until they can be restored. */
    private final Map<BlockPos, BlockData> stale = new LinkedHashMap<>();
    private boolean dirty = false;

    public TempBlockService(Blocks blocks, TempBlockJournal journal, IntSupplier clock, Logger logger) {
        this.blocks = blocks;
        this.journal = journal;
        this.clock = clock;
        this.logger = logger;
    }

    /** All the positions of a solid cube of the given edge length, centred on the block at (centerX, centerY, centerZ). */
    public static List<BlockPos> cube(World world, int centerX, int centerY, int centerZ, int size) {
        return cube(world.getUID(), centerX, centerY, centerZ, size);
    }

    public static List<BlockPos> cube(java.util.UUID world, int centerX, int centerY, int centerZ, int size) {
        List<BlockPos> list = new ArrayList<>();
        int low = -((size - 1) / 2);
        int high = size / 2;
        for (int dx = low; dx <= high; dx++) {
            for (int dy = low; dy <= high; dy++) {
                for (int dz = low; dz <= high; dz++) {
                    list.add(new BlockPos(world, centerX + dx, centerY + dy, centerZ + dz));
                }
            }
        }
        return list;
    }

    /**
     * Places the block at every position, replacing whatever is there (solid blocks included; containers are
     * deleted with their contents). Ends by itself after durationTicks, or when remove() is called. A duration
     * of 0 or less lasts until remove().
     */
    public TempBlocks place(Collection<BlockPos> positions, BlockData data, int durationTicks) {
        int expiresAt = durationTicks > 0 ? clock.getAsInt() + durationTicks : -1;
        TempBlocks group = new TempBlocks(this, data, expiresAt);
        List<BlockPos> placed = new ArrayList<>();

        for (BlockPos pos : new LinkedHashSet<>(positions)) {
            try {
                Cell cell = cells.get(pos);
                if (cell == null) {
                    BlockData original = blocks.capture(pos);
                    if (original == null) {
                        continue;
                    }
                    cell = new Cell(original);
                    cells.put(pos, cell);
                }
                cell.layers.add(group);
                blocks.set(pos, data);
                placed.add(pos);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Could not place a temporary block at " + pos, e);
                Cell cell = cells.get(pos);
                if (cell != null) {
                    cell.layers.remove(group);
                    if (cell.layers.isEmpty()) {
                        cells.remove(pos);
                    }
                }
            }
        }

        group.positions = placed;
        groups.add(group);
        dirty = true;
        return group;
    }

    /** Ends one set: positions with no other layer go back to the original, the rest show the next layer down. */
    void release(TempBlocks group) {
        if (!group.active) {
            return;
        }
        group.active = false;
        groups.remove(group);

        for (BlockPos pos : group.positions) {
            Cell cell = cells.get(pos);
            if (cell == null) {
                continue;
            }
            cell.layers.remove(group);
            try {
                if (cell.layers.isEmpty()) {
                    blocks.set(pos, cell.original);
                    cells.remove(pos);
                } else {
                    blocks.set(pos, cell.shown());
                }
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Could not restore the block at " + pos + "; it stays in the journal", e);
                if (cell.layers.isEmpty()) {
                    stale.put(pos, cell.original);
                    cells.remove(pos);
                }
            }
        }
        dirty = true;
    }

    /** True if a temporary block is at this position right now. */
    public boolean isTracked(BlockPos pos) {
        return cells.containsKey(pos);
    }

    public int trackedBlocks() {
        return cells.size();
    }

    public int activeSets() {
        return groups.size();
    }

    /** Once a tick: ends sets that ran out, and saves the journal if anything changed. */
    public void tick() {
        int now = clock.getAsInt();
        for (TempBlocks group : new ArrayList<>(groups)) {
            if (group.expiresAt >= 0 && now >= group.expiresAt) {
                release(group);
            }
        }
        flush();
    }

    private void flush() {
        if (!dirty) {
            return;
        }
        dirty = false;
        Map<BlockPos, BlockData> originals = new LinkedHashMap<>(stale);
        for (Map.Entry<BlockPos, Cell> entry : cells.entrySet()) {
            originals.put(entry.getKey(), entry.getValue().original);
        }
        journal.save(originals);
    }

    /** Server start: puts back anything a crash left behind. Returns how many blocks were restored. */
    public int recover() {
        Map<BlockPos, BlockData> left = journal.load();
        int restored = 0;
        for (Map.Entry<BlockPos, BlockData> entry : left.entrySet()) {
            if (cells.containsKey(entry.getKey())) {
                continue;
            }
            try {
                blocks.set(entry.getKey(), entry.getValue());
                restored++;
            } catch (RuntimeException e) {
                stale.put(entry.getKey(), entry.getValue());    // world not loaded yet: try again at the next start
            }
        }
        dirty = true;
        flush();
        return restored;
    }

    /** Plugin disable: ends every set now, so nothing is left in the world. */
    public void restoreAll() {
        for (TempBlocks group : new ArrayList<>(groups)) {
            release(group);
        }
        flush();
    }

    /** The journal for the real server: block data is saved as its text form. */
    public static TempBlockJournal journalFor(java.io.File file, Logger logger) {
        return new TempBlockJournal(file, logger, BlockData::getAsString, Bukkit::createBlockData);
    }
}
