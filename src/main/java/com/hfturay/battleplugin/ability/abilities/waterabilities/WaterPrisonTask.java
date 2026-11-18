package com.hfturay.battleplugin.ability.abilities.waterabilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.targetingservice.Targets;
import com.hfturay.battleplugin.tempblockservice.BlockPos;
import com.hfturay.battleplugin.tempblockservice.TempBlockService;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * Water Prison for one target, start to finish:
 *   FLYING   a real water block homes in on the target (and ends if the target becomes untargetable or it runs out of time)
 *   POPPING  it hit: the target is launched upward, and is already immune, untargetable and silenced
 *   HELD     a solid cube of still water forms around the target, who is frozen inside it
 * When it ends the water is removed and the target gets fall immunity for the drop.
 */
final class WaterPrisonTask extends ScheduledAbilityTask {

    /** Every number, already read from abilities.yml. */
    record Settings(double speed, double hitbox, int maxTicks, int cubeSize, int popTicks, int heldTicks, double popLift, int trailTicks) { }

    private enum Phase { FLYING, POPPING, HELD, DONE }

    private final BattlePlayer owner;
    private final Entity target;
    private final Settings settings;

    private Phase phase = Phase.FLYING;
    private final Location bolt;
    private final Vector heading = new Vector(0, 1, 0);
    private int flightTicks = 0;
    private int popLeft = 0;
    private int heldLeft = 0;

    private final List<TempBlocks> trail = new ArrayList<>();
    private TempBlocks water;
    private boolean stunApplied = false;

    WaterPrisonTask(BattlePlayer owner, Ability ability, Entity target, Settings settings) {
        // The duration is only a backstop: the task ends itself when the prison ends.
        super(settings.maxTicks() + settings.popTicks() + settings.heldTicks() + 40, ability);
        this.owner = owner;
        this.target = target;
        this.settings = settings;

        this.bolt = owner.getPlayer().getEyeLocation().subtract(0, 0.45, 0);
        Vector toTarget = target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(bolt.toVector());
        if (toTarget.lengthSquared() > 1.0E-8) {
            heading.copy(toTarget.normalize());
        }
    }

    /** If the caster dies mid-flight the water block is gone, but a prison that already formed stays until it ends. */
    @Override
    public boolean cancelOnOwnerDeath() {
        return phase == Phase.FLYING;
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case FLYING -> fly();
            case POPPING -> {
                if (!target.isValid()) {
                    cancel();
                } else if (--popLeft <= 0) {
                    form();
                }
            }
            case HELD -> {
                if (!target.isValid()) {
                    cancel();
                } else if (--heldLeft <= 0) {
                    finishNow();
                }
            }
            case DONE -> { }
        }
    }

    private void fly() {
        World world = bolt.getWorld();
        if (!target.isValid() || target.getWorld() != world || !Targets.isTargetable(owner, target) || ++flightTicks > settings.maxTicks()) {
            cancel();     // lost the target, or it became untargetable, or the block ran out of time
            return;
        }

        Location goal = target.getLocation().add(0, target.getHeight() / 2, 0);
        Vector toGoal = goal.toVector().subtract(bolt.toVector());
        double distance = toGoal.length();

        // Close enough that the next step would arrive: it hits. (Never overshoots, however fast it is.)
        if (distance <= Math.max(settings.speed(), settings.hitbox())) {
            hit(goal);
            return;
        }

        heading.copy(toGoal.multiply(1.0 / distance));
        Location from = bolt.clone();
        bolt.add(heading.clone().multiply(settings.speed()));
        leaveWater(from, bolt);
    }

    /** Fills every block the water block passed through this tick with real water, which then expires by itself. */
    private void leaveWater(Location from, Location to) {
        trail.removeIf(t -> !t.isActive());
        trail.add(WaterTrail.leave(owner, from, to, settings.trailTicks()));
    }

    /** The water block connected: lock the target down and launch them. */
    private void hit(Location at) {
        BattlePlugin plugin = BattlePlugin.getInstance();
        int total = settings.popTicks() + settings.heldTicks() + 2;

        plugin.getImmunityService().grant(target, total);
        plugin.getImmunityService().grantUntargetable(target, total);
        plugin.getSilenceService().silence(target.getUniqueId(), total);

        Knockback.set(target, new Vector(0, settings.popLift(), 0));
        plugin.getParticleService().burst(owner, WaterParticles.PRISON_HIT, at);

        phase = Phase.POPPING;
        popLeft = Math.max(1, settings.popTicks());
    }

    /** Fills a solid cube with still water around the target and freezes them in it. */
    private void form() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        Location at = target.getLocation();

        List<BlockPos> cube = TempBlockService.cube(at.getWorld(), at.getBlockX(),
            (int) Math.floor(at.getY() + target.getHeight() / 2), at.getBlockZ(), settings.cubeSize());
        water = plugin.getTempBlockService().place(cube, Material.WATER.createBlockData(), settings.heldTicks());

        Stun hold = new Stun(target, settings.heldTicks());
        hold.lockX = true;
        hold.lockY = true;
        hold.lockZ = true;
        hold.grantImmunity = true;
        hold.showSparks = false;
        plugin.getStunService().add(target, hold);
        stunApplied = true;

        if (target instanceof LivingEntity living) {
            // Underwater for the whole prison would otherwise drown them (and the status ends with the prison).
            living.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, settings.heldTicks() + 10, 0, false, false, false));
        }

        plugin.getParticleService().burst(owner, WaterParticles.PRISON_FORM, at.clone().add(0, target.getHeight() / 2, 0));

        phase = Phase.HELD;
        heldLeft = Math.max(1, settings.heldTicks());
    }

    @Override
    protected void onFinish() {
        release(false);
    }

    @Override
    protected void onCancel() {
        release(true);
    }

    /** Undoes everything. `early` means it was cut short, so the statuses are taken back too (they expire by themselves otherwise). */
    private void release(boolean early) {
        BattlePlugin plugin = BattlePlugin.getInstance();
        boolean wasHeld = phase == Phase.POPPING || phase == Phase.HELD;
        phase = Phase.DONE;

        for (TempBlocks t : trail) {
            t.remove();
        }
        trail.clear();
        if (water != null) {
            water.remove();
        }
        if (stunApplied) {
            plugin.getStunService().removeStun(target);
        }
        if (target instanceof LivingEntity living) {
            living.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }

        if (early && wasHeld) {
            plugin.getImmunityService().clearAll(target.getUniqueId());
            plugin.getSilenceService().clear(target.getUniqueId());
        }
        if (wasHeld && target.isValid()) {
            plugin.getFallImmunityService().grantFallImmunity(target);    // the drop out of the air must not hurt
        }
    }
}
