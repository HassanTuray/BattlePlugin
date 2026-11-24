package com.hfturay.battleplugin.shikigamiservice;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Mob;

/**
 * Everything that makes one kind of shikigami what it is. A new kind is a new spec: pick a targeting strategy, a
 * movement, some despawn rules, an end reaction and optional triggers. The numbers come from abilities.yml
 * when the spec is built, so a reload applies to the next summon.
 *
 * @param name            shown above its head (prefixed with the owner's name by the caller)
 * @param maxHealth       health points (2 = one heart)
 * @param damage          health points per hit (before any buff on the owner's ability)
 * @param attackRange     how close (blocks, edge to edge) it must be to hit
 * @param attackInterval  ticks between hits
 * @param targetRange     how far it looks for enemies
 * @param look            dresses the mob (equipment, size ...). The mob is the hitbox whatever it looks like.
 * @param model           the BetterModel model to draw it with (file name without .bbmodel), or null for a plain mob
 * @param animations      which model animations the brain's names mean, for example "attack" -> [swing_1, swing_2]
 * @param hitDelay        ticks between starting the attack animation and the hit landing (0 = at once)
 * @param onAttack        runs the moment it starts an attack (a sound, a flash), before the hit lands; may do nothing
 */
public record ShikigamiSpec(
    String name,
    double maxHealth,
    double damage,
    double attackRange,
    int attackInterval,
    double targetRange,
    TargetingStrategy targeting,
    Movement movement,
    List<DespawnRule> rules,
    EndReaction onEnd,
    List<ShikigamiTrigger> triggers,
    Consumer<Mob> look,
    String model,
    java.util.Map<String, List<String>> animations,
    int hitDelay,
    Consumer<BrainShikigami> onAttack) {

    /** A spec with nothing extra happening when it attacks. */
    public ShikigamiSpec(String name, double maxHealth, double damage, double attackRange, int attackInterval,
                         double targetRange, TargetingStrategy targeting, Movement movement, List<DespawnRule> rules,
                         EndReaction onEnd, List<ShikigamiTrigger> triggers, Consumer<Mob> look, String model,
                         java.util.Map<String, List<String>> animations, int hitDelay) {
        this(name, maxHealth, damage, attackRange, attackInterval, targetRange, targeting, movement, rules, onEnd,
            triggers, look, model, animations, hitDelay, self -> { });
    }
}
