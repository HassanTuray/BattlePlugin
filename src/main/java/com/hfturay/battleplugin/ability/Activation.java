package com.hfturay.battleplugin.ability;

/** What an ability reports back from onActivate(), so the cooldown is handled in one place. */
public enum Activation {
    /** It worked. Start the cooldown. */
    USED,

    /** It did nothing (missed, no target, nothing to do). No cooldown, the player can try again. */
    FAILED,

    /**
     * It worked, but the ability starts its own cooldown later (combos like Three-Hits and Grand Slam, or Shortcut's
     * set-then-use). The ability calls startCooldown() when it is ready for one.
     */
    USED_NO_COOLDOWN
}
