package com.hfturay.battleplugin.ability.abilities.damonabilities;

import org.bukkit.Particle;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class SwitchOnALazer extends Ability{

    public SwitchOnALazer(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Switch on a lazer");
        this.setCoolDown(30);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        Ability ability = owner.getAttackAbilities().get(2);
        
        if (ability instanceof Lazer) {
            ((Lazer)ability).setDamage(((Lazer)ability).getDamage() * 2);
        }
        
        playParticleEffect();

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        abilityLogger.schedule(new LazerBuffRepeater(owner, 200));
        
        cooldownLogger.startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        EffectManager effectManger = BattlePlugin.getInstance().getEffectManager();

        SphereEffect sparks = new SphereEffect(effectManger);

        sparks.particles = 1;
        sparks.particle = Particle.FIREWORK;

        sparks.yOffset = 1;
        sparks.radiusIncrease = 0;
        sparks.setEntity(owner.getPlayer());

        sparks.radius = 1;


        sparks.period = 1;
        sparks.iterations = 200;

        sparks.start();
    }
    
}
