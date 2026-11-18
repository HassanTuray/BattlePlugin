package com.hfturay.battleplugin.ability.abilities.fireabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.Anchor;
import com.hfturay.battleplugin.targetingservice.AOE;

/**
 * A huge blast of fire in a wide rectangle in front of the caster, reaching a little behind them too. Everyone inside
 * is hit at once and set on fire; the flames shown scatter outward over a few ticks. Every number comes from
 * abilities.yml (MajesticDestroyerFlame section), read each time it is cast.
 *
 * The area points exactly where the caster looks, up and down included (straight up shoots straight up), and
 * spreads to both sides of that direction. It is checked against each target's body centre. Walls do not stop it.
 */
@RequiresSettings({ "cooldown", "damage", "fire-ticks", "range", "width", "back-reach", "height", "speed", "count", "smoke-count" })
public class MajesticDestroyerFlame extends Ability {

    public MajesticDestroyerFlame(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Majestic Destroyer Flame");
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();
        cast(player.getLocation().add(0, 1.0, 0), player.getEyeLocation().getDirection(), player.getLocation().getYaw());
        return Activation.USED;
    }

    /**
     * Fires the blast from `center` along `direction`, exactly as a player's cast does, with the owner as the
     * credited attacker. A shikigami calls this (on its own instance of the ability) to cast with the same numbers.
     * No cooldown or checks here: the caller does those. `yawDegrees` only matters when aiming straight up or down.
     */
    public void cast(Location center, Vector direction, double yawDegrees) {
        double range = setting("range");
        double width = setting("width");
        double back = setting("back-reach");
        double height = setting("height");
        double damage = setting("damage");
        int fireTicks = settingInt("fire-ticks");

        // The blast has its own three axes: forward is exactly where the caster looks (up and down included), right
        // is sideways to that, and up is perpendicular to both. The area is a box along those axes.
        Vector forward = direction.clone().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());      // forward x world-up
        if (right.lengthSquared() < 1.0E-6) {
            // Looking straight up or down: there is no "sideways" from the view, so use the way the body faces.
            double yaw = Math.toRadians(yawDegrees);
            right = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        }
        right.normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        double farthest = Math.sqrt(Math.max(range, back) * Math.max(range, back) + width * width + height * height) + 2;
        List<Entity> candidates = AOE.getTargets(owner, center, farthest);

        for (Entity target : candidates) {
            Vector offset = target.getBoundingBox().getCenter().subtract(center.toVector());
            double along = offset.dot(forward);
            double across = offset.dot(right);
            double vertical = offset.dot(up);

            boolean inside = along >= -back && along <= range && Math.abs(across) <= width && Math.abs(vertical) <= height;
            if (!inside || !(target instanceof LivingEntity living)) {
                continue;
            }
            // Fire only lands on someone the hit actually reached (immune or untargetable targets are skipped).
            if (BattlePlugin.getInstance().getDamageService().damage(owner, target, damage)) {
                living.setFireTicks(Math.max(living.getFireTicks(), fireTicks));
            }
        }

        FlameBlastSpec flames = new FlameBlastSpec(forward, right, up, range, width, height, settingInt("count"), settingInt("smoke-count"), setting("speed"));
        BattlePlugin.getInstance().getParticleService().play(owner, flames, Anchor.at(center), null,
            com.hfturay.battleplugin.particleservice.ParticleService.UNTIL_DONE);
    }
}
