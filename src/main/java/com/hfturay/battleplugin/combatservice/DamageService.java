package com.hfturay.battleplugin.combatservice;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * The one way abilities hurt things.
 *
 * damage(attacker, victim, amount) checks immunity, then damages with the attacker as the real source, so the
 * hit shows up as an entity-attack event (kill credit, "slain by", other plugins) instead of anonymous damage.
 * Immune targets take nothing and get no hurt animation.
 *
 * The server normally adds a small vanilla knockback to hits that have an attacker. Abilities do their own
 * knockback, so by default the victim's velocity is put back to what it was.
 *
 * As a listener it also stops vanilla damage (hits, fire, falls...) on immune entities by cancelling the event,
 * instead of zeroing it, which used to leave the hurt animation and knockback in place.
 */
public final class DamageService implements Listener {

    /** Deals the damage. Replaced in tests; the real one asks the server. */
    @FunctionalInterface
    public interface Dealer {
        void deal(Damageable victim, double amount, Entity source);
    }

    /** Damage that is never blocked by immunity, so it cannot be used to make someone unkillable. */
    private static final Set<DamageCause> UNBLOCKABLE = EnumSet.of(
        DamageCause.VOID, DamageCause.KILL, DamageCause.SUICIDE, DamageCause.WORLD_BORDER);

    /** Deepest a damage call may be nested inside another one. A safety net against ability loops. */
    private static final int MAX_NESTING = 4;

    private final ImmunityService immunity;
    private final Dealer dealer;
    private int dealing = 0;

    public DamageService(ImmunityService immunity) {
        this(immunity, (victim, amount, source) -> {
            if (source != null) {
                victim.damage(amount, source);
            } else {
                victim.damage(amount);
            }
        });
    }

    public DamageService(ImmunityService immunity, Dealer dealer) {
        this.immunity = immunity;
        this.dealer = dealer;
    }

    /**
     * True while this service is in the middle of dealing damage. The resulting server event looks exactly like
     * a player punching someone, so listeners that treat punches as ability triggers must skip it, or the
     * ability would fire itself again (this was an endless loop that crashed the server).
     */
    public boolean isDealing() {
        return dealing > 0;
    }

    /** Damage with vanilla knockback removed. Returns true if the hit was dealt. */
    public boolean damage(BattlePlayer attacker, Entity victim, double amount) {
        return damage(attacker, victim, amount, false);
    }

    /** @param vanillaKnockback keep the knockback the server adds to hits with an attacker */
    public boolean damage(BattlePlayer attacker, Entity victim, double amount, boolean vanillaKnockback) {
        if (!(victim instanceof Damageable damageable) || !victim.isValid()) {
            return false;
        }
        if (!(amount > 0) || Double.isInfinite(amount)) {   // also rejects NaN
            return false;
        }
        if (immunity.isImmune(victim.getUniqueId()) || dealing >= MAX_NESTING) {
            return false;
        }

        Entity source = attacker == null ? null : attacker.getPlayer();
        if (source != null && source.equals(victim)) {
            source = null;    // hurting yourself is not an attack
        }

        Vector before = vanillaKnockback || source == null ? null : victim.getVelocity().clone();

        // While this runs, the server fires an entity-attack event with the attacker as the damager. Anything
        // that reacts to attacks (the click handler) must ignore it: see isDealing().
        dealing++;
        try {
            dealer.deal(damageable, amount, source);
        } finally {
            dealing--;
        }

        if (before != null && victim.isValid()) {
            Knockback.set(victim, before);
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (UNBLOCKABLE.contains(event.getCause())) {
            return;
        }
        if (immunity.isImmune(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** Mobs (shikigami, gas-cloud zombies) cannot pick an untargetable entity as their target. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() != null && immunity.isUntargetable(event.getTarget().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
