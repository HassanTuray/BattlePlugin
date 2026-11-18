package com.hfturay.battleplugin.ability.abilities.damonabilities;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;

public class LazerBuffRepeater extends ScheduledAbilityTask{

    private final double multiplier;

    /** @param ability the Lazer that was buffed. It is the same instance that gets restored. */
    public LazerBuffRepeater(int delayTicks, double multiplier, Ability ability) {
        super(delayTicks, ability);
        this.multiplier = multiplier;
    }


    // Also undo it when cancelled, so the extra damage never outlives the buff.
    @Override
    protected void onCancel() {
        onFinish();
    }

    @Override
    protected void onFinish() {
        // Take the multiplier back off the Lazer that was buffed, even if the player has since removed it or loaded another one.
        if (ability instanceof Lazer lazer) {
            lazer.setDamageScale(lazer.getDamageScale() / multiplier);
        }
    }
}
