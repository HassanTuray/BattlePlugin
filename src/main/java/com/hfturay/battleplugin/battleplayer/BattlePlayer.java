package com.hfturay.battleplugin.battleplayer;

import org.bukkit.entity.Player;

import java.util.ArrayList;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.items.AttackItem;
import com.hfturay.battleplugin.items.BuffItem;
import com.hfturay.battleplugin.items.ElementalItem;
import com.hfturay.battleplugin.items.MobilityItem;
import com.hfturay.battleplugin.items.ShikigamiItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BattlePlayer {
    private long timestamp;
    private Player bukkitPlayer;

    private AttackItem attackItem;
    private MobilityItem mobilityItem;
    private BuffItem buffItem;
    private ElementalItem elementalItem;
    private ShikigamiItem shikigamiItem;

    // What this player has on each item, and which one each item has selected.
    // Any number of abilities per item (including none) is fine.
    private final AbilityLoadout loadout = new AbilityLoadout();

    public BattlePlayer(Player player) {
        this.bukkitPlayer = player;

        this.attackItem = new AttackItem(this);
        this.mobilityItem = new MobilityItem(this);
        this.buffItem = new BuffItem(this);
        this.elementalItem = new ElementalItem(this);
        this.shikigamiItem = new ShikigamiItem(this);

        // The loadout starts empty. PlayerLogger.register fills it from the saved file or kits.yml.
    }

    // Can just return the player whenever we want members from this
    public Player getPlayer() {
        return this.bukkitPlayer;
    }

    public AbilityLoadout getLoadout() {
        return this.loadout;
    }

    public ArrayList<Ability> getAttackAbilities() {
        return loadout.list(AbilitySlot.ATTACK);
    }

    public ArrayList<Ability> getMobilityAbilities() {
        return loadout.list(AbilitySlot.MOBILITY);
    }

    public ArrayList<Ability> getBuffAbilities() {
        return loadout.list(AbilitySlot.BUFF);
    }

    public ArrayList<Ability> getElementalAbilities() {
        return loadout.list(AbilitySlot.ELEMENTAL);
    }

    /** The first ability of this class the player has on any item, or null if they do not have it. */
    public <T extends Ability> T getAbility(Class<T> abilityClass) {
        return loadout.find(abilityClass);
    }

    // ---- selecting and using the current ability on an item ----

    /** Right-click: move to the next ability on this item and show its name. */
    public void nextAbility(AbilitySlot slot) {
        Ability ability = loadout.next(slot);

        if (ability == null) {
            sendNoAbilities(slot);
            return;
        }

        this.bukkitPlayer.sendActionBar(Component.text(ability.getDisplayName(), NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    }

    /** Left-click: use the ability currently selected on this item. Says so if the item has none. */
    public boolean activateCurrent(AbilitySlot slot) {
        Ability ability = loadout.current(slot);

        if (ability == null) {
            sendNoAbilities(slot);
            return false;
        }

        return ability.activate();
    }

    private void sendNoAbilities(AbilitySlot slot) {
        this.bukkitPlayer.sendActionBar(Component.text("No " + slot.label() + " abilities loaded", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
    }

    public void nextAttackAbility() {
        nextAbility(AbilitySlot.ATTACK);
    }

    public void nextMobilityAbility() {
        nextAbility(AbilitySlot.MOBILITY);
    }

    public void nextBuffAbility() {
        nextAbility(AbilitySlot.BUFF);
    }

    /** May be null if the item has no abilities. */
    public Ability getCurrentAttackAbility() {
        return loadout.current(AbilitySlot.ATTACK);
    }

    /** May be null if the item has no abilities. */
    public Ability getCurrentMobilityAbility() {
        return loadout.current(AbilitySlot.MOBILITY);
    }

    /** May be null if the item has no abilities. */
    public Ability getCurrentBuffAbility() {
        return loadout.current(AbilitySlot.BUFF);
    }


    public AttackItem getAttackItem() {
        return this.attackItem;
    }

    public BuffItem getBuffItem() {
        return this.buffItem;
    }

    public ElementalItem getElementalItem() {
        return this.elementalItem;
    }

    public ShikigamiItem getShikigamiItem() {
        return this.shikigamiItem;
    }

    public ArrayList<Ability> getShikigamiAbilities() {
        return loadout.list(AbilitySlot.SHIKIGAMI);
    }

    public MobilityItem getMobilityItem() {
        return this.mobilityItem;
    }
}
