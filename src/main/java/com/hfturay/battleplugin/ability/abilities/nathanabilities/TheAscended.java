package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.AnimatedBallEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class TheAscended extends Ability {
    private int duration = 260;
    private int buff = 3;

    public TheAscended(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("The Ascended");
        this.setCoolDown(30);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
    }

    @Override
    public boolean activate() {
       if (owner.getPlayer().getHealth() > 8) {
            this.owner.getPlayer().sendActionBar(Component.text("You cannot ascend yet", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            return false;
        }

        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        Player player = owner.getPlayer();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        GrandSlam grandSlam = owner.getAbility(GrandSlam.class);
        grandSlam.damage = grandSlam.damage + buff;

        ThreeHits threeHits = owner.getAbility(ThreeHits.class);
        threeHits.setDamage(threeHits.getDamage() + buff); 

        OrbThrow orbThrow = owner.getAbility(OrbThrow.class);
        orbThrow.orbThrowCallback.damage = orbThrow.orbThrowCallback.damage + buff;
        
        PotionEffect ascendedSpeed = new PotionEffect(PotionEffectType.SPEED, duration, 7);
        PotionEffect ascendedJump = new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 3);

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        TheAscendedRepeater reverseBuffs = new TheAscendedRepeater(duration, owner, buff);
        abilityLogger.schedule(reverseBuffs);

        AttributeInstance movement = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return false;
        } 

        movement.setBaseValue(0.18); 

        player.addPotionEffect(ascendedJump);
        player.addPotionEffect(ascendedSpeed);

        playParticleEffect();

        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManager = BattlePlugin.getInstance().getEffectManager();
        AnimatedBallEffect effect = new AnimatedBallEffect(effectManager);

        effect.setEntity(owner.getPlayer());
        
        effect.particle = Particle.DUST;
        effect.colorList = new ArrayList<Color>(Arrays.asList(Color.BLACK, Color.RED, Color.PURPLE));
        effect.particles = 150;
        effect.particlesPerIteration = 10;
        effect.size = 1f;

        effect.xFactor = 1f;
        effect.yFactor = 2f;
        effect.zFactor = 1f;

        effect.period = 1;
        effect.iterations = 260;

        effect.start();
    }
}

