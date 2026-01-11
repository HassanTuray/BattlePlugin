package com.hfturay.battleplugin.ability.abilities.genericabilities;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Adrenaline extends Ability{
    private int duration = 2400;
    private int amplifier = 3;


    public Adrenaline(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Adrenaline");
        this.setCoolDown(30);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
        
        BattlePlugin.getInstance().getLogger().info("Adrenaline was constructed");
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        PotionEffect potionEffect = new PotionEffect(
            PotionEffectType.SPEED,
            duration,
            amplifier
        );

        PotionEffect healingEffect = new PotionEffect(
            PotionEffectType.REGENERATION,
            80,
            amplifier
        );

        owner.getPlayer().addPotionEffect(potionEffect);
        owner.getPlayer().addPotionEffect(healingEffect);

        cooldownLogger.startCooldown(owner, this);

        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
