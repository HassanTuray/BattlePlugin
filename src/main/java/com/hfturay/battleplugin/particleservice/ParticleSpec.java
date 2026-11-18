package com.hfturay.battleplugin.particleservice;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable description of a visual. Define it once (a static constant per ability) and hand it to the
 * ParticleService; every play gets its own instance, so two projectiles never share state.
 */
@FunctionalInterface
public interface ParticleSpec {

    ParticleInstance create(PlayContext context);

    /** Plays several specs together as one (rocket body + flame, two-colour sphere, ...). */
    static ParticleSpec all(ParticleSpec... specs) {
        return context -> {
            List<ParticleInstance> started = new ArrayList<>(specs.length);
            try {
                for (ParticleSpec spec : specs) {
                    started.add(spec.create(context));
                }
            } catch (RuntimeException e) {
                started.forEach(ParticleInstance::stop);
                throw e;
            }

            return new ParticleInstance() {
                @Override
                public void tick(int age) {
                    for (ParticleInstance instance : started) {
                        instance.tick(age);
                    }
                }

                @Override
                public boolean isFinished() {
                    for (ParticleInstance instance : started) {
                        if (!instance.isFinished()) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public void stop() {
                    started.forEach(ParticleInstance::stop);
                }
            };
        };
    }
}
