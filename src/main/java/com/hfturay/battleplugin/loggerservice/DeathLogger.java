package com.hfturay.battleplugin.loggerservice;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.shikigamiservice.Shikigami;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiLogger;

public class DeathLogger implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        killShikigami(player.getUniqueId());
    }

    public static void killShikigami(UUID uuid) {
        ShikigamiLogger shikigamiLogger = BattlePlugin.getInstance().getShikigamiLogger();
        List<Shikigami> shikigami = shikigamiLogger.getShikigami();

        for (Shikigami shiki : shikigami) {
            if (shiki.getOwner().getPlayer().getUniqueId().equals(uuid)) {
                shiki.kill();
            }
        }
    }
}
