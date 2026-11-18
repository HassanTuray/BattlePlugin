package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.HitScanAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.Anchor;
import com.hfturay.battleplugin.particleservice.ParticleService;
import com.hfturay.battleplugin.targetingservice.HitScan;

/**
 * An instant beam to the first thing in `range`. Every number comes from abilities.yml (Lazer section).
 * Switch On A Lazer doubles it for a while through the damage scale, which is not a setting: it is the state of that buff.
 */
@RequiresSettings({ "cooldown", "range", "damage" })
public class Lazer extends HitScanAbility {
    private Location targetLocation = null;

    /** 1 normally; Switch On A Lazer multiplies it while its buff lasts. */
    private double damageScale = 1;

    public Lazer(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Lazer");
    }

    @Override
    protected Activation onActivate() {
        BattlePlayer player = this.getOwner();
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity target = HitScan.HitScanEntities(player, setting("range"), collisionMode);

        if (target == null) {
            return Activation.FAILED;
        }

        if (target instanceof LivingEntity) {
            targetLocation = target.getLocation().clone().add(0, 0.5, 0);
            BattlePlugin.getInstance().getDamageService().damage(player, target, setting("damage") * damageScale);
            playParticleEffect();
        }

        return Activation.USED;
    }

    @Override
    public void playParticleEffect() {
        Location eye = owner.getPlayer().getEyeLocation();
        Location start = eye.clone().add(eye.getDirection().multiply(0.3));

        BattlePlugin.getInstance().getParticleService()
            .beam(owner, LazerParticles.BEAM, Anchor.at(start), Anchor.at(targetLocation), ParticleService.UNTIL_DONE);
    }

    public double getDamageScale() {
        return this.damageScale;
    }

    public void setDamageScale(double damageScale) {
        this.damageScale = damageScale;
    }
}
