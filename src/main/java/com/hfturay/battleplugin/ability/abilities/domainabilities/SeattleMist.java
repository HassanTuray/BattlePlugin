package com.hfturay.battleplugin.ability.abilities.domainabilities;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
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
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiTrigger;
import com.hfturay.battleplugin.shikigamiservice.TargetingStrategy;

/**
 * Domain: Seattle Mist (on the amethyst shard). A grey dome closes, and a cutscene shows it filling with mist until
 * almost nothing can be seen; then "The Mist is Here" (see SeattleMistEffects). The mist stays for the whole domain,
 * and the Mist Stalker is let loose in it. The mist is drawn by the MistFog resource pack (resourcepack/mistfog).
 *
 * The Mist Stalker: a gaunt black thing about 4 blocks tall on six spindly legs, with a jutting neck and tiny white eyes
 * (models/mist_stalker.bbmodel, made by models/tools/make_mist_stalker.js). It hunts one chosen player caught in the
 * domain, picked by SeattleMistEffects: only they can see it, hear its footsteps and its screech, and it runs at them
 * and strikes when it reaches them. When they are gone another player is chosen.
 *
 * For testing: the mist also covers the caster (affects-caster: 1), the caster can be the chosen one
 * (stalker-hunts-caster: 1; it chases and lunges at them, but cannot hurt its own caster), and the domain costs no
 * chaos (chaos-cost: 0). Every number comes from abilities.yml (SeattleMist section), read when it is cast.
 */
@RequiresSettings({ "cooldown", "chaos-cost", "radius", "thickness", "form-ticks", "duration-ticks",
    "mist-thickness", "affects-caster", "stalker-hunts-caster",
    "health", "damage", "attack-range", "attack-interval-ticks", "attack-hit-delay-ticks",
    "target-range", "follow-distance", "leash-distance", "speed" })
public class SeattleMist extends DomainAbility {
    /** The BetterModel model: plugins/BetterModel/models/mist_stalker.bbmodel (copied by deploy). */
    private static final String MODEL = "mist_stalker";
    /** Ticks between footsteps while it walks: half its 10-tick stride. */
    private static final int STEP_TICKS = 5;

    public SeattleMist(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Domain: Seattle Mist");
    }

    @Override
    protected DomainService.Spec buildSpec() {
        SeattleMistEffects.Settings settings = new SeattleMistEffects.Settings(
            Math.max(0, Math.min(1, setting("mist-thickness"))), setting("affects-caster") > 0,
            setting("stalker-hunts-caster") > 0);
        SeattleMistEffects effects = new SeattleMistEffects(settings, this::releaseStalker);
        double radius = setting("radius");

        return new DomainService.Spec("Seattle Mist", radius, setting("thickness"),
            settingInt("form-ticks"), settingInt("duration-ticks"), Material.LIGHT_GRAY_CONCRETE,
            (int) tuning().number(getId(), "night-vision-amplifier", 0), () -> effects.scene(radius), effects);
    }

    /**
     * Lets the Mist Stalker loose at this spot, hunting whoever `chosen` says. It lasts until the domain ends (or
     * something kills it first).
     */
    private BrainShikigami releaseStalker(Location where, Supplier<Player> chosen) {
        Zombie zombie = where.getWorld().spawn(where, Zombie.class);
        BrainShikigami stalker = new BrainShikigami(zombie, owner, stalkerSpec(chosen));
        BattlePlugin.getInstance().getShikigamiService().add(stalker);
        return stalker;
    }

    /** Always the chosen one, while they are in the stalker's world; nobody else, ever. */
    private static TargetingStrategy hunt(Supplier<Player> chosen) {
        return self -> {
            Player prey = chosen.get();
            return prey != null && prey.isOnline() && !prey.isDead() && prey.getWorld().equals(self.mob().getWorld()) ? prey : null;
        };
    }

    /** Plays a sound at the stalker that only the chosen one hears. */
    private static void toChosen(Supplier<Player> chosen, BrainShikigami self, Sound sound, float volume, float pitch) {
        Player prey = chosen.get();
        if (prey != null && prey.isOnline()) {
            prey.playSound(self.mob().getEyeLocation(), sound, SoundCategory.HOSTILE, volume, pitch);
        }
    }

    private ShikigamiSpec stalkerSpec(Supplier<Player> chosen) {
        return new ShikigamiSpec(
            "Mist Stalker",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            hunt(chosen),
            new FollowAndChase(setting("follow-distance"), setting("leash-distance"), setting("speed")),
            // no time limit of its own: the domain takes it away when it ends
            List.of(
                DespawnRules.ownerDied(),
                DespawnRules.ownerOffline()),
            EndReactions.none(),
            List.of(
                // Spider steps while it walks: one for each set of three legs that steps, so two every half-second
                // stride (the walk animation in make_mist_stalker.js). Only the chosen one hears them.
                new ShikigamiTrigger(
                    self -> self.mob().getPathfinder().hasPath(),
                    STEP_TICKS,
                    self -> toChosen(chosen, self, Sound.ENTITY_SPIDER_STEP, 1.0f,
                        0.65f + ThreadLocalRandom.current().nextFloat() * 0.2f))),
            mob -> {
                if (!(mob instanceof Zombie zombie)) {
                    return;
                }
                zombie.setBaby(false);
                zombie.setShouldBurnInDay(false);
                zombie.setSilent(true);             // none of a zombie's own noises: its sounds are below
            },
            // Drawn as the Mist Stalker model (BetterModel). Without that plugin it stays a plain zombie.
            // The model's animations: idle, walk and death play on their own; attack is the one we trigger.
            MODEL,
            Map.of("attack", List.of("attack")),
            settingInt("attack-hit-delay-ticks"),
            // As it lunges: a ghast's scream and an enderman's scream together, both pitched down. Only the chosen one
            // hears it.
            self -> {
                toChosen(chosen, self, Sound.ENTITY_GHAST_SCREAM, 2.0f, 0.6f);
                toChosen(chosen, self, Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
            });
    }
}
