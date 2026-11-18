package com.hfturay.battleplugin.particleservice;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;

/**
 * For shapes that are worth EffectLib (Atom, Wave, Equation, Vortex, ...). The factory builds a FRESH effect for
 * every play, so nothing is shared between projectiles or activations. The service places it on the anchor,
 * keeps it following if the anchor moves, and cancels it when the play ends.
 *
 * In the factory, size the effect with ctx.iterationsFor(period, n):
 * <pre>
 *   EffectLibSpec.of((em, ctx) -> {
 *       AtomEffect e = new AtomEffect(em);
 *       e.period = 1;
 *       e.iterations = ctx.iterationsFor(e.period, 3);   // N ticks, forever, or a fixed 3 when UNTIL_DONE
 *       return e;
 *   });
 * </pre>
 * Anchored to an entity, the effect follows it through EffectLib itself (use the effect's own yOffset/offset).
 */
public final class EffectLibSpec implements ParticleSpec {

    @FunctionalInterface
    public interface Factory {
        Effect create(EffectManager effects, PlayContext context);
    }

    private final Factory factory;

    private EffectLibSpec(Factory factory) {
        this.factory = factory;
    }

    public static EffectLibSpec of(Factory factory) {
        return new EffectLibSpec(factory);
    }

    @Override
    public ParticleInstance create(PlayContext context) {
        Effect effect = factory.create(context.effects(), context);
        Anchor anchor = context.anchor();
        Anchor target = context.target();

        Entity entity = anchor.entity();
        if (entity != null) {
            effect.setEntity(entity);
        } else {
            effect.setLocation(place(anchor));
        }
        if (target != null) {
            effect.setTarget(target.location().clone());
        }
        effect.start();

        return new ParticleInstance() {
            @Override
            public void tick(int age) {
                if (age == 0 || effect.isDone()) {
                    return;
                }
                if (entity == null && anchor.isMoving()) {
                    effect.setLocation(place(anchor));
                }
                if (target != null && target.isMoving()) {
                    effect.setTarget(target.location().clone());
                }
            }

            @Override
            public boolean isFinished() {
                return effect.isDone();
            }

            @Override
            public void stop() {
                if (!effect.isDone()) {
                    effect.cancel(false);
                }
            }
        };
    }

    private static Location place(Anchor anchor) {
        Location location = anchor.location().clone();
        location.setDirection(anchor.direction());
        return location;
    }
}
