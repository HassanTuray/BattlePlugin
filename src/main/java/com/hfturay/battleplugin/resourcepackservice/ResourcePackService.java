package com.hfturay.battleplugin.resourcepackservice;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

/**
 * Sends the resource pack (the textures for BetterModel shikigami) to players when they join.
 *
 * The pack is a zip somewhere with a direct download link (config.yml, resource-pack.url). When the plugin starts or
 * is reloaded it downloads that link once, works out the pack's SHA-1 itself, and then sends the link and hash to
 * each player who joins. So after replacing the zip, a "/kits reload" is all it takes; no hash to copy by hand.
 * Players who are already online get the new pack on their next join (or straight away when it first becomes ready).
 *
 * If the link is empty or cannot be downloaded, no pack is sent and the console says why.
 */
public final class ResourcePackService implements Listener {
    /** Larger than any pack we expect, so a wrong link to some huge file cannot fill the server's memory. */
    private static final long MAX_BYTES = 200L * 1024 * 1024;

    private record Pack(String url, byte[] sha1, UUID id) { }

    private final Plugin plugin;
    private final Logger logger;
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private volatile Pack pack;
    private volatile boolean required = true;
    private volatile Component prompt = Component.empty();
    private int generation = 0;     // so a slow download of an old link cannot replace a newer one

    public ResourcePackService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** Reads the settings and (re)downloads the pack in the background. Call from the main thread. */
    public void reload(ConfigurationSection config) {
        String url = config.getString("resource-pack.url", "").trim();
        required = config.getBoolean("resource-pack.required", true);
        String promptText = config.getString("resource-pack.prompt", "");
        prompt = promptText == null || promptText.isBlank() ? Component.empty() : Component.text(promptText);

        int mine = ++generation;
        pack = null;
        if (url.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                byte[] sha1 = download(url);
                Pack ready = new Pack(url, sha1, UUID.nameUUIDFromBytes(url.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (mine != generation) {
                        return;     // reloaded again while this was downloading
                    }
                    pack = ready;
                    logger.info("Resource pack ready (" + url + "). It is sent to players when they join.");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        send(player);
                    }
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not download the resource pack from " + url
                    + ". No pack will be sent until this is fixed and /kits reload is run.", e);
            }
        });
    }

    /** Downloads the link and returns the SHA-1 of what it served. Runs off the main thread. */
    private byte[] download(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("the link answered HTTP " + response.statusCode()
                + " (it must be a direct download of the zip)");
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        long total = 0;
        try (InputStream in = response.body()) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IllegalStateException("the file is larger than " + (MAX_BYTES / 1024 / 1024) + " MB");
                }
                sha1.update(buffer, 0, read);
            }
        }
        if (total == 0) {
            throw new IllegalStateException("the link served an empty file");
        }
        return sha1.digest();
    }

    private void send(Player player) {
        Pack current = pack;
        if (current == null || !player.isOnline()) {
            return;
        }
        try {
            player.setResourcePack(current.id(), current.url(), current.sha1(), prompt, required);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not send the resource pack to " + player.getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        send(event.getPlayer());
    }
}
