package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class TheAscendedRepeater extends ScheduledAbilityTask {
    private BattlePlayer owner;
    private int buff;

    public TheAscendedRepeater(int duration, BattlePlayer owner, int buff) {
        super(duration);
        this.owner = owner;
        this.buff = buff;
    }

    @Override
    public void run() {
        GrandSlam grandSlam = owner.getAbility(GrandSlam.class);
        grandSlam.damage = grandSlam.damage - buff;

        ThreeHits threeHits = owner.getAbility(ThreeHits.class);
        threeHits.setDamage(threeHits.getDamage()-  buff); 

        OrbThrow orbThrow = owner.getAbility(OrbThrow.class);
        orbThrow.orbThrowCallback.damage = orbThrow.orbThrowCallback.damage - buff;

        AttributeInstance movement = owner.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return;
        } 

        movement.setBaseValue(0.1); 

        owner.getPlayer().setHealth(8);
    }
}
