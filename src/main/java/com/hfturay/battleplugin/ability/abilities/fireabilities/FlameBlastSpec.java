package com.hfturay.battleplugin.ability.abilities.fireabilities;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.particleservice.ParticleInstance;
import com.hfturay.battleplugin.particleservice.ParticleSpec;
import com.hfturay.battleplugin.particleservice.PlayContext;

/**
 * A wave of flames scattering out from the anchor along the blast's direction: each tick a band of fire appears across
 * the full width, one step further out than the last, until it reaches the end of the blast. Plain data once built;
 * every play gets its own running copy. Played at the caster's body centre.
 */
final class FlameBlastSpec implements ParticleSpec {

    /** How far above and below the blast's centre line the scattered fire reaches, at most. */
    private static final double SCATTER_HEIGHT = 1.25;

    private final Vector forward;     // the three axes are unit length and perpendicular to each other
    private final Vector right;
    private final Vector up;
    private final double range;
    private final double width;
    private final double height;
    private final int count;
    private final int smokeCount;
    private final double speed;

    /** The burst of smoke: how far in front of the caster it starts, and how fast its puffs fly outward. */
    private static final double SMOKE_DISTANCE = 2.5;
    private static final double SMOKE_SPEED = 0.25;

    /** The crumbs of magma mixed in with the flames. */
    private static final BlockData MAGMA = Material.MAGMA_BLOCK.createBlockData();

    /**
     * @param forward the way the blast goes (where the caster looks)
     * @param right   sideways to the blast
     * @param up      perpendicular to both
     * @param range   how far out the flames reach
     * @param width   how far to each side they spread
     * @param height  the most the fire may spread along `up` (capped at a body's height, so it stays a flat sheet)
     * @param count   flames drawn per tick
     * @param smokeCount puffs in the opening smoke burst
     * @param speed   blocks the wave front advances per tick
     */
    FlameBlastSpec(Vector forward, Vector right, Vector up, double range, double width, double height, int count, int smokeCount, double speed) {
        this.forward = forward.clone();
        this.right = right.clone();
        this.up = up.clone();
        this.range = range;
        this.width = width;
        this.height = Math.min(height, SCATTER_HEIGHT);
        this.count = Math.max(1, count);
        this.smokeCount = Math.max(0, smokeCount);
        this.speed = Math.max(0.1, speed);
    }

    @Override
    public ParticleInstance create(PlayContext context) {
        int frames = (int) Math.ceil(range / speed);

        return new ParticleInstance() {
            private int age = 0;

            @Override
            public void tick(int tick) {
                age = tick + 1;
                Location at = context.anchor().location();
                if (at == null || at.getWorld() == null) {
                    return;
                }
                World world = at.getWorld();
                ThreadLocalRandom random = ThreadLocalRandom.current();

                if (tick == 0) {
                    burstSmoke(world, at, random);
                }
                int n = Math.max(1, (int) Math.round(count * context.density()));

                // The band the wave front covers this tick, so the blast looks like it is thrown outward.
                double from = tick * speed;
                double to = Math.min(range, (tick + 1) * speed);

                for (int i = 0; i < n; i++) {
                    double along = from + random.nextDouble() * (to - from);
                    double across = (random.nextDouble() * 2 - 1) * width;
                    double lift = (random.nextDouble() * 2 - 1) * height;

                    double x = at.getX() + forward.getX() * along + right.getX() * across + up.getX() * lift;
                    double y = at.getY() + forward.getY() * along + right.getY() * across + up.getY() * lift;
                    double z = at.getZ() + forward.getZ() * along + right.getZ() * across + up.getZ() * lift;
                    int kind = random.nextInt(6);
                    if (kind <= 1) {
                        world.spawnParticle(Particle.BLOCK, x, y, z, 1, 0.1, 0.1, 0.1, 0.05, MAGMA);
                    } else if (kind == 2) {
                        world.spawnParticle(Particle.SMOKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
                    } else {
                        world.spawnParticle(Particle.FLAME, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }

            /** A small ball of campfire smoke in front of the caster that bursts outward in every direction. */
            private void burstSmoke(World world, Location at, ThreadLocalRandom random) {
                double cx = at.getX() + forward.getX() * SMOKE_DISTANCE;
                double cy = at.getY() + forward.getY() * SMOKE_DISTANCE;
                double cz = at.getZ() + forward.getZ() * SMOKE_DISTANCE;
                int n = (int) Math.round(smokeCount * context.density());

                for (int i = 0; i < n; i++) {
                    // a random direction on the unit sphere; with count 0 the offset is the puff's velocity
                    double y = random.nextDouble() * 2 - 1;
                    double ring = Math.sqrt(1 - y * y);
                    double angle = random.nextDouble() * Math.PI * 2;
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, cx, cy, cz, 0,
                        Math.cos(angle) * ring, y, Math.sin(angle) * ring, SMOKE_SPEED);
                }
            }

            @Override
            public boolean isFinished() {
                return age >= frames;
            }

            @Override
            public void stop() {
            }
        };
    }
}
