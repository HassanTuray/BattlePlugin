package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;

/** What the marking arrow does when it lands: marks, and nothing else. No damage, no knockback. */
final class MarkRaijinCallback implements ProjectileHitCallback {
    private final MarkRaijin ability;

    MarkRaijinCallback(MarkRaijin ability) {
        this.ability = ability;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        if (result.hasHitEntity()) {
            ability.mark(result.getHitEntity());
            return;
        }

        // The arrow ends up on the surface it hit; step back along its flight so the mark is in the open air.
        Location spot = result.getHitLocation();
        Vector back = result.getVirtualProjectile().getVelocity().clone();
        if (back.lengthSquared() > 1.0E-9) {
            spot.subtract(back.normalize().multiply(0.35));
        }
        ability.mark(spot);
    }
}
