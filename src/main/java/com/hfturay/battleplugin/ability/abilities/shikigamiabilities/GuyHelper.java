package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilityFactory;
import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.AbilityUseLog;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.targetingservice.Targets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * What Guy does in a fight. Whenever his owner damages something, Guy dashes to it a moment later, bumps into it
 * (a little damage and a small shove in a random direction, upward), then flies back to the owner.
 *
 * Guy's own hit is dealt while the "striking" flag is up, and the listener ignores any damage that happens during it.
 * That is what stops Guy from reacting to his own hit and dashing forever. His hit is still credited to the owner
 * (kills, immunity), it just is not treated as a new attack by the owner.
 *
 * If Guy is already out on a dash, new hits by the owner are ignored until he is back. If he cannot reach the target
 * within giveUpTicks he gives up and returns anyway.
 *
 * Learning: while he is out, Guy adds up the damage every player does to his owner. When one player has done learnDamage
 * in total, he flies to them, scans them, comes back and hands the owner the attack or elemental ability they hurt the
 * owner with the most: it is added to the owner's attack item (the magma cream). Guy never uses it himself.
 */
public final class GuyHelper implements Listener {

    /** The numbers for one summoned Guy, read from abilities.yml when he is summoned. */
    public record Settings(double damage, double knockback, double lift, double dashSpeed, int delayTicks,
                           int giveUpTicks, double returnSpeed, double hoverHeight, double hoverBehind,
                           double learnDamage, int windowTicks, int scanTicks, double scanDistance) { }

    private static final int MAX_RETURN_TICKS = 60;
    private static final int MAX_SCAN_TRIP_TICKS = 100;    // longest Guy will fly to someone he wants to scan
    private static final int SCAN_RETRY_TICKS = 200;        // wait this long before trying again if the trip failed
    private static final Particle.DustOptions SCAN_BLUE = new Particle.DustOptions(Color.fromRGB(120, 210, 255), 1.0f);

    /** How close to its hover spot Guy must get to be "home" after a dash, and how fast he settles into it when idle. */
    private static final double HOME_DISTANCE = 0.8;
    private static final double SETTLE = 0.25;      // the fraction of the gap he closes each tick
    private static final double SNAP_DISTANCE = 24; // farther than this (or another world) and he is put straight there

    private final Map<UUID, Guy> guys = new HashMap<>();
    private boolean striking = false;
    private BukkitTask hoverTask;

    private static final class Guy {
        final BrainShikigami brain;
        final Settings settings;
        Dash dash;
        Scan scan;

        /** Damage each player has done to the owner while this Guy has been out, and how much came from each ability. */
        final Map<UUID, Tally> tallies = new HashMap<>();
        UUID pendingAttacker;                       // someone Guy is about to scan
        Class<? extends Ability> pendingType;       // and what he will learn from them
        int retryAt = 0;

        Guy(BrainShikigami brain, Settings settings) {
            this.brain = brain;
            this.settings = settings;
        }
    }

    private static final class Tally {
        double total = 0;
        final Map<Class<? extends Ability>, Double> byAbility = new HashMap<>();
    }

    /** Starts watching for this owner's hits on behalf of this Guy. A newer Guy replaces an older one. */
    public void register(BrainShikigami brain, Settings settings) {
        brain.mob().setGravity(false);              // he floats
        guys.put(brain.ownerId(), new Guy(brain, settings));
        if (hoverTask == null) {
            hoverTask = Bukkit.getScheduler().runTaskTimer(BattlePlugin.getInstance(), this::hover, 1L, 1L);
        }
    }

