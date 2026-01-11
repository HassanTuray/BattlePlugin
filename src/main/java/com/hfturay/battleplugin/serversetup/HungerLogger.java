package com.hfturay.battleplugin.serversetup;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerLogger implements Listener {

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        } 

        player.setFoodLevel(16);
        player.setSaturation(20f);

        event.setCancelled(true);
    } 
    
}
