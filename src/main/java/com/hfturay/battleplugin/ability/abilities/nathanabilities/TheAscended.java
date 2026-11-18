package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

/**
 * Only usable at low health. Fully heals the caster, adds `damage-bonus` to their Grand Slam, Three-Hits and Orb
 * Throw, and makes them fast and bouncy for `duration-ticks`; when it ends they are left at `end-health`.
 * Every number comes from abilities.yml (TheAscended section).
 */
@RequiresSettings({ "cooldown", "duration-ticks", "damage-bonus", "speed-amplifier", "jump-amplifier", "walk-speed",
    "health-threshold", "end-health" })
public class TheAscended extends Ability {

    public TheAscended(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("The Ascended");
    }

    @Override
    protected String blockedReason() {
        return owner.getPlayer().getHealth() > setting("health-threshold") ? "You cannot ascend yet" : null;
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();
        int duration = settingInt("duration-ticks");
        double buff = setting("damage-bonus");

        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        // Only the abilities this player actually has get the bonus. Any of them can be missing.
        GrandSlam grandSlam = owner.getAbility(GrandSlam.class);
        if (grandSlam != null) {
            grandSlam.addDamageBonus(buff);
        }

        ThreeHits threeHits = owner.getAbility(ThreeHits.class);
        if (threeHits != null) {
            threeHits.addDamageBonus(buff);
        }

        OrbThrow orbThrow = owner.getAbility(OrbThrow.class);
        if (orbThrow != null) {
            orbThrow.addDamageBonus(buff);
        }

        PotionEffect ascendedSpeed = new PotionEffect(PotionEffectType.SPEED, duration, settingInt("speed-amplifier"));
        PotionEffect ascendedJump = new PotionEffect(PotionEffectType.JUMP_BOOST, duration, settingInt("jump-amplifier"));

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        TheAscendedRepeater reverseBuffs = new TheAscendedRepeater(duration, owner, buff, setting("end-health"), this,
            grandSlam, threeHits, orbThrow);
        abilityScheduler.schedule(reverseBuffs);

        AttributeInstance movement = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return Activation.FAILED;
        }

        movement.setBaseValue(setting("walk-speed"));

        player.addPotionEffect(ascendedJump);
        player.addPotionEffect(ascendedSpeed);

        playParticleEffect(duration);

        return Activation.USED;
    }

    private void playParticleEffect(int duration) {
        BattlePlugin.getInstance().getParticleService().follow(owner, NathanParticles.ASCENDED_AURA, owner.getPlayer(), duration);
    }
}
