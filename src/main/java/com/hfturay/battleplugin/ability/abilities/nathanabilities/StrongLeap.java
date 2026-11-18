package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.ability.abilities.genericabilities.LeapParticles;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.FallImmunityService;
import com.hfturay.battleplugin.combatservice.Knockback;

/**
 * A bigger Leap. The first use is as written in abilities.yml (StrongLeap section); every use after that is multiplied
 * by the number of seconds since the last one, but never by less than the two minimums.
 */
@RequiresSettings({ "cooldown", "horizontal-force", "lift", "pitch-lift", "min-horizontal-multiplier", "min-vertical-multiplier" })
public class StrongLeap extends Ability {
    private long lastUseTime = -1;
    private double horizMutliplier = 1;
    private double vertMultiplier = 1;

    public StrongLeap(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Strong Leap");
    }

    @Override
    protected Activation onActivate() {
        long currentUseTime = System.currentTimeMillis();

        if (lastUseTime == -1) {
            lastUseTime = System.currentTimeMillis();
        } else {
            horizMutliplier = Math.max(settingInt("min-horizontal-multiplier"), (currentUseTime - lastUseTime) / 1000L);
            vertMultiplier = Math.max(settingInt("min-vertical-multiplier"), (currentUseTime - lastUseTime) / 1000L);

            lastUseTime = currentUseTime;
        }

        playParticleEffect();

        Vector direction = owner.getPlayer().getEyeLocation().getDirection().normalize();
        Vector launch = direction.multiply(setting("horizontal-force") * horizMutliplier);

        double pitch = owner.getPlayer().getEyeLocation().getPitch();
        double pitchFactor = (90 - Math.abs(pitch)) / 90.0;

        launch.setY(setting("lift") * vertMultiplier + (pitchFactor * setting("pitch-lift")));

        Knockback.set(owner.getPlayer(), launch);
        owner.getPlayer().setFallDistance(0);

        FallImmunityService fallImmunity = BattlePlugin.getInstance().getFallImmunityService();
        fallImmunity.grantFallImmunity(owner.getPlayer());

        return Activation.USED;
    }

    @Override
    public void playParticleEffect() {
        BattlePlugin.getInstance().getParticleService().burst(owner, LeapParticles.SPEC, owner.getPlayer().getLocation());
    }
}
