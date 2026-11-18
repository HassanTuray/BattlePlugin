package com.hfturay.battleplugin.ability.abilities.airabilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.targetingservice.AOE;

/**
 * One Air Dome. For its whole duration everyone inside the dome (the caster excluded) is pushed away from the caster
 * every tick, so they cannot stay or walk in. Each one takes damage the moment the dome first touches them and then
 * again every `damageIntervalTicks` while it keeps pushing them (so it lines up with the server's own hit cooldown).
 *
 * Speed comes from the server's speed potion at a high level, renewed by the task until it ends.
 */
final class AirDomeTask extends ScheduledAbilityTask {

    /** Every number, already read from abilities.yml. `damage` is in health points (2 = one heart). */
    record Settings(int durationTicks, double radius, double damage, int speedAmplifier, double pushStrength,
                    double pushLift, int damageIntervalTicks, int particleCount) { }

    /** The dome is its upper half: someone this far below the caster's feet or lower is outside it. */
    private static final double BELOW_FEET_LIMIT = 1.5;

    /** The speed effect is re-applied this often, each time lasting a little longer, so it never lapses. */
    private static final int SPEED_REFRESH_TICKS = 20;
    private static final int SPEED_TICKS = 40;

    private final BattlePlayer owner;
    private final Settings settings;
    private final ParticleHandle swirl;
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private int age = 0;
    private boolean speedGiven = false;

    AirDomeTask(BattlePlayer owner, Ability ability, Settings settings) {
        super(settings.durationTicks(), ability);
        this.owner = owner;
        this.settings = settings;

        this.swirl = BattlePlugin.getInstance().getParticleService().follow(owner,
            new AirDomeSpec(settings.radius(), settings.particleCount()), owner.getPlayer(), settings.durationTicks());
    }

    @Override
    protected void onTick() {
        Player player = owner.getPlayer();
        if (player == null || !player.isOnline() || player.isDead()) {
            cancel();
            return;
        }

        if (age % SPEED_REFRESH_TICKS == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_TICKS, settings.speedAmplifier(), false, false, true));
            speedGiven = true;
        }

        Location center = player.getLocation();
        for (Entity target : AOE.getTargets(owner, center, settings.radius())) {
            if (target.getLocation().getY() < center.getY() - BELOW_FEET_LIMIT) {
                continue;
            }
            Knockback.awayWithLift(center, target, settings.pushStrength(), settings.pushLift());

            Integer last = lastHit.get(target.getUniqueId());
            if (target instanceof LivingEntity && (last == null || age - last >= settings.damageIntervalTicks())) {
                if (BattlePlugin.getInstance().getDamageService().damage(owner, target, settings.damage())) {
                    lastHit.put(target.getUniqueId(), age);
                }
            }
        }
        age++;
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
        swirl.cancel();
        Player player = owner.getPlayer();
        if (speedGiven && player != null) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        lastHit.clear();
    }
}
