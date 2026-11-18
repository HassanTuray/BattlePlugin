package com.hfturay.battleplugin.ability;

import java.lang.reflect.Constructor;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Builds abilities for players. Which abilities a player starts with is decided by kits.yml (see KitService),
 * not here.
 */
public class AbilityFactory {

    /** Builds a fresh instance of an ability for a player. Used by kits, saved loadouts and the /load command. */
    public Ability create(Class<? extends Ability> abilityClass, BattlePlayer owner) {
        try {
            Constructor<? extends Ability> abilityConstructor = abilityClass.getConstructor(BattlePlayer.class);
            return abilityConstructor.newInstance(owner);

        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate ability: " + abilityClass.getName(), e);
        }
    }
}
