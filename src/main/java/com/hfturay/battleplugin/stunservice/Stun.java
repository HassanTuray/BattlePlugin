package com.hfturay.battleplugin.stunservice;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.combatservice.ImmunityService;
import com.hfturay.battleplugin.combatservice.Knockback;
import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.ParticleHandle;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * One stun on one entity. Configure the fields, then hand it to StunService.add(). The service ticks it.
 *
 * lockX / lockY / lockZ freeze that axis of movement. forcedVelocity overrides movement completely
 * (the entity is thrown with that velocity every tick). grantImmunity makes the target immune to damage
 * for as long as the stun lasts.
 */
public class Stun {
    /** Crit sparks around the stunned entity's chest. Played once per stun, not rebuilt every tick. */
    private static final ParticleSpec VISUAL = DirectSpec.of(Particle.CRIT)
        .shape(DirectSpec.Shape.POINT)
        .count(5)
        .spread(0.25, 0.4, 0.25)
        .offset(0, 1, 0)
        .build();

    private ParticleHandle visual;
    private boolean immunityGranted = false;

    public int duration;
    public Entity target;
    public boolean grantImmunity = false;
    /** The crit sparks around the stunned entity. Turn off for stuns that have their own look (a water prison). */
    public boolean showSparks = true;
    public boolean lockX = false;
    public boolean lockY = false;
    public boolean lockZ = false;
    public Vector forcedVelocity = null;

    public Stun(Entity target, int duration) {
        this.target = target;
        this.duration = duration;
    }

    public boolean isActive() {
        return duration > 0;
    }

    /** One tick of the stun. Returns true when it is over and should be dropped. */
    boolean tick(ImmunityService immunity) {
        if (duration <= 0) {
            return true;
        }

        if (!target.isValid()) {    // died, left, or despawned
            cancel(immunity);
            return true;
        }

        duration -= 1;
        startVisual();

        if (grantImmunity && !immunityGranted) {
            immunityGranted = true;
            immunity.grant(target.getUniqueId(), duration + 1);   // lasts exactly as long as the stun
        }

        if (forcedVelocity != null) {
            Knockback.set(target, forcedVelocity);
            return duration <= 0;
        }

        Vector newVelocity = target.getVelocity().clone();
        if (lockX) {
            newVelocity.setX(0);
        }
        if (lockY) {
            newVelocity.setY(0);
        }
        if (lockZ) {
            newVelocity.setZ(0);
        }
        Knockback.set(target, newVelocity);

        return duration <= 0;
    }

    /** Ends the stun now: stops the sparks and takes back the immunity it gave. */
    void cancel(ImmunityService immunity) {
        this.duration = 0;
        stopVisual();
        if (immunityGranted) {
            immunityGranted = false;
            immunity.clear(target.getUniqueId());
        }
    }

    /** Starts the sparks on the first tick of the stun; the particle service follows the entity and ends them with the stun. */
    private void startVisual() {
        if (visual != null || !showSparks) {
            return;
        }
        BattlePlugin plugin = BattlePlugin.getInstance();
        if (plugin == null || plugin.getParticleService() == null) {
            return;
        }
        // duration was already decremented for this tick, so +1 covers the whole stun
        visual = plugin.getParticleService().follow(null, VISUAL, target, duration + 1);
    }

    /** Ends the sparks early (stun replaced or removed). */
    public void stopVisual() {
        if (visual != null) {
            visual.cancel();
        }
    }
}
