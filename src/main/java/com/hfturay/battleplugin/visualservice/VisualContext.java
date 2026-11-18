package com.hfturay.battleplugin.visualservice;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * What a spec gets when its visual starts.
 *
 * @param at        where the anchor is right now
 * @param direction which way the anchor is travelling
 * @param tagger    call this on every entity you spawn, so the service can recognise leftovers
 * @param length    the anchor's length hint (a beam's length in blocks); 0 for visuals that have no length
 */
public record VisualContext(Location at, Vector direction, Consumer<Entity> tagger, double length) {

    public VisualContext(Location at, Vector direction, Consumer<Entity> tagger) {
        this(at, direction, tagger, 0);
    }
}
