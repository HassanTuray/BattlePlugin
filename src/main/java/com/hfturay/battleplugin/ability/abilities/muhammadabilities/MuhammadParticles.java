package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.particleservice.DirectSpec;
import com.hfturay.battleplugin.particleservice.EffectLibSpec;
import com.hfturay.battleplugin.particleservice.ParticleSpec;

import de.slikey.effectlib.effect.WaveEffect;

/** All of Muhammad's particle effects, one constant per effect. (He has no block-based effects, so no visuals file.) */
final class MuhammadParticles {
    private MuhammadParticles() {}

    // ---- Meltdown of Waves ----

    /**
     * The big wave, on EffectLib's WaveEffect: black dust with a yellow cloud. A fresh effect is built for each
     * projectile and runs until the projectile ends. It draws every 2 ticks.
     *
     * How long a dust particle stays on screen is decided by the client and is tied to its size: as far as I know a
     * dust particle lives (8 / a random number from 0.2 to 1.0) ticks, multiplied by its size. So the size below is
     * the knob for how long the wave lingers: 1.2 lived up to about 48 ticks (the smear), 0.7 up to about 28,
     * 0.5 up to about 20. Smaller also means smaller dots, so raise particlesFront/Back to keep it as full.
     */
    private static final float WAVE_PARTICLE_SIZE = 0.7F;

    static final ParticleSpec MELTDOWN_WAVE = EffectLibSpec.of((effects, ctx) -> {
        WaveEffect wave = new WaveEffect(effects);
        wave.autoOrient = true;
        wave.offset = new Vector(0, -0.75, 0);
        wave.mainParticle = Particle.DUST;
        wave.cloudParticle = Particle.DUST;
        wave.color = Color.BLACK;
        wave.cloudColor = Color.YELLOW;
        wave.period = 2;
        wave.iterations = ctx.iterationsFor(wave.period, 1);
        wave.height = 4;
        wave.width = 3;
        wave.heightBack = 0.5F;
        wave.rows = 20;
        wave.lengthFront = 1.5F;
        wave.lengthBack = 4;
        wave.depthFront = 1.5F;
        wave.particleSize = WAVE_PARTICLE_SIZE;
        wave.particlesFront = 15;
        wave.particlesBack = 15;
        return wave;
    });

    // ---- Pipebomb ----

    /** A few small flames trailing off the pipe (the pipe itself is the iron block in MuhammadVisuals.PIPE_BOMB). */
    static final ParticleSpec PIPE_BOMB = DirectSpec.of(Particle.FLAME)
        .shape(DirectSpec.Shape.POINT)
        .count(3)
        .spread(0.08, 0.08, 0.08)
        .build();

    /** Two explosion puffs a block above the impact point. */
    static final ParticleSpec PIPE_BOMB_EXPLOSION = DirectSpec.of(Particle.EXPLOSION)
        .shape(DirectSpec.Shape.POINT)
        .count(2)
        .offset(0, 1, 0)
        .build();

    // ---- Anthrax Package ----

    private static final ParticleSpec ANTHRAX_BLACK = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.BLACK)
        .shape(DirectSpec.Shape.POINT)
        .count(8)
        .spread(0.08, 0.08, 0.08)
        .build();

    private static final ParticleSpec ANTHRAX_GREEN = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.LIME)
        .shape(DirectSpec.Shape.POINT)
        .count(8)
        .spread(0.08, 0.08, 0.08)
        .build();

    /** A black and green ball of dust around the package in flight. */
    static final ParticleSpec ANTHRAX_PACKAGE = ParticleSpec.all(ANTHRAX_BLACK, ANTHRAX_GREEN);

    /**
     * The poison cloud on the ground after impact: one flat ring of radius 4 (matches the damage radius),
     * alternating black and green points. Played once for the whole cloud, drawn every other tick.
     */
    static final ParticleSpec ANTHRAX_ZONE = DirectSpec.of(Particle.DUST)
        .dust(1.5f, Color.BLACK, Color.LIME)
        .shape(DirectSpec.Shape.RING)
        .radius(4)
        .count(30)
        .offset(0, 0.65, 0)
        .interval(2)
        .build();

    // ---- Shortcut ----

    /** Portal swirl marking the saved shortcut location. Played once for the time it stays valid. */
    static final ParticleSpec SHORTCUT_MARKER = DirectSpec.of(Particle.PORTAL)
        .shape(DirectSpec.Shape.POINT)
        .count(6)
        .spread(0.3, 0.5, 0.3)
        .offset(0, 1, 0)
        .interval(2)
        .build();

    // ---- Revolutionary Spirit ----

    /** A red burst that expands out of the player. */
    static final ParticleSpec REVOLUTIONARY_BURST = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.RED)
        .shape(DirectSpec.Shape.SPHERE)
        .count(40)
        .expanding(1.0, 4.2, 16)
        .offset(0, 1, 0)
        .build();

    /** The red aura the burst settles into for the length of the buff. */
    static final ParticleSpec REVOLUTIONARY_AURA = DirectSpec.of(Particle.DUST)
        .dust(1.0f, Color.RED)
        .shape(DirectSpec.Shape.SPHERE)
        .count(40)
        .radius(4)
        .offset(0, 1, 0)
        .interval(2)
        .build();
}
