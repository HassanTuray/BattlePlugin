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
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;

/**
 * Summons Guy: a small floating AI helper (a round white head with a black visor and light blue eyes, over a white cone
 * body). He follows his owner and does not pick fights himself: every time the owner damages something he dashes to it,
 * bumps it (see GuyHelper) and flies back. Drawn by the guy model (BetterModel); the mob under it is only the hitbox.
 * Every number comes from abilities.yml (SummonGuy section), read when he is summoned.
 */
@RequiresSettings({ "cooldown", "health", "damage", "knockback", "lift", "dash-speed", "dash-delay-ticks", "hit-memory-ticks",
    "hover-height", "hover-behind", "learn-damage", "learn-window-ticks", "scan-ticks", "scan-distance", "lifetime-ticks", "idle-ticks", "max-active" })
public class SummonGuy extends Ability {
    /** The BetterModel model: plugins/BetterModel/models/guy.bbmodel (made by models/tools/make_guy.js, copied by deploy). */
    private static final String MODEL = "guy";

    public SummonGuy(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Guy");
    }

    @Override
    protected String blockedReason() {
        return BattlePlugin.getInstance().getShikigamiService()
            .summonBlockedReason(owner, "Guy", settingInt("max-active"));
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
        BrainShikigami guy = new BrainShikigami(zombie, owner, spec());
        BattlePlugin.getInstance().getShikigamiService().add(guy);
        BattlePlugin.getInstance().getGuyHelper().register(guy, new GuyHelper.Settings(
            setting("damage"), setting("knockback"), setting("lift"), setting("dash-speed"),
            settingInt("dash-delay-ticks"), settingInt("hit-memory-ticks"), setting("dash-speed") * 0.7,
            setting("hover-height"), setting("hover-behind"), setting("learn-damage"), settingInt("learn-window-ticks"),
            settingInt("scan-ticks"), setting("scan-distance")));
        return Activation.USED;
    }

    private ShikigamiSpec spec() {
        return new ShikigamiSpec(
            owner.getPlayer().getName() + "'s Guy",
            setting("health"),
            0,                      // he never hits on his own: the dash (GuyHelper) does the hitting
            0,
            20,
            12,
            self -> null,           // and never chooses a target
            // He does not walk: GuyHelper floats him above and behind the owner every tick.
            self -> { },
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
                zombie.setSilent(true);             // a helper robot should not groan
            },
            // Drawn as the guy model (BetterModel). Without that plugin it stays a plain zombie.
            // The model's animations: idle, walk and death play on their own; attack is the one we trigger.
            MODEL,
            Map.of("attack", List.of("attack")),
            0);
    }
}
