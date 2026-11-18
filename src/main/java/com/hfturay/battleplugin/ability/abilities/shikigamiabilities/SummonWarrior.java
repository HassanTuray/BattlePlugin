package com.hfturay.battleplugin.ability.abilities.shikigamiabilities;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.ability.abilities.fireabilities.MajesticDestroyerFlame;
import com.hfturay.battleplugin.shikigamiservice.BrainShikigami;
import com.hfturay.battleplugin.shikigamiservice.DespawnRules;
import com.hfturay.battleplugin.shikigamiservice.EndReactions;
import com.hfturay.battleplugin.shikigamiservice.FollowAndChase;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiSpec;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiTrigger;
import com.hfturay.battleplugin.shikigamiservice.Targeting;

/**
 * Summons the Warrior: an iron-armoured fighter that follows its owner and attacks the nearest enemy.
 * Ends when its lifetime runs out, it goes idle, it dies or its owner dies. Nothing happens to the owner when it ends.
 * Every number comes from abilities.yml (SummonWarrior section), read when it is summoned.
 *
 * The recipe lives in spec(): swap the targeting, rules or end reaction there to make a different shikigami.
 */
@RequiresSettings({ "cooldown", "health", "damage", "attack-range", "attack-interval-ticks", "target-range",
    "follow-distance", "leash-distance", "speed", "lifetime-ticks", "idle-ticks", "max-active", "hit-memory-ticks", "flame-interval-ticks", "attack-hit-delay-ticks" })
public class SummonWarrior extends Ability {
    /** The BetterModel model: plugins/BetterModel/models/demon_knight.bbmodel (BetterModel installs it itself). */
    private static final String MODEL = "demon_knight";

    public SummonWarrior(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Warrior");
    }

    @Override
    protected String blockedReason() {
        return BattlePlugin.getInstance().getShikigamiService()
            .summonBlockedReason(owner, "Warrior", settingInt("max-active"));
    }

    @Override
    protected Activation onActivate() {
        Player player = owner.getPlayer();

        // A couple of blocks in front of the owner, on the flat.
        Vector ahead = player.getLocation().getDirection().setY(0);
        if (ahead.lengthSquared() < 1.0e-6) {
            ahead = new Vector(0, 0, 1);
        }
        Location where = player.getLocation().add(ahead.normalize().multiply(2));
        where.setDirection(player.getLocation().getDirection().multiply(-1));   // facing the owner at first

        Zombie zombie = where.getWorld().spawn(where, Zombie.class);
        BrainShikigami warrior = new BrainShikigami(zombie, owner, spec());
        BattlePlugin.getInstance().getShikigamiService().add(warrior);
        return Activation.USED;
    }

    /** Aims at the current target from the warrior's own body and fires, as a player would. */
    private void castFlame(BrainShikigami self) {
        Mob mob = self.mob();
        LivingEntity target = self.target();
        if (target == null) {
            return;
        }

        Vector aim = target.getBoundingBox().getCenter().subtract(mob.getEyeLocation().toVector());
        if (aim.lengthSquared() < 1.0E-6) {
            return;
        }

        mob.lookAt(target);
        self.body().play("flame");
        new MajesticDestroyerFlame(owner).cast(mob.getLocation().add(0, 1.0, 0), aim, mob.getLocation().getYaw());
    }

    private ShikigamiSpec spec() {
        return new ShikigamiSpec(
            owner.getPlayer().getName() + "'s Warrior",
            setting("health"),
            setting("damage"),
            setting("attack-range"),
            Math.max(1, settingInt("attack-interval-ticks")),
            setting("target-range"),
            // Whatever the owner hit most recently (any mob or player), otherwise the nearest enemy.
            Targeting.ownersLastTarget(settingInt("hit-memory-ticks")).orElse(Targeting.nearest()),
            new FollowAndChase(setting("follow-distance"), setting("leash-distance"), setting("speed")),
            List.of(
                DespawnRules.timeout(settingInt("lifetime-ticks")),
                DespawnRules.inactivity(settingInt("idle-ticks")),
                DespawnRules.ownerDied(),
                DespawnRules.ownerOffline()),
            EndReactions.none(),
            List.of(
                // Casts Majestic Destroyer Flame, with the owner's own numbers from abilities.yml, whenever its target
                // is within the blast's range, then waits flame-interval-ticks.
                new ShikigamiTrigger(
                    ShikigamiTrigger.Conditions.targetWithin(tuning().number("MajesticDestroyerFlame", "range", 15)),
                    settingInt("flame-interval-ticks"),
                    this::castFlame)),
            mob -> {
                if (!(mob instanceof Zombie zombie)) {
                    return;
                }
                zombie.setBaby(false);
                zombie.setShouldBurnInDay(false);

                EntityEquipment gear = zombie.getEquipment();
                gear.setHelmet(new ItemStack(Material.IRON_HELMET));
                gear.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                gear.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                gear.setBoots(new ItemStack(Material.IRON_BOOTS));
                gear.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                gear.setHelmetDropChance(0f);
                gear.setChestplateDropChance(0f);
                gear.setLeggingsDropChance(0f);
                gear.setBootsDropChance(0f);
                gear.setItemInMainHandDropChance(0f);
            },
            // Drawn as the demon knight model (BetterModel). Without that plugin it stays an armoured zombie.
            // The model's animations: idle, walk, damage and death play on their own; these are the ones we trigger.
            MODEL,
            Map.of(
                "attack", List.of("hammer_attack_1", "hammer_attack_2", "hammer_attack_3"),
                "flame", List.of("shield_attack_2")),
            settingInt("attack-hit-delay-ticks"));
    }
}
