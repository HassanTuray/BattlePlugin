package com.hfturay.battleplugin.ability.abilities.damonabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.targetingservice.AOE;
import com.hfturay.battleplugin.combatservice.Knockback;

public class LandMineRepeater extends ScheduledAbilityTask{
    private Location location;
    private final double radius;
    private final double damage;
    private final double knockback;
    private BattlePlayer owner;

    // The armed-mine visual is started once and lives exactly as long as the mine can trigger.
    private final ParticleHandle visual;

    public LandMineRepeater(BattlePlayer owner, Location location, int duration, double radius, double damage, double knockback, Ability ability) {
        super(duration, ability);

        this.owner = owner;
        this.location = location;
        this.radius = radius;
        this.damage = damage;
        this.knockback = knockback;
        this.visual = BattlePlugin.getInstance().getParticleService()
            .zone(owner, LandMineParticles.SPEC, location, duration)
            .survivesOwnerDeath();      // the mine stays armed after its owner dies, so it stays visible
    }

    // A placed mine stays armed even if its owner dies.
    @Override
    public boolean cancelOnOwnerDeath() {
        return false;
    }

    @Override
    protected void onCancel() {
        visual.cancel();
    }

    @Override
    protected void onTick() {
        List<Entity> targets = AOE.getTargets(owner, location, radius);

        if (targets.isEmpty()) {
            return;
        }

        visual.cancel();
        BattlePlugin.getInstance().getParticleService().burst(owner, LandMineParticles.EXPLOSION, location);
        for (Entity entity : targets) {
            if (entity instanceof Damageable) {
                BattlePlugin.getInstance().getDamageService().damage(owner, entity, damage);

                Knockback.away(location, entity, knockback);
            }
        }

        cancel();    // triggered: no more ticks (the visual is already ended above)
    }

    @Override
    protected void onFinish() {
        // Nobody stepped on it before it timed out.
        visual.cancel();
    }
}
