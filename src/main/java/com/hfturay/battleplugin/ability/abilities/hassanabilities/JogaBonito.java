package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * Joga Bonito: kicks a soccer ball from the caster's feet, along where they look.
 *
 * If the ball hits a creature it does a little damage, knocks them back a little, holds their movement still for a
 * moment and flashes their screen white; then the ball rolls back to the caster and Joga Bonito can be used again at
 * once, with no cooldown. If the ball misses (it hits a wall or the ground, or runs out of range) the cooldown starts.
 * While the ball is out it cannot be kicked again.
 *
 * Every number comes from abilities.yml (JogaBonito section), read each time the ball is kicked.
 */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "gravity", "damage", "knockback", "lift", "stun-ticks",
    "return-speed" })
public class JogaBonito extends ProjectileAbility {

    private boolean out = false;       // the ball is flying or rolling back
    private boolean hit = false;       // the ball in the air has hit a creature

    public JogaBonito(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Joga Bonito");
        this.particles = HassanParticles.BALL_TRAIL;
    }

    @Override
    protected String blockedReason() {
        return out ? "Your ball is still out" : null;
    }

    @Override
    protected Activation onActivate() {
        double gravity = setting("gravity");
        Player player = owner.getPlayer();

        // from the feet (a little in front, so it does not start inside the caster), flying where they look
        Vector direction = player.getEyeLocation().getDirection();
        Location start = player.getLocation().add(0, 0.3, 0).add(direction.clone().setY(0).normalize().multiply(0.6));

        VirtualProjectile ball = new VirtualProjectile(
            start,
            direction,
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new JogaBonitoCallback(this, setting("damage"), setting("knockback"), setting("lift"), settingInt("stun-ticks")),
            particles
        );
        ball.setVisual(HassanVisuals.SOCCER_BALL);          // removed the moment the ball ends
        ball.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));

        out = true;
        hit = false;
        double returnSpeed = setting("return-speed");
        this.virtualProjectile = ball;
        launch();

        // Watches the ball. If it ends without having hit a creature, that was a miss: the cooldown starts now.
        BattlePlugin.getInstance().getAbilityScheduler().schedule(new ScheduledAbilityTask(settingInt("max-ticks") + 5, this) {
            @Override
            protected void onTick() {
                if (ball.isFinished()) {
                    finishNow();
                }
            }

            @Override
            protected void onFinish() {
                if (!hit) {
                    out = false;
                    startCooldown();
                }
            }

            @Override
            protected void onCancel() {
                if (!hit) {
                    out = false;
                }
            }
        });

        player.getWorld().playSound(start, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 1.3f);
        this.returnSpeed = returnSpeed;
        return Activation.USED_NO_COOLDOWN;
    }

    private double returnSpeed;

    /** Called by the ball when it hits a creature: it rolls back to the caster, and no cooldown is started. */
    void onBallHit(Location where) {
        hit = true;
        BattlePlugin.getInstance().getAbilityScheduler().schedule(new BallReturnTask(this, owner, where, returnSpeed));
    }

    /** Called when the ball is back with the caster (or the trip was cut short): it can be kicked again. */
    void ballHome() {
        out = false;
    }
}
