package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Flying Raijin, part two: teleports the caster to whatever the marking arrow last hit (an entity, wherever it is
 * now, or a spot), or to the arrow itself if it is still flying (which stops the arrow). A poof of white smoke is left where the caster stood, and the mark is erased: it must be marked
 * again. Every number comes from abilities.yml (UseRaijin section).
 */
@RequiresSettings({ "cooldown" })
public class UseRaijin extends Ability {

    public UseRaijin(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Use Raijin");
    }

    private MarkRaijin mark() {
        return owner.getLoadout().find(MarkRaijin.class);
    }

    @Override
    protected String blockedReason() {
        MarkRaijin mark = mark();
        if (mark != null && mark.arrowLocation() != null) {
            return null;                    // an arrow is in the air: jump to it
        }
        if (mark == null || !mark.hasMark()) {
            return "Nothing is marked. Use the Raijin Mark arrow first";
        }
        if (mark.where() == null) {
            mark.clear();
            return "The marked target is gone";
        }
        return null;
    }

    @Override
    protected Activation onActivate() {
        MarkRaijin mark = mark();
        if (mark == null) {
            return Activation.FAILED;
        }

        // An arrow still in flight wins: the caster jumps to it and the arrow ends there.
        Location destination = mark.arrowLocation();
        boolean toArrow = destination != null;
        if (!toArrow) {
            destination = mark.where();
        }
        if (destination == null) {
            return Activation.FAILED;
        }

        Player player = owner.getPlayer();
        Location from = player.getLocation();

        destination = clearOfBlocks(destination);
        destination.setYaw(from.getYaw());
        destination.setPitch(from.getPitch());

        BattlePlugin.getInstance().getParticleService().burst(owner, HassanParticles.RAIJIN_POOF, from);
        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.5f);

        player.teleport(destination);
        player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.8f);

        if (toArrow) {
            mark.stopArrow();
        }
        mark.clear();
        return Activation.USED;
    }

    /** Moves the spot up (at most 3 blocks) until a player fits there, so a mark in a wall's face never buries the caster. */
    private static Location clearOfBlocks(Location spot) {
        Location at = spot.clone();
        for (int i = 0; i < 3; i++) {
            if (at.getBlock().isPassable() && at.clone().add(0, 1, 0).getBlock().isPassable()) {
                break;
            }
            at.add(0, 1, 0);
        }
        return at;
    }
}
