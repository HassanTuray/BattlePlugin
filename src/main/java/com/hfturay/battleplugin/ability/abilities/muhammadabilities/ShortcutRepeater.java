package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ParticleEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ShortcutRepeater extends ScheduledAbilityTask {
    private Location location;
    private Shortcut ability;

    public ShortcutRepeater(int duration, Location location, Shortcut ability) {
        super(duration);

        this.location = location;
        this.ability = ability;
    }

    @Override
    public boolean update() {
        playParticleEffect();
        
        return super.update();
    }

    @Override
    public void run() {
        ability.getOwner().getPlayer().sendActionBar(Component.text("Shortcut expired", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        ability.resetShortcut();
    }  

    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        ParticleEffect effect = new ParticleEffect(effectManager);

        effect.setLocation(location);

        effect.particle = Particle.PORTAL;
        effect.particleCount = 10;

        effect.start();
    }
}
