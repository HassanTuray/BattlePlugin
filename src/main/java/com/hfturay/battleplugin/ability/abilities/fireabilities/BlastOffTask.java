package com.hfturay.battleplugin.ability.abilities.fireabilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.FallImmunityService;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.targetingservice.AOE;

/**
 * One Blast Off. For its whole duration the caster is held in the flying pose and given a velocity along their view
 * every tick; anyone within reach is burned on a fixed rhythm. It ends early if the caster lands (after a short
 * grace while they leave the ground) or dies. The caster keeps fall immunity until they land.
 */
final class BlastOffTask extends ScheduledAbilityTask {

    /** Every number, already read from abilities.yml. `damage` is in health points (2 = one heart). */
    record Settings(int durationTicks, double speed, double launchLift, double damage, double radius, double knockback, double lift) { }

    /** Ticks at the start that always push upward so the caster leaves the ground. */
    private static final int LIFT_TICKS = 4;

    /** Ticks before touching the ground can end the flight. */
    private static final int LANDING_GRACE_TICKS = 10;

    private final BattlePlayer owner;
    private final Settings settings;
    private int age = 0;
    private final ParticleHandle feet;

    BlastOffTask(BattlePlayer owner, Ability ability, Settings settings) {
        super(settings.durationTicks(), ability);
        this.owner = owner;
        this.settings = settings;

        BattlePlugin plugin = BattlePlugin.getInstance();
        Player player = owner.getPlayer();
        plugin.getFallImmunityService().grantFallImmunity(player);
        plugin.getGlideService().begin(player);
        this.feet = plugin.getParticleService().follow(owner, FireParticles.BLAST_OFF_FEET, player, settings.durationTicks());
    }

    @Override
    protected void onTick() {
        Player player = owner.getPlayer();
        if (player == null || !player.isOnline() || player.isDead()) {
            cancel();
            return;
        }
        if (age >= LANDING_GRACE_TICKS && FallImmunityService.isGrounded(player)) {
            explode(player);              // they came down: a small explosion, and the flight is over
            finishNow();
            return;
        }

        BattlePlugin.getInstance().getGlideService().keep(player);

        Vector push = player.getEyeLocation().getDirection().multiply(settings.speed());
        if (age < LIFT_TICKS) {
            push.setY(Math.max(push.getY(), settings.launchLift()));
        }
        player.setVelocity(push);
        age++;
    }

    /** The landing blast: hurts everything within `radius` of the caster and shoves it away and a little up. */
    private void explode(Player player) {
        Location body = player.getLocation().add(0, 0.5, 0);
        World world = body.getWorld();
        world.spawnParticle(Particle.EXPLOSION, body, 1);
        world.spawnParticle(Particle.FLAME, body, 40, 1.5, 0.3, 1.5, 0.1);
        world.playSound(body, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.2f);

        for (Entity target : AOE.getTargets(owner, body, settings.radius())) {
            if (!(target instanceof LivingEntity living)) {
                continue;
            }
            BattlePlugin.getInstance().getDamageService().damage(owner, living, settings.damage());
            Vector away = living.getLocation().toVector().subtract(body.toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-6) {
                away = new Vector(1, 0, 0);
            }
            living.setVelocity(away.normalize().multiply(settings.knockback()).setY(settings.lift()));
        }
    }

    @Override
    protected void onFinish() {
        release();
    }

    @Override
    protected void onCancel() {
        release();
    }

    private void release() {
        Player player = owner.getPlayer();
        if (player != null) {
            BattlePlugin.getInstance().getGlideService().end(player);
        }
        feet.cancel();
    }
}
