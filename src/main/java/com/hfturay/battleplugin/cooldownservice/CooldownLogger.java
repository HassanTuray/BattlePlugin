package com.hfturay.battleplugin.cooldownservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hfturay.battleplugin.ability.Ability;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class CooldownLogger {
    private Map<UUID, Map <String, Long>> cooldowns = new HashMap<>();

    public void startCooldown(BattlePlayer player, Ability ability) {
        UUID uuid = player.getPlayer().getUniqueId();
        String abilityId = ability.getId();

        long nextUseTime = System.currentTimeMillis() + (ability.getCooldown() * 1000L);

        Map<String, Long> abilityMap = cooldowns.get(uuid);

        if (abilityMap == null) {
            abilityMap = new HashMap<>();
            cooldowns.put(uuid, abilityMap);
        }
        
        abilityMap.put(abilityId, nextUseTime);
    }

    public boolean isOnCooldown(BattlePlayer player, Ability ability) {
        UUID uuid = player.getPlayer().getUniqueId();
        String abilityId = ability.getId();

        Map<String, Long> abilityMap = cooldowns.get(uuid);

        if (abilityMap == null) {
            return false;
        }

        Long nextValidTime = abilityMap.get(abilityId);
        if (nextValidTime == null) {
            return false;
        }

        return System.currentTimeMillis() < nextValidTime;
    }

    public long getRemainingCooldown(BattlePlayer player, Ability ability) {
        UUID uuid = player.getPlayer().getUniqueId();
        String abilityId = ability.getId();

        Map<String, Long> abilityMap = cooldowns.get(uuid);

        if (abilityMap == null) {
            return 0;
        }

        Long nextValidTime = abilityMap.get(abilityId);
        if (nextValidTime == null) {
            return 0;
        }

        long remainingMillis = nextValidTime - System.currentTimeMillis();

        if (remainingMillis > 0) {
            return remainingMillis / 1000L;
        } else {
            return 0;
        }
    }
}
