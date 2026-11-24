package com.hfturay.battleplugin.shikigamiservice;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.Keys;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.Targets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A shikigami that thinks for itself, built from a ShikigamiSpec. Each tick:
 *   1. the despawn rules are checked (any one can end it),
 *   2. every few ticks it re-chooses its target and moves,
 *   3. it hits its target if close enough and its attack is ready,
 *   4. its triggers run.
 * The vanilla mob AI is removed, so nothing it does comes from the game's own mob behaviour.
 */
public class BrainShikigami extends Shikigami {
    private static final int RETARGET_TICKS = 10;
    private static final int MOVE_TICKS = 5;

    private final ShikigamiSpec spec;
    private Body body;

    private int age = 0;
    private int pendingHitAt = -1;
    private LivingEntity pendingTarget;
    private int lastActiveAge = 0;
    private int lastAttackAge;
    private LivingEntity target;
    private final int[] triggerReadyAt;
    private boolean takenOver = false;

    public BrainShikigami(Mob mob, BattlePlayer owner, ShikigamiSpec spec) {
        super(mob, owner);
        this.spec = spec;
        this.lastAttackAge = -spec.attackInterval();
        this.triggerReadyAt = new int[spec.triggers().size()];
        this.setDisplayName(spec.name());

        // The mob is set up first (a model copies its name for the name tag), then dressed in its body.
        configureShikigami(mob, owner);
        this.body = ShikigamiBodies.create(mob, spec.model(), spec.animations());
        if (body.providesNametag()) {
            mob.setCustomNameVisible(false);
        }
    }

    public ShikigamiSpec spec() {
        return spec;
    }

    public Body body() {
        return body;
    }

    public Mob mob() {
        return getMob();
    }

    /** Ticks since it was summoned. */
    public int ageTicks() {
        return age;
    }

    /** Ticks since it last had a target or hit something. */
    public int ticksSinceActive() {
        return age - lastActiveAge;
    }

    /** Who it is currently going after, or null. */
    public LivingEntity target() {
        return target;
    }

    /**
     * Something else drives the mob from now on (Pink!'s End it). The brain stops choosing targets, moving, hitting
     * and running its triggers; it only still checks its despawn rules. Cannot be undone.
     */
    public void takeOver() {
        takenOver = true;
        target = null;
        pendingHitAt = -1;
        pendingTarget = null;
        mob().getPathfinder().stopPathfinding();
    }

    @Override
    public void configureShikigami(Mob mob, BattlePlayer owner) {
        mob.customName(Component.text(spec.name(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        mob.setCustomNameVisible(true);

        AttributeInstance maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(spec.maxHealth());
        }
        mob.setHealth(spec.maxHealth());

        // Pathfinding gives up on targets beyond this, so it must cover the search range.
        AttributeInstance follow = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(Math.max(follow.getBaseValue(), spec.targetRange() + 16));
        }
        AttributeInstance burn = mob.getAttribute(Attribute.GENERIC_BURNING_TIME);
        if (burn != null) {
            burn.setBaseValue(0);
        }

        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);

        // No vanilla behaviour: this class decides everything. Pathfinding (moveTo) still works without goals.
        Bukkit.getMobGoals().removeAllGoals(mob);
        mob.setTarget(null);

        mob.getPersistentDataContainer().set(Keys.shikigamiOwner, PersistentDataType.STRING,
            owner.getPlayer().getUniqueId().toString());

        spec.look().accept(mob);
    }

    @Override
    public boolean tick() {
        age++;

        for (DespawnRule rule : spec.rules()) {
            EndReason reason = rule.check(this);
            if (reason != null) {
                setEndReason(reason);
                return true;
            }
        }
        if (takenOver) {
            return false;
        }

        if (age % RETARGET_TICKS == 0 || !targetStillValid()) {
            target = spec.targeting().pick(this);
        }
        if (target != null) {
            lastActiveAge = age;
        }

        if (age % MOVE_TICKS == 0) {
            spec.movement().move(this);
        }

        tryAttack();
        runTriggers();
        return false;
    }

    private boolean targetStillValid() {
        if (target == null) {
            return false;
        }
        if (!target.isValid() || target.isDead() || !mayHit(target) || !target.getWorld().equals(mob().getWorld())) {
            return false;
        }
        double range = spec.targetRange() * 2;
        return target.getLocation().distanceSquared(mob().getLocation()) <= range * range;
    }

    private void tryAttack() {
        // A swing already under way: wait for the moment the hit lands.
        if (pendingHitAt >= 0) {
            if (age >= pendingHitAt) {
                LivingEntity victim = pendingTarget;
                pendingHitAt = -1;
                pendingTarget = null;
                landHit(victim);
            }
            return;
        }

        if (target == null || age - lastAttackAge < spec.attackInterval()) {
            return;
        }
        // Edge to edge, so big and small targets are fair.
        if (!mob().getBoundingBox().expand(spec.attackRange()).overlaps(target.getBoundingBox())) {
            return;
        }

        lastAttackAge = age;
        lastActiveAge = age;
        body.play("attack");
        spec.onAttack().accept(this);

        if (spec.hitDelay() <= 0) {
            landHit(target);
        } else {
            // The model's swing takes a moment to reach the target, so the damage waits for it.
            pendingHitAt = age + spec.hitDelay();
            pendingTarget = target;
        }
    }

    /** Whoever its owner may hit; and its owner too, if the spec allows it (canHurtOwner, for testing). */
    private boolean mayHit(LivingEntity entity) {
        return Targets.isTargetable(getOwner(), entity)
            || (spec.canHurtOwner() && entity.getUniqueId().equals(ownerId()));
    }

    private void landHit(LivingEntity victim) {
        if (victim == null || !victim.isValid() || victim.isDead() || !mayHit(victim)) {
            return;
        }
        // They may have stepped away during the swing: allow a little extra reach, then it is a miss.
        if (!mob().getBoundingBox().expand(spec.attackRange() + 1.0).overlaps(victim.getBoundingBox())) {
            return;
        }
        // The owner is the credited attacker, so kills, immunity and damage rules all work as for any ability.
        BattlePlugin.getInstance().getDamageService().damage(getOwner(), victim, spec.damage());
    }

    private void runTriggers() {
        for (int i = 0; i < triggerReadyAt.length; i++) {
            ShikigamiTrigger trigger = spec.triggers().get(i);
            if (age >= triggerReadyAt[i] && trigger.when().test(this)) {
                triggerReadyAt[i] = age + trigger.cooldownTicks();
                lastActiveAge = age;
                trigger.action().run(this);
            }
        }
    }

    @Override
    protected void onEnded(EndReason reason) {
        body.remove();
        if (reason == EndReason.SHUTDOWN) {
            return;
        }
        spec.onEnd().onEnd(this, reason);
    }

    /** For the service's tracking of who is who. */
    public UUID ownerId() {
        return getOwner().getPlayer().getUniqueId();
    }
}
