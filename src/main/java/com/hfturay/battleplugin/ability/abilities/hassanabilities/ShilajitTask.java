package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.visualservice.TumbleModel;

/**
 * The coal blocks orbiting the caster while Shilajit lasts: two rings, one low and one high, turning in opposite
 * directions. Each block is its own entity; all of them are removed when the task ends or is cancelled.
 */
final class ShilajitTask extends ScheduledAbilityTask {

    private static final double BLOCK_SIZE = 0.35;
    private static final double LOW_RING_HEIGHT = 0.5;       // above the caster's feet
    private static final double HIGH_RING_HEIGHT = 1.3;
    private static final double TURN_PER_TICK = 0.12;        // radians: about 1 turn every 2.6 seconds

    private final BattlePlayer owner;
    private final double radius;
    private final int perRing;
    private final List<BlockDisplay> low = new ArrayList<>();
    private final List<BlockDisplay> high = new ArrayList<>();
    private int age = 0;

    ShilajitTask(Ability ability, BattlePlayer owner, int duration, double radius, int perRing) {
        super(duration, ability);
        this.owner = owner;
        this.radius = radius;
        this.perRing = perRing;
    }

    @Override
    protected void onTick() {
        Player player = owner.getPlayer();
        if (player == null || !player.isValid()) {
            cancel();
            return;
        }

        if (low.isEmpty()) {
            spawnRing(low, player.getLocation());
            spawnRing(high, player.getLocation());
        }

        age++;
        Location feet = player.getLocation();
        place(low, feet, LOW_RING_HEIGHT, age * TURN_PER_TICK);
        place(high, feet, HIGH_RING_HEIGHT, -age * TURN_PER_TICK + Math.PI / perRing);     // the other way, offset
    }

    private void spawnRing(List<BlockDisplay> ring, Location at) {
        for (int i = 0; i < perRing; i++) {
            ring.add(at.getWorld().spawn(at, BlockDisplay.class, entity -> {
                entity.setBlock(Material.COAL_BLOCK.createBlockData());          // charcoal is not a block: coal stands in
                entity.setTransformation(TumbleModel.transformation(BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, new Quaternionf()));
                entity.setBrightness(new Display.Brightness(10, 10));
                entity.setTeleportDuration(1);                                   // glides between ticks
                entity.setShadowRadius(0f);
                entity.setPersistent(false);                                     // never saved with the world
            }));
        }
    }

    /** Puts the blocks of one ring evenly round the caster, turned by this angle. */
    private void place(List<BlockDisplay> ring, Location feet, double height, double turn) {
        for (int i = 0; i < ring.size(); i++) {
            double angle = turn + i * (2 * Math.PI / ring.size());
            ring.get(i).teleport(new Location(feet.getWorld(),
                feet.getX() + Math.cos(angle) * radius, feet.getY() + height, feet.getZ() + Math.sin(angle) * radius));
        }
    }

    @Override
    protected void onFinish() {
        removeAll();
    }

    @Override
    protected void onCancel() {
        removeAll();
    }

    private void removeAll() {
        for (List<BlockDisplay> ring : List.of(low, high)) {
            for (BlockDisplay display : ring) {
                try {
                    display.remove();
                } catch (RuntimeException ignored) {
                    // already gone
                }
            }
            ring.clear();
        }
    }
}
