package com.hfturay.battleplugin.shikigamiservice;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

/**
 * Picks what a shikigami looks like: the named BetterModel model if there is one and the BetterModel plugin is
 * running, otherwise the plain mob. The model classes are only loaded on the first branch, so the server works
 * without BetterModel installed.
 */
public final class ShikigamiBodies {
    private ShikigamiBodies() { }

    /** @param model the BetterModel model name (a .bbmodel file name without the extension), or null for a plain mob */
    public static Body create(Mob mob, String model, Map<String, List<String>> animations) {
        if (model != null && !model.isBlank() && Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
            Body body = BetterModelBody.tryCreate(mob, model, animations);
            if (body != null) {
                return body;
            }
        }
        return new VanillaBody(mob);
    }
}
