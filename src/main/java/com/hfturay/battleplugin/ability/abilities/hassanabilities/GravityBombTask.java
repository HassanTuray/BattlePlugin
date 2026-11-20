package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.visualservice.TumbleModel;
import com.hfturay.battleplugin.targetingservice.Targets;

/**
 * One Explosive Gravity Bomb. The black hole hangs where it was made. Every tick it pulls in everything within
 * pullRadius and it shrinks in a straight line from startRadius to endRadius; when it reaches endRadius it explodes:
 * everything within blastRadius is thrown away from it (harder the closer it is) and everything within `radius` is hurt.
 * The caster, their allies' shikigami and anything untargetable are not pulled, thrown or hurt.
 */
final class GravityBombTask extends ScheduledAbilityTask {

    /** The numbers, from abilities.yml. damage is health points (2 = one heart). */
    record Settings(int durationTicks, double startRadius, double endRadius, double pullRadius, double pullForce,
                    double radius, double blastRadius, double damage, double knockback) { }

    /** Fastest an entity is allowed to be dragged in, in blocks per tick. */
    private static final double MAX_PULL_SPEED = 1.2;

    /** Closer than this to the middle the pull stops, so things hover there instead of jittering through it. */
    private static final double HOVER_DISTANCE = 1.0;

    private static final Particle.DustOptions HOLE = new Particle.DustOptions(Color.fromRGB(8, 0, 16), 2.0f);
    private static final Particle.DustOptions RIM = new Particle.DustOptions(Color.fromRGB(120, 40, 200), 1.2f);
    private static final Particle.DustOptions DISC = new Particle.DustOptions(Color.fromRGB(255, 140, 40), 1.0f);

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    // ---- the blocks swirling in ----

    private static final Material[] DEBRIS = {
        Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
        Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK, Material.BASALT, Material.COAL_BLOCK };
    private static final int MAX_DEBRIS = 60;
    private static final int DEBRIS_PER_TICK = 2;
    private static final double DEBRIS_MIN_START = 8.0;
    private static final double DEBRIS_MAX_START = 16.0;
    private static final int NO_NEW_DEBRIS_LAST_TICKS = 15;

    /** One block spiralling in. It is its own entity and is removed the moment it touches the middle. */
    private static final class Debris {
        final BlockDisplay display;
        final double startDistance;
        final double startHeight;
        final double size;
        final Vector spinAxis;
        final double spinSpeed;
        double distance;
        double angle;
        double spin = 0;

        Debris(BlockDisplay display, double distance, double angle, double height, double size, Vector spinAxis, double spinSpeed) {
            this.display = display;
            this.startDistance = distance;
            this.distance = distance;
            this.angle = angle;
            this.startHeight = height;
            this.size = size;
            this.spinAxis = spinAxis;
            this.spinSpeed = spinSpeed;
        }
    }

    private final List<Debris> debris = new ArrayList<>();
    private final BattlePlayer owner;
    private final Location ball;
    private final Settings settings;
    private int age = 0;

    GravityBombTask(Ability ability, BattlePlayer owner, Location ball, Settings settings) {
        super(settings.durationTicks(), ability);
        this.owner = owner;
        this.ball = ball.clone();
        this.settings = settings;

        ball.getWorld().playSound(ball, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 3.0f, 0.6f);
    }

    /** How big the black hole is this tick: a straight shrink from the start size to the end size. */
    private double radiusNow() {
        double progress = Math.min(1.0, age / (double) Math.max(1, settings.durationTicks()));
        return settings.startRadius() + (settings.endRadius() - settings.startRadius()) * progress;
    }

    @Override
    protected void onTick() {
        World world = ball.getWorld();
        age++;

        pull();
        draw(world, radiusNow());
        swirlDebris(world, radiusNow());

        if (age % 10 == 0) {       // a heartbeat that quickens as it shrinks
            float pitch = 0.5f + 1.2f * (age / (float) Math.max(1, settings.durationTicks()));
            world.playSound(ball, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 3.0f, pitch);
        }
    }

    @Override
    protected void onFinish() {
        removeDebris();
        explode();
    }

    @Override
    protected void onCancel() {
        removeDebris();
    }

    // ---- the swirling blocks ----

