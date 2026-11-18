package com.hfturay.battleplugin.visualservice;

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
 * A beam: one thin block stretched from the anchor out along its direction, as long as the anchor's length() says.
 *
 * The anchor is the start (the muzzle). Each tick the beam is moved to it, turned to face the direction and
 * stretched to the current length, so a beam that is cut short by a wall or a target shortens with it.
 * It is a single entity, however long the beam is.
 *
 * spinning(degreesPerTick) makes the beam roll around its own length, like a drill bit turning. It only shows if
 * the beam is not a perfect square, because a square looks the same at every angle: use of(material, width, height).
 */
public final class BeamModel implements VisualSpec {

    /** A beam shorter than this is drawn this long, so it never collapses to nothing. */
    private static final double MIN_LENGTH = 0.05;
    private static final double LENGTH_EPSILON = 0.005;

    private final BlockData block;
    private final double width;
    private final double height;
    private final double spinRadiansPerTick;

    private BeamModel(BlockData block, double width, double height, double spinRadiansPerTick) {
        this.block = block;
        this.width = width;
        this.height = height;
        this.spinRadiansPerTick = spinRadiansPerTick;
    }

    /** @param thickness width and height of the beam in blocks */
    public static BeamModel of(Material material, double thickness) {
        return of(material.createBlockData(), thickness, thickness);
    }

    public static BeamModel of(BlockData block, double thickness) {
        return of(block, thickness, thickness);
    }

    /** A beam that is not square: width across and height up, in blocks, when the beam is not rolled. */
    public static BeamModel of(Material material, double width, double height) {
        return of(material.createBlockData(), width, height);
    }

    public static BeamModel of(BlockData block, double width, double height) {
        if (!(width > 0) || !(height > 0)) {
            throw new IllegalArgumentException("A beam needs a width and height above zero");
        }
        return new BeamModel(block, width, height, 0);
    }

    /** The same beam, rolling around its own length by this many degrees every tick (20 ticks = 1 second). */
    public BeamModel spinning(double degreesPerTick) {
        return new BeamModel(block, width, height, Math.toRadians(degreesPerTick));
    }

    // ---- the maths, kept separate so it can be checked without a server ----

    /** The entity transformation for a square beam of this length, not rolled. */
    public static Transformation transformation(BlockData block, double thickness, double length, Vector direction) {
        return transformation(thickness, thickness, length, direction, 0);
    }

    /**
     * The transformation for a beam: width x height x length, starting at the anchor and pointing along the
     * direction, rolled by `roll` radians around its own length.
     *
     * A block display draws its block from the entity's position outward, so it is shifted back by half its width
     * and height to put the beam's middle on the axis. The shift is turned together with the beam, so the beam
     * rolls around its own centre line however it is rolled.
     */
    public static Transformation transformation(double width, double height, double length, Vector direction, double roll) {
        double clamped = Double.isFinite(length) ? Math.max(MIN_LENGTH, length) : MIN_LENGTH;
        Quaternionf rotation = BlockModel.orientation(direction).rotateZ((float) roll);
        Vector3f size = new Vector3f((float) width, (float) height, (float) clamped);
        Vector3f offset = new Vector3f(-size.x / 2f, -size.y / 2f, 0f);
        rotation.transform(offset);
        return new Transformation(offset, rotation, size, new Quaternionf());
    }

    // ---- spawning ----

    @Override
    public VisualInstance create(VisualContext context) {
        World world = context.at().getWorld();
        Location position = flat(context.at());
        Transformation transformation = transformation(width, height, context.length(), context.direction(), 0);

        BlockDisplay display = world.spawn(position, BlockDisplay.class, entity -> {
            entity.setBlock(block);
            entity.setTransformation(transformation);
            entity.setBrightness(new Display.Brightness(15, 15));    // glows, not dark in caves or at night
            entity.setTeleportDuration(1);
            entity.setInterpolationDuration(1);
            entity.setShadowRadius(0f);
            entity.setPersistent(false);                             // never saved with the world
            context.tagger().accept(entity);
        });
        return new Instance(display, context.direction(), context.length());
    }

    private static Location flat(Location at) {
        return new Location(at.getWorld(), at.getX(), at.getY(), at.getZ(), 0f, 0f);
    }

    private final class Instance implements VisualInstance {
        private final BlockDisplay display;
        private Vector facing;
        private double lastLength;
        private double roll = 0;

        Instance(BlockDisplay display, Vector direction, double length) {
            this.display = display;
            this.facing = direction.clone().normalize();
            this.lastLength = length;
        }

        @Override
        public void update(Location at, Vector direction) {
            update(at, direction, lastLength);
        }

        @Override
        public void update(Location at, Vector direction, double length) {
            display.teleport(flat(at));

            Vector now = direction.clone();
            boolean turned = now.lengthSquared() > 1.0E-9 && now.normalize().dot(facing) < 0.99999;
            boolean resized = Math.abs(length - lastLength) > LENGTH_EPSILON;
            boolean spinning = spinRadiansPerTick != 0;

            if (turned || resized || spinning) {
                if (turned) {
                    facing = now;
                }
                lastLength = length;
                if (spinning) {
                    // keep the angle small so it never grows without limit
                    roll = (roll + spinRadiansPerTick) % (2 * Math.PI);
                    display.setInterpolationDelay(0);     // start gliding to the new angle straight away
                }
                display.setTransformation(transformation(width, height, length, facing, roll));
            }
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
