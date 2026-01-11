package com.hfturay.battleplugin.cooldownservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hfturay.battleplugin.battleplayer.BattlePlayer;

public class SilenceLogger {
    private Map<UUID, Long> silences = new HashMap<>();

    public void startSilence(BattlePlayer player, int duration) {
        UUID uuid = player.getPlayer().getUniqueId();
        Long unsilenceTime = System.currentTimeMillis() + (duration * 1000L);

        silences.put(uuid, unsilenceTime);
    }

    public boolean isSilenced(BattlePlayer player) {
        UUID uuid = player.getPlayer().getUniqueId();

        if (!silences.containsKey(uuid)) {
            silences.put(uuid, 0L);
        }

        return System.currentTimeMillis() < silences.get(uuid);
    }

    public long getRemainingSilence(BattlePlayer player) {
        UUID uuid = player.getPlayer().getUniqueId();

        if (!silences.containsKey(uuid)) {
            silences.put(uuid, 0L);
        }

        long remainingMillis = silences.get(uuid) - System.currentTimeMillis();

        if (remainingMillis > 0) {
            return remainingMillis / 1000L;
        } else {
            return 0;
        }
    }
}
