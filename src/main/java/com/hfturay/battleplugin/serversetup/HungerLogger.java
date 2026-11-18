package com.hfturay.battleplugin.serversetup;

import java.util.function.IntSupplier;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/** Hunger is off: food stays at the level from config.yml. */
public class HungerLogger implements Listener {
    private final IntSupplier foodLevel;

    public HungerLogger(IntSupplier foodLevel) {
        this.foodLevel = foodLevel;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        player.setFoodLevel(foodLevel.getAsInt());
        player.setSaturation(20f);

        event.setCancelled(true);
    }

}
