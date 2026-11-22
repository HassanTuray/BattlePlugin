package com.hfturay.battleplugin.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyDomain;
import com.hfturay.battleplugin.ability.abilities.domainabilities.NeverendingNight;
import com.hfturay.battleplugin.ability.abilities.domainabilities.TitansWrath;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyFallDamageImmunity;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyHitScan;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyHitScanBlocks;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyImmunity;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyProjectile;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummySilence;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunBlastAway;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunFloat;
import com.hfturay.battleplugin.ability.abilities.dummyabilities.DummyStunLock;
import com.hfturay.battleplugin.ability.abilities.airabilities.AirDome;
import com.hfturay.battleplugin.ability.abilities.airabilities.SonicBoom;
import com.hfturay.battleplugin.ability.abilities.earthabilities.EarthDome;
import com.hfturay.battleplugin.ability.abilities.earthabilities.StubbornEarth;
import com.hfturay.battleplugin.ability.abilities.fireabilities.BlastOff;
import com.hfturay.battleplugin.ability.abilities.fireabilities.MajesticDestroyerFlame;
import com.hfturay.battleplugin.ability.abilities.waterabilities.WaterBarrage;
import com.hfturay.battleplugin.ability.abilities.waterabilities.WaterPrison;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Adrenaline;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Brick;
import com.hfturay.battleplugin.ability.abilities.genericabilities.Leap;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.ExplosiveGravityBomb;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.JogaBonito;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.Malware;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.MarkRaijin;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.Shilajit;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.UseRaijin;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.AnthraxPackage;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.MetldownOfWaves;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.PipeBomb;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.RevolutionarySpirit;
import com.hfturay.battleplugin.ability.abilities.muhammadabilities.Shortcut;
import com.hfturay.battleplugin.ability.abilities.shikigamiabilities.SummonGuy;
import com.hfturay.battleplugin.ability.abilities.shikigamiabilities.SummonPink;
import com.hfturay.battleplugin.ability.abilities.shikigamiabilities.SummonWarrior;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.GrandSlam;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.OrbThrow;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.StrongLeap;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.TheAscended;
import com.hfturay.battleplugin.ability.abilities.nathanabilities.ThreeHits;

/**
 * Every ability that exists, and which item it goes on. This is what lets a name typed in a command
 * ("pharahrocket") turn into an ability on the right item.
 *
 * Names are matched ignoring case, spaces and punctuation, so "Pharah Rocket", "pharah-rocket" and
 * "pharahrocket" are the same. When you add a new ability, add one line here.
 */
public final class AbilityRegistry {
    private AbilityRegistry() {}

    /** @param key the name shown in tab-completion: the class name, lower-cased */
    public record Entry(String key, Class<? extends Ability> type, AbilitySlot slot) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Entry> BY_NAME = new HashMap<>();

    private static void register(AbilitySlot slot, Class<? extends Ability> type, String... aliases) {
        Entry entry = new Entry(normalize(type.getSimpleName()), type, slot);
        ENTRIES.add(entry);
        BY_NAME.put(entry.key(), entry);
        for (String alias : aliases) {
            BY_NAME.put(normalize(alias), entry);
        }
    }

    /**
     * A move made of several abilities that are loaded and removed together (Flying Raijin = mark + use). Each part is
     * still its own ability on its item; the bundle is only a name that /load, /remove and kits.yml expand.
     */
    private static final Map<String, List<Entry>> BUNDLES = new HashMap<>();
    private static final List<String> BUNDLE_KEYS = new ArrayList<>();

    private static void bundle(String name, Class<?>... parts) {
        List<Entry> entries = new ArrayList<>();
        for (Class<?> part : parts) {
            entries.add(ENTRIES.stream().filter(e -> e.type() == part).findFirst().orElseThrow());
        }
        BUNDLES.put(normalize(name), List.copyOf(entries));
        BUNDLE_KEYS.add(normalize(name));
    }

