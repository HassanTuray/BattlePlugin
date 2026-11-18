package com.hfturay.battleplugin.ability.abilities.waterabilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * One Water Barrage: launches its blasts one after another. Each leaves along the caster's view and keeps flying away
 * at a constant speed, but its heading bends toward wherever the caster looks while it flies. A blast is a VirtualProjectile (so hits, walls and targetability work like every other projectile)
 * and looks like Water Prison's: real water in every block it passes through, gone a few ticks later.
 * The task ends when every blast has been launched and has finished flying.
 */
final class WaterBarrageTask extends ScheduledAbilityTask {

    /** Every number, already read from abilities.yml. `damage` is in health points (2 = one heart). */
    record Settings(double damage, double speed, double hitbox, int maxTicks, int intervalTicks, int count, int trailTicks, double turnRate) { }

    /** A blast in flight, and where its water trail last reached. */
    private static final class Blast {
        final VirtualProjectile projectile;
        Location last;

        Blast(VirtualProjectile projectile, Location last) {
            this.projectile = projectile;
            this.last = last;
        }
    }

    private final BattlePlayer owner;
    private final Settings settings;

    private final List<Blast> blasts = new ArrayList<>();
    private final List<TempBlocks> trail = new ArrayList<>();
    private int launched = 0;
    private int age = 0;

    WaterBarrageTask(BattlePlayer owner, Ability ability, Settings settings) {
        // The duration is only a backstop: the task ends itself once the last blast is over.
        super((settings.count() - 1) * settings.intervalTicks() + settings.maxTicks() + 10, ability);
        this.owner = owner;
        this.settings = settings;
    }

    @Override
    protected void onTick() {
        if (launched < settings.count() && age % settings.intervalTicks() == 0) {
            launch();
        }
        age++;

        Iterator<Blast> it = blasts.iterator();
        while (it.hasNext()) {
            Blast blast = it.next();
            if (blast.projectile.isFinished()) {
                it.remove();       // its last position is where it hit: nothing more to draw
                continue;
            }
            Location now = blast.projectile.getLocation().clone();
            trail.removeIf(t -> !t.isActive());
            trail.add(WaterTrail.leave(owner, blast.last, now, settings.trailTicks()));
            blast.last = now;
        }

        if (launched >= settings.count() && blasts.isEmpty()) {
            finishNow();
        }
    }

    /** Sends one blast from the caster's eyes, along where they are looking right now. */
    private void launch() {
        Location eye = owner.getPlayer().getEyeLocation();
        VirtualProjectile projectile = new VirtualProjectile(
            eye, eye.getDirection(),
            settings.speed(), settings.maxTicks(),
            owner, settings.hitbox(),
            (result, shooter) -> {
                BattlePlugin plugin = BattlePlugin.getInstance();
                plugin.getParticleService().burst(shooter, WaterParticles.BARRAGE_HIT, result.getHitLocation());
                Entity entity = result.getHitEntity();
                if (entity instanceof LivingEntity) {
                    plugin.getDamageService().damage(shooter, entity, settings.damage());
                }
            },
            null);

        // The blast never stops moving away from the caster, but its heading bends toward wherever they look NOW.
        projectile.setMotionFunction((velocity, tick) -> {
            Player shooter = owner.getPlayer();
            if (shooter == null || !shooter.isOnline()) {
                return velocity;
            }
            double speed = velocity.length();
            Vector heading = velocity.clone().multiply(1.0 / speed);
            Vector wanted = shooter.getEyeLocation().getDirection();
            Vector bent = heading.add(wanted.subtract(heading).multiply(settings.turnRate()));
            if (bent.lengthSquared() < 1.0E-8) {
                return velocity;      // looking exactly the opposite way: keep going rather than stall
            }
            return bent.normalize().multiply(speed);
        });

        BattlePlugin.getInstance().getProjectileService().add(projectile);
        blasts.add(new Blast(projectile, eye.clone()));
        launched++;
    }

    @Override
    protected void onFinish() {
        release();
    }

    @Override
    protected void onCancel() {
        for (Blast blast : blasts) {
            blast.projectile.cancel();
        }
        release();
    }

    private void release() {
        for (TempBlocks t : trail) {
            t.remove();
        }
        trail.clear();
        blasts.clear();
    }
}
