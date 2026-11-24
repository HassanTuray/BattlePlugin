package com.hfturay.battleplugin.cutsceneservice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.stunservice.Stun;

/**
 * Plays cutscenes: a camera flies along a path while the viewers watch, locked and protected.
 *
 * How it works: each scene spawns one invisible display entity (the camera), visible only to the viewers. Viewers are
 * put in spectator mode and spectate it, so they see through it. Every tick the camera is moved along the path; the
 * display entity smooths the movement on the client. (A scene can ask for a living camera instead, an invisible
 * marker armor stand, when it needs to give the camera effects; see Scene.Builder.livingCamera.) Spectator mode freezes the viewers, and on top of that they are
 * silenced and made immune for the whole scene. Mobs nearby are frozen and made immune too.
 *
 * Safety: before anyone is put in spectator mode their game mode, position and flight are written to cutscenes.yml.
 * They are put back when the scene ends, when they quit, when the plugin stops, and (if the server crashed
 * mid-scene) when they next join, so nobody is ever stuck in spectator mode.
 */
public final class CutsceneService implements Listener {
    /** The camera display entity smooths its movement over this many ticks. */
    private static final int TELEPORT_DURATION = 2;
    /** Immunity kept for this long after a scene, so nobody is hit the instant they are released. */
    private static final int GRACE_TICKS = 40;
    private static final int REATTACH_INTERVAL = 10;

    /** What a viewer was like before the scene. */
    private record Saved(GameMode mode, Location location, boolean allowFlight, boolean flying) { }

    private final class Session {
        final Scene scene;
        final Location anchor;
        final List<Player> viewers;
        final Runnable onFinish;
        Entity camera;
        BukkitTask task;
        int tick = 0;
        boolean ended = false;

        Session(Scene scene, Location anchor, List<Player> viewers, Runnable onFinish) {
            this.scene = scene;
            this.anchor = anchor;
            this.viewers = viewers;
            this.onFinish = onFinish;
        }
    }

    private final Plugin plugin;
    private final File journal;
    private final Logger logger;
    private final NamespacedKey cameraKey;

    private final Map<UUID, Session> byViewer = new HashMap<>();
    private final Set<Session> sessions = new HashSet<>();
    // Everyone who is in spectator mode because of a scene, and not yet put back. Mirrors cutscenes.yml.
    private final Map<UUID, Saved> saved = new LinkedHashMap<>();

    public CutsceneService(Plugin plugin, File journal) {
        this.plugin = plugin;
        this.journal = journal;
        this.logger = plugin.getLogger();
        this.cameraKey = new NamespacedKey(plugin, "cutscene-camera");
    }

    // ---- playing ----

    public boolean isWatching(UUID player) {
        return byViewer.containsKey(player);
    }

    public boolean isPlaying() {
        return !sessions.isEmpty();
    }

