package com.hfturay.battleplugin.domainservice.neverendingnight;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A giant eye made of three flat blocks (white, blue, black), hung in the air and turned to face a direction.
 *
 * The eye's middle is at the location it was made at. update() turns it to face a direction at a size (1 = full
 * size, about 5 blocks across); the displays glide between updates. remove() deletes it. Never saved with the world.
 */
final class EyeModel {

    /** One flat block of the eye: its size in blocks at full scale, and where it starts along the facing direction. */
    private record Layer(Material material, double width, double height, double depth, double start) { }

    // the eye faces +Z (before turning): the white behind, the blue in front of it, the black pupil in front of that
    private static final List<Layer> LAYERS = List.of(
        new Layer(Material.WHITE_CONCRETE, 5.0, 5.0, 0.8, -0.8),
        new Layer(Material.LIGHT_BLUE_CONCRETE, 3.2, 3.2, 0.5, 0.0),
        new Layer(Material.BLACK_CONCRETE, 1.6, 1.6, 0.5, 0.5));

    private final List<BlockDisplay> parts = new ArrayList<>();

    EyeModel(Location center, Vector direction, double scale) {
        for (Layer layer : LAYERS) {
            BlockDisplay display = center.getWorld().spawn(center, BlockDisplay.class, entity -> {
                entity.setBlock(layer.material().createBlockData());
                entity.setBrightness(new Display.Brightness(15, 15));    // glows, even in the dark
                entity.setShadowRadius(0f);
                entity.setPersistent(false);
                entity.setTransformation(transformation(layer, direction, scale));
            });
            display.setInterpolationDuration(2);
            parts.add(display);
        }
    }

    void update(Vector direction, double scale) {
        for (int i = 0; i < parts.size(); i++) {
            BlockDisplay display = parts.get(i);
            if (!display.isValid()) {
                continue;
            }
            display.setTransformation(transformation(LAYERS.get(i), direction, scale));
            display.setInterpolationDelay(0);       // start gliding now
            display.setInterpolationDuration(2);
        }
    }

    boolean isValid() {
        for (BlockDisplay part : parts) {
            if (!part.isValid()) {
                return false;
            }
        }
        return !parts.isEmpty();
    }

    void remove() {
        for (BlockDisplay part : parts) {
            part.remove();
        }
        parts.clear();
    }

    private static Transformation transformation(Layer layer, Vector direction, double scale) {
        Vector facing = direction.clone();
        if (facing.lengthSquared() < 1.0E-9) {
            facing = new Vector(0, 0, 1);
        }
        facing.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(0f, 0f, 1f, (float) facing.getX(), (float) facing.getY(), (float) facing.getZ());

        double s = Math.max(0.01, scale);
        Vector3f size = new Vector3f((float) (layer.width() * s), (float) (layer.height() * s), (float) (layer.depth() * s));
        // a block display draws from its corner, so shift it back by half its width and height to centre it
        Vector3f offset = new Vector3f(-size.x / 2f, -size.y / 2f, (float) (layer.start() * s));
        rotation.transform(offset);
        return new Transformation(offset, rotation, size, new Quaternionf());
    }
}
