package com.hfturay.battleplugin.projectileservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import com.hfturay.battleplugin.BattlePlugin;

public class ProjectileLogger {
    private List<VirtualProjectile> projectiles = new ArrayList<>();
    private int projectileLoggerDelay = 1;
    private int projectilePeriodDelay = 1;

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<VirtualProjectile> iter = projectiles.iterator();

                while (iter.hasNext()) {
                    VirtualProjectile proj = iter.next();
                    boolean finished = proj.tick();
                    if (finished) {
                        iter.remove();
                    }
                }
            }
        }.runTaskTimer(BattlePlugin.getInstance(), projectileLoggerDelay, projectilePeriodDelay);
    }
    
    public void add(VirtualProjectile projectile) {
        projectiles.add(projectile);
    }
}
