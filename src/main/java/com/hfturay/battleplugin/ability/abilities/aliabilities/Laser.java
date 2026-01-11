package com.hfturay.battleplugin.ability.abilities.aliabilities;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.HitScanAbility;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Laser extends HitScanAbility {
    private int duration = 140;

    public Laser(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Laser");
        this.setCoolDown(15);

        String abilityNameId = this.getDisplayName().replace(" ", "_");
        String id = owner.getPlayer().getName() + ":" + abilityNameId;
        this.setId(id);

        this.range = 9;
        this.damage = 1;
    }

    @Override
    public boolean activate() {
        CooldownLogger cooldownLogger = BattlePlugin.getInstance().getCoolDownLogger();

        if (cooldownLogger.isOnCooldown(owner, this)) {
            long remainingCooldown = cooldownLogger.getRemainingCooldown(owner, this);
            this.owner.getPlayer().sendActionBar(Component.text("Cooldown: " + remainingCooldown + " seconds", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

            return false;
        }

        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        LaserRepeater laserDamage = new LaserRepeater(duration, owner, damage, range);
        abilityLogger.schedule(laserDamage);

        cooldownLogger.startCooldown(owner, this);
        return true;
    }
    
    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }
    
}
