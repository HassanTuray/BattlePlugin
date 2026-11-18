package com.hfturay.battleplugin.ability.abilities.fireabilities;

import org.bukkit.Location;
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
    record Settings(int durationTicks, double speed, double launchLift, double damage, double radius, int intervalTicks) { }

    /** Ticks at the start that always push upward so the caster leaves the ground. */
    private static final int LIFT_TICKS = 4;

    /** Ticks before touching the ground can end the flight. */
    private static final int LANDING_GRACE_TICKS = 10;

    /** How far from the caster (their body centre) a target can be found before the exact reach check. */
    private static final double SEARCH_RADIUS = 5.0;

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
            finishNow();                  // they came down: the flight is over
            return;
        }

        BattlePlugin.getInstance().getGlideService().keep(player);

        Vector push = player.getEyeLocation().getDirection().multiply(settings.speed());
        if (age < LIFT_TICKS) {
            push.setY(Math.max(push.getY(), settings.launchLift()));
        }
        player.setVelocity(push);

        if (age % settings.intervalTicks() == 0) {
            burnNearby(player);
        }
        age++;
    }

    /** Burns everything whose body is within `radius` of the caster's body. */
    private void burnNearby(Player player) {
        Location body = player.getLocation().add(0, 0.5, 0);
        for (Entity target : AOE.getTargets(owner, body, SEARCH_RADIUS)) {
            if (target instanceof LivingEntity && target.getBoundingBox().expand(settings.radius()).contains(body.toVector())) {
                BattlePlugin.getInstance().getDamageService().damage(owner, target, settings.damage());
            }
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
