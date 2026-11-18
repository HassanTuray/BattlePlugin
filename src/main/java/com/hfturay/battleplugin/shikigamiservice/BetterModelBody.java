package com.hfturay.battleplugin.shikigamiservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import com.hfturay.battleplugin.BattlePlugin;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;

/**
 * A shikigami drawn by a Blockbench model (through the BetterModel plugin). The mob stays the hitbox and the brain
 * keeps driving it; BetterModel hides the mob and draws the model on top, and plays idle / walk / damage / death
 * on its own. This class only adds the animations the brain asks for.
 *
 * The brain asks for logical names ("attack", "flame"). The spec says which model animations those mean, and a name
 * with several animations rotates through them. A name the spec does not map is tried as an animation name directly.
 *
 * Only touched when the BetterModel plugin is installed (see ShikigamiBodies), because it needs its classes.
 */
final class BetterModelBody implements Body {
    /** The model's death animation (2.5 seconds for the demon knight) needs time to finish before the model is removed. */
    private static final int DEATH_GRACE_TICKS = 80;

    private final Mob mob;
    private final EntityTracker tracker;
    private final Map<String, List<String>> animations;
    private final Map<String, Integer> rotation = new HashMap<>();

    private BetterModelBody(Mob mob, EntityTracker tracker, Map<String, List<String>> animations) {
        this.mob = mob;
        this.tracker = tracker;
        this.animations = animations;
    }

    /** Puts the named model on the mob, or returns null (and says why in the console) if that cannot be done. */
    static BetterModelBody tryCreate(Mob mob, String model, Map<String, List<String>> animations) {
        ModelRenderer renderer = BetterModel.modelOrNull(model);
        if (renderer == null) {
            BattlePlugin.getInstance().getLogger().warning("BetterModel has no model named '" + model
                + "' (put " + model + ".bbmodel in plugins/BetterModel/models and run /bettermodel reload). "
                + "The shikigami will be a plain mob.");
            return null;
        }
        try {
            EntityTracker tracker = renderer.create(BukkitAdapter.adapt(mob), TrackerModifier.DEFAULT, t -> { });
            return new BetterModelBody(mob, tracker, animations);
        } catch (RuntimeException e) {
            BattlePlugin.getInstance().getLogger().log(java.util.logging.Level.WARNING,
                "Could not put model '" + model + "' on a shikigami. It will be a plain mob.", e);
            return null;
        }
    }

    @Override
    public Mob mob() {
        return mob;
    }

    @Override
    public boolean providesNametag() {
        return tracker.bone("tag_name") != null;
    }

    @Override
    public void play(String animation) {
        if (tracker.isClosed()) {
            return;
        }
        List<String> options = animations.getOrDefault(animation, List.of(animation));
        if (options.isEmpty()) {
            return;
        }
        int index = rotation.merge(animation, 1, Integer::sum) - 1;
        tracker.animate(options.get(index % options.size()), AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
    }

    @Override
    public void remove() {
        // A normal ending kills the mob, and BetterModel then plays the model's death animation and closes it itself.
        // This is the safety net (and the only thing that runs on shutdown, when no timers can be set).
        Plugin plugin = BattlePlugin.getInstance();
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, this::closeNow, DEATH_GRACE_TICKS);
        } else {
            closeNow();
        }
    }

    private void closeNow() {
        if (!tracker.isClosed()) {
            tracker.close();
        }
    }
}
