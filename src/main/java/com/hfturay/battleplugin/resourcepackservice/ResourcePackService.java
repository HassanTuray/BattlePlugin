package com.hfturay.battleplugin.resourcepackservice;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;

/**
 * Sends the resource packs (BetterModel textures, the red chaos bar) to players when they join.
 *
 * Each pack is a zip somewhere with a direct download link (config.yml, resource-pack.url and extra-urls). When the
 * plugin starts or is reloaded it downloads each link once, works out the SHA-1 itself, and then sends all the packs
 * to each player in ONE request. (Sending them one after another does not work: every send replaces the packs the
 * player already has, so only the last one would stay.) After replacing a zip, "/kits reload" is all it takes.
 *
 * If a link cannot be downloaded, that pack is left out and the console says why.
 */
public final class ResourcePackService implements Listener {
    /** Larger than any pack we expect, so a wrong link to some huge file cannot fill the server's memory. */
    private static final long MAX_BYTES = 200L * 1024 * 1024;

    private record Pack(String url, String sha1Hex, UUID id) { }

    private final Plugin plugin;
    private final Logger logger;
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private final List<Pack> packs = new CopyOnWriteArrayList<>();
    private volatile boolean required = true;
    private volatile Component prompt = Component.empty();
    private int generation = 0;     // so a slow download of an old link cannot replace a newer one

    public ResourcePackService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** Reads the settings and (re)downloads the packs in the background. Call from the main thread. */
    public void reload(ConfigurationSection config) {
        List<String> urls = new ArrayList<>();
        String main = config.getString("resource-pack.url", "").trim();
        if (!main.isEmpty()) {
            urls.add(main);
        }
        for (String extra : config.getStringList("resource-pack.extra-urls")) {
            if (extra != null && !extra.isBlank()) {
                urls.add(extra.trim());
            }
        }
        required = config.getBoolean("resource-pack.required", true);
        String promptText = config.getString("resource-pack.prompt", "");
        prompt = promptText == null || promptText.isBlank() ? Component.empty() : Component.text(promptText);

        int mine = ++generation;
        packs.clear();
        if (urls.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Pack> ready = new ArrayList<>();
            for (String url : urls) {
                try {
                    ready.add(new Pack(url, download(url),
                        UUID.nameUUIDFromBytes(url.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not download the resource pack from " + url
                        + ". It will not be sent until this is fixed and /kits reload is run.", e);
                }
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (mine != generation) {
                    return;     // reloaded again while this was downloading
                }
                packs.addAll(ready);
                logger.info("Resource packs ready (" + ready.size() + "). They are sent to players when they join.");
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    send(player);
                }
            });
        });
    }

    /** Downloads the link and returns the SHA-1 (hex) of what it served. Runs off the main thread. */
    private String download(String url) throws Exception {
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
        StringBuilder hex = new StringBuilder();
        for (byte b : sha1.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private void send(Player player) {
        if (packs.isEmpty() || !player.isOnline()) {
            return;
        }
        try {
            List<ResourcePackInfo> infos = new ArrayList<>();
            for (Pack pack : packs) {
                infos.add(ResourcePackInfo.resourcePackInfo()
                    .id(pack.id()).uri(URI.create(pack.url())).hash(pack.sha1Hex()).build());
            }
            player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(infos).prompt(prompt).required(required).replace(true).build());
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not send the resource packs to " + player.getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        send(event.getPlayer());
    }
}
