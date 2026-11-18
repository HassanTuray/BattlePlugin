package com.hfturay.battleplugin.ability.abilities.earthabilities;

import java.util.List;

import org.bukkit.Material;

/**
 * The blocks the earth moves are made of. Earth Dome builds its wall from them and Stubborn Earth shows them as
 * crumbs, so they always match. No ores, gravel or sand.
 */
final class EarthBlocks {
    private EarthBlocks() {}

    /** Repeats make a block more common when one is picked at random. */
    static final List<Material> PALETTE = List.of(
        Material.DIRT, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.PODZOL, Material.MUD,
        Material.PACKED_MUD,
        Material.COBBLESTONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE,
        Material.STONE, Material.STONE, Material.ANDESITE, Material.GRANITE, Material.TUFF, Material.COBBLED_DEEPSLATE);

    /** Each block once, in palette order. */
    static List<Material> distinct() {
        return PALETTE.stream().distinct().toList();
    }
}
