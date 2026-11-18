package com.hfturay.battleplugin.ability.abilities.airabilities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

/**
 * A thin, fast burst of air flying straight where the caster looks. The first player it touches is hurt and thrown
 * far back along its path. Every number comes from abilities.yml (SonicBoom section), read each time it is cast.
 */
@RequiresSettings({ "cooldown", "damage", "speed", "hitbox", "max-ticks", "knockback", "lift" })
public class SonicBoom extends Ability {

    public SonicBoom(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Sonic Boom");
    }

    @Override
    protected Activation onActivate() {
        double damage = setting("damage");
        double knockback = setting("knockback");
        double lift = setting("lift");

        VirtualProjectile blast = new VirtualProjectile(
            setting("speed"), settingInt("max-ticks"), owner, setting("hitbox"),
            (result, shooter) -> {
                BattlePlugin plugin = BattlePlugin.getInstance();
                plugin.getParticleService().burst(shooter, AirParticles.BOOM_HIT, result.getHitLocation());

                Entity entity = result.getHitEntity();
                if (entity instanceof LivingEntity) {
                    plugin.getDamageService().damage(shooter, entity, damage);

                    // After the damage (which puts the victim's own velocity back), so the throw is what is left.
                    Vector thrust = result.getVirtualProjectile().getDirection().setY(0);
                    if (thrust.lengthSquared() > 1.0E-8) {
                        thrust.normalize().multiply(knockback);
                    }
                    thrust.setY(lift);
                    Knockback.set(entity, thrust);
                }
            },
            AirParticles.BOOM_BODY);

        BattlePlugin.getInstance().getProjectileService().add(blast);
        return Activation.USED;
    }
}
