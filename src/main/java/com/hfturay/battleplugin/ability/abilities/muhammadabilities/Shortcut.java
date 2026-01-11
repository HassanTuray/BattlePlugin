package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.targetingservice.AOE;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Shortcut extends Ability {
    private boolean locationSet = false;
    private Location shortcutLocation = null;
    private ShortcutRepeater repeater;

    public Shortcut(BattlePlayer owner) {
        super(owner);
        
        this.setDisplayName("Shortcut.");
        this.setCoolDown(10);

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

        if (locationSet) {
            return useShortCut();
        } else {
            return setShortcut();
        }
    }

    private boolean setShortcut() {
        shortcutLocation = owner.getPlayer().getLocation();
        this.owner.getPlayer().sendActionBar(Component.text("Shortcut set", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        repeater = new ShortcutRepeater(200, shortcutLocation, this);
        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();
        abilityLogger.schedule(repeater);

        locationSet = true;
        return false;
    }

    private boolean useShortCut() {
        Player player = owner.getPlayer();
        Location location = player.getLocation();

        shortcutLocation.setPitch(location.getPitch());
        shortcutLocation.setYaw(location.getYaw());

        player.teleport(shortcutLocation);

        PotionEffect shortcutBlindess = new PotionEffect(PotionEffectType.BLINDNESS, 40, 2);
        PotionEffect shortcutNightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 2);

        List<Entity> targets = AOE.getTargets(owner, shortcutLocation, 3);
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(shortcutBlindess);
                ((LivingEntity) entity).addPotionEffect(shortcutNightVision);
            }
        }

        repeater.cancel();
        resetShortcut();
        BattlePlugin.getInstance().getCoolDownLogger().startCooldown(owner, this);
        return true;
    }

    @Override
    public void playParticleEffect() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playParticleEffect'");
    }


    public void resetShortcut() {
        locationSet = false;
        shortcutLocation = null;
    }
}
