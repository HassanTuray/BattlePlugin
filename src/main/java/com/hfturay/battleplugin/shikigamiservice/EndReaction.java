package com.hfturay.battleplugin.shikigamiservice;

/**
 * What happens to the owner (or the world) when a shikigami ends. Runs once, never on SHUTDOWN.
 * The reason is given so a reaction can care only about some endings (see EndReactions.only).
 */
@FunctionalInterface
public interface EndReaction {
    void onEnd(BrainShikigami self, EndReason reason);

    /** Do this one, then the other. */
    default EndReaction and(EndReaction other) {
        return (self, reason) -> {
            onEnd(self, reason);
            other.onEnd(self, reason);
        };
    }
}
