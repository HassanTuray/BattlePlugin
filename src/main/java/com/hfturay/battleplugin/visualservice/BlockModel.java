package com.hfturay.battleplugin.visualservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * A shape built from stretched blocks (a rocket, a missile, a spear) shown with BlockDisplay entities.
 *
 * Parts are described in the model's own space, where +z is the way the model points:
 * the anchor is at z = 0 and a part at z = -1 sits one block behind it. Each part is a block stretched to
 * (sizeX, sizeY, sizeZ) blocks and centred on (x, y, z). Everything is multiplied by the model's scale.
 *
 * Every part is its own entity, all placed at the anchor. The shape is turned to face the direction of travel with
 * each part's transformation, which is only rewritten when the direction changes. Each tick only moves the
 * entities, and the client smooths the movement.
 */
public final class BlockModel implements VisualSpec {

    /**
     * One stretched block. Sizes are in blocks before scaling.
     *
     * @param tiltDegrees leans the part about its own centre around the model's x axis (left to right): a positive
     *                    angle tips the top of the part forward, towards +z. 0 leaves it upright.
     */
    public record Part(BlockData block, double x, double y, double z, double sizeX, double sizeY, double sizeZ, double tiltDegrees) {

        public Part(BlockData block, double x, double y, double z, double sizeX, double sizeY, double sizeZ) {
            this(block, x, y, z, sizeX, sizeY, sizeZ, 0);
        }
    }

    private final List<Part> parts;
    private final double scale;
    private final boolean level;

    private BlockModel(List<Part> parts, double scale, boolean level) {
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
        this.scale = scale;
        this.level = level;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Part> parts() {
        return parts;
    }

    public double scale() {
        return scale;
    }

    // ---- the maths, kept separate so it can be checked without a server ----

    /** The rotation that turns the model's +z axis to point along the direction. */
    public static Quaternionf orientation(Vector direction) {
        double length = direction.length();
        if (!(length > 1.0E-9) || Double.isInfinite(length)) {
            return new Quaternionf();
        }
        return new Quaternionf().rotationTo(0f, 0f, 1f,
            (float) (direction.getX() / length), (float) (direction.getY() / length), (float) (direction.getZ() / length));
    }

    /**
     * The entity transformation that places a part. A block display draws its block from the entity's position
     * outward, not around it, so the part is shifted back by half its size before the whole model is turned.
     */
    public static Transformation transformation(Part part, double scale, Vector direction) {
        Quaternionf orientation = orientation(direction);
        Quaternionf tilt = new Quaternionf().rotationX((float) Math.toRadians(part.tiltDegrees()));
        Vector3f size = new Vector3f((float) (part.sizeX() * scale), (float) (part.sizeY() * scale), (float) (part.sizeZ() * scale));

        // The block is scaled in its own axes, tilted about its own centre, then the whole model is turned.
        // So the block's corner (the entity origin) sits at: centre - tilt * (size / 2), all turned by the orientation.
        Vector3f halfSize = new Vector3f(size).mul(0.5f);
        tilt.transform(halfSize);
        Vector3f offset = new Vector3f(
            (float) (part.x() * scale) - halfSize.x,
            (float) (part.y() * scale) - halfSize.y,
            (float) (part.z() * scale) - halfSize.z);
        orientation.transform(offset);

        Quaternionf rotation = new Quaternionf(orientation).mul(tilt);
        return new Transformation(offset, rotation, size, new Quaternionf());
    }

    // ---- spawning ----

    @Override
    public VisualInstance create(VisualContext context) {
        World world = context.at().getWorld();
        Location position = flat(context.at());
        List<BlockDisplay> spawned = new ArrayList<>(parts.size());
        Vector direction = facingOf(context.direction());

        try {
            for (Part part : parts) {
                Transformation transformation = transformation(part, scale, direction);
                BlockDisplay display = world.spawn(position, BlockDisplay.class, entity -> {
                    entity.setBlock(part.block());
                    entity.setTransformation(transformation);
                    entity.setBrightness(new Display.Brightness(15, 15));    // not dark in caves or at night
                    entity.setTeleportDuration(1);                           // glide between ticks
                    entity.setInterpolationDuration(1);                      // and when it turns
                    entity.setShadowRadius(0f);
                    entity.setPersistent(false);                             // never saved with the world
                    context.tagger().accept(entity);
                });
                spawned.add(display);
            }
        } catch (RuntimeException e) {
            removeAll(spawned);
            throw e;
        }

        return new Instance(spawned, direction);
    }

    /** The direction the model faces: the anchor's direction, flattened to the ground when the model is level. */
    private Vector facingOf(Vector direction) {
        Vector facing = direction.clone();
        if (level) {
            facing.setY(0);
        }
        return facing;
    }

    /** The location with no rotation: the entity's own turn is left alone, the transformation does all the turning. */
    private static Location flat(Location at) {
        return new Location(at.getWorld(), at.getX(), at.getY(), at.getZ(), 0f, 0f);
    }

    private static void removeAll(List<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            try {
                display.remove();
            } catch (RuntimeException ignored) {
                // keep removing the rest
            }
        }
    }

