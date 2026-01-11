package com.hfturay.battleplugin.customeffects;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.util.ParticleUtil;

public class ExpandingCircleEffect extends CircleEffect {

    public double maxRadius = 2.5;
    public double growthPerTick = 0.2;
    public boolean linger = false;

    public ExpandingCircleEffect(EffectManager manager) {
        super(manager);

        type = EffectType.REPEATING;
        particle = ParticleUtil.getParticle("VILLAGER_HAPPY");
        period = 2;
        iterations = 50;
    }

    @Override
    public void onRun() {

        if (radius < maxRadius) {
            radius += growthPerTick;
        } else if (radius >= maxRadius && !linger) {
            cancel();
            return;
        }

        super.onRun();
    }
}
