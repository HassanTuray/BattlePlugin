package com.hfturay.battleplugin.visualservice;

import java.util.UUID;

/** Returned by VisualService.follow. Lets the caller end the visual early. Holding on to it is optional. */
public final class VisualHandle {
    private final UUID owner;
    final VisualSpec spec;
    final VisualAnchor anchor;

    VisualInstance instance;
    int age = 0;
    private boolean active = true;

    VisualHandle(UUID owner, VisualSpec spec, VisualAnchor anchor) {
        this.owner = owner;
        this.spec = spec;
        this.anchor = anchor;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Removes the visual's entities right now, in the same tick. Safe to call repeatedly. A visual that has not
     * spawned yet (its first tick has not come) simply never spawns.
     */
    public void cancel() {
        if (!active) {
            return;
        }
        active = false;
        if (instance != null) {
            try {
                instance.remove();
            } catch (RuntimeException e) {
                // The entities are tagged and not saved, so a failed removal is cleaned up at the next shutdown or
                // chunk load. It must never stop whoever is cancelling (a projectile ending) from carrying on.
            }
        }
    }
}
