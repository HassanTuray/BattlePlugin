package com.hfturay.battleplugin.ability;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hfturay.battleplugin.ability.abilities.aliabilities.BigBang;
import com.hfturay.battleplugin.ability.abilities.aliabilities.CheeseSandwichAndTangerines;
import com.hfturay.battleplugin.ability.abilities.aliabilities.Laser;
import com.hfturay.battleplugin.ability.abilities.aliabilities.PharahRocket;
import com.hfturay.battleplugin.ability.abilities.aliabilities.Teleport;
import com.hfturay.battleplugin.ability.abilities.damonabilities.Disengage;
import com.hfturay.battleplugin.ability.abilities.damonabilities.Homebrew;
import com.hfturay.battleplugin.ability.abilities.damonabilities.LandMine;
import com.hfturay.battleplugin.ability.abilities.damonabilities.Lazer;
import com.hfturay.battleplugin.ability.abilities.damonabilities.SwitchOnALazer;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyAOE;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyFallDamageImmunity;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyHitScan;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyHitScanBlocks;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyImmunity;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyProjectile;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummySilence;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunBlastAway;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunFloat;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunLock;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Adrenaline;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Brick;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Leap;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.AnthraxPackage;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.MetldownOfWaves;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.PipeBomb;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.RevolutionarySpirit;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.Shortcut;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.GrandSlam;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.OrbThrow;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.StrongLeap;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.TheAscended;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.ThreeHits;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class AbilityFactory {
    // local copy 
    private String KingGorbnakID = "9879aa16-ac68-3671-97cd-94014f583414";

    // public copy
    // private String KingGorbnakID = "d3a36ab7-6397-47c9-91ac-1e84bf1762f1";
    private String BeshboyID = "9ca465a8-9ce6-47ba-a835-da96a4243128";
    private String TheDamesterID = "91ecb5bc-04d8-4f46-9ae1-2fd48823ac2c";
    private String MoID = "2c9324ec-31d8-48b8-b0dc-43878d09e1cc";
    private String EpikramID = "0c62a5a2-e5d4-447a-a710-18e376c2792f";

    private Map<UUID, List<Class<? extends Ability>>> attackLoadouts = Map.of(
        UUID.fromString(KingGorbnakID), List.of(Brick.class, PharahRocket.class, Homebrew.class, AnthraxPackage.class),
        UUID.fromString(BeshboyID), List.of(Brick.class, PharahRocket.class, Laser.class, BigBang.class),
        UUID.fromString(TheDamesterID), List.of(Brick.class, Homebrew.class, Lazer.class, LandMine.class),
        UUID.fromString(MoID), List.of(Brick.class, PipeBomb.class, AnthraxPackage.class, MetldownOfWaves.class),
        UUID.fromString(EpikramID), List.of(Brick.class, OrbThrow.class, ThreeHits.class, GrandSlam.class)
    );

    private Map<UUID, List<Class<? extends Ability>>> mobilityLoadouts = Map.of(
        UUID.fromString(KingGorbnakID), List.of(Leap.class, StrongLeap.class),
        UUID.fromString(BeshboyID), List.of(Leap.class, Teleport.class),
        UUID.fromString(TheDamesterID), List.of(Leap.class, Disengage.class),
        UUID.fromString(MoID), List.of(Leap.class, Shortcut.class),
        UUID.fromString(EpikramID), List.of(Leap.class, StrongLeap.class)
    );

    private Map<UUID, List<Class<? extends Ability>>> buffLoadouts = Map.of(
        UUID.fromString(KingGorbnakID), List.of(Adrenaline.class, TheAscended.class),
        UUID.fromString(BeshboyID), List.of(Adrenaline.class, CheeseSandwichAndTangerines.class),
        UUID.fromString(TheDamesterID), List.of(Adrenaline.class, SwitchOnALazer.class),
        UUID.fromString(MoID), List.of(Adrenaline.class, RevolutionarySpirit.class),
        UUID.fromString(EpikramID), List.of(Adrenaline.class, TheAscended.class)
    ); 

    public void assignAbilities(BattlePlayer battlePlayer) {
        UUID uuid = battlePlayer.getPlayer().getUniqueId();

        for (Class<? extends Ability> abilityClass : 
            attackLoadouts.getOrDefault(uuid, List.of())) {

            battlePlayer.getAttackAbilities().add(instantiate(abilityClass, battlePlayer));
        }

        for (Class<? extends Ability> abilityClass : 
            mobilityLoadouts.getOrDefault(uuid, List.of())) {

            battlePlayer.getMobilityAbilities().add(instantiate(abilityClass, battlePlayer));
        }

        for (Class<? extends Ability> abilityClass : 
            buffLoadouts.getOrDefault(uuid, List.of())) {

            battlePlayer.getBuffAbilities().add(instantiate(abilityClass, battlePlayer));
        }
    }

    private Ability instantiate(Class<? extends Ability> abilityClass, BattlePlayer owner) {
        try {
            Constructor<? extends Ability> abilityConstructor = abilityClass.getConstructor(BattlePlayer.class);
            return abilityConstructor.newInstance(owner);

        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate ability: " + abilityClass.getName(), e);
        }
    }
}
