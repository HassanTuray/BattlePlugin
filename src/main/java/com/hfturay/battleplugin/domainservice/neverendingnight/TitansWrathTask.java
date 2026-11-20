package com.hfturay.battleplugin.domainservice.neverendingnight;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.Targets;
import com.hfturay.battleplugin.visualservice.BeamModel;
import com.hfturay.battleplugin.visualservice.VisualAnchor;
import com.hfturay.battleplugin.visualservice.VisualHandle;
import com.hfturay.battleplugin.visualservice.VisualSpec;

/**
 * Titan's Wrath: the giant eye forms above the obsidian tower, then fires a beam that follows the caster's cursor.
 *
 * Three stages: the eye grows (charge), the beam fires and hurts everything it crosses every interval ticks (fire),
 * then the eye shrinks away (retract). The eye turns to face wherever the caster is looking at (the point their
 * cursor hits). Everything it made is removed when the task ends or is cancelled.
 */
public final class TitansWrathTask extends ScheduledAbilityTask {

    /** The numbers, from abilities.yml. damage is health points (2 per heart). */
    public record Params(int chargeTicks, int fireTicks, int intervalTicks, double damage, double range, double width) { }

    private static final int RETRACT_TICKS = 10;

    // blue outer beam with a white-hot core
    private static final VisualSpec OUTER = BeamModel.of(Material.LIGHT_BLUE_CONCRETE, 0.9);
    private static final VisualSpec CORE = BeamModel.of(Material.WHITE_CONCRETE, 0.35);

    private final BattlePlayer owner;
    private final NeverendingNightEffects night;
    private final NeverendingNightService service;
    private final Params params;

    private VisualHandle outerBeam;
    private VisualHandle coreBeam;
    private int age = 0;
    private boolean firing = false;

    // where the beam is this tick (read by the beam visuals)
    private Location beamStart;
    private Vector beamDirection = new Vector(0, -1, 0);
    private double beamLength;

    public TitansWrathTask(Ability ability, BattlePlayer owner, NeverendingNightEffects night,
                           NeverendingNightService service, Params params) {
        super(params.chargeTicks() + params.fireTicks() + RETRACT_TICKS, ability);
        this.owner = owner;
        this.night = night;
        this.service = service;
        this.params = params;
    }

    @Override
    protected void onTick() {
        Location center = night.eyeCenter();
        EyeModel eye = night.eye();         // the eye on the tower, placed during the domain's cutscene
        Player player = owner.getPlayer();
        if (center == null || eye == null || !player.isValid()) {
            cancel();
            return;
        }
        age++;

        Location target = aimPoint(player, center.getWorld());
        Vector toTarget = target.toVector().subtract(center.toVector());
        Vector facing = toTarget.lengthSquared() < 1.0E-6 ? player.getEyeLocation().getDirection() : toTarget.clone().normalize();

        // the eye swells while it charges, stays swollen while it fires, then settles back to its normal size
        double scale;
        if (age <= params.chargeTicks()) {
            scale = 1.0 + 0.3 * age / Math.max(1, params.chargeTicks());
        } else if (age <= params.chargeTicks() + params.fireTicks()) {
            scale = 1.3;
        } else {
            scale = 1.0 + 0.3 * Math.max(0.0, 1.0 - (age - params.chargeTicks() - params.fireTicks()) / (double) RETRACT_TICKS);
        }

        World world = center.getWorld();
        if (age == 1) {
            night.setEyeBusy(true);         // from now the eye follows the beam, not the idle gaze
            world.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.MASTER, 4.0f, 0.7f);
        }
        eye.update(facing, scale);

        int fireAge = age - params.chargeTicks();
        if (fireAge >= 1 && fireAge <= params.fireTicks()) {
            fire(world, center, facing, target, fireAge);
        } else if (firing) {
            stopBeams();
        }
    }

    /** The point the caster's cursor is on: the first thing their crosshair meets (a creature, or a block), or far ahead. */
    private Location aimPoint(Player player, World world) {
        Location eyePosition = player.getEyeLocation();
        Vector direction = eyePosition.getDirection().normalize();
        RayTraceResult result = world.rayTrace(eyePosition, direction, params.range(), FluidCollisionMode.NEVER, true, 0.4,
            entity -> Targets.isTargetable(owner, entity) && !service.isCreature(entity));
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(world);
        }
        return eyePosition.clone().add(direction.multiply(params.range()));
    }

    private void fire(World world, Location center, Vector facing, Location target, int fireAge) {
        beamStart = center.clone().add(facing.clone().multiply(1.3));
        Vector line = target.toVector().subtract(beamStart.toVector());
        beamLength = line.length();
        beamDirection = beamLength < 1.0E-6 ? facing.clone() : line.clone().normalize();

        if (!firing) {
            firing = true;
            world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 4.0f, 0.6f);
            outerBeam = BattlePlugin.getInstance().getVisualService().follow(owner, OUTER, anchor());
            coreBeam = BattlePlugin.getInstance().getVisualService().follow(owner, CORE, anchor());
        }

        world.spawnParticle(Particle.SOUL_FIRE_FLAME, target, 6, 0.4, 0.4, 0.4, 0.05);
        world.spawnParticle(Particle.END_ROD, target, 3, 0.3, 0.3, 0.3, 0.1);
        if (fireAge % 5 == 0) {
            world.playSound(target, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.5f, 1.4f);
        }

        if ((fireAge - 1) % Math.max(1, params.intervalTicks()) == 0) {
            damageAlongBeam(world, target);
        }
    }

    /** Hurts every living thing the beam crosses (the caster, their allies' shikigami and the dome's creatures are spared). */
    private void damageAlongBeam(World world, Location target) {
        Vector start = beamStart.toVector();
        BoundingBox area = BoundingBox.of(start, target.toVector()).expand(params.width() + 1);
        for (Entity entity : world.getNearbyEntities(area)) {
            if (!(entity instanceof LivingEntity living) || service.isCreature(living) || !Targets.isTargetable(owner, living)) {
                continue;
            }
            BoundingBox box = living.getBoundingBox().expand(params.width());
            if (box.contains(start) || box.rayTrace(start, beamDirection, beamLength) != null) {
                BattlePlugin.getInstance().getDamageService().damage(owner, living, params.damage());
            }
        }
    }

    private VisualAnchor anchor() {
        return new VisualAnchor() {
            @Override public Location location() { return beamStart; }
            @Override public Vector direction() { return beamDirection; }
            @Override public double length() { return beamLength; }
            @Override public boolean isValid() { return firing && beamStart != null && isActive(); }
        };
    }

    private void stopBeams() {
        firing = false;
        if (outerBeam != null) {
            outerBeam.cancel();
        }
        if (coreBeam != null) {
            coreBeam.cancel();
        }
    }

    @Override
    protected void onFinish() {
        cleanUp();
    }

    @Override
    protected void onCancel() {
        cleanUp();
    }

    private void cleanUp() {
        stopBeams();
        night.setEyeBusy(false);            // the eye goes back to watching the arena (it stays on the tower)
    }
}
