package com.hfturay.battleplugin.projectileservice;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class VirtualProjectile {
    private BattlePlayer owner;
    
    private Location location;
    private Vector direction;
    private MotionFunction motionFunction;
    private Vector velocity;

    private int maxTicks;
    private int ticksLived = 0;

    private ProjectileHitCallback callback;
    private double hitbox;
    private ParticleEffectCallback effect;
    private int particleEffectPeriod = 1;

    private boolean stopOnHitPlayer = true;
    private boolean stopOnHitBlock = true;
    private boolean hitPlayers = true;
    private boolean hitBlocks = true;

    public VirtualProjectile (
        Location start, 
        Vector direction, 
        double speed, 
        int maxTicks, 
        BattlePlayer owner, 
        double hitbox, 
        ProjectileHitCallback callback,
        ParticleEffectCallback effect) 
    {
        this.location = start.clone();
        this.direction = direction.clone().normalize();
        this.maxTicks = maxTicks;
        this.owner = owner;
        this.motionFunction = (v, t) -> v;

        this.callback = callback;
        this.velocity = direction.clone().multiply(speed);
        this.effect = effect;
        this.hitbox = hitbox;
    }

    public VirtualProjectile (
        double speed, 
        int maxTicks, 
        BattlePlayer owner, 
        double hitbox, 
        ProjectileHitCallback callback,
        ParticleEffectCallback effect) 
    {   

        this.location = owner.getPlayer().getEyeLocation().clone();
        this.direction = location.getDirection();

        this.direction = direction.clone().normalize();
        this.maxTicks = maxTicks;
        this.owner = owner;
        this.motionFunction = (v, t) -> v;

        this.callback = callback;
        this.velocity = direction.clone().multiply(speed);
        this.effect = effect;
        this.hitbox = hitbox;
    }

    public boolean tick() {
        if (ticksLived % particleEffectPeriod == 0 || ticksLived == 0) {
            effect.playParticleEffect(this);
        }

        ticksLived++;
        
        velocity = motionFunction.apply(velocity, ticksLived);

        location.add(velocity);
        
        getHitEntity(hitbox);

        Entity hit = getHitEntity(this.hitbox);
        if (hit != null && !hit.equals(owner.getPlayer()) && hitPlayers) {
            callback.onHit(new ProjectileHitResult(hit, location, this), owner);

            if (stopOnHitPlayer) {
                this.cancel();
                return true;
            }
        }

        if (!location.getBlock().isPassable() && hitBlocks) {
            callback.onHit(new ProjectileHitResult(location.getBlock(), location, this), owner);
            if (stopOnHitBlock) {
                this.cancel();
                return true;
            } 
        }
        
        if (ticksLived >= maxTicks) {
            return true;
        }

        return false;
    }

    public double getHitbox() {
        return hitbox;
    }

    public void setHitbox(double hitbox) {
        this.hitbox = hitbox;
    }

    private Entity getHitEntity(double hitbox) {
        Predicate<Entity> playersAndMobsFilter =  entity -> (entity instanceof Player) || (entity instanceof Mob);

        for (Entity entity : location.getWorld().getNearbyEntities(location, hitbox, hitbox, hitbox)) {
            if (playersAndMobsFilter.test(entity)) {
                return entity;
            }
        }
        return null;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setParticleEffectPeriod(int period) {
        this.particleEffectPeriod = period;
    }

    public void setStopOnHitPlayer(boolean stop) {
        this.stopOnHitPlayer = stop;
    }

    public void setStopOnHitBlock(boolean stop) {
        this.stopOnHitBlock = stop;
    }

    public void setHitPlayers(boolean hitPlayers) {
        this.hitPlayers = hitPlayers;
    }

    public void setHitBlocks(boolean hitBlocks) {
        this.hitBlocks = hitBlocks;
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public void setMotionFunction(MotionFunction function) {
        this.motionFunction = function;
    }

    public ParticleEffectCallback getParticleEffectCallback() {
        return this.effect;
    }

    public ProjectileHitCallback getProjectileHitCallback() {
        return this.callback;
    }

    public Vector getDirection() {
        return direction.clone();
    }

    public int getRemainingTicks() {
        return this.maxTicks - this.ticksLived;
    }

    public BattlePlayer getOwner() {
        return this.owner;
    }

    public void cancel() {
        this.ticksLived = maxTicks;
    }
}
