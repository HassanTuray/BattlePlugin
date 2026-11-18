package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.entity.Zombie;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiService;

public class AnthraxPackageCallBack implements ProjectileHitCallback {
    private final AnthraxPackage ability;
    private final AnthraxPackage.Cloud cloud;

    public AnthraxPackageCallBack(AnthraxPackage ability, AnthraxPackage.Cloud cloud) {
        this.ability = ability;
        this.cloud = cloud;
    }

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location location = result.getHitLocation().clone();
        AnthraxPackageRepeater repeater = new AnthraxPackageRepeater(cloud, owner, location, ability);
        AbilityScheduler abilityScheduler = BattlePlugin.getInstance().getAbilityScheduler();
        ShikigamiService shikigamiService = BattlePlugin.getInstance().getShikigamiService();

        for (int i = 0; i < cloud.zombieCount(); i++) {
            Zombie zombie = location.getWorld().spawn(location.clone().add(0, 1, 0), Zombie.class);
            shikigamiService.add(new AnthraxZombie(zombie, owner, cloud.zombieHealth(), cloud.zombieLifetimeTicks(), cloud.zombieResistance()));
        }

        abilityScheduler.schedule(repeater);
    }
}