    /** Spawns a few more blocks far out, and moves every block one step along its spiral in. */
    private void swirlDebris(World world, double holeRadius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (getTicksRemaining() > NO_NEW_DEBRIS_LAST_TICKS) {
            for (int i = 0; i < DEBRIS_PER_TICK && debris.size() < MAX_DEBRIS; i++) {
                double distance = DEBRIS_MIN_START + random.nextDouble() * (DEBRIS_MAX_START - DEBRIS_MIN_START);
                double height = (random.nextDouble() - 0.5) * 12;
                double size = 0.4 + random.nextDouble() * 0.7;
                double angle = random.nextDouble() * Math.PI * 2;
                Material material = DEBRIS[random.nextInt(DEBRIS.length)];
                Vector axis = new Vector(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
                if (axis.lengthSquared() < 1.0E-6) {
                    axis = new Vector(0, 1, 0);
                }

                Location start = place(distance, angle, height);
                BlockDisplay display = world.spawn(start, BlockDisplay.class, entity -> {
                    entity.setBlock(material.createBlockData());
                    entity.setBrightness(new Display.Brightness(12, 12));
                    entity.setTeleportDuration(2);
                    entity.setInterpolationDuration(2);
                    entity.setShadowRadius(0f);
                    entity.setPersistent(false);                       // never saved with the world
                });
                debris.add(new Debris(display, distance, angle, height, size, axis.normalize(), 0.15 + random.nextDouble() * 0.25));
            }
        }

        Iterator<Debris> each = debris.iterator();
        while (each.hasNext()) {
            Debris block = each.next();
            if (!block.display.isValid()) {
                each.remove();
                continue;
            }

            double closeness = 1.0 - block.distance / block.startDistance;           // 0 far out, 1 at the middle
            block.distance -= 0.10 + 0.55 * closeness * closeness;                    // falls faster as it gets close
            block.angle += 0.10 + 0.55 * closeness;                                   // and swirls faster
            block.spin += block.spinSpeed;

            if (block.distance <= Math.max(holeRadius, 0.6)) {                        // it touched the middle
                block.display.remove();
                each.remove();
                world.spawnParticle(Particle.PORTAL, ball, 6, 0.3, 0.3, 0.3, 0.2);
                continue;
            }

            double height = block.startHeight * (block.distance / block.startDistance);   // the swirl funnels down to the middle
            block.display.teleport(place(block.distance, block.angle, height));
            block.display.setInterpolationDelay(0);
            block.display.setTransformation(TumbleModel.transformation(block.size, block.size, block.size,
                new Quaternionf().rotateAxis((float) block.spin,
                    (float) block.spinAxis.getX(), (float) block.spinAxis.getY(), (float) block.spinAxis.getZ())));
        }
    }

    /** The spot at this distance, angle round the middle and height from it. */
    private Location place(double distance, double angle, double height) {
        return new Location(ball.getWorld(), ball.getX() + Math.cos(angle) * distance, ball.getY() + height,
            ball.getZ() + Math.sin(angle) * distance);
    }

    private void removeDebris() {
        for (Debris block : debris) {
            try {
                block.display.remove();
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
        debris.clear();
    }

    // ---- the pull ----

    private void pull() {
        double reach = settings.pullRadius();
        for (Entity entity : ball.getWorld().getNearbyEntities(ball, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity living) || !Targets.isTargetable(owner, living)) {
                continue;
            }
            Vector toBall = ball.toVector().subtract(living.getLocation().add(0, living.getHeight() / 2, 0).toVector());
            double distance = toBall.length();
            if (distance > reach || distance < HOVER_DISTANCE) {
                continue;
            }

            Vector velocity = living.getVelocity().add(toBall.multiply(settings.pullForce() / distance));
            if (velocity.length() > MAX_PULL_SPEED) {
                velocity.normalize().multiply(MAX_PULL_SPEED);
            }
            Knockback.set(living, velocity);
        }
    }

    // ---- the explosion ----

    private void explode() {
        World world = ball.getWorld();

        world.spawnParticle(Particle.EXPLOSION_EMITTER, ball, 3, 1.5, 1.5, 1.5, 0);
        world.spawnParticle(Particle.FLASH, ball, 1);
        world.spawnParticle(Particle.CLOUD, ball, 250, 2.5, 2.5, 2.5, 1.2);
        world.spawnParticle(Particle.REVERSE_PORTAL, ball, 200, 1.5, 1.5, 1.5, 1.5);
        world.playSound(ball, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 5.0f, 0.6f);
        world.playSound(ball, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 5.0f, 0.5f);

        double reach = settings.blastRadius();
        for (Entity entity : world.getNearbyEntities(ball, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity living) || !Targets.isTargetable(owner, living)) {
                continue;
            }
            Location at = living.getLocation().add(0, living.getHeight() / 2, 0);
            double distance = at.distance(ball);
            if (distance > reach) {
                continue;
            }

            if (distance <= settings.radius()) {
                BattlePlugin.getInstance().getDamageService().damage(owner, living, settings.damage());
            }
            // thrown away from the middle: full strength up close, a third of it at the edge of the blast
            double strength = settings.knockback() * (1.0 - 0.66 * distance / reach);
            Knockback.away(ball, living, strength);
        }
    }

    // ---- drawing ----

    /** A dark sphere with a violet rim, a spinning orange ring around its middle, and light streaming into it. */
    private void draw(World world, double radius) {
        int points = (int) Math.min(400, 14 + 22 * radius * radius);
        for (int i = 0; i < points; i++) {
            double y = 1 - 2 * (i + 0.5) / points;
            double ring = Math.sqrt(Math.max(0, 1 - y * y));
            double angle = i * GOLDEN_ANGLE + age * 0.05;
            Particle.DustOptions colour = i % 5 == 0 ? RIM : HOLE;
            world.spawnParticle(Particle.DUST, ball.getX() + Math.cos(angle) * ring * radius, ball.getY() + y * radius,
                ball.getZ() + Math.sin(angle) * ring * radius, 1, 0, 0, 0, 0, colour);
        }

        // the ring of hot matter spinning round it, tipped a little
        int beads = (int) Math.min(120, 24 + 16 * radius);
        double discRadius = radius * 1.8 + 0.4;
        for (int i = 0; i < beads; i++) {
            double angle = i * (2 * Math.PI / beads) + age * 0.25;
            double x = Math.cos(angle) * discRadius;
            double z = Math.sin(angle) * discRadius;
            double tilt = 0.35;
            world.spawnParticle(Particle.DUST, ball.getX() + x, ball.getY() + z * Math.sin(tilt),
                ball.getZ() + z * Math.cos(tilt), 1, 0, 0, 0, 0, DISC);
        }

        // portal particles are drawn in towards where they are spawned: matter falling into the hole
        world.spawnParticle(Particle.PORTAL, ball, 40, 6, 6, 6, 0.6);
    }
}
