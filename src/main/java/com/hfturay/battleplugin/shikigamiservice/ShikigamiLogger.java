package com.hfturay.battleplugin.shikigamiservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import com.hfturay.battleplugin.BattlePlugin;

public class ShikigamiLogger {
    private List<Shikigami> shikigami = new ArrayList<>();

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Shikigami> iter = shikigami.iterator();

                while (iter.hasNext()) {
                    Shikigami shikigami = iter.next();

                    if (shikigami.isDead()) {
                        shikigami.getMob().setHealth(0);
                        iter.remove();
                        continue;
                    }

                    boolean finished = shikigami.tick();
                    if (finished) {
                        shikigami.getMob().setHealth(0);
                        iter.remove();
                    }
                }
            }
        }.runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);
    }

    public void add(Shikigami shikigami) {
        this.shikigami.add(shikigami);
    }

    public List<Shikigami> getShikigami() {
        return this.shikigami;
    }
}
