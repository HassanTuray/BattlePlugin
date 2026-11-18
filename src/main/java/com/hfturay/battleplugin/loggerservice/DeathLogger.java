package com.hfturay.battleplugin.loggerservice;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.hfturay.battleplugin.BattlePlugin;

public class DeathLogger implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        BattlePlugin.getInstance().getShikigamiService().killOwnedBy(player.getUniqueId());
        BattlePlugin.getInstance().getAbilityScheduler().cancelOnDeath(player.getUniqueId());
        BattlePlugin.getInstance().getParticleService().cancelOnDeath(player.getUniqueId());
        BattlePlugin.getInstance().getStunService().clear(player.getUniqueId());
        BattlePlugin.getInstance().getImmunityService().clearAll(player.getUniqueId());
        BattlePlugin.getInstance().getFallImmunityService().clearFallImmunity(player.getUniqueId());
    }
}
