package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class TheAscendedRepeater extends ScheduledAbilityTask {
    /** The server's normal walking speed, put back when the buff ends. Not a setting: it is the game's own default. */
    private static final double NORMAL_WALK_SPEED = 0.1;

    private final BattlePlayer owner;
    private final double buff;
    private final double endHealth;

    // The exact abilities that received the bonus when The Ascended was used. Any of them can be null
    // (the player did not have it). Taking the bonus back from these same instances, not whatever the
    // player has now, keeps damage correct if abilities are loaded or removed while the buff is active.
    private final GrandSlam grandSlam;
    private final ThreeHits threeHits;
    private final OrbThrow orbThrow;

    public TheAscendedRepeater(int duration, BattlePlayer owner, double buff, double endHealth, Ability ability,
                               GrandSlam grandSlam, ThreeHits threeHits, OrbThrow orbThrow) {
        super(duration, ability);
        this.owner = owner;
        this.buff = buff;
        this.endHealth = endHealth;
        this.grandSlam = grandSlam;
        this.threeHits = threeHits;
        this.orbThrow = orbThrow;
    }

    @Override
    protected void onFinish() {
        revertDamage();

        AttributeInstance movement = owner.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        movement.setBaseValue(NORMAL_WALK_SPEED);

        owner.getPlayer().setHealth(endHealth);
    }

    // Cancelled early (died, left, ability removed, shutdown): take the bonus back, but skip the health penalty.
    @Override
    protected void onCancel() {
        revertDamage();

        AttributeInstance movement = owner.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(NORMAL_WALK_SPEED);
        }
    }

    private void revertDamage() {
        if (grandSlam != null) {
            grandSlam.addDamageBonus(-buff);
        }

        if (threeHits != null) {
            threeHits.addDamageBonus(-buff);
        }

        if (orbThrow != null) {
            orbThrow.addDamageBonus(-buff);
        }
    }
}
