package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.Keys;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.shikigamiservice.Shikigami;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class AnthraxZombie extends Shikigami {
    // No numbers here: they come from abilities.yml through the AnthraxPackage that spawned it.
    private final double maxHealth;
    private final int resistanceAmplifier;
    private int ticksRemaining;

    public AnthraxZombie(Mob shikigami, BattlePlayer owner, double maxHealth, int lifetimeTicks, int resistanceAmplifier) {
        super(shikigami, owner);

        this.maxHealth = maxHealth;
        this.ticksRemaining = lifetimeTicks;
        this.resistanceAmplifier = resistanceAmplifier;

        this.setDisplayName("Anthrax Zombie");

        configureShikigami(shikigami, owner);
    }

    @Override
    public boolean tick() {
        ticksRemaining--;
        return ticksRemaining <= 0;
    }

    @Override
	public void configureShikigami(Mob shikigami, BattlePlayer owner) {
        shikigami.customName(Component.text(this.getDisplayName(), NamedTextColor.BLACK).decorate(TextDecoration.BOLD));
        shikigami.setCustomNameVisible(true);

        AttributeInstance maxHealthAttr = shikigami.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        maxHealthAttr.setBaseValue(maxHealth);
        shikigami.setHealth(maxHealth);

        AttributeInstance burnTime = shikigami.getAttribute(Attribute.GENERIC_BURNING_TIME);
        burnTime.setBaseValue(0);

        shikigami.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, resistanceAmplifier));

        UUID uuid = owner.getPlayer().getUniqueId();
        PersistentDataContainer shikigamiPDC = shikigami.getPersistentDataContainer();
        shikigamiPDC.set(Keys.shikigamiOwner, PersistentDataType.STRING, uuid.toString());
	}
}