    /** The spot Guy floats at: above the owner and behind them (the way they are not facing). */
    private static Location hoverPoint(Player owner, Settings settings) {
        Location at = owner.getLocation();
        Vector back = at.getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-6) {
            back = new Vector(0, 0, 1);
        }
        back.normalize().multiply(-settings.hoverBehind());
        at.add(back).add(0, settings.hoverHeight(), 0);
        return at;
    }

    /** Every tick, while he is not on a dash, Guy drifts to his spot above and behind his owner and faces where they face. */
    private void hover() {
        for (Guy guy : guys.values().toArray(new Guy[0])) {
            Mob mob = guy.brain.mob();
            Player player = guy.brain.getOwner().getPlayer();
            if (!mob.isValid() || player == null || !player.isValid()) {
                if (!mob.isValid()) {
                    guys.remove(guy.brain.ownerId(), guy);
                }
                continue;
            }
            if (guy.dash != null || guy.scan != null) {
                continue;
            }
            if (guy.pendingAttacker != null && Bukkit.getCurrentTick() >= guy.retryAt) {
                guy.scan = new Scan(guy, guy.pendingAttacker, guy.pendingType);
                guy.scan.runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);
                continue;
            }

            Location goal = hoverPoint(player, guy.settings);
            Location now = mob.getLocation();
            boolean far = !now.getWorld().equals(goal.getWorld()) || now.distanceSquared(goal) > SNAP_DISTANCE * SNAP_DISTANCE;
            Location next = far ? goal : now.clone().add(goal.clone().subtract(now).toVector().multiply(SETTLE));
            next.setYaw(player.getLocation().getYaw());
            next.setPitch(0);
            mob.teleport(next);
            mob.setVelocity(new Vector());
            mob.setFallDistance(0);
        }
    }

    /** True while this Guy is out on a dash, so the normal following must leave him alone. */
    public boolean busy(BrainShikigami brain) {
        Guy guy = guys.get(brain.ownerId());
        return guy != null && guy.brain == brain && (guy.dash != null || guy.scan != null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (striking || !(event.getDamager() instanceof Player owner) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Guy guy = guys.get(owner.getUniqueId());
        if (guy == null) {
            return;
        }
        Mob mob = guy.brain.mob();
        if (!mob.isValid()) {
            guys.remove(owner.getUniqueId());
            return;
        }
        if (guy.dash != null || guy.scan != null || victim.equals(mob) || !Targets.isTargetable(guy.brain.getOwner(), victim)) {
            return;
        }

        guy.dash = new Dash(guy, victim);
        guy.dash.runTaskTimer(BattlePlugin.getInstance(), Math.max(1, guy.settings.delayTicks()), 1L);
    }

    /** Adds up the damage players do to the owner, and what they did it with. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerHurt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner) || !(event.getDamager() instanceof Player attacker) || owner.equals(attacker)) {
            return;
        }
        Guy guy = guys.get(owner.getUniqueId());
        if (guy == null || !guy.brain.mob().isValid()) {
            return;
        }
        double amount = event.getFinalDamage();
        if (!(amount > 0)) {
            return;
        }

        // Only damage that came from an ability can be tied to one (a plain punch has none). An ability's damage is
        // dealt through the DamageService, which is "dealing" while this event runs.
        Class<? extends Ability> used = null;
        AbilityUseLog log = BattlePlugin.getInstance().getAbilityUseLog();
        if (log != null && BattlePlugin.getInstance().getDamageService().isDealing()) {
            used = log.recent(attacker.getUniqueId(), guy.settings.windowTicks());
        }

        Tally tally = guy.tallies.computeIfAbsent(attacker.getUniqueId(), id -> new Tally());
        tally.total += amount;
        if (used != null) {
            tally.byAbility.merge(used, amount, Double::sum);
        }
        considerLearning(guy, attacker.getUniqueId());
    }

    /** Once one player has done enough damage, and an ability Guy does not know yet is among it, line up a scan. */
    private void considerLearning(Guy guy, UUID attacker) {
        Tally tally = guy.tallies.get(attacker);
        if (guy.pendingAttacker != null || tally == null || tally.total < guy.settings.learnDamage()) {
            return;
        }
        Class<? extends Ability> best = null;
        double bestDamage = 0;
        for (Map.Entry<Class<? extends Ability>, Double> entry : tally.byAbility.entrySet()) {
            if (!guy.brain.getOwner().getLoadout().has(entry.getKey()) && entry.getValue() > bestDamage) {
                best = entry.getKey();
                bestDamage = entry.getValue();
            }
        }
        if (best != null) {
            guy.pendingAttacker = attacker;
            guy.pendingType = best;
        }
    }

    private static String nameOf(Ability ability) {
        String name = ability.getDisplayName();
        return name == null || name.startsWith("Default") ? ability.getId() : name;
    }

    private static void announce(Guy guy, Ability ability) {
        Location at = guy.brain.mob().getLocation();
        Component message = Component.text("[Guy] ", NamedTextColor.AQUA, TextDecoration.BOLD)
            .append(Component.text("Machine Learning complete! I can now use " + nameOf(ability) + "!", NamedTextColor.WHITE));
        for (Player player : at.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(at) <= 32 * 32) {
                player.sendMessage(message);
            }
        }
        at.getWorld().playSound(at, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.6f);
    }

    /** Moves a mob one step along a direction, facing it. Arriving never overshoots. */
    private static void flyStep(Mob mob, Vector toward, double speed) {
        double length = toward.length();
        if (length < 1.0E-6) {
            return;
        }
        Location next = mob.getLocation().add(toward.clone().multiply(Math.min(speed, length) / length));
        if (toward.getX() * toward.getX() + toward.getZ() * toward.getZ() > 1.0E-6) {
            next.setDirection(toward);
        }
        mob.teleport(next);
        mob.setFallDistance(0);
    }

    /** The scan: out to the player, a sweep of light blue over them, back to the owner, then the ability is learned. */
    private final class Scan extends BukkitRunnable {
        private final Guy guy;
        private final UUID attackerId;
        private final Class<? extends Ability> type;
        private int phase = 0;              // 0 = going out, 1 = scanning, 2 = coming back
        private int age = 0;
        private boolean scanned = false;

        Scan(Guy guy, UUID attackerId, Class<? extends Ability> type) {
            this.guy = guy;
            this.attackerId = attackerId;
            this.type = type;
            guy.brain.mob().getPathfinder().stopPathfinding();
        }

        @Override
        public void run() {
            Mob mob = guy.brain.mob();
            Player owner = guy.brain.getOwner().getPlayer();
            if (!mob.isValid() || owner == null || !owner.isValid()) {
                guy.scan = null;
                cancel();
                return;
            }
            age++;
            Player victim = Bukkit.getPlayer(attackerId);
            boolean victimHere = victim != null && victim.isValid() && victim.getWorld().equals(mob.getWorld());

            if (phase == 0) {
                if (!victimHere || age > MAX_SCAN_TRIP_TICKS) {
                    phase = 2;                  // cannot get to them: go home and try again later
                    age = 0;
                    return;
                }
                Vector toward = standPoint(mob, victim).toVector().subtract(mob.getLocation().toVector());
                if (toward.length() <= 0.6) {
                    phase = 1;
                    age = 0;
                    return;
                }
                flyStep(mob, toward, guy.settings.dashSpeed());
            } else if (phase == 1) {
                if (!victimHere) {
                    phase = 2;
                    age = 0;
                    return;
                }
                hold(mob, victim);
                sweep(mob, victim);
                if (age >= guy.settings.scanTicks()) {
                    scanned = true;
                    phase = 2;
                    age = 0;
                }
            } else {
                Vector toward = hoverPoint(owner, guy.settings).toVector().subtract(mob.getLocation().toVector());
                if (!mob.getWorld().equals(owner.getWorld()) || toward.length() <= HOME_DISTANCE || age > MAX_RETURN_TICKS) {
                    finish();
                    return;
                }
                flyStep(mob, toward, guy.settings.returnSpeed());
            }
        }

        /** Where Guy hovers to scan: out from the player on the side he came from, at about chest height. */
        private Location standPoint(Mob mob, Player victim) {
            Vector away = mob.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-6) {
                away = new Vector(0, 0, 1);
            }
            away.normalize().multiply(guy.settings.scanDistance());
            return victim.getLocation().add(away).add(0, victim.getHeight() * 0.6, 0);
        }

        /** Stays at the stand point, looking at the player, following them if they move. */
        private void hold(Mob mob, Player victim) {
            Location goal = standPoint(mob, victim);
            Location next = mob.getLocation().add(goal.toVector().subtract(mob.getLocation().toVector()).multiply(0.35));
            next.setDirection(victim.getBoundingBox().getCenter().subtract(next.toVector()));
            mob.teleport(next);
        }

        /** Light blue rings sweeping up and down over the player, and a beam from Guy, like a scanner. */
        private void sweep(Mob mob, Player victim) {
            double progress = age / (double) Math.max(1, guy.settings.scanTicks());
            double fraction = (progress * 2) % 1.0;                          // two passes, up then down
            double level = (fraction < 0.5 ? fraction * 2 : 2 - fraction * 2) * victim.getHeight();
            Location base = victim.getLocation();
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8;
                base.getWorld().spawnParticle(Particle.DUST, base.getX() + Math.cos(angle) * 0.8, base.getY() + level,
                    base.getZ() + Math.sin(angle) * 0.8, 1, 0, 0, 0, 0, SCAN_BLUE);
            }
            Location body = victim.getBoundingBox().getCenter().toLocation(base.getWorld());
            base.getWorld().spawnParticle(Particle.END_ROD, body, 2, 0.3, 0.5, 0.3, 0.01);

            if (age % 2 == 0) {                                              // a beam from Guy to the player
                Vector line = body.toVector().subtract(mob.getBoundingBox().getCenter());
                for (int i = 1; i <= 8; i++) {
                    Location point = mob.getBoundingBox().getCenter().add(line.clone().multiply(i / 9.0)).toLocation(base.getWorld());
                    base.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, SCAN_BLUE);
                }
            }
            if (age % 10 == 1) {
                base.getWorld().playSound(body, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, (float) (1.0 + progress));
            }
        }

        private void finish() {
            guy.scan = null;
            cancel();
            if (!scanned) {
                guy.retryAt = Bukkit.getCurrentTick() + SCAN_RETRY_TICKS;    // the scan did not happen: still wanted
                return;
            }
            guy.pendingAttacker = null;
            guy.pendingType = null;
            guy.tallies.remove(attackerId);
            BattlePlayer owner = guy.brain.getOwner();
            if (!owner.getLoadout().has(type)) {
                Ability ability = new AbilityFactory().create(type, owner);
                owner.getLoadout().add(AbilitySlot.ATTACK, ability);        // onto the magma cream, whichever item it came from
                announce(guy, ability);
            }
        }
    }

    /** One trip: out to the victim, a bump, and back. */
    private final class Dash extends BukkitRunnable {
        private final Guy guy;
        private final LivingEntity victim;
        private boolean returning = false;
        private int age = 0;

        Dash(Guy guy, LivingEntity victim) {
            this.guy = guy;
            this.victim = victim;
            guy.brain.mob().getPathfinder().stopPathfinding();
        }

        @Override
        public void run() {
            Mob mob = guy.brain.mob();
            Player player = guy.brain.getOwner().getPlayer();
            if (!mob.isValid() || player == null || !player.isValid()) {
                finish();
                return;
            }
            age++;

            if (!returning) {
                out(mob);
            } else {
                back(mob, player);
            }
        }

        private void out(Mob mob) {
            boolean gone = !victim.isValid() || victim.isDead() || !victim.getWorld().equals(mob.getWorld());
            if (gone || age > guy.settings.giveUpTicks()) {
                startReturn();                      // could not get there in time (or they are already gone)
                return;
            }

            if (mob.getBoundingBox().expand(0.3).overlaps(victim.getBoundingBox())) {
                bump(mob);
                startReturn();
                return;
            }

            Vector toward = victim.getBoundingBox().getCenter().subtract(mob.getBoundingBox().getCenter());
            fly(mob, toward, guy.settings.dashSpeed());
        }

        private void back(Mob mob, Player player) {
            Vector toward = hoverPoint(player, guy.settings).toVector().subtract(mob.getLocation().toVector());
            if (!mob.getWorld().equals(player.getWorld()) || toward.length() <= HOME_DISTANCE
                || age > MAX_RETURN_TICKS) {
                finish();
                return;
            }
            fly(mob, toward, guy.settings.returnSpeed());
        }

        private void startReturn() {
            returning = true;
            age = 0;
        }

        /** Moves the mob one step along a direction, facing it. Arriving never overshoots. */
        private void fly(Mob mob, Vector toward, double speed) {
            double length = toward.length();
            if (length < 1.0E-6) {
                return;
            }
            Vector step = toward.clone().multiply(Math.min(speed, length) / length);
            Location next = mob.getLocation().add(step);
            next.setDirection(new Vector(toward.getX(), 0, toward.getZ()).lengthSquared() < 1.0E-6 ? mob.getLocation().getDirection() : toward);
            mob.teleport(next);
            mob.setFallDistance(0);
        }

        private void bump(Mob mob) {
            BattlePlayer owner = guy.brain.getOwner();
            // Not counted as the owner attacking: the listener ignores damage dealt while this flag is up.
            striking = true;
            try {
                BattlePlugin.getInstance().getDamageService().damage(owner, victim, guy.settings.damage());
            } finally {
                striking = false;
            }

            // a small shove in a random direction, and up
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            Knockback.set(victim, new Vector(Math.cos(angle) * guy.settings.knockback(), guy.settings.lift(),
                Math.sin(angle) * guy.settings.knockback()));

            Location at = victim.getBoundingBox().getCenter().toLocation(victim.getWorld());
            at.getWorld().spawnParticle(Particle.CRIT, at, 8, 0.25, 0.25, 0.25, 0.1);
            at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 1.3f);
            guy.brain.body().play("attack");
        }

        private void finish() {
            guy.dash = null;
            cancel();
        }
    }
}
