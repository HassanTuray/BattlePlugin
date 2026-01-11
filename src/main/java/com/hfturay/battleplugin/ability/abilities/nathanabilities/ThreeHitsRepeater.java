package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

public class ThreeHitsRepeater extends ScheduledAbilityTask {
    private ThreeHits ability;

    public ThreeHitsRepeater(int duration, ThreeHits ability) {
        super(duration);
        this.ability = ability;
    }

    @Override
    public void run() {
        BattlePlugin.getInstance().getLogger().info("Combo stopped");
        this.ability.resetRepeater();
        this.ability.resetAttack();
        
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();
        cooldownLogger.startCooldown(ability.getOwner(), ability);
    }
}
