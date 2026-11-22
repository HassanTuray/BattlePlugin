package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
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
import com.hfturay.battleplugin.shikigamiservice.Targeting;

/**
 * Summons Pink!: a shikigami shaped like a normal player, wearing the pink skin (models/skins/pink.png). For now it
 * fights like the Warrior without the flame: it follows its owner and punches whatever they hit, or the nearest enemy.
 * Drawn by the pink model (BetterModel); the mob under it is only the hitbox.
 * Every number comes from abilities.yml (SummonPink section), read when it is summoned.
 */
@RequiresSettings({ "cooldown", "health", "damage", "attack-range", "attack-interval-ticks", "attack-hit-delay-ticks", "target-range",
    "follow-distance", "leash-distance", "speed", "lifetime-ticks", "idle-ticks", "max-active", "hit-memory-ticks" })
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
        return Activation.USED;
    }

    private ShikigamiSpec spec() {
        return new ShikigamiSpec(
            owner.getPlayer().getName() + "'s Pink!",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            // Whatever the owner hit most recently (any mob or player), otherwise the nearest enemy.
            Targeting.ownersLastTarget(settingInt("hit-memory-ticks")).orElse(Targeting.nearest()),
            new FollowAndChase(setting("follow-distance"), setting("leash-distance"), setting("speed")),
            List.of(
                DespawnRules.timeout(settingInt("lifetime-ticks")),
                DespawnRules.inactivity(settingInt("idle-ticks")),
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
                zombie.setSilent(true);             // it looks like a player, so no zombie groans
            },
            // Drawn as the pink model (BetterModel). Without that plugin it stays a plain zombie.
            // The model's animations: idle, walk and death play on their own; attack is the one we trigger.
            MODEL,
            Map.of("attack", List.of("attack")),
            settingInt("attack-hit-delay-ticks"));
    }
}
