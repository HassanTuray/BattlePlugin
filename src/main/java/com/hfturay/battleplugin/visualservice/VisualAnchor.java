package com.hfturay.battleplugin.visualservice;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * What a visual follows. The service asks it for the position every tick, and ends the visual the tick
 * isValid() turns false (a projectile that hit something, ran out, or was cancelled).
 */
public interface VisualAnchor {

    /** Current position. May be a shared, mutable object: copy what you need. */
    Location location();

    /** Current direction of travel. */
    Vector direction();

    boolean isValid();

    /**
     * A length for visuals that stretch (a beam: how far it reaches this tick, in blocks). Visuals that do not
     * stretch ignore it, so anchors that have no length need not say anything.
     */
    default double length() {
        return 0;
    }
}
