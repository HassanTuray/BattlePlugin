package com.hfturay.battleplugin.projectileservice;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class ProjectileHitResult {
    private Entity hitEntity;
    private Block hitBlock;
    private Location hitLocation;
    private VirtualProjectile projectile;

    public ProjectileHitResult(Entity entity, Location location, VirtualProjectile projectile) {
        this.hitEntity = entity;
        this.hitBlock = null;
        this.hitLocation = location.clone();
        this.projectile = projectile;
    }

    public ProjectileHitResult(Block block, Location location, VirtualProjectile projectile) {
        this.hitEntity = null;
        this.hitBlock = block;
        this.hitLocation = location.clone();
        this.projectile = projectile;
    }

    public boolean hasHitEntity() {
        return hitEntity != null;
    }

    public boolean hasHitBlock() {
        return hitBlock != null;
    }

    public Entity getHitEntity() {
        return hitEntity;
    }

    public Block getHitBlock() {
        return hitBlock;
    }

    public Location getHitLocation() {
        return hitLocation;
    }

    public VirtualProjectile getVirtualProjectile() {
        return this.projectile;
    }
}