    static {
        // attack item
        register(AbilitySlot.ATTACK, Brick.class);
        register(AbilitySlot.ATTACK, Homebrew.class);
        register(AbilitySlot.ATTACK, Lazer.class);
        register(AbilitySlot.ATTACK, LandMine.class);
        register(AbilitySlot.ATTACK, PharahRocket.class);
        register(AbilitySlot.ATTACK, Laser.class);
        register(AbilitySlot.ATTACK, BigBang.class, "PersonalSpace");
        register(AbilitySlot.ATTACK, PipeBomb.class);
        register(AbilitySlot.ATTACK, AnthraxPackage.class);
        register(AbilitySlot.ATTACK, MetldownOfWaves.class, "MeltdownOfWaves");
        register(AbilitySlot.ATTACK, OrbThrow.class);
        register(AbilitySlot.ATTACK, ThreeHits.class);
        register(AbilitySlot.ATTACK, GrandSlam.class);
        register(AbilitySlot.ATTACK, Malware.class);
        register(AbilitySlot.ATTACK, ExplosiveGravityBomb.class);
        register(AbilitySlot.ATTACK, JogaBonito.class);

        // mobility item
        register(AbilitySlot.MOBILITY, Leap.class);
        register(AbilitySlot.MOBILITY, Disengage.class);
        register(AbilitySlot.MOBILITY, Teleport.class);
        register(AbilitySlot.MOBILITY, Shortcut.class);
        register(AbilitySlot.MOBILITY, StrongLeap.class);
        register(AbilitySlot.MOBILITY, MarkRaijin.class);
        register(AbilitySlot.MOBILITY, UseRaijin.class);

        // buff item
        register(AbilitySlot.BUFF, Adrenaline.class);
        register(AbilitySlot.BUFF, SwitchOnALazer.class);
        register(AbilitySlot.BUFF, CheeseSandwichAndTangerines.class, "CheeseSandwich");
        register(AbilitySlot.BUFF, RevolutionarySpirit.class);
        register(AbilitySlot.BUFF, TheAscended.class);
        register(AbilitySlot.BUFF, Shilajit.class);

        // elemental item
        register(AbilitySlot.ELEMENTAL, WaterPrison.class);
        register(AbilitySlot.ELEMENTAL, WaterBarrage.class);
        register(AbilitySlot.ELEMENTAL, EarthDome.class);
        register(AbilitySlot.ELEMENTAL, StubbornEarth.class);
        register(AbilitySlot.ELEMENTAL, BlastOff.class);
        register(AbilitySlot.ELEMENTAL, MajesticDestroyerFlame.class);
        register(AbilitySlot.ELEMENTAL, AirDome.class);
        register(AbilitySlot.ELEMENTAL, SonicBoom.class);

        // shikigami item (breeze rod)
        register(AbilitySlot.SHIKIGAMI, SummonWarrior.class, "Warrior");
        register(AbilitySlot.SHIKIGAMI, SummonGuy.class, "Guy");
        register(AbilitySlot.SHIKIGAMI, SummonPink.class, "Pink");

        // domain item (amethyst shard)
        register(AbilitySlot.DOMAIN, NeverendingNight.class);
        register(AbilitySlot.DOMAIN, TitansWrath.class);     // lent to the caster during Neverending Night
        register(AbilitySlot.DOMAIN, DummyDomain.class);

        // bundles (every part must be registered above)
        bundle("FlyingRaijin", MarkRaijin.class, UseRaijin.class);

        // test abilities
        register(AbilitySlot.ATTACK, DummyAOE.class);
        register(AbilitySlot.ATTACK, DummyHitScan.class);
        register(AbilitySlot.ATTACK, DummyHitScanBlocks.class);
        register(AbilitySlot.ATTACK, DummyProjectile.class);
        register(AbilitySlot.BUFF, DummyFallDamageImmunity.class);
        register(AbilitySlot.BUFF, DummyImmunity.class);
        register(AbilitySlot.BUFF, DummySilence.class);
        register(AbilitySlot.BUFF, DummyStunBlastAway.class);
        register(AbilitySlot.BUFF, DummyStunFloat.class);
        register(AbilitySlot.BUFF, DummyStunLock.class);
    }

    /** Lower-cases and strips everything that is not a letter or digit. */
    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Looks an ability up by whatever the user typed. Empty if there is no such ability. */
    public static Optional<Entry> find(String input) {
        return Optional.ofNullable(BY_NAME.get(normalize(input)));
    }

    /** The parts of a bundle ("flyingraijin") in the order they load, or empty if the name is not a bundle. */
    public static Optional<List<Entry>> findBundle(String input) {
        return Optional.ofNullable(BUNDLES.get(normalize(input)));
    }

    /** Everything /load and /remove accept: every ability plus the bundles. */
    public static List<String> loadableKeys() {
        List<String> keys = keys();
        keys.addAll(BUNDLE_KEYS);
        return keys;
    }

    /** The entry for an ability class, or empty if it is not registered. */
    public static Optional<Entry> of(Class<? extends Ability> type) {
        for (Entry entry : ENTRIES) {
            if (entry.type() == type) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /** The names to offer in tab-completion. */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>(ENTRIES.size());
        for (Entry entry : ENTRIES) {
            keys.add(entry.key());
        }
        return keys;
    }
}
