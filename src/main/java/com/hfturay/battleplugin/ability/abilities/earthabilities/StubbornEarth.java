package com.hfturay.battleplugin.ability.abilities.earthabilities;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

/**
 * The caster becomes hard to hurt and hard to move: strong resistance and slowness for a while, with earth crumbs
 * swirling around them in a sphere. Every number comes from abilities.yml (StubbornEarth section), read each cast.
 *
 * Resistance is the server's own potion effect, and damage in this plugin is ordinary server damage, so it is
 * reduced like any other hit. Amplifier 0 is level I, so level IV is 3 and Slowness III is 2.
 */
@RequiresSettings({ "cooldown", "duration-ticks", "resistance-amplifier", "slow-amplifier", "radius", "count" })
public class StubbornEarth extends Ability {

    /** Radians the swirl turns per tick. */
    private static final double SPIN = 0.12;

    public StubbornEarth(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Stubborn Earth");
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();
        int duration = settingInt("duration-ticks");

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, settingInt("resistance-amplifier"), false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, settingInt("slow-amplifier"), false, false, true));

        ParticleSpec swirl = new SwirlingBlocksSpec(EarthBlocks.distinct(), settingInt("count"), setting("radius"), SPIN);
        BattlePlugin.getInstance().getParticleService().follow(owner, swirl, player, duration);
        return Activation.USED;
    }
}
