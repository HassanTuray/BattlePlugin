package com.hfturay.battleplugin.ability.abilities.airabilities;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import com.hfturay.battleplugin.particleservice.ParticleInstance;
import com.hfturay.battleplugin.particleservice.ParticleSpec;
import com.hfturay.battleplugin.particleservice.PlayContext;

/**
 * A hollow dome of wind around the anchor: puffs of cloud spread over the surface of the upper half of a sphere, all
 * turning around the vertical axis (the higher up, the faster), with the odd large gust among them.
 * Plain data once built; every play gets its own running copy. Played at the caster's feet.
 */
final class AirDomeSpec implements ParticleSpec {

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    /** Radians the swirl turns per tick at the bottom of the dome. */
    private static final double SPIN = 0.18;

    /** One puff in this many is a big gust. */
    private static final int GUST_ONE_IN = 25;

    private final double radius;
    private final int count;

    /**
     * @param radius radius of the dome, in blocks
     * @param count  puffs drawn per tick
     */
    AirDomeSpec(double radius, int count) {
        this.radius = radius;
        this.count = Math.max(1, count);
    }

    @Override
    public ParticleInstance create(PlayContext context) {
        return new ParticleInstance() {
            @Override
            public void tick(int age) {
                Location at = context.anchor().location();
                if (at == null || at.getWorld() == null) {
                    return;
                }
                World world = at.getWorld();
                ThreadLocalRandom random = ThreadLocalRandom.current();
                int n = Math.max(1, (int) Math.round(count * context.density()));

                for (int i = 0; i < n; i++) {
                    double y = (i + 0.5) / n;                                   // 0 (ground) .. 1 (top of the dome)
                    double ring = Math.sqrt(Math.max(0, 1 - y * y));
                    double phi = i * GOLDEN_ANGLE + age * SPIN * (1 + y);

                    double x = at.getX() + Math.cos(phi) * ring * radius;
                    double z = at.getZ() + Math.sin(phi) * ring * radius;
                    double height = at.getY() + y * radius;

                    if (random.nextInt(GUST_ONE_IN) == 0) {
                        world.spawnParticle(Particle.GUST, x, height, z, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.CLOUD, x, height, z, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }

            @Override
            public void stop() {
            }
        };
    }
}