    private final class Instance implements VisualInstance {
        private final List<BlockDisplay> displays;
        private Vector facing;

        Instance(List<BlockDisplay> displays, Vector direction) {
            this.displays = displays;
            // a model aimed straight up (level) has no left-right direction: face forward until it has one
            this.facing = direction.lengthSquared() > 1.0E-9 ? direction.clone().normalize() : new Vector(0, 0, 1);
        }

        @Override
        public void update(Location at, Vector direction) {
            Location position = flat(at);
            for (BlockDisplay display : displays) {
                display.teleport(position);
            }

            Vector now = facingOf(direction);
            if (now.lengthSquared() > 1.0E-9 && now.normalize().dot(facing) < 0.99999) {
                facing = now;
                for (int i = 0; i < displays.size(); i++) {
                    displays.get(i).setTransformation(transformation(parts.get(i), scale, facing));
                }
            }
        }

        @Override
        public boolean isValid() {
            for (BlockDisplay display : displays) {
                if (!display.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void remove() {
            removeAll(displays);
        }
    }

    // ---- builder ----

    public static final class Builder {
        private final List<Part> parts = new ArrayList<>();
        private double scale = 1.0;
        private boolean level = false;

        /** Multiplies every size and position. */
        public Builder scale(double scale) {
            this.scale = scale;
            return this;
        }

        /**
         * Keeps the model upright: it turns left and right to face where the anchor travels, but never tips up or
         * down with it (a wall that should stand on the ground however high or low it was aimed).
         */
        public Builder level() {
            this.level = true;
            return this;
        }

        public Builder part(Material material, double x, double y, double z, double sizeX, double sizeY, double sizeZ) {
            return part(material.createBlockData(), x, y, z, sizeX, sizeY, sizeZ);
        }

        public Builder part(BlockData block, double x, double y, double z, double sizeX, double sizeY, double sizeZ) {
            return part(block, x, y, z, sizeX, sizeY, sizeZ, 0);
        }

        /** A part that is tipped forward (positive) or back (negative) by tiltDegrees about its own centre. */
        public Builder part(Material material, double x, double y, double z, double sizeX, double sizeY, double sizeZ, double tiltDegrees) {
            return part(material.createBlockData(), x, y, z, sizeX, sizeY, sizeZ, tiltDegrees);
        }

        public Builder part(BlockData block, double x, double y, double z, double sizeX, double sizeY, double sizeZ, double tiltDegrees) {
            parts.add(new Part(block, x, y, z, sizeX, sizeY, sizeZ, tiltDegrees));
            return this;
        }

        public BlockModel build() {
            if (parts.isEmpty()) {
                throw new IllegalStateException("A block model needs at least one part");
            }
            return new BlockModel(parts, scale, level);
        }
    }
}
