package com.hfturay.battleplugin.shikigamiservice;

/** Why a shikigami stopped existing. Despawn rules return one, and end reactions can depend on it. */
public enum EndReason {
    /** Its health reached zero (an enemy, or anything else, killed it). */
    KILLED,
    /** Its lifetime ran out. */
    TIMEOUT,
    /** It went too long without fighting. */
    INACTIVE,
    /** Its owner died. */
    OWNER_DIED,
    /** Its owner left the server. */
    OWNER_LEFT,
    /** Removed on purpose (a command, the mob vanished or was unloaded). */
    DISMISSED,
    /** The plugin is stopping. Nothing is allowed to react to this. */
    SHUTDOWN
}
