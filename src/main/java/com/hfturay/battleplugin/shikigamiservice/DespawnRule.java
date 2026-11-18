package com.hfturay.battleplugin.shikigamiservice;

/** One reason a shikigami may go away. Checked every tick. Return why it should end, or null to keep it. */
@FunctionalInterface
public interface DespawnRule {
    EndReason check(BrainShikigami self);
}
