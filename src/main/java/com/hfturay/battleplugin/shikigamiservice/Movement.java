package com.hfturay.battleplugin.shikigamiservice;

/** How a shikigami moves around. Called a few times a second, not every tick. */
@FunctionalInterface
public interface Movement {
    void move(BrainShikigami self);
}
