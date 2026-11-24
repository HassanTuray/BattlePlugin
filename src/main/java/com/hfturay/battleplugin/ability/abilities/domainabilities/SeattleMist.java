package com.hfturay.battleplugin.ability.abilities.domainabilities;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.shikigamiservice.DespawnRules;
import com.hfturay.battleplugin.shikigamiservice.EndReactions;
import com.hfturay.battleplugin.shikigamiservice.FollowAndChase;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;
import com.hfturay.battleplugin.shikigamiservice.TargetingStrategy;
import com.hfturay.battleplugin.shikigamiservice.Targeting;
import com.hfturay.battleplugin.targetingservice.Targets;

/**
 * Domain: Seattle Mist (on the amethyst shard). The finished domain will fill with fog, hide everyone inside from one
 * another (but not from the caster), and set the Mist Stalker on one chosen player, who alone can see it.
 *
 * For now this only summons the Mist Stalker, for testing it: a gaunt black thing about 4 blocks tall on six spindly
 * legs, with a jutting neck and tiny white eyes (models/mist_stalker.bbmodel, made by models/tools/make_mist_stalker.js).
 * It runs at the nearest enemy (a player, another player's shikigami or a hostile mob) and strikes when it reaches
 * them; with nobody about it follows its caster. Everyone can see it for now.
 * Every number comes from abilities.yml (SeattleMist section), read when it is used.
 */
@RequiresSettings({ "cooldown", "health", "damage", "attack-range", "attack-interval-ticks", "attack-hit-delay-ticks",
    "target-range", "follow-distance", "leash-distance", "speed", "lifetime-ticks", "max-active" })
public class SeattleMist extends Ability {
    /** The BetterModel model: plugins/BetterModel/models/mist_stalker.bbmodel (copied by deploy). */
    private static final String MODEL = "mist_stalker";

    public SeattleMist(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Seattle Mist");
    }

    @Override
    protected String blockedReason() {
        return BattlePlugin.getInstance().getShikigamiService()
            .summonBlockedReason(owner, "Mist Stalker", settingInt("max-active"));
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();

        // A few blocks in front of the caster, on the flat (it is big), facing them.
        Vector ahead = player.getLocation().getDirection().setY(0);
        if (ahead.lengthSquared() < 1.0e-6) {
            ahead = new Vector(0, 0, 1);
        }
        Location where = player.getLocation().add(ahead.normalize().multiply(4));
        where.setDirection(player.getLocation().getDirection().multiply(-1));

        Zombie zombie = where.getWorld().spawn(where, Zombie.class);
        BattlePlugin.getInstance().getShikigamiService().add(new BrainShikigami(zombie, owner, spec()));
        return Activation.USED;
    }

    /**
     * The closest enemy within target-range: a player or another player's shikigami that the caster may hit, or a
     * hostile mob (so it has something to stalk with nobody else around). Never the caster or their shikigami.
     */
    private TargetingStrategy nearestPrey() {
        return self -> {
            double range = self.spec().targetRange();
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Entity entity : self.mob().getNearbyEntities(range, range, range)) {
                if (!(entity instanceof LivingEntity living) || !Targets.isTargetable(owner, living)) {
                    continue;
                }
                if (!Targeting.isEnemy(owner, living) && !(living instanceof Enemy)) {
                    continue;
                }
                double distance = living.getLocation().distanceSquared(self.mob().getLocation());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = living;
                }
            }
            return best;
        };
    }

    private ShikigamiSpec spec() {
        return new ShikigamiSpec(
            "Mist Stalker",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            nearestPrey(),
            new FollowAndChase(setting("follow-distance"), setting("leash-distance"), setting("speed")),
            List.of(
                DespawnRules.timeout(settingInt("lifetime-ticks")),
                DespawnRules.ownerDied(),
                DespawnRules.ownerOffline()),
            EndReactions.none(),
            List.of(),
            mob -> {
                if (!(mob instanceof Zombie zombie)) {
                    return;
                }
                zombie.setBaby(false);
                zombie.setShouldBurnInDay(false);
                zombie.setSilent(true);             // it makes no sound at all, which is worse
            },
            // Drawn as the Mist Stalker model (BetterModel). Without that plugin it stays a plain zombie.
            // The model's animations: idle, walk and death play on their own; attack is the one we trigger.
            MODEL,
            Map.of("attack", List.of("attack")),
            settingInt("attack-hit-delay-ticks"));
    }
}
