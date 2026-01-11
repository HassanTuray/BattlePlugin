package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

public class GrandSlamRepeater extends ScheduledAbilityTask {
    private GrandSlam ability; 

    public GrandSlamRepeater(int duration, GrandSlam ability) {
        super(duration);
        
        this.ability = ability;
    }

    @Override
    public void run() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (!cooldownLogger.isOnCooldown(ability.getOwner(), ability)) {
            cooldownLogger.startCooldown(ability.getOwner(), ability);
            ability.reset();

            BattlePlugin.getInstance().getLogger().info("Did not activate slam, going on cooldown");
        } 
    }
}
