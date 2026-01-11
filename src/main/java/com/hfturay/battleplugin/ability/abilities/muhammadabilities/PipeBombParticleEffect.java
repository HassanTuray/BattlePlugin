package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;

public class PipeBombParticleEffect implements ParticleEffectCallback {
    EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
    World world = BattlePlugin.getInstance().getWorld();

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        CylinderEffect pipeBomb = new CylinderEffect(effectManager);

        pipeBomb.setLocation(virtualProjectile.getLocation());
        pipeBomb.particle = Particle.DUST;
        pipeBomb.color = Color.WHITE;
        pipeBomb.particleSize = 0.2f;
        pipeBomb.particles = 100;
        pipeBomb.solid = true;

        pipeBomb.iterations = 3;
        pipeBomb.period = 1;

        pipeBomb.height = 0.5f;
        pipeBomb.radius = 0.1f;

        pipeBomb.start();

    }
}
