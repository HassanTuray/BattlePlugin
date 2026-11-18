package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.MeleeAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.abilityschedulerservice.ComboWindow;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.stunservice.Stun;
import com.hfturay.battleplugin.stunservice.StunService;
import com.hfturay.battleplugin.targetingservice.HitScan;

/**
 * Two moves: an upward slash that launches both players (`lift`), then, within `combo-ticks`, a slam that pins the
 * target in place for `stun-ticks` and drives both down (`slam-force`, a downward speed) until they land.
 * Every number comes from abilities.yml (GrandSlam section). The damage includes any buff from The Ascended.
 */
@RequiresSettings({ "cooldown", "range", "damage", "lift", "slam-force", "combo-ticks", "stun-ticks" })
public class GrandSlam extends MeleeAbility {
    private final ComboWindow combo;

    public GrandSlam(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Grand Slam");

        // If the player does not follow up with the slam in time, the combo ends and the cooldown starts.
        this.combo = new ComboWindow(this, () -> settingInt("combo-ticks"), this::startCooldown,
            () -> BattlePlugin.getInstance().getAbilityScheduler());
    }

    private double hitDamage() {
        return setting("damage") + getDamageBonus();
    }

    @Override
    protected Activation onActivate() {
        if (combo.stage() == 1) {
            slam();
        } else {
            upwardSlash();
        }

        return Activation.USED_NO_COOLDOWN;
    }

    private void upwardSlash() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, setting("range"), collisionMode);

        if (entity == null) {
            return;
        }

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();

        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, hitDamage());
        }

        upwardSlashEffect(entity);

        double lift = setting("lift");
        GrandSlamKnockbackRepeater targetLift = new GrandSlamKnockbackRepeater(1, entity, lift, this);
        GrandSlamKnockbackRepeater castorLift = new GrandSlamKnockbackRepeater(1, owner.getPlayer(), lift, this);

        abilityScheduler.schedule(targetLift);
        abilityScheduler.schedule(castorLift);

        combo.hit(1);
    }

    private void slam() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, setting("range"), collisionMode);

        if (entity == null) {
            // Missed the slam: back to the first swing. The window keeps running and starts the cooldown when it ends.
            combo.setStage(0);
            return;
        }

        double damage = hitDamage();
        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);
        }

        int stunTicks = settingInt("stun-ticks");
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        Stun lockHoriz = new Stun(entity, stunTicks);
        lockHoriz.lockX = true;
        lockHoriz.lockZ = true;

        stunLogger.add(entity, lockHoriz);

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();

        double slam = -setting("slam-force");   // stored as a positive downward speed; the push is negative
        GrandSlamKnockbackRepeater targetSlamDown = new GrandSlamKnockbackRepeater(1, entity, slam, this);
        GrandSlamKnockbackRepeater castorSlamDown = new GrandSlamKnockbackRepeater(1, owner.getPlayer(), slam, this);
        GrandSlamDownRepeater targetFallTracker = new GrandSlamDownRepeater(stunTicks, entity, damage, this);

        abilityScheduler.schedule(targetSlamDown);
        abilityScheduler.schedule(castorSlamDown);
        abilityScheduler.schedule(targetFallTracker);

        combo.reset();
        startCooldown();
    }

    private void upwardSlashEffect(Entity target) {
        BattlePlugin.getInstance().getParticleService()
            .burst(owner, NathanParticles.UPWARD_SLASH, NathanParticles.midpoint(owner, target));
    }

    @Override
    public void playParticleEffect() {
    }
}
