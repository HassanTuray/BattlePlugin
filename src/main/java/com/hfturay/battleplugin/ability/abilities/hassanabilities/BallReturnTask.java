package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.visualservice.VisualAnchor;
import com.hfturay.battleplugin.visualservice.VisualHandle;

/**
 * The soccer ball rolling back to the caster after it hit someone. It flies straight at wherever the caster is now,
 * through anything in the way, and when it arrives (or the trip takes too long) Joga Bonito can be used again.
 */
final class BallReturnTask extends ScheduledAbilityTask {

    /** The longest the trip may take, in ticks, so a caster who runs away can never leave the ball stuck out. */
    private static final int MAX_TICKS = 100;

    private final JogaBonito ability;
    private final BattlePlayer owner;
    private final double speed;
    private final Location position;
    private Vector direction = new Vector(0, 0, 1);
    private VisualHandle visual;

    BallReturnTask(JogaBonito ability, BattlePlayer owner, Location from, double speed) {
        super(MAX_TICKS, ability);
        this.ability = ability;
        this.owner = owner;
        this.speed = Math.max(0.2, speed);
        this.position = from.clone();
    }

    @Override
    protected void onTick() {
        Player player = owner.getPlayer();
        if (player == null || !player.isValid() || player.getWorld() != position.getWorld()) {
            cancel();
            return;
        }

        if (visual == null) {       // started on the first tick, when the task is running and the anchor is valid
            visual = BattlePlugin.getInstance().getVisualService().follow(owner, HassanVisuals.SOCCER_BALL, anchor());
        }

        Vector toward = player.getLocation().add(0, 0.8, 0).toVector().subtract(position.toVector());
        double distance = toward.length();
        if (distance <= Math.max(speed, 1.0)) {
            finishNow();            // it has reached them
            return;
        }

        direction = toward.multiply(1.0 / distance);
        position.add(direction.clone().multiply(speed));
        position.getWorld().spawnParticle(Particle.CLOUD, position, 1, 0.05, 0.05, 0.05, 0);
    }

    private VisualAnchor anchor() {
        return new VisualAnchor() {
            @Override public Location location() { return position; }
            @Override public Vector direction() { return direction; }
            @Override public boolean isValid() { return isActive(); }
        };
    }

    @Override
    protected void onFinish() {
        stopVisual();
        Player player = owner.getPlayer();
        if (player != null && player.isValid()) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOL_HIT, SoundCategory.PLAYERS, 1.0f, 1.4f);
        }
        ability.ballHome();
    }

    @Override
    protected void onCancel() {
        stopVisual();
        ability.ballHome();
    }

    private void stopVisual() {
        if (visual != null) {
            visual.cancel();
        }
    }
}
