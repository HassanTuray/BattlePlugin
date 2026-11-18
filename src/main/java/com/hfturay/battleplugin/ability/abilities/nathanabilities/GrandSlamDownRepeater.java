package com.hfturay.battleplugin.ability.abilities.nathanabilities;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.abilityschedulerservice.ScheduledAbilityTask;
import com.hfturay.battleplugin.combatservice.FallImmunityService;

import com.hfturay.battleplugin.stunservice.StunService;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.FlameEffect;

public class GrandSlamDownRepeater extends ScheduledAbilityTask {
    private Entity target;
    private double damage;

    public GrandSlamDownRepeater(int duration, Entity target, Double damage, Ability ability) {
        super(duration, ability);
        this.target = target;
        this.damage = damage;
    }

    @Override
    protected void onTick() {
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        if (!stunLogger.isStunned(target)) {
            cancel();    // the stun ended some other way, nothing left to do
            return;
        }

        if (!(target instanceof Damageable)) {
            finishNow(); // cannot be hurt: just release it
            return;
        }

        if (FallImmunityService.isGrounded(target) ||
            target.isInWater() ||
            target.isInLava() ||
            target.isInPowderedSnow()) {

            BattlePlugin.getInstance().getDamageService().damage(ability.getOwner(), target, damage);
            slamEffect(target);
            finishNow(); // releases the stun
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
        StunService stunLogger = BattlePlugin.getInstance().getStunService();
        if (stunLogger.isStunned(target)) {
            stunLogger.removeStun(target);
        }
    }

    private void slamEffect(Entity target) {
        BattlePlugin.getInstance().getParticleService()
            .burst(ability.getOwner(), NathanParticles.SLAM_LANDING, target.getLocation());
    }
}
