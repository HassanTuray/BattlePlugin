package com.hfturay.battleplugin.abilityschedulerservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import com.hfturay.battleplugin.BattlePlugin;

public class AbilityLogger {
    private final List<ScheduledAbilityTask> tasks = new ArrayList<>();

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<ScheduledAbilityTask> it = tasks.iterator();
        
                while (it.hasNext()) {
                    ScheduledAbilityTask task = it.next();
                    if (task.update()) { 
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);
    }

    public void schedule(ScheduledAbilityTask task) {
        tasks.add(task);
    }

}
