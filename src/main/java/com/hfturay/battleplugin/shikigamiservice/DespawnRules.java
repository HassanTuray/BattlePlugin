package com.hfturay.battleplugin.shikigamiservice;

/**
 * The ready-made despawn rules. A shikigami lists the ones it wants. (Its own death is not a rule: the service
 * always notices that.) A rule given 0 or less is switched off.
 */
public final class DespawnRules {
    private DespawnRules() { }

    /** Ends after this many ticks alive. */
    public static DespawnRule timeout(int ticks) {
        return self -> ticks > 0 && self.ageTicks() >= ticks ? EndReason.TIMEOUT : null;
    }

    /** Ends if it goes this many ticks without a target or an attack. */
    public static DespawnRule inactivity(int ticks) {
        return self -> ticks > 0 && self.ticksSinceActive() >= ticks ? EndReason.INACTIVE : null;
    }

    public static DespawnRule ownerDied() {
        return self -> self.getOwner().getPlayer().isDead() ? EndReason.OWNER_DIED : null;
    }

    public static DespawnRule ownerOffline() {
        return self -> self.getOwner().getPlayer().isOnline() ? null : EndReason.OWNER_LEFT;
    }
}
