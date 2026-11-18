package com.hfturay.battleplugin.ability.abilities.earthabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import com.hfturay.battleplugin.particleservice.ParticleInstance;
import com.hfturay.battleplugin.particleservice.ParticleSpec;
import com.hfturay.battleplugin.particleservice.PlayContext;

/**
 * Block-break crumbs spread over the surface of a sphere around the anchor, all turning around it. Each point shows
 * one of the given blocks (they take turns), and the upper half turns a little faster than the lower, so the sphere
 * looks like it is swirling and not just rotating.
 * Plain data once built: every play gets its own running copy.
 */
final class SwirlingBlocksSpec implements ParticleSpec {

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    /** Height of the sphere's centre above the anchor (the anchor is an entity's feet). */
    private static final double CENTER_HEIGHT = 1.0;

    private final BlockData[] blocks;
    private final int count;
    private final double radius;
    private final double spin;

    /**
     * @param materials the blocks to show
     * @param count     crumbs drawn per tick
     * @param radius    radius of the sphere, in blocks
     * @param spin      radians turned per tick
     */
    SwirlingBlocksSpec(List<Material> materials, int count, double radius, double spin) {
        this.blocks = materials.stream().map(Material::createBlockData).toArray(BlockData[]::new);
        this.count = Math.max(1, count);
        this.radius = radius;
        this.spin = spin;
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
                int n = Math.max(1, (int) Math.round(count * context.density()));

                for (int i = 0; i < n; i++) {
                    double y = 1 - 2 * (i + 0.5) / n;                       // -1 (bottom) .. 1 (top)
                    double ring = Math.sqrt(Math.max(0, 1 - y * y));
                    double phi = i * GOLDEN_ANGLE + age * spin * (1 + 0.6 * y);

                    world.spawnParticle(Particle.BLOCK,
                        at.getX() + Math.cos(phi) * ring * radius,
                        at.getY() + CENTER_HEIGHT + y * radius,
                        at.getZ() + Math.sin(phi) * ring * radius,
                        1, 0, 0, 0, 0, blocks[i % blocks.length]);
                }
            }

            @Override
            public void stop() {
            }
        };
    }
}