    /**
     * Plays the scene for these viewers around the anchor (its position and yaw place the camera path).
     * Viewers already in a cutscene are skipped. Returns false if nobody could be shown it.
     * onFinish (may be null) runs once the scene reaches its end, not when it is cancelled.
     */
    public boolean play(Scene scene, Location anchor, Collection<Player> viewers, Runnable onFinish) {
        World world = anchor.getWorld();
        if (world == null) {
            return false;
        }
        List<Player> watching = new ArrayList<>();
        for (Player player : viewers) {
            if (player.isOnline() && !player.isDead() && player.getWorld().equals(world) && !byViewer.containsKey(player.getUniqueId())) {
                watching.add(player);
            }
        }
        if (watching.isEmpty()) {
            return false;
        }

        Session session = new Session(scene, anchor.clone(), watching, onFinish);
        Location start = place(session, scene.path().pose(0));
        if (scene.livingCamera()) {
            // a marker armor stand: living (so it can carry effects the viewers see through), with no size of its own
            session.camera = world.spawn(start, ArmorStand.class, stand -> {
                stand.setMarker(true);
                stand.setInvisible(true);
                stand.setSilent(true);
                stand.setVisibleByDefault(false);
                stand.setPersistent(false);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.getPersistentDataContainer().set(cameraKey, PersistentDataType.BYTE, (byte) 1);
            });
        } else {
            session.camera = world.spawn(start, ItemDisplay.class, display -> {
                display.setVisibleByDefault(false);
                display.setPersistent(false);
                display.setGravity(false);
                display.setInvulnerable(true);
                display.setTeleportDuration(TELEPORT_DURATION);
                display.getPersistentDataContainer().set(cameraKey, PersistentDataType.BYTE, (byte) 1);
            });
        }

        int lock = scene.totalTicks() + 5;
        for (Player player : watching) {
            saved.put(player.getUniqueId(), new Saved(player.getGameMode(), player.getLocation().clone(),
                player.getAllowFlight(), player.isFlying()));
        }
        writeJournal();

        for (Player player : watching) {
            byViewer.put(player.getUniqueId(), session);
            BattlePlugin.getInstance().getImmunityService().grant(player, lock + GRACE_TICKS);
            BattlePlugin.getInstance().getSilenceService().silence(player.getUniqueId(), lock);
            player.setGameMode(GameMode.SPECTATOR);
            player.showEntity(plugin, session.camera);
            player.setSpectatorTarget(session.camera);
        }
        freezeMobs(session, lock);

        sessions.add(session);
        session.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(session), 1L, 1L);
        return true;
    }

    /** Stops every running scene at once and puts everyone back. */
    public void cancelAll() {
        for (Session session : new ArrayList<>(sessions)) {
            end(session, false);
        }
    }

    /** Stops the scene this player is watching (and so everyone else's in it). */
    public boolean cancel(UUID viewer) {
        Session session = byViewer.get(viewer);
        if (session == null) {
            return false;
        }
        end(session, false);
        return true;
    }

    private void tick(Session session) {
        if (session.ended) {
            return;
        }
        session.viewers.removeIf(player -> !player.isOnline() || !byViewer.containsKey(player.getUniqueId()));
        if (session.viewers.isEmpty() || !session.camera.isValid()) {
            end(session, false);
            return;
        }
        if (session.tick > session.scene.totalTicks()) {
            end(session, true);
            return;
        }

        session.camera.teleport(place(session, session.scene.path().pose(session.tick)));

        for (Scene.Cue cue : session.scene.cues()) {
            if (cue.tick() == session.tick) {
                try {
                    cue.action().accept(new Scene.Context(session.anchor, List.copyOf(session.viewers), session.tick,
                        session.camera.getLocation(), session.camera));
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "A cue in the cutscene '" + session.scene.name() + "' failed at tick " + session.tick, e);
                }
            }
        }

        if (session.tick % REATTACH_INTERVAL == 0) {
            for (Player player : session.viewers) {
                if (player.getGameMode() != GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                if (player.getSpectatorTarget() != session.camera) {
                    player.setSpectatorTarget(session.camera);
                }
            }
        }
        session.tick++;
    }

    private void end(Session session, boolean finished) {
        if (session.ended) {
            return;
        }
        session.ended = true;
        sessions.remove(session);
        if (session.task != null) {
            session.task.cancel();
        }
        for (Player player : new ArrayList<>(session.viewers)) {
            byViewer.remove(player.getUniqueId(), session);
            restore(player);
        }
        // Anyone who left mid-scene was already put back by the quit handler.
        if (session.camera != null && session.camera.isValid()) {
            session.camera.remove();
        }
        writeJournal();

        if (finished && session.onFinish != null) {
            try {
                session.onFinish.run();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "The follow-up after the cutscene '" + session.scene.name() + "' failed", e);
            }
        }
    }

    /** Turns anchor-relative camera numbers into a real location. */
    private static Location place(Session session, CameraPath.Pose pose) {
        Location anchor = session.anchor;
        double yaw = Math.toRadians(anchor.getYaw());
        // facing: (-sin yaw, cos yaw); the right-hand side of that: (-cos yaw, -sin yaw)
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        return new Location(anchor.getWorld(),
            anchor.getX() + rx * pose.right() + fx * pose.forward(),
            anchor.getY() + pose.up(),
            anchor.getZ() + rz * pose.right() + fz * pose.forward(),
            (float) (anchor.getYaw() + pose.yaw()),
            (float) pose.pitch());
    }

    /** Mobs near the scene cannot move or be hurt while it plays (shikigami stop fighting too). */
    private void freezeMobs(Session session, int ticks) {
        for (LivingEntity mob : session.anchor.getWorld().getNearbyLivingEntities(session.anchor, session.scene.lockRadius())) {
            if (mob instanceof Player || !mob.isValid()) {
                continue;
            }
            Stun stun = new Stun(mob, ticks);
            stun.lockX = true;
            stun.lockY = true;
            stun.lockZ = true;
            stun.grantImmunity = true;
            stun.showSparks = false;
            BattlePlugin.getInstance().getStunService().add(mob, stun);
        }
    }

    // ---- putting players back ----

    /** Puts one player back as they were before the scene. Safe to call for someone who was not saved. */
    private void restore(Player player) {
        Saved state = saved.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        player.setSpectatorTarget(null);
        player.setGameMode(state.mode());
        player.setAllowFlight(state.allowFlight());
        if (state.allowFlight()) {
            player.setFlying(state.flying());
        }
        if (state.location().getWorld() != null) {
            player.teleport(state.location());
        }
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        BattlePlugin.getInstance().getImmunityService().grant(player, GRACE_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Session session = byViewer.remove(player.getUniqueId());
        if (session != null) {
            session.viewers.remove(player);
        }
        restore(player);
        writeJournal();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // A crash mid-scene left this player in spectator mode. Put them back once they are fully in.
        if (saved.containsKey(player.getUniqueId()) && !byViewer.containsKey(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    restore(player);
                    writeJournal();
                }
            });
        }
    }

    /** Sneaking would normally let a spectator leave the camera. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        if (byViewer.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** Spectators can teleport to other players; not during a scene. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        if (byViewer.containsKey(event.getPlayer().getUniqueId())
            && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.setCancelled(true);
        }
    }

    // ---- start and stop of the plugin ----

    /** Call when the plugin starts: restores anyone a crash left in spectator mode and removes stray cameras. */
    public void recover() {
        loadJournal();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (saved.containsKey(player.getUniqueId())) {
                restore(player);
            }
        }
        writeJournal();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {        // display cameras and living (armor stand) cameras
                if (entity.getPersistentDataContainer().has(cameraKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    /** Call when the plugin stops: ends every scene and puts everyone back. */
    public void shutdown() {
        cancelAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            restore(player);
        }
        writeJournal();
    }

    // ---- the journal ----

    private void writeJournal() {
        if (saved.isEmpty()) {
            if (journal.exists() && !journal.delete()) {
                logger.warning("Could not delete " + journal.getName());
            }
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Saved> entry : saved.entrySet()) {
            Saved s = entry.getValue();
            ConfigurationSection section = yaml.createSection(entry.getKey().toString());
            section.set("mode", s.mode().name());
            section.set("allow-flight", s.allowFlight());
            section.set("flying", s.flying());
            if (s.location().getWorld() != null) {
                section.set("world", s.location().getWorld().getUID().toString());
                section.set("x", s.location().getX());
                section.set("y", s.location().getY());
                section.set("z", s.location().getZ());
                section.set("yaw", s.location().getYaw());
                section.set("pitch", s.location().getPitch());
            }
        }
        try {
            yaml.save(journal);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save " + journal.getName(), e);
        }
    }

    private void loadJournal() {
        if (!journal.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(journal);
        for (String key : yaml.getKeys(false)) {
            try {
                ConfigurationSection s = yaml.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                World world = s.isSet("world") ? Bukkit.getWorld(UUID.fromString(s.getString("world", ""))) : null;
                Location location = world == null ? new Location(null, 0, 0, 0) : new Location(world,
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
                GameMode mode = GameMode.valueOf(s.getString("mode", "SURVIVAL"));
                saved.put(UUID.fromString(key), new Saved(mode, location, s.getBoolean("allow-flight"), s.getBoolean("flying")));
            } catch (IllegalArgumentException e) {
                logger.warning(journal.getName() + ": skipped a bad entry '" + key + "'.");
            }
        }
    }
}
