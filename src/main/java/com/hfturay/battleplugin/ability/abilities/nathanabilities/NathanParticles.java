package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.particleservice.EffectLibSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

import de.slikey.effectlib.effect.AnimatedBallEffect;
import de.slikey.effectlib.effect.EquationEffect;
import de.slikey.effectlib.effect.FlameEffect;

/**
 * Nathan's visuals. These shapes (equation curves, animated ball, flame) are worth EffectLib, so they are
 * EffectLibSpecs: a fresh effect is built for every play and the ParticleService owns it.
 */
final class NathanParticles {
    private NathanParticles() {}

    private static final String ARC_X = "2 * sin(t * 3.14 / 180)";
    private static final String ARC_Z = "2 * cos(t * 3.14 / 180)";

    /** A 5-tick curved slash. The look differs per swing; the shape and timing are the same. */
    private static ParticleSpec slash(float size, String y, String z, Consumer<EquationEffect> colors) {
        return EffectLibSpec.of((effects, ctx) -> {
            EquationEffect e = new EquationEffect(effects);
            colors.accept(e);
            e.particle = Particle.DUST;
            e.particleSize = size;
            e.xEquation = ARC_X;
            e.yEquation = y;
            e.zEquation = z;
            e.particles = 36;
            e.period = 1;
            e.iterations = ctx.iterationsFor(e.period, 5);
            e.orient = true;
            e.orientPitch = true;
            e.maxSteps = 360;
            return e;
        });
    }

    // Three-Hits: flat purple swing, then a red upward swing, then a black downward swing.
    static final ParticleSpec THREE_HITS_FIRST = slash(0.75f, "0", ARC_Z, e -> e.color = Color.PURPLE);
    static final ParticleSpec THREE_HITS_SECOND = slash(0.75f, "-2 * cos(t * 3.14 / 180)", "0", e -> e.color = Color.RED);
    static final ParticleSpec THREE_HITS_THIRD = slash(0.75f, "2 * cos(t * 3.14 / 180)", "0",
        e -> e.color = Color.BLACK);

    // Grand Slam's upward slash.
    static final ParticleSpec UPWARD_SLASH = slash(1f, "-2 * cos(t * 3.14 / 180)", "0",
        e -> e.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE)));

    /** Flames where a slammed target hits the ground. */
    static final ParticleSpec SLAM_LANDING = EffectLibSpec.of((effects, ctx) -> {
        FlameEffect e = new FlameEffect(effects);
        e.particle = Particle.DUST;
        e.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE));
        e.particleSize = 1.5f;
        e.particles = 60;
        e.period = 1;
        e.iterations = ctx.iterationsFor(e.period, 2);
        return e;
    });

    /** The Ascended: a tall swirling ball around the player for the whole buff. */
    static final ParticleSpec ASCENDED_AURA = EffectLibSpec.of((effects, ctx) -> {
        AnimatedBallEffect e = new AnimatedBallEffect(effects);
        e.particle = Particle.DUST;
        e.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE));
        e.particles = 150;
        e.particlesPerIteration = 10;
        e.size = 1f;
        e.xFactor = 1f;
        e.yFactor = 2f;
        e.zFactor = 1f;
        e.period = 1;
        e.iterations = ctx.iterationsFor(e.period, 260);
        return e;
    });

    /** Where a swing's slash is drawn: halfway between caster and target, a block up, facing the way the caster looks. */
    static Location midpoint(BattlePlayer owner, Entity target) {
        Location ownerLocation = owner.getPlayer().getLocation();
        Location targetLocation = target.getLocation();
        return ownerLocation.clone().add(targetLocation).multiply(0.5).add(0, 1.0, 0);
    }
}
