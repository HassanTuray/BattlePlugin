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
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.ability.Activation;

/**
 * Every number comes from abilities.yml (Shortcut section). The cooldown starts when the shortcut is used,
 * not when the marker is placed.
 */
@RequiresSettings({ "cooldown", "duration-ticks", "radius", "effect-ticks", "blindness-amplifier", "night-vision-amplifier" })
public class Shortcut extends Ability {
    private boolean locationSet = false;
    private Location shortcutLocation = null;
    private ShortcutRepeater repeater;

    public Shortcut(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Shortcut.");
    }

    @Override
    protected Activation onActivate() {
        if (locationSet) {
            return useShortCut();
        } else {
            return setShortcut();
        }
    }

    private Activation setShortcut() {
        shortcutLocation = owner.getPlayer().getLocation();
        this.owner.getPlayer().sendActionBar(Component.text("Shortcut set", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        repeater = new ShortcutRepeater(settingInt("duration-ticks"), shortcutLocation, this);
        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        abilityScheduler.schedule(repeater);

        locationSet = true;
        return Activation.USED_NO_COOLDOWN;
    }

    private Activation useShortCut() {
        Player player = owner.getPlayer();
        Location location = player.getLocation();

        shortcutLocation.setPitch(location.getPitch());
        shortcutLocation.setYaw(location.getYaw());

        player.teleport(shortcutLocation);

        int effectTicks = settingInt("effect-ticks");
        PotionEffect shortcutBlindess = new PotionEffect(PotionEffectType.BLINDNESS, effectTicks, settingInt("blindness-amplifier"));
        PotionEffect shortcutNightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, effectTicks, settingInt("night-vision-amplifier"));

        List<Entity> targets = AOE.getTargets(owner, shortcutLocation, setting("radius"));
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(shortcutBlindess);
                ((LivingEntity) entity).addPotionEffect(shortcutNightVision);
            }
        }

        repeater.cancel();
        resetShortcut();
        return Activation.USED;
    }



    public void resetShortcut() {
        locationSet = false;
        shortcutLocation = null;
    }
}
