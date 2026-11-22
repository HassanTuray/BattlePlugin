package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.shikigamiservice.EndReason;
import com.hfturay.battleplugin.targetingservice.AOE;
import com.hfturay.battleplugin.targetingservice.Targets;

/**
 * End it: lent to Pink!'s owner (on the shikigami item) for as long as Pink! is out, and taken away the moment it is
 * used. Pink! stops whatever it was doing, grows to `height` blocks tall over grow-ticks (every other size grows by
 * the same amount), then runs at its target and blows up with a blast of `radius` blocks when it reaches them. The
 * blast kills Pink!, which starts the summon's cooldown (see SummonPink). If it loses its target, or has been running
 * for max-ticks, it blows up where it is.
 * Every number comes from abilities.yml (EndIt section), read when it is used.
 */
@RequiresSettings({ "cooldown", "height", "grow-ticks", "speed", "max-ticks", "radius", "damage", "horizontal-force", "lift" })
public class EndIt extends Ability {
    /** How tall the pink model is at normal size, in blocks (a player is 2). */
    private static final double MODEL_HEIGHT = 2.0;

    private BrainShikigami pink;

    public EndIt(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("End it");
    }

    /** The Pink! this End it belongs to. Set by SummonPink when it lends the ability. */
    void bind(BrainShikigami pink) {
        this.pink = pink;
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    protected String blockedReason() {
        if (pink == null || !pink.mob().isValid()) {
            return "Pink! is not out";
        }
        if (pink.target() == null) {
            return "Pink! has nobody to run at";
        }
        return null;
    }

    @Override
    protected Activation onActivate() {
        LivingEntity target = pink.target();
        pink.takeOver();
        new Charge(pink, target).runTaskTimer(BattlePlugin.getInstance(), 1L, 1L);

        // used up: take it off the item (next tick, so the loadout is not changed while it is being used)
        BattlePlugin.getInstance().getServer().getScheduler().runTask(BattlePlugin.getInstance(),
            () -> owner.getLoadout().remove(EndIt.class));
        return Activation.USED_NO_COOLDOWN;
    }

    /** Grows, then runs at the target, then explodes. Stops on its own if Pink! dies some other way first. */
    private final class Charge extends BukkitRunnable {
        private final BrainShikigami pink;
        private LivingEntity target;
        private final double scale = setting("height") / MODEL_HEIGHT;
        private final int growTicks = Math.max(1, settingInt("grow-ticks"));
        private final int maxTicks = settingInt("max-ticks");
        private final double speed = setting("speed");
        private int age = 0;

        Charge(BrainShikigami pink, LivingEntity target) {
            this.pink = pink;
            this.target = target;
            Mob mob = pink.mob();
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 3.0f, 1.4f);
        }

        @Override
        public void run() {
            Mob mob = pink.mob();
            if (!mob.isValid() || mob.isDead()) {
                cancel();
                return;
            }
            age++;

            // growing: stands still and swells up to its full size
            if (age <= growTicks) {
                resize(mob, 1 + (scale - 1) * age / growTicks);
                mob.setVelocity(new Vector(0, mob.getVelocity().getY(), 0));
                if (age == growTicks) {
                    mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.6f);
                }
                return;
            }

            if (!stillThere(target)) {
                explode(mob);           // the target is gone: it goes off where it stands
                return;
            }
            if (age - growTicks > maxTicks || mob.getBoundingBox().expand(1.0).overlaps(target.getBoundingBox())) {
                explode(mob);
                return;
            }

            // running: straight at the target, turned to face them; gravity still works on it
            Vector flat = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0);
            if (flat.lengthSquared() > 1.0E-6) {
                flat.normalize().multiply(speed);
                mob.setVelocity(new Vector(flat.getX(), mob.getVelocity().getY(), flat.getZ()));
                Location look = mob.getLocation().setDirection(flat);
                mob.setRotation(look.getYaw(), 0);
            }
        }

        private boolean stillThere(LivingEntity entity) {
            return entity != null && entity.isValid() && !entity.isDead() && Targets.isTargetable(owner, entity)
                && entity.getWorld().equals(pink.mob().getWorld());
        }

        /** Model and hitbox both, so it is as big to hit as it looks. */
        private void resize(Mob mob, double factor) {
            pink.body().scale(factor);
            AttributeInstance size = mob.getAttribute(Attribute.GENERIC_SCALE);
            if (size != null) {
                size.setBaseValue(factor);
            }
        }

        private void explode(Mob mob) {
            cancel();
            World world = mob.getWorld();
            Location center = mob.getLocation().add(0, 1, 0);
            double radius = setting("radius");

            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 8.0f, 0.5f);
            world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.MASTER, 6.0f, 0.6f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 6, radius / 3, radius / 4, radius / 3, 0);
            world.spawnParticle(Particle.FLAME, center, 200, radius / 2, radius / 3, radius / 2, 0.3);
            world.spawnParticle(Particle.LARGE_SMOKE, center, 120, radius / 2, radius / 3, radius / 2, 0.1);

            // The owner is the credited attacker; the blast never hurts them or their own shikigami.
            for (Entity entity : AOE.getTargets(owner, center, radius)) {
                if (entity instanceof Damageable) {
                    BattlePlugin.getInstance().getDamageService().damage(owner, entity, setting("damage"));
                    Knockback.awayWithLift(center, entity, setting("horizontal-force"), setting("lift"));
                }
            }

            // the blast is the end of Pink!: killing it starts the summon's cooldown (SummonPink's end reaction)
            pink.setEndReason(EndReason.KILLED);
            mob.setHealth(0);
        }
    }
}
