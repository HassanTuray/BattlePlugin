package com.hfturay.battleplugin.shikigamiservice;

import org.bukkit.entity.LivingEntity;

/** Decides who a shikigami wants to attack right now. Return null for "nobody". Add new ones freely. */
@FunctionalInterface
public interface TargetingStrategy {
    LivingEntity pick(BrainShikigami self);

    /** Try this one first; if it finds nobody, ask the other. */
    default TargetingStrategy orElse(TargetingStrategy other) {
        return self -> {
            LivingEntity first = pick(self);
            return first != null ? first : other.pick(self);
        };
    }
}
