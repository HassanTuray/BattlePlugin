package com.hfturay.battleplugin.ability.abilities.aliabilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.projectileservice.ParticleEffectCallback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CubeEffect;
import de.slikey.effectlib.effect.ParticleEffect;

public class PharahRocketParticleEffect implements ParticleEffectCallback {

    @Override
    public void playParticleEffect(VirtualProjectile virtualProjectile) {
        Location projectileLocation = virtualProjectile.getLocation();

        rocketNose(projectileLocation);
        rocketBody(projectileLocation, virtualProjectile);
        rocketFlames(projectileLocation, virtualProjectile);
    }

    private void rocketNose(Location projectileLocation) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        CubeEffect nose = new CubeEffect(effectManager);
        
        nose.setLocation(projectileLocation);

        nose.particle = Particle.DUST;
        nose.color = Color.RED;
        nose.particles = 5;
        nose.particleSize = 0.5f;

        nose.outlineOnly = false;
        nose.orient = true;
        nose.enableRotation = false;
        nose.edgeLength = 0.2f;

        nose.period = 1;
        nose.iterations = 1;

        nose.start();
    }

    private void rocketBody(Location projectileLocation, VirtualProjectile virtualProjectile) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        float edge = 0.4f;
        Vector forward = virtualProjectile.getDirection().clone().normalize();

        double[] offsets = {-0.2f, edge * -1, edge * -2};

        for (double offset : offsets) {
            Location cubeLocation = projectileLocation.clone()
            .add(forward.clone().multiply(offset));

            CubeEffect body = new CubeEffect(effectManager);
        
            body.setLocation(cubeLocation);

            body.particle = Particle.DUST;
            body.color = Color.WHITE;
            body.particles = 10;
            body.particleSize = 0.5f;

            body.outlineOnly = false;
            body.orient = true;
            body.enableRotation = false;
            body.edgeLength = edge;

            body.period = 1;
            body.iterations = 1;

            body.start();
        }
    }

    private void rocketFlames(Location projectileLocation, VirtualProjectile virtualProjectile) {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        ParticleEffect flame = new ParticleEffect(effectManager);

        Vector forward = virtualProjectile.getDirection().clone().normalize();

        flame.particle = Particle.FLAME;
        flame.particleCount = 5;

        flame.period = 1;
        flame.iterations = 1;

        Location flameLocation = projectileLocation.clone().add(forward.clone().multiply(-1.1));
        flame.setLocation(flameLocation);
        flame.start();
    }
}
