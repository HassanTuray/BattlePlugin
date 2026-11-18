package com.hfturay.battleplugin.ability.abilities.earthabilities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * Keeps an Earth Dome alive: while it stands, the caster inside it has resistance and everyone inside has night vision.
 * Both are short effects renewed every few ticks, so they fade by themselves soon after someone steps out.
 * When it ends the blocks are put back (the world's own blocks return, see TempBlockService).
 *
 * Resistance is the server's own potion effect. Damage in this plugin is dealt as ordinary server damage, so it is
 * reduced exactly like any other hit: amplifier 0 is Resistance I (20% less).
 */
final class EarthDomeTask extends ScheduledAbilityTask {

    record Settings(double radius, int durationTicks, int resistanceAmplifier) { }

    private static final int REFRESH_TICKS = 5;
    private static final int EFFECT_TICKS = 30;     // longer than the refresh, so it never flickers

    private final BattlePlayer owner;
    private final Location center;
    private final Settings settings;
    private final List<TempBlocks> blocks;
    private final Set<UUID> nightVisionGiven = new HashSet<>();
    private boolean resistanceGiven = false;

    EarthDomeTask(BattlePlayer owner, Ability ability, Location center, Settings settings, List<TempBlocks> blocks) {
        super(settings.durationTicks(), ability);
        this.owner = owner;
        this.center = center.clone();
        this.settings = settings;
        this.blocks = blocks;
    }

    /** The dome is a placed thing: it stands for its full time even if the caster dies. */
    @Override
    public boolean cancelOnOwnerDeath() {
        return false;
    }

    @Override
    protected void onTick() {
        if (getTicksRemaining() % REFRESH_TICKS != 0) {
            return;
        }

        double limit = settings.radius() * settings.radius();
        Player caster = owner.getPlayer();

        for (Player player : center.getWorld().getPlayers()) {
            if (player.isDead() || player.getLocation().distanceSquared(center) > limit) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, EFFECT_TICKS, 0, false, false, false));
            nightVisionGiven.add(player.getUniqueId());

            if (player.equals(caster)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, EFFECT_TICKS, settings.resistanceAmplifier(), false, false, true));
                resistanceGiven = true;
            }
        }
    }

    @Override
    protected void onFinish() {
        release();
    }

    @Override
    protected void onCancel() {
        release();
    }

    private void release() {
        for (TempBlocks set : blocks) {
            set.remove();
        }
        blocks.clear();

        for (UUID id : nightVisionGiven) {
            Player player = org.bukkit.Bukkit.getPlayer(id);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }
        nightVisionGiven.clear();

        Player caster = owner.getPlayer();
        if (resistanceGiven && caster != null) {
            caster.removePotionEffect(PotionEffectType.RESISTANCE);
        }
    }
}
