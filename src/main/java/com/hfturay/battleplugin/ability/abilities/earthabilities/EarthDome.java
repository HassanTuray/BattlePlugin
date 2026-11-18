package com.hfturay.battleplugin.ability.abilities.earthabilities;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.tempblockservice.BlockPos;
import com.hfturay.battleplugin.tempblockservice.TempBlockService;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * Raises a dome (the top half of a sphere, not a whole one) of earth blocks around the caster. Whatever was inside it
 * is turned to air for as long as the dome stands, the caster gets resistance and everyone inside gets night vision.
 * Every number comes from abilities.yml (EarthDome section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "radius", "thickness", "duration-ticks", "resistance-amplifier" })
public class EarthDome extends Ability {

    /** How far below the caster's feet the wall reaches to close gaps on uneven ground (only over open air). */
    private static final int RIM_DEPTH = 3;

    public EarthDome(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Earth Dome");
    }

    @Override
    protected Activation onActivate() {
        double radius = setting("radius");
        double thickness = setting("thickness");
        int duration = settingInt("duration-ticks");

        Location base = owner.getPlayer().getLocation();
        World world = base.getWorld();
        int cx = base.getBlockX();
        int cy = base.getBlockY();
        int cz = base.getBlockZ();
        int reach = (int) Math.ceil(radius) + 1;
        double innerLimit = radius - thickness;

        List<BlockPos> clear = new ArrayList<>();
        Map<Material, List<BlockPos>> wall = new EnumMap<>(Material.class);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -RIM_DEPTH; dy <= reach; dy++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > radius) {
                        continue;
                    }
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);

                    if (distance <= innerLimit) {
                        if (dy >= 0 && !block.getType().isAir()) {
                            clear.add(BlockPos.of(block));        // inside: whatever is here is cleared
                        }
                    } else if (dy >= 0 || !block.getType().isSolid()) {
                        // the wall; below the caster's feet it only fills open air, so it never digs into the ground
                        wall.computeIfAbsent(EarthBlocks.PALETTE.get(random.nextInt(EarthBlocks.PALETTE.size())), m -> new ArrayList<>())
                            .add(BlockPos.of(block));
                    }
                }
            }
        }

        TempBlockService blocks = BattlePlugin.getInstance().getTempBlockService();
        List<TempBlocks> placed = new ArrayList<>();
        if (!clear.isEmpty()) {
            placed.add(blocks.place(clear, Material.AIR.createBlockData(), duration));
        }
        for (Map.Entry<Material, List<BlockPos>> entry : wall.entrySet()) {
            placed.add(blocks.place(entry.getValue(), entry.getKey().createBlockData(), duration));
        }

        EarthDomeTask.Settings settings = new EarthDomeTask.Settings(radius, duration, settingInt("resistance-amplifier"));
        BattlePlugin.getInstance().getAbilityScheduler().schedule(new EarthDomeTask(owner, this, base, settings, placed));
        return Activation.USED;
    }
}
