package com.hfturay.battleplugin.visualservice;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/** One running copy of a visual: the entities of a single rocket, for example. */
public interface VisualInstance {

    /** Moves the visual to the anchor's position. Called every tick after the one that created it. */
    void update(Location at, Vector direction);

    /**
     * Same, with the anchor's length hint. Only stretching visuals (beams) need to override this; the default
     * ignores the length.
     */
    default void update(Location at, Vector direction, double length) {
        update(at, direction);
    }

    /** False if any of its entities is gone (chunk unloaded, killed by something else). The visual then ends. */
    boolean isValid();

    /** Removes every entity. Must be safe to call more than once. */
    void remove();
}
