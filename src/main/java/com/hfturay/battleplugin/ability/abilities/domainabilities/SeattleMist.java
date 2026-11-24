package com.hfturay.battleplugin.ability.abilities.domainabilities;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.domainservice.DomainService;
import com.hfturay.battleplugin.domainservice.seattlemist.SeattleMistEffects;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.shikigamiservice.DespawnRules;
import com.hfturay.battleplugin.shikigamiservice.EndReactions;
import com.hfturay.battleplugin.shikigamiservice.FollowAndChase;
import com.hfturay.battleplugin.shikigamiservice.Shikigami;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;
import com.hfturay.battleplugin.shikigamiservice.Targeting;
import com.hfturay.battleplugin.shikigamiservice.TargetingStrategy;
import com.hfturay.battleplugin.targetingservice.Targets;

/**
 * Domain: Seattle Mist (on the amethyst shard). A grey dome closes, and a cutscene shows it filling with mist until
 * almost nothing can be seen; then "The Mist is Here" (see SeattleMistEffects). The mist stays for the whole domain,
 * and the Mist Stalker is let loose in it. The mist is drawn by the MistFog resource pack (resourcepack/mistfog).
 *
 * The Mist Stalker: a gaunt black thing about 4 blocks tall on six spindly legs, with a jutting neck and tiny white eyes
 * (models/mist_stalker.bbmodel, made by models/tools/make_mist_stalker.js). For now it runs at the nearest enemy (a
 * player, another player's shikigami or a hostile mob) and strikes when it reaches them, or follows the caster, and
 * everyone can see it. Later it will hunt one chosen player, who alone can see it.
 *
 * For testing, the mist also covers the caster (affects-caster: 1) and the domain costs no chaos (chaos-cost: 0).
 * Every number comes from abilities.yml (SeattleMist section), read when it is cast.
 */
@RequiresSettings({ "cooldown", "chaos-cost", "radius", "thickness", "form-ticks", "duration-ticks",
    "mist-thickness", "affects-caster",
    "health", "damage", "attack-range", "attack-interval-ticks", "attack-hit-delay-ticks",
    "target-range", "follow-distance", "leash-distance", "speed" })
public class SeattleMist extends DomainAbility {
    /** The BetterModel model: plugins/BetterModel/models/mist_stalker.bbmodel (copied by deploy). */
    private static final String MODEL = "mist_stalker";

    public SeattleMist(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Domain: Seattle Mist");
    }

    @Override
    protected DomainService.Spec buildSpec() {
        SeattleMistEffects.Settings settings = new SeattleMistEffects.Settings(
            Math.max(0, Math.min(1, setting("mist-thickness"))), setting("affects-caster") > 0);
        SeattleMistEffects effects = new SeattleMistEffects(settings, this::releaseStalker);
        double radius = setting("radius");

        return new DomainService.Spec("Seattle Mist", radius, setting("thickness"),
            settingInt("form-ticks"), settingInt("duration-ticks"), Material.LIGHT_GRAY_CONCRETE,
            (int) tuning().number(getId(), "night-vision-amplifier", 0), () -> effects.scene(radius), effects);
    }

    /** Lets the Mist Stalker loose at this spot. It lasts until the domain ends (or something kills it first). */
    private Shikigami releaseStalker(Location where) {
        Zombie zombie = where.getWorld().spawn(where, Zombie.class);
        BrainShikigami stalker = new BrainShikigami(zombie, owner, stalkerSpec());
        BattlePlugin.getInstance().getShikigamiService().add(stalker);
        return stalker;
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

    private ShikigamiSpec stalkerSpec() {
        return new ShikigamiSpec(
            "Mist Stalker",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            nearestPrey(),
            new FollowAndChase(setting("follow-distance"), setting("leash-distance"), setting("speed")),
            // no time limit of its own: the domain takes it away when it ends
            List.of(
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
