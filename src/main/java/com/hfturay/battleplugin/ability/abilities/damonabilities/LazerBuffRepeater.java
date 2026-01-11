package com.hfturay.battleplugin.ability.abilities.damonabilities;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class LazerBuffRepeater extends ScheduledAbilityTask{

    private BattlePlayer owner;

    public LazerBuffRepeater(BattlePlayer owner, int delayTicks) {
        super(delayTicks);
        this.owner = owner;
    }


    @Override
    public void run() {
        Ability ability = owner.getAttackAbilities().get(2);

        if (ability instanceof Lazer) {
            ((Lazer)ability).setDamage(((Lazer)ability).getDamage() / 2);
        }
    }    
}
