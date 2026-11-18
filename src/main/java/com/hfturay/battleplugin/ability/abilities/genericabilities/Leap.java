package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.FallImmunityService;
import com.hfturay.battleplugin.combatservice.Knockback;

/**
 * Launches the caster the way they are looking. Every number comes from abilities.yml (Leap section).
 * `lift` is the flat upward speed; `pitch-lift` adds more the more level the caster looks (none when looking straight up or down).
 */
@RequiresSettings({ "cooldown", "horizontal-force", "lift", "pitch-lift" })
public class Leap extends Ability {

    public Leap(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Leap");
    }

    @Override
    protected Activation onActivate() {
        playParticleEffect();

        Vector direction = owner.getPlayer().getEyeLocation().getDirection().normalize();
        Vector launch = direction.multiply(setting("horizontal-force"));

        double pitch = owner.getPlayer().getEyeLocation().getPitch();
        double pitchFactor = (90 - Math.abs(pitch)) / 90.0;

        launch.setY(setting("lift") + (pitchFactor * setting("pitch-lift")));

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
