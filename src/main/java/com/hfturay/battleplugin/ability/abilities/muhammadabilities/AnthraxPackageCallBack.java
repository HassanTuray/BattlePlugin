package com.hfturay.battleplugin.ability.abilities.muhammadabilities;

import org.bukkit.Location;
import org.bukkit.entity.Zombie;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.ProjectileHitCallback;
import com.hfturay.battleplugin.projectileservice.ProjectileHitResult;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiLogger;

public class AnthraxPackageCallBack implements ProjectileHitCallback {

    @Override
    public void onHit(ProjectileHitResult result, BattlePlayer owner) {
        Location location = result.getHitLocation().clone();
        AnthraxPackageRepeater repeater = new AnthraxPackageRepeater(200, owner, location, 10);
        AbilityLogger abilityLogger = BattlePlugin.getInstance().getAbilityLogger();

        Zombie zombie1 = location.getWorld().spawn(location.clone().add(0, 1, 0), Zombie.class);
        Zombie zombie2 = location.getWorld().spawn(location.clone().add(0, 1, 0), Zombie.class);

        AnthraxZombie anthraxZombie1 = new AnthraxZombie(zombie1, owner);
        AnthraxZombie anthraxZombie2 = new AnthraxZombie(zombie2, owner);

        ShikigamiLogger shikigamiLogger = BattlePlugin.getInstance().getShikigamiLogger();
        shikigamiLogger.add(anthraxZombie1);
        shikigamiLogger.add(anthraxZombie2);

        abilityLogger.schedule(repeater);
    }
}
