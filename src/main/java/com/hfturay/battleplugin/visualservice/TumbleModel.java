package com.hfturay.battleplugin.visualservice;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One block that tumbles through the air, like a thrown brick: it turns on all three of its own axes at once,
 * and the turning speed drifts smoothly on each axis, so no two throws look the same and the motion never settles
 * into an obvious loop.
 *
 * The block is a box of (width x height x length) with its length along the direction of travel when it is not
 * turned. It stays centred on the anchor while it turns. Each play of the visual gets its own random start angles
 * and its own drift, so two bricks in the air tumble differently.
 *
 * Each tick it moves one entity and rewrites that entity's transformation. The client smooths between ticks.
 */
public final class TumbleModel implements VisualSpec {

    private final BlockData block;
    private final double sizeX;      // width: the axis it tumbles end over end around
    private final double sizeY;      // height
    private final double sizeZ;      // length: along the direction of travel
    private final double spinX;      // radians per tick on each of the block's own axes
    private final double spinY;
    private final double spinZ;
    private final double wobble;     // 0..1: how far the spin speed drifts above and below its base value

    private TumbleModel(BlockData block, double sizeX, double sizeY, double sizeZ,
                        double spinX, double spinY, double spinZ, double wobble) {
        this.block = block;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.spinX = spinX;
        this.spinY = spinY;
        this.spinZ = spinZ;
        this.wobble = wobble;
    }

    /** A block of width x height x length (in blocks) that does not turn yet. Add tumbling(...). */
    public static TumbleModel of(Material material, double width, double height, double length) {
        return of(material.createBlockData(), width, height, length);
    }

    public static TumbleModel of(BlockData block, double width, double height, double length) {
        if (!(width > 0) || !(height > 0) || !(length > 0)) {
            throw new IllegalArgumentException("A tumbling block needs a width, height and length above zero");
        }
        return new TumbleModel(block, width, height, length, 0, 0, 0, 0);
    }

    /**
     * The same block, turning this many degrees every tick (20 ticks = 1 second) around each of its own axes:
     * x is end over end, y is a flat spin like a frisbee, z is a roll along its length.
     *
     * @param wobble how much each speed drifts, as a fraction of itself: 0.35 means each speed wanders between
     *               65% and 135% of the number given, smoothly, and differently for every throw
     */
    public TumbleModel tumbling(double degreesPerTickX, double degreesPerTickY, double degreesPerTickZ, double wobble) {
        return new TumbleModel(block, sizeX, sizeY, sizeZ,
            Math.toRadians(degreesPerTickX), Math.toRadians(degreesPerTickY), Math.toRadians(degreesPerTickZ),
            Math.max(0, Math.min(1, wobble)));
    }

    // ---- the maths, kept separate so it can be checked without a server ----

    /** The block's turn: face the direction of travel, then tumble by these angles on its own axes. */
    public static Quaternionf rotation(Vector direction, double angleX, double angleY, double angleZ) {
        return BlockModel.orientation(direction)
            .rotateX((float) angleX)
            .rotateY((float) angleY)
            .rotateZ((float) angleZ);
    }

    /** The entity transformation that turns a box of this size about its own middle, which stays on the anchor. */
    public static Transformation transformation(double sizeX, double sizeY, double sizeZ, Quaternionf rotation) {
        Vector3f size = new Vector3f((float) sizeX, (float) sizeY, (float) sizeZ);
        // A block display draws from the entity's position outward, so shift back by half the (turned) size.
        Vector3f offset = new Vector3f(size).mul(-0.5f);
        rotation.transform(offset);
        return new Transformation(offset, new Quaternionf(rotation), size, new Quaternionf());
    }

    /**
     * One throw's tumble: where it starts, and how its speed drifts. Each axis speed is
     * base * (1 + wobble * (sine A + sine B) / 2), where the two sines have their own random speed and starting
     * point, so the drift is smooth, never repeats exactly, and stays inside base * (1 +/- wobble).
     */
    public static final class Motion {
        private final double[] base;
        private final double wobble;
        private final double[] angle = new double[3];
        private final double[] phase1 = new double[3], phase2 = new double[3];
        private final double[] rate1 = new double[3], rate2 = new double[3];
        private int tick = 0;

        public Motion(double spinX, double spinY, double spinZ, double wobble, Random random) {
            this.base = new double[] { spinX, spinY, spinZ };
            this.wobble = wobble;
            // starts at a random end-over-end angle, and a slightly different tilt each time
            angle[0] = random.nextDouble() * 2 * Math.PI;
            angle[1] = (random.nextDouble() - 0.5) * 1.0;
            angle[2] = (random.nextDouble() - 0.5) * 1.0;
            for (int i = 0; i < 3; i++) {
                phase1[i] = random.nextDouble() * 2 * Math.PI;
                phase2[i] = random.nextDouble() * 2 * Math.PI;
                rate1[i] = 0.05 + random.nextDouble() * 0.10;     // radians of drift per tick: a slow wander,
                rate2[i] = 0.11 + random.nextDouble() * 0.17;     // and a quicker one on top of it
            }
        }

        /** The speed on an axis this tick, in radians per tick. */
        public double speed(int axis, int atTick) {
            double drift = (Math.sin(phase1[axis] + rate1[axis] * atTick) + Math.sin(phase2[axis] + rate2[axis] * atTick)) / 2.0;
            return base[axis] * (1.0 + wobble * drift);
        }

        /** Moves on one tick. */
        public void step() {
            tick++;
            for (int i = 0; i < 3; i++) {
                angle[i] = (angle[i] + speed(i, tick)) % (2 * Math.PI);
            }
        }

        public double angle(int axis) {
            return angle[axis];
        }
    }

    // ---- spawning ----

    @Override
    public VisualInstance create(VisualContext context) {
        World world = context.at().getWorld();
        Location position = flat(context.at());
        Motion motion = new Motion(spinX, spinY, spinZ, wobble, ThreadLocalRandom.current());
        Transformation start = transformation(sizeX, sizeY, sizeZ,
            rotation(context.direction(), motion.angle(0), motion.angle(1), motion.angle(2)));

        BlockDisplay display = world.spawn(position, BlockDisplay.class, entity -> {
            entity.setBlock(block);
            entity.setTransformation(start);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setTeleportDuration(1);
            entity.setInterpolationDuration(1);
            entity.setShadowRadius(0f);
            entity.setPersistent(false);                             // never saved with the world
            context.tagger().accept(entity);
        });
        return new Instance(display, motion, context.direction());
    }

    private static Location flat(Location at) {
        return new Location(at.getWorld(), at.getX(), at.getY(), at.getZ(), 0f, 0f);
    }

    private final class Instance implements VisualInstance {
        private final BlockDisplay display;
        private final Motion motion;
        private Vector facing;

        Instance(BlockDisplay display, Motion motion, Vector direction) {
            this.display = display;
            this.motion = motion;
            this.facing = direction.lengthSquared() > 1.0E-9 ? direction.clone().normalize() : new Vector(0, 0, 1);
        }

        @Override
        public void update(Location at, Vector direction) {
            display.teleport(flat(at));

            if (direction.lengthSquared() > 1.0E-9) {
                facing = direction.clone().normalize();
            }
            motion.step();
            display.setInterpolationDelay(0);      // start gliding to the new angle straight away
            display.setTransformation(transformation(sizeX, sizeY, sizeZ,
                rotation(facing, motion.angle(0), motion.angle(1), motion.angle(2))));
        }

        @Override
        public boolean isValid() {
            return display.isValid();
        }

        @Override
        public void remove() {
            try {
                display.remove();
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
    }
}
