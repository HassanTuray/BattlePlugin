package com.hfturay.battleplugin.projectileservice;

import org.bukkit.util.Vector;

public interface MotionFunction {
    Vector apply(Vector previousVelocity, int tick);
}
