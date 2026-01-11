package com.hfturay.battleplugin.battleplayer;

import org.bukkit.entity.Player;

import java.util.ArrayList;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.AbilityFactory;
import com.hfturay.battleplugin.items.AttackItem;
import com.hfturay.battleplugin.items.BuffItem;
import com.hfturay.battleplugin.items.MobilityItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BattlePlayer {
    private long timestamp;
    private Player bukkitPlayer;

    private AttackItem attackItem;
    private MobilityItem mobilityItem;
    private BuffItem buffItem;

    private ArrayList<Ability> attackAbilities;
    private ArrayList<Ability> mobilityAbilities;
    private ArrayList<Ability> buffAbilities;

    private int numAttacks = 4;
    private int numMobility = 2;
    private int numBuff = 2;

    private int currAttackIndex = 0;
    private int currMobilityIndex = 0;
    private int currBuffIndex = 0;
    

    public BattlePlayer(Player player) {
        this.bukkitPlayer = player;

        this.attackAbilities = new ArrayList<>();
        this.mobilityAbilities = new ArrayList<>();
        this.buffAbilities = new ArrayList<>();

        this.attackItem = new AttackItem(this);
        this.mobilityItem = new MobilityItem(this);
        this.buffItem = new BuffItem(this);

        BattlePlugin.getInstance().getAbilityFactory().assignAbilities(this);

        BattlePlugin.getInstance().getLogger().info("Size: " + attackAbilities.size());
    }

    // Can just return the player whenever we want members from this
    public Player getPlayer() {
        return this.bukkitPlayer;
    }

    public ArrayList<Ability> getAttackAbilities() {
        return this.attackAbilities;
    }

    public ArrayList<Ability> getMobilityAbilities() {
        return this.mobilityAbilities;
    }

    public ArrayList<Ability> getBuffAbilities() {
        return this.buffAbilities;
    }

    public <T extends Ability> T getAbility(Class<T> abilityClass) {

    for (Ability ability : attackAbilities) {
        if (abilityClass.isInstance(ability)) {
            return abilityClass.cast(ability);
        }
    }

    for (Ability ability : mobilityAbilities) {
        if (abilityClass.isInstance(ability)) {
            return abilityClass.cast(ability);
        }
    }

    for (Ability ability : buffAbilities) {
        if (abilityClass.isInstance(ability)) {
            return abilityClass.cast(ability);
        }
    }

    return null;
}


    public void nextAttackAbility() {
        currAttackIndex += 1;
        if (currAttackIndex >= numAttacks ) {
            currAttackIndex = 0;
        }

        this.bukkitPlayer.sendActionBar(Component.text(this.attackAbilities.get(currAttackIndex).getDisplayName(), NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    } 

    public void nextMobilityAbility() {
        currMobilityIndex += 1;
        if (currMobilityIndex >= numMobility) {
            currMobilityIndex = 0;
        }

        this.bukkitPlayer.sendActionBar(Component.text(this.mobilityAbilities.get(currMobilityIndex).getDisplayName(), NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    }

    public void nextBuffAbility() {
        currBuffIndex += 1;
        if (currBuffIndex >= numBuff) {
            currBuffIndex = 0;
        }

        this.bukkitPlayer.sendActionBar(Component.text(this.buffAbilities.get(currBuffIndex).getDisplayName(), NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    }

    public Ability getCurrentAttackAbility() {
        return this.attackAbilities.get(currAttackIndex);
    }

    public Ability getCurrentMobilityAbility() {
        return this.mobilityAbilities.get(currMobilityIndex);
    }

    public Ability getCurrentBuffAbility() {
        return this.buffAbilities.get(currBuffIndex);
    }


    public AttackItem getAttackItem() {
        return this.attackItem;
    }

    public BuffItem getBuffItem() {
        return this.buffItem;
    }

    public MobilityItem getMobilityItem() {
        return this.mobilityItem;
    }
}

