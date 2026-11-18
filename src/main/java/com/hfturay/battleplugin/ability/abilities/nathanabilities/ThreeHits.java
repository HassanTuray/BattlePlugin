package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.MeleeAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.abilityschedulerservice.ComboWindow;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.HitScan;

/**
 * Three melee hits in a row: a plain hit, a hit that lifts the target (`lift`), and a hit that slams them down
 * (`slam-force`, a downward speed). Each must land within `combo-ticks` of the last or the combo ends and the cooldown starts.
 * Every number comes from abilities.yml (ThreeHits section). The damage includes any buff from The Ascended.
 */
@RequiresSettings({ "cooldown", "range", "damage", "lift", "slam-force", "combo-ticks" })
public class ThreeHits extends MeleeAbility {
    private final ComboWindow combo;

    public ThreeHits(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Three-Hits");

        // The cooldown starts when the combo window runs out.
        this.combo = new ComboWindow(this, () -> settingInt("combo-ticks"), this::startCooldown,
            () -> BattlePlugin.getInstance().getAbilityScheduler());
    }

    private double hitDamage() {
        return setting("damage") + getDamageBonus();
    }

    @Override
    protected Activation onActivate() {
        int attack = combo.stage();
        if (attack == 0) {
            firstSwing();
        } else if (attack == 1) {
            secondSwing();
        } else {
            thirdSwing();
        }

        return Activation.USED_NO_COOLDOWN;
    }

    public void firstSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, setting("range"), collisionMode);

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, hitDamage());
        }

        firstSwingEffect(entity);
        combo.hit(1);
    }

    public void secondSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, setting("range"), collisionMode);

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, hitDamage());
        }

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        ThreeHitsKnockbackRepeater knockbackRepeater = new ThreeHitsKnockbackRepeater(1, entity, setting("lift"), this);
        abilityScheduler.schedule(knockbackRepeater);

        secondSwingEffect(entity);
        combo.hit(2);
    }

    public void thirdSwing() {
        FluidCollisionMode collisionMode = FluidCollisionMode.NEVER;
        Entity entity = HitScan.HitScanEntities(owner, setting("range"), collisionMode);

        if (entity == null) {
            return;
        }

        if (entity instanceof Damageable) {
            BattlePlugin.getInstance().getDamageService().damage(owner, entity, hitDamage());
        }

        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        ThreeHitsKnockbackRepeater knockbackRepeater = new ThreeHitsKnockbackRepeater(1, entity, -setting("slam-force"), this);
        abilityScheduler.schedule(knockbackRepeater);

        thirdSwingEffect(entity);
        combo.hit(0);
    }

    @Override
    public void playParticleEffect() {

    }

    private void firstSwingEffect(Entity target) {
        BattlePlugin.getInstance().getParticleService()
            .burst(owner, NathanParticles.THREE_HITS_FIRST, NathanParticles.midpoint(owner, target));
    }

    private void secondSwingEffect(Entity target) {
        BattlePlugin.getInstance().getParticleService()
            .burst(owner, NathanParticles.THREE_HITS_SECOND, NathanParticles.midpoint(owner, target));
    }

    private void thirdSwingEffect(Entity target) {
        BattlePlugin.getInstance().getParticleService()
            .burst(owner, NathanParticles.THREE_HITS_THIRD, NathanParticles.midpoint(owner, target));
    }
}
