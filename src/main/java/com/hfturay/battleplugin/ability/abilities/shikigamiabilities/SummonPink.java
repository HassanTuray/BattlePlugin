package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.PipeBombCallBack;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.shikigamiservice.DespawnRules;
import com.hfturay.battleplugin.shikigamiservice.GuardOwner;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiTrigger;
import com.hfturay.battleplugin.shikigamiservice.Targeting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Summons Pink!: a shikigami shaped like a normal player, wearing the pink skin (models/skins/pink.png). It follows its
 * owner and only joins the owner's own fight: its target is whoever the owner last hit or whoever last hurt the owner,
 * whichever happened more recently. Every bomb-interval-ticks it throws a cartoon bomb (PinkVisuals.BOMB, with the
 * pipe bomb's blast) at that target in a fast arc, and it punches the
 * target when close. With no target it throws nothing. It never strays more than guard-range from its owner, even
 * when its target is farther away (see GuardOwner).
 * While it is out, its owner also has End it (see EndIt) on the shikigami item. The summon's cooldown starts when Pink!
 * is gone (blown up, killed, timed out), not when it is summoned.
 * Drawn by the pink model (BetterModel); the mob under it is only the hitbox.
 * Every number comes from abilities.yml (SummonPink section), read when it is summoned.
 */
@RequiresSettings({ "cooldown", "health", "damage", "attack-range", "attack-interval-ticks", "attack-hit-delay-ticks", "target-range",
    "guard-range", "follow-distance", "leash-distance", "speed", "lifetime-ticks", "idle-ticks", "max-active", "hit-memory-ticks",
    "bomb-interval-ticks", "bomb-speed", "bomb-gravity", "bomb-hitbox", "bomb-max-ticks", "bomb-radius", "bomb-damage", "bomb-knockback" })
public class SummonPink extends Ability {
    /** The BetterModel model: plugins/BetterModel/models/pink.bbmodel (made by models/tools/make_pink.js, copied by deploy). */
    private static final String MODEL = "pink";

    public SummonPink(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Pink!");
    }

    @Override
    protected String blockedReason() {
        return BattlePlugin.getInstance().getShikigamiService()
            .summonBlockedReason(owner, "Pink!", settingInt("max-active"));
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();

        // A couple of blocks in front of the owner, on the flat.
        Vector ahead = player.getLocation().getDirection().setY(0);
        if (ahead.lengthSquared() < 1.0e-6) {
            ahead = new Vector(0, 0, 1);
        }
        Location where = player.getLocation().add(ahead.normalize().multiply(2));
        where.setDirection(player.getLocation().getDirection().multiply(-1));   // facing the owner at first

        Zombie zombie = where.getWorld().spawn(where, Zombie.class);
        BrainShikigami pink = new BrainShikigami(zombie, owner, spec());
        BattlePlugin.getInstance().getShikigamiService().add(pink);
        lendEndIt(pink);
        // The cooldown starts when Pink! is gone (its end reaction), not now.
        return Activation.USED_NO_COOLDOWN;
    }

    /** Puts End it on the shikigami item for as long as this Pink! is out, and selects it so the next click uses it. */
    private void lendEndIt(BrainShikigami pink) {
        EndIt endIt = new EndIt(owner);
        endIt.bind(pink);
        if (!owner.getLoadout().add(AbilitySlot.SHIKIGAMI, endIt)) {
            return;
        }
        for (int i = 0; i < owner.getLoadout().size(AbilitySlot.SHIKIGAMI)
            && owner.getLoadout().current(AbilitySlot.SHIKIGAMI) != endIt; i++) {
            owner.getLoadout().next(AbilitySlot.SHIKIGAMI);
        }
        owner.getPlayer().sendActionBar(Component.text("End it unlocked", NamedTextColor.LIGHT_PURPLE));
    }

    /** Pink! is gone, however it went: End it goes away and the summon's cooldown starts. */
    private void onPinkEnded() {
        owner.getLoadout().remove(EndIt.class);
        startCooldown();
    }

    /**
     * Throws the cartoon bomb from Pink!'s hand at the middle of its target. It always covers the ground at bomb-speed
     * blocks a tick, so it is quick at any distance, and it is thrown up just hard enough that, falling bomb-gravity
     * every tick, it comes down on the target. More gravity means a higher arc in the same time.
     */
    private void throwBomb(BrainShikigami self) {
        Mob mob = self.mob();
        LivingEntity target = self.target();
        if (target == null) {
            return;
        }

        Location hand = mob.getLocation().add(0, 1.5, 0);
        Vector toTarget = target.getBoundingBox().getCenter().subtract(hand.toVector());
        double flat = Math.hypot(toTarget.getX(), toTarget.getZ());
        if (flat < 1.0E-3) {
            return;                                 // straight above or below: no sensible throw
        }

        double across = setting("bomb-speed");
        double gravity = setting("bomb-gravity");
        // Each tick the bomb first drops by gravity, then moves. After t ticks it has risen t * up - gravity * t(t+1)/2,
        // so to be at the target's height after flat / across ticks it needs this much upward speed.
        double ticks = Math.max(1, flat / across);
        double up = (toTarget.getY() + gravity * ticks * (ticks + 1) / 2) / ticks;
        Vector velocity = new Vector(toTarget.getX() / flat * across, up, toTarget.getZ() / flat * across);

        mob.lookAt(target);
        self.body().play("throw");

        VirtualProjectile bomb = new VirtualProjectile(hand, velocity, velocity.length(), settingInt("bomb-max-ticks"),
            owner, setting("bomb-hitbox"),
            new PipeBombCallBack(setting("bomb-radius"), setting("bomb-damage"), setting("bomb-knockback")),
            PinkParticles.BOMB_FUSE);
        bomb.setVisual(PinkVisuals.BOMB);
        bomb.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));
        BattlePlugin.getInstance().getProjectileService().add(bomb);
    }

    private ShikigamiSpec spec() {
        return new ShikigamiSpec(
            owner.getPlayer().getName() + "'s Pink!",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            // Only the owner's own fight: whoever they hit or whoever hurt them, whichever was more recent.
            // Nobody else: when the owner is not fighting, Pink! has no target and throws nothing.
            Targeting.ownersFight(settingInt("hit-memory-ticks")),
            // Never strays more than guard-range from its owner, even with a target; the bombs reach the rest.
            new GuardOwner(setting("guard-range"), setting("follow-distance"), setting("leash-distance"), setting("speed")),
            List.of(
                DespawnRules.timeout(settingInt("lifetime-ticks")),
                DespawnRules.inactivity(settingInt("idle-ticks")),
                DespawnRules.ownerDied(),
                DespawnRules.ownerOffline()),
            (self, reason) -> onPinkEnded(),
            List.of(
                // Throws a bomb at its target, then waits bomb-interval-ticks. No target, no bomb.
                new ShikigamiTrigger(
                    ShikigamiTrigger.Conditions.hasTarget(),
                    Math.max(1, settingInt("bomb-interval-ticks")),
                    this::throwBomb)),
            mob -> {
                if (!(mob instanceof Zombie zombie)) {
                    return;
                }
                zombie.setBaby(false);
                zombie.setShouldBurnInDay(false);
                zombie.setSilent(true);             // it looks like a player, so no zombie groans
            },
            // Drawn as the pink model (BetterModel). Without that plugin it stays a plain zombie.
            // The model's animations: idle, walk and death play on their own; attack is the one we trigger.
            MODEL,
            Map.of(
                "attack", List.of("attack"),
                "throw", List.of("attack")),
            settingInt("attack-hit-delay-ticks"));
    }
}
