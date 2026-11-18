package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.particleservice.ParticleHandle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ShortcutRepeater extends ScheduledAbilityTask {
    private Shortcut ability;

    // The marker is started once and ends with the shortcut (expiry, or cancel() when it is used).
    private final ParticleHandle marker;

    public ShortcutRepeater(int duration, Location location, Shortcut ability) {
        super(duration, ability);

        this.ability = ability;
        this.marker = BattlePlugin.getInstance().getParticleService()
            .zone(ability.getOwner(), MuhammadParticles.SHORTCUT_MARKER, location, duration);
    }

    @Override
    protected void onCancel() {
        marker.cancel();
        ability.resetShortcut();
    }

    @Override
    protected void onFinish() {
        marker.cancel();
        ability.getOwner().getPlayer().sendActionBar(Component.text("Shortcut expired", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        ability.resetShortcut();
    }
}
